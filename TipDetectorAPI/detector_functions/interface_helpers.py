import json
import os

from detector_functions.matrix_helpers import get_nm_from_matrix


def extract_nm_from_path(image_path: str) -> float:
    """Extracts the nm from the image path.

    Finds the number between "nmx" and "nm" in the image path due to the format 45nmx45nm.

    Parameters:
        image_path (str): Path of the image

    Returns:
        float: Extracted scan size in nm
    """
    try:
        temp_path = image_path
        temp_path = temp_path.split("nmx")[1]
        scan_nm = float(temp_path.split("nm")[0])
        print(f"Detected scan size: {scan_nm} nm")
    except (IndexError, ValueError):
        print("Could not detect scan size from the image path")
        scan_nm = float(input("Enter the scan size in nm: "))
    return scan_nm


def initialize_scan_configs(scan_configs: dict) -> None:
    """Create the scan configurations file.

    Parameters:
        scan_configs (dict): Scan configurations to be saved
    """
    if not os.path.exists("configs"):
        os.makedirs("configs")
    with open("configs/scan_configs.json", "w") as f:
        json.dump(scan_configs, f, indent=4)


def get_configs(path: str, scan_configs: dict, SCAN_CONFIG_PARAMETERS: list) -> dict:
    """Get the configurations for the scan from the user.

    Parameters:
        path (str): Path of the image
        scan_configs (dict): Scan configurations
        SCAN_CONFIG_PARAMETERS (list): List of scan configuration parameters

    Returns:
        dict: Complete scan configurations
    """
    configs = {}
    path_printed = False
    for param in SCAN_CONFIG_PARAMETERS:
        if not path_printed and scan_configs == {}:
            scan_configs = {}
            print(f"\nCreating configs for {path}")
            path_printed = True
        if param in scan_configs:
            configs[param] = scan_configs[param]
        else:
            if not path_printed:
                print(f"\nMissing {param} value for {path}")
                path_printed = True
            if param == "scan_nm":
                if path.endswith(".Z_mtrx"):
                    configs[param] = get_nm_from_matrix(path)
                else:
                    configs[param] = extract_nm_from_path(path)
            elif param == "direction":
                configs[param] = int(input("Enter the trace/direction index: "))
            else:
                configs[param] = float(input(f"Enter the {param} value: "))
    return configs
