import json
import os
import select
import socket
import sys
import time

import threading
from tensorflow.python.ops.gen_control_flow_ops import abort

sys.path.append('../PythonInterface')
sys.path.append('../StepEdgeDetector')

from AutoTipCondition import condition_tip, subtract_bg_plane
from AutoTipCondition import set_abort

import MatrixPythonAPI as stm
import STMUtils as util

from helpers.main_functions import detect_steps_alt, detect_steps, auto_detect_edges, auto_detect_creep

# Disable OneDNN optimizations and CPU instructions messages
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"

import cv2
import matplotlib.pyplot as plt
import numpy as np
import gdstk
from scipy.spatial.transform import Rotation as R

from tensorflow.keras.models import load_model  # type: ignore

from detector_functions.main_functions import detect_tip
from detector_functions.matrix_helpers import get_nm_from_matrix, matrix_to_img_array, matrix_to_img_array_with_z_range

import xmltodict


# Handle arguments
host = "localhost"
port = 5050
interrupt_port = 5052
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

def get_bg_plane(img, width_nm, height_nm):
    dzdx_ave = 0
    dzdy_ave = 0
    
    num_cols = len(img[0])
    num_rows = len(img)
    
    for yIdx in range(num_cols):
        dzdxAve += img[num_rows-1][yIdx] - img[0][yIdx]
    
    for xIdx in range(num_rows):
        dzdyAve += img[xIdx][num_cols-1] - img[xIdx][0]
        
    dzdxAve /= num_cols*(num_rows-1)
    dzdyAve /= (num_cols-1)*num_rows
    
    #convert from nm(dz)/px to nm(dz)/nm(dx or dy)
    dzdxAve *= num_cols/width_nm
    dzdyAve *= num_rows/height_nm
    
    return dzdxAve, dzdyAve

    

def get_model_and_config():
    return model, config

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
    
    z_range = 1

    if scan_path.endswith(".Z_mtrx"):
        if "matrix_options" not in data:
            raise ValueError("Matrix options are required for matrix files")
        matrix_options = data["matrix_options"]

        if "direction" not in matrix_options:
            raise ValueError("Direction is required for matrix files")

        #img = matrix_to_img_array(
        img,z_range = matrix_to_img_array_with_z_range(
            scan_path,
            matrix_options["direction"],
            plane_slopes=matrix_options.get("plane_slopes", None),
        )
        print('matrix image z range:')
        print(z_range)
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
    min_height = detector_options.get("minHeight",0.11)
    max_height = detector_options.get("maxHeight",0.35)

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
        z_range=z_range,
        min_height=min_height,
        max_height=max_height
    )

    # Convert numpy types to Python types
    serializable_data = convert_to_serializable(tip_data)
    return serializable_data


