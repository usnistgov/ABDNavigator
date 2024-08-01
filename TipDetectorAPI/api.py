"""
Example input JSON:
{
    "scan_path": "path/to/scan.Z_mtrx", # The path to the scan image. Can be a .Z_mtrx file or a regular image file.
    "detector_options": {
        "scan_nm": 10, # DO NOT USE FOR MATRIX FILES UNLESS OVERRIDING. The scan size in nanometers.
        "contrast": 1.0, # The contrast of the image. Default is 1.0.
        "rotation": 0.0 # The rotation of the image. Default is 0.0.
    }
    "matrix_options": { # Required for matrix files.
        "direction": 0 # The direction of the scan to use.
            # {0: 'forward/up', 1: 'backward/up', 2: 'forward/down', 3: 'backward/down'}
        "plane_slopes": [0.0, 0.0] # The slopes of the scan plane to subtract in dz/dx and dz/dy. Optional.
}

Example success JSON:
{
    "sharp": 10, # The number of sharp tips detected.
    "dull": 5, # The number of dull tips detected.
    "total": 15 # The total number of tips detected.
    "roi_data": {
        "constants": {
            "nm_size": 2, # The size of the ROI in nanometers.
            "pixel_size": 50, # The size of the ROI in pixels.
        },
        "locations": [
            {
                "x": 10, # The top left x-coordinate of the ROI.
                "y": 20, # The top left y-coordinate of the ROI.
                "prediction": 0.8, # The prediction of the ROI.
            },
            ...
        ]
    }
}

Example error JSON:
{
    "error": "Error opening the matrix file: {Exception message}"
}
"""

import json
import os
import select
import socket
import sys
import time

# Disable OneDNN optimizations and CPU instructions messages
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"

import cv2
import numpy as np
from tensorflow.keras.models import load_model  # type: ignore

from detector_functions.main_functions import detect_tip
from detector_functions.matrix_helpers import get_nm_from_matrix, matrix_to_img_array

# Handle arguments
host = "localhost"
port = 5050
if len(sys.argv) > 1:
    if "--host" in sys.argv:
        host_index = sys.argv.index("--host")
        host = sys.argv[host_index + 1]
    if "--port" in sys.argv:
        port_index = sys.argv.index("--port")
        port = int(sys.argv[port_index + 1])

# Load config file
with open("config.json") as f:
    config = json.load(f)

# Load the model
model = load_model("model.h5")


def convert_to_serializable(obj):
    """Recursively converts numpy types to native Python types for serialization.

    Parameters:
        obj: Object to be converted.

    Returns:
        Object: Converted object with numpy types replaced by Python types.
    """
    if isinstance(obj, np.integer):
        return int(obj)
    if isinstance(obj, np.floating):
        return float(obj)
    if isinstance(obj, np.ndarray):
        return obj.tolist()
    if isinstance(obj, dict):
        return {k: convert_to_serializable(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [convert_to_serializable(v) for v in obj]
    return obj


def process_image(data: dict) -> dict:
    if "scan_path" not in data:
        raise ValueError("Scan path is required")
    if "detector_options" not in data:
        raise ValueError("Detector options are required")

    scan_path = data["scan_path"]
    detector_options = data["detector_options"]

    if scan_path.endswith(".Z_mtrx"):
        if "matrix_options" not in data:
            raise ValueError("Matrix options are required for matrix files")
        matrix_options = data["matrix_options"]

        if "direction" not in matrix_options:
            raise ValueError("Direction is required for matrix files")

        img = matrix_to_img_array(
            scan_path,
            matrix_options["direction"],
            plane_slopes=matrix_options.get("plane_slopes", None),
        )
        if img is None:
            raise Exception("Unable to open the matrix file")
        scan_nm = (
            get_nm_from_matrix(scan_path)
            if "scan_nm" not in detector_options
            else detector_options["scan_nm"]
        )
    else:
        img = cv2.imread(scan_path)
        if img is None:
            raise ValueError("Unable to read the image file")

        scan_nm = detector_options.get("scan_nm", 0)
        if scan_nm == 0:
            raise ValueError("Scan size in nanometers is required for regular images")

    contrast = detector_options.get("contrast", 1.0)
    rotation = detector_options.get("rotation", 0.0)

    tip_data = detect_tip(
        img,
        scan_nm=scan_nm,
        roi_nm_size=config["ROI_NM_SIZE"],
        model=model,
        cross_size=config["DETECTOR_CROSS_SIZE"],
        contrast=contrast,
        rotation=rotation,
        scan_debug=False,
        roi_debug=False,
    )

    # Convert numpy types to Python types
    serializable_data = convert_to_serializable(tip_data)
    return serializable_data


def receive_json(client_socket: socket.socket, timeout=5) -> dict:
    data = ""
    start_time = time.time()
    while True:
        ready = select.select([client_socket], [], [], timeout)
        if ready[0]:
            chunk = client_socket.recv(1024).decode("utf-8")
            if not chunk:
                raise ConnectionError("Connection closed by client")
            data += chunk
            try:
                return json.loads(data)
            except json.JSONDecodeError:
                if time.time() - start_time > timeout:
                    raise TimeoutError("Timeout while waiting for complete JSON")
                continue
        else:
            raise TimeoutError("Timeout while waiting for data")


def handle_client(client_socket: socket.socket) -> None:
    try:
        # Receive data from the client
        input_data = receive_json(client_socket)

        # Process the image
        result = process_image(input_data)

        # Send the result back to the client
        client_socket.send(json.dumps(result).encode("utf-8") + b"\n")

    except json.JSONDecodeError as e:
        error_message = f"Invalid JSON data: {str(e)}"
        client_socket.send(json.dumps({"error": error_message}).encode("utf-8") + b"\n")
    except TimeoutError as e:
        error_message = f"Timeout error: {str(e)}"
        client_socket.send(json.dumps({"error": error_message}).encode("utf-8") + b"\n")
    except ConnectionError as e:
        error_message = f"Connection error: {str(e)}"
        print(error_message)  # Log to server console
    except Exception as e:
        error_message = f"Unexpected error: {str(e)}"
        client_socket.send(json.dumps({"error": error_message}).encode("utf-8") + b"\n")

    finally:
        # Close the connection
        client_socket.close()


def start_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, port))
    server_socket.listen(5)
    print(f"Server listening on {host}:{port}")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"Accepted connection from {addr}")
        handle_client(client_socket)


if __name__ == "__main__":
    start_server()