def receive_json(client_socket: socket.socket, timeout=500) -> dict:
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
        if "command" not in input_data:
            raise ValueError("Command is required")
            
        command = input_data["command"]
        print(command)
        result = ""
        match command:
            case "autoFab":
                xml = input_data["xml"]
                
                dzdx_ave = None
                dzdy_ave = None
                dzdx = 0
                dzdy = 0
                 
                dict = xmltodict.parse(xml)
                control = dict["ControlGroupLayer"]
                
                
                theta = float( control["@angle"] )
                
                x0 = float( control["@x"] )
                
                y0 = float( control["@y"] )

                gds_layer = control["GDSLayer"]
                #gds_path = gds_layer["@img"]
                #gds_path = "Y:/Sample Logbook/W62 All Device LT" + gds_path
                gds_path = gds_layer["@absolutePath"]
                print(gds_path)
                lib = gdstk.read_gds(gds_path, 1e-9)
                top = lib.top_level()[0]
                bbox = top.bounding_box()
                if bbox is None:
                    print("Failed to read GDS File")
                else:
                    max_x, max_y = bbox[1]
                    max_x = int(max_x)*2
                    max_y = int(max_y)*2
                    patterned = np.zeros((max_y, max_x, 3), dtype=np.uint8)
                    polygons = top.polygons
                    polys = []
                    for polygon in polygons:
                        x, y = zip(*polygon.points)
                        poly = []
                        for i in range(len(x)):
                            x_vertex = x[i]+max_x/2
                            y_vertex = y[i]+max_y/2
                            poly.append([x_vertex, y_vertex])
                        polys.append(np.array(poly, np.int32))
                    cv2.fillPoly(patterned, polys, (255, 0, 0))
                    #patterned = cv2.cvtColor(patterned, cv2.COLOR_RGB2GRAY) 
                    patterned = np.flipud(patterned)
                    print(patterned.shape)
                    fig, ax = plt.subplots(1, 2, figsize=(15, 5))
                    ax[0].imshow(patterned)
                    ax[0].set_title('Verify GDS Pattern')
                    ax[0].axis('off')
                    ax[1].remove()
                    plt.show()
                    cv2.waitKey(0) 
                    cv2.destroyAllWindows()
                    #patterned = np.zeros((max_y, max_x, 3), dtype=np.uint8)

                first_scan = 1
                x_offset = 0
                y_offset = 0
                
                scan_settings_list = control["ScanSettingsLayer"]
                for scan_settings in scan_settings_list:
                    
                    
                    scan_control_ID = scan_settings["@controlID"]
                    xT = float( scan_settings["@x"] )
                    yT = float( scan_settings["@y"] )
                    
                    r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )
                    
                    [x,y,z] = r.apply([xT,yT,0]) + np.array([x0,y0,0])
                    print("(x,y):")
                    print(x)
                    print(y)
                    
                    if "LithoRasterLayer" not in scan_settings:
                        condition_settings = scan_settings["TipConditionLayer"]
                        #condition_control_ID = condition_settings["@controlID"]
                        #print('condition control ID:')
                        #print(condition_control_ID)
                        
                        #stm.ref_command(condition_control_ID, 'condition')
                        
                        
                        '''
                        detection_contrast = data["detectionContrast"]
                        prediction_threshold = data["predictionThreshold"]
                        majority_threshold = data["majorityThreshold"]
                        lattice_angle = data["latticeAngle"]
                        dzdx = data["dzdx"]
                        dzdy = data["dzdy"]
                        scan_x = data["scanX"]
                        scan_y = data["scanY"]
                        scan_scale_x = data["scanScaleX"]
                        scan_scale_y = data["scanScaleY"]
                        condition_x = data["conditionX"]
                        condition_y = data["conditionY"]
                        condition_scale_x = data["conditionScaleX"]
                        condition_scale_y = data["conditionScaleY"]
                        image_first = data["imageFirst"]
                        min_height = data["minHeight"]
                        max_height = data["maxHeight"]
                        manip_dz = data["manipDZ"]
                        manip_V = data["manipV"]
                        settle_time = data["settleTime"]
                        '''
                        detection_contrast = float( condition_settings["@detectionContrast"] )
                        prediction_threshold = float( condition_settings["@predictionThreshold"] )
                        majority_threshold = float( condition_settings["@majorityThreshold"] )
                        lattice_angle = float( condition_settings["@latticeAngle"] )
                        scan_x = float( condition_settings["@scanPositionX"] )
                        scan_y = float( condition_settings["@scanPositionY"] )
                        scan_scale_x = float( condition_settings["@scanScaleX"] )#condition_settings["scanScaleX"]
                        scan_scale_y = float( condition_settings["@scanScaleY"] )
                        
                        dzdx = float( condition_settings["@dzdx"] )
                        dzdy = float( condition_settings["@dzdy"] )                      
                        
                        condition_x = float( condition_settings["@conditionPositionX"] )
                        condition_y = float( condition_settings["@conditionPositionY"] )
                        condition_scale_x = float( condition_settings["@conditionScaleX"] ) #data["conditionScaleX"]
                        condition_scale_y = float( condition_settings["@conditionScaleY"] ) #data["conditionScaleY"]
                        
                        image_first = bool( condition_settings["@imageFirst"] )
                        min_height = float( condition_settings["@minHeight"] )
                        max_height = float( condition_settings["@maxHeight"] )
                        manip_dz = float( condition_settings["@manipDZ"] )
                        manip_V = float( condition_settings["@manipV"] )
                        settle_time = float( condition_settings["@settleTime"] )
                        
                        input_data = {
                            "detectionContrast": detection_contrast,
                            "predictionThreshold": prediction_threshold,
                            "majorityThreshold": majority_threshold,
                            "latticeAngle": lattice_angle,
                            "dzdx": dzdx,
                            "dzdy": dzdy,
                            "scanX": scan_x,
                            "scanY": scan_y,
                            "scanScaleX": scan_scale_x,
                            "scanScaleY": scan_scale_y,
                            "conditionX": condition_x,
                            "conditionY": condition_y,
                            "conditionScaleX": condition_scale_x,
                            "conditionScaleY": condition_scale_y,
                            "imageFirst": image_first,
                            "minHeight": min_height,
                            "maxHeight": max_height,
                            "manipDZ": manip_dz,
                            "manipV": manip_V,
                            "settleTime": settle_time
                        }
                        
                        print(input_data)
                        condition_tip(input_data, model, config)
                        
                        
                        
                        scan_control_ID = prev_scan_settings["@controlID"]
                        xT = float( prev_scan_settings["@x"] )
                        yT = float( prev_scan_settings["@y"] )
                    
                        r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )
                    
                        [x,y,z] = r.apply([xT,yT,0]) + np.array([x0,y0,0])
                        
                        #move scan region
                        stm.setWindowPosition( (x,y) )
                        time.sleep(20)
                                             
                        #apply scan settings
                        stm.ref_command(scan_control_ID, 'apply')
                        
                        print("settling...")
                        time.sleep(60)
                        #time.sleep(1)
                        
                        #acquire the post-litho image
                        imgInfo = util.getNewImage()
                        
                        #setting up parameters for litho detection:
                        
                        litho_detect_input = {
                            "img_width": int( prev_scan_settings["@pixelsX"] ),
                            "img_height": int( prev_scan_settings["@pixelsY"] ),
                            "scan_settings_x": float( prev_scan_settings["@x"] ),
                            "scan_settings_y": float( prev_scan_settings["@y"] ),
                            "img_scale_x": float( prev_scan_settings["@scaleX"] ),
                            "img_scale_y": float( prev_scan_settings["@scaleY"] ),
                            "scan_settings_angle": float( prev_scan_settings["@angle"] ),
                            "litho_img": True,
                            "gds_path": gds_path, 
                            "patterned": patterned,
                            "dzdx": dzdx,
                            "dzdy": dzdy,
                            "overlap": False
                        }
                        
                        print("img_width:")
                        print(litho_detect_input["img_width"])
                        print("img_height:")
                        print(litho_detect_input["img_height"])
                        print("scan_settings_x:")
                        print(litho_detect_input["scan_settings_x"])
                        print("scan_settings_y:")
                        print(litho_detect_input["scan_settings_y"])
                        print("img_scale_x:")
                        print(litho_detect_input["img_scale_x"])
                        print("img_scale_y:")
                        print(litho_detect_input["img_scale_y"])
                        print("scan_settings_angle:")
                        print(litho_detect_input["scan_settings_angle"])
                        print("gds_path:")
                        print(litho_detect_input["gds_path"])
                        
                        #subtract bg plane
                        #npImg, z_range = subtract_bg_plane(imgInfo[1], litho_detect_input["img_scale_x"], litho_detect_input["img_scale_y"], dzdx, dzdy)
                        #print(npImg.shape)

                        #detect litho: returns img array of where litho is detected & boolean pass/fail
                        detected_litho, pass_litho, patterned, x_offset, y_offset = auto_detect_edges(imgInfo[1], litho_detect_input, show_plots=True)
                        #print("detected_litho:")
                        #print(detected_litho)
                        #print()
                        #print("litho_error:")
                        #print(litho_error)
                        #print()
                        print("pass_litho:")
                        print(pass_litho)
                        print("x_offset:")
                        print(x_offset)
                        print("y_offset:")
                        print(y_offset)
                        
                        #creep correction: returns x & y value to offset window position
                        #x_offset, y_offset = auto_detect_creep(litho_error, litho_detect_input)

                        #if(not pass_litho):
                        #    litho_detect_input["scan_settings_x"] -= x_offset/2
                        #    litho_detect_input["scan_settings_y"] -= y_offset/2
                        #    detected_litho, litho_error, pass_litho, patterned = auto_detect_edges(imgInfo[1], litho_detect_input, show_plots=True)
                        #    if(not pass_litho):
                        #        print("Error: Failed to Detect Litho")
                        #        print("User Input Required to Proceed")
                        #    new_x_offset, new_y_offset = auto_detect_creep(litho_error, litho_detect_input)
                        #    x_offset -= new_x_offset/2
                        #    y_offset -= new_y_offset/2       
                    
                    else:
                        litho_settings = scan_settings["LithoRasterLayer"]
                        litho_control_ID = litho_settings["@controlID"]
                        print('litho_control_ID:')
                        print(litho_control_ID)
                    
                        #move scan region
                        stm.setWindowPosition( (x,y) )
                        print("settling...")
                        time.sleep(60)
                        
                        #apply scan settings
                        stm.ref_command(scan_control_ID, 'apply')
                        time.sleep(1)
                        
                        #acquire the pre-litho image
                        imgInfo = util.getNewImage()           

                        #setting up parameters for step edge detection:
                        if ( first_scan == 1 ):
                            step_edge_input = {
                                "img_width": int( scan_settings["@pixelsX"] ),
                                "img_height": int( scan_settings["@pixelsY"] ),
                                "scan_settings_x": float( scan_settings["@x"] ),
                                "scan_settings_y": float( scan_settings["@y"] ),
                                "img_scale_x": float( scan_settings["@scaleX"] ),
                                "img_scale_y": float( scan_settings["@scaleY"] ),
                                "scan_settings_angle": float( scan_settings["@angle"] ),
                                "litho_img": False,
                                "gds_path": gds_path,
                                "patterned": patterned,
                                "dzdx": dzdx,
                                "dzdy": dzdy,
                                "overlap": False
                            }
                            print("img_width:")
                            print(step_edge_input["img_width"])
                            print("img_height:")
                            print(step_edge_input["img_height"])
                            print("scan_settings_x:")
                            print(step_edge_input["scan_settings_x"])
                            print("scan_settings_y:")
                            print(step_edge_input["scan_settings_y"])
                            print("img_scale_x:")
                            print(step_edge_input["img_scale_x"])
                            print("img_scale_y:")
                            print(step_edge_input["img_scale_y"])
                            print("scan_settings_angle:")
                            print(step_edge_input["scan_settings_angle"])
                            print("gds_path:")
                            print(step_edge_input["gds_path"])

                            #detect step edges: returns binary mask of detected step edges
                            step_edges = auto_detect_edges(imgInfo[1], step_edge_input, show_plots=True)

                            first_scan = 0

                        else:
                            litho_detect_input = {
                                "img_width": int( scan_settings["@pixelsX"] ),
                                "img_height": int( scan_settings["@pixelsY"] ),
                                "scan_settings_x": float( scan_settings["@x"] ),
                                "scan_settings_y": float( scan_settings["@y"] ),
                                "img_scale_x": float( scan_settings["@scaleX"] ),
                                "img_scale_y": float( scan_settings["@scaleY"] ),
                                "scan_settings_angle": float( scan_settings["@angle"] ),
                                "litho_img": True,
                                "gds_path": gds_path, 
                                "patterned": patterned,
                                "dzdx": dzdx,
                                "dzdy": dzdy,
                                "overlap": True
                            }

                            # ******** update patterned to earse gds array to be checked in detect litho function

                             #detect litho: returns img array of where litho is detected & boolean pass/fail
                            detected_litho, pass_litho, patterned, x_offset, y_offset = auto_detect_edges(imgInfo[1], litho_detect_input, show_plots=True)                      
                            print("pass_litho:")
                            print(pass_litho)
                            print("x_offset:")
                            print(x_offset)
                            print("y_offset:")
                            print(y_offset)
                            
                            if ( pass_litho ):
                                #move scan region based on offset values
                                xT = float( scan_settings["@x"] )
                                yT = float( scan_settings["@y"] )
                                r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )                  
                                [x,y,z] = r.apply([xT+x_offset,yT-y_offset,0]) + np.array([x0,y0,0])
                                #stm.setWindowPosition( (x+x_offset, y-y_offset) )
                                print("new x,y:")
                                print(x)
                                print(y)
                                print("settling...")
                                time.sleep(60)
                        
                        #write the pattern
                        stm.ref_command(litho_control_ID, 'litho')
                        
                        #acquire the post-litho image
                        #imgInfo = util.getNewImage()
                        prev_scan_settings = scan_settings
                    
                               
                
            case "checkTipQuality":
                result = process_image(input_data)
            case "conditionTip":
                condition_tip(input_data, model, config)
            case "detectStepEdges":
                print()
                print(np.array(input_data["img"]))
                print('height and width in pixels:')
                print(input_data["img_width"])
                print(input_data["img_height"])
                print('image size in nm:')
                print(input_data["img_scale_x"])
                print(input_data["img_scale_y"])
                print('captured lines start and end:')
                print(input_data["captured_lines_start"])
                print(input_data["captured_lines_end"])
                print('nm from z:')
                print(input_data["nm_from_z"])
                print('scan settings x,y and scale_x, scale_y (nm), angle (deg):')
                print(input_data["scan_settings_x"])
                print(input_data["scan_settings_y"])
                print(input_data["scan_settings_scale_x"])
                print(input_data["scan_settings_scale_y"])
                print(input_data["scan_settings_angle"])
                print('roughness threshold:')
                print(input_data["roughnessThreshold"])
                print('low resolution?:')
                print(input_data["lowResolution"])
                print('lithography method:')
                print(input_data["findLithoMethod"])
                print('step edge thickness:')
                print(input_data["thickness"])
                print('verify with GDS file?:')
                print(input_data["verifyGDS"])
                print('gds file path')
                #print(input_data["gds_path"])
                print()

                detect_steps(
                    np.array(input_data["img"]), 
					          img_width=int(input_data["img_width"]),
                    img_height=int(input_data["img_height"]),
                    show_plots=False, 
                    show_output=True, 
                    blur=int(input_data["blur"]), 
                    postprocessing=input_data["zoomedIn"], 
                    max_pxl=400,
                    nm_from_z=float(input_data["nm_from_z"]),
                    roughnessThreshold=float(input_data["roughnessThreshold"]),
                    lowResolution=input_data["lowResolution"],
                    find_litho=input_data["findLithoMethod"],
                    thickness=float(input_data["thickness"]),
                    verifyGDS=input_data["verifyGDS"],
                    scan_settings_x=input_data["scan_settings_x"],
                    scan_settings_y=input_data["scan_settings_y"],
                    #scan_settings_x=0,
                    #scan_settings_y=0,
                    img_rescale_x=input_data["img_rescale_x"],
                    img_rescale_y=input_data["img_rescale_y"],
                    scan_settings_angle=input_data["scan_settings_angle"]
                    #scan_settings_angle=-183
                    )
                
                print('all done')

            case "altDetectStepEdges":
                print()
                print(np.array(input_data["img"]))
                print('height and width in pixels:')
                print(input_data["img_width"])
                print(input_data["img_height"])
                print('image size in nm:')
                print(input_data["img_scale_x"])
                print(input_data["img_scale_y"])
                print('captured lines start and end:')
                print(input_data["captured_lines_start"])
                print(input_data["captured_lines_end"])
                                
                img = np.array(input_data["img"])
                img = img.reshape( int(input_data["img_height"]), int(input_data["img_width"]) )
                
                detect_steps_alt(
                    img, 
                    img_width_nm=float(input_data["img_scale_x"]), 
                    img_height_nm=float(input_data["img_scale_x"]) 
                    )
                
                print('all done')

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

def handle_interrupt(client_socket: socket.socket) -> None:
    #global abort
    
    try:
        # Receive data from the client
        input_data = receive_json(client_socket)
        print(input_data)
        
        interrupt = input_data["interrupt"]
        #print(command)
        match interrupt:
            case "abort":
                #abort = True
                set_abort(True)
        
    #except json.JSONDecodeError as e:
    #    error_message = f"Invalid JSON data: {str(e)}"
    #    client_socket.send(json.dumps({"error": error_message}).encode("utf-8") + b"\n")
    #except TimeoutError as e:
    #    error_message = f"Timeout error: {str(e)}"
    #    client_socket.send(json.dumps({"error": error_message}).encode("utf-8") + b"\n")
    #except ConnectionError as e:
    #    error_message = f"Connection error: {str(e)}"
    #    print(error_message)  # Log to server console
    #except Exception as e:
    #    error_message = f"Unexpected error: {str(e)}"
    #    client_socket.send(json.dumps({"error": error_message}).encode("utf-8") + b"\n")

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

def start_interrupt_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind((host, interrupt_port))
    server_socket.listen(5)
    print(f"Interrupt Server listening on {host}:{interrupt_port}")
    
    while True:
        client_socket, addr = server_socket.accept()
        print(f"Accepted connection from {addr}")
        handle_interrupt(client_socket)


if __name__ == "__main__":
    i_thread = threading.Thread(target=start_interrupt_server)
    i_thread.start()
    
    start_server()
