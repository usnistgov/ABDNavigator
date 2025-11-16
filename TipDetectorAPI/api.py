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
from detector_functions.matrix_helpers import get_nm_from_matrix, matrix_to_height_array #, matrix_to_img_array_with_z_range

import xmltodict
from tqdm import tqdm


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
    '''
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
    '''
    
    matrix_options = data["matrix_options"]
    img = matrix_to_height_array(scan_path, matrix_options['direction'])
    
    if img is None:
        raise Exception("Unable to open the matrix file")
    
    scan_nm = ( 
        get_nm_from_matrix(scan_path) 
        if "scan_nm" not in detector_options
        else detector_options["scan_nm"]
    )
    
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
        #z_range=z_range,
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
            case "test":
                print('testing xml ref...')
                ref = input_data['ref']
                xml_string = stm.get_xml_for(str(ref))
                
                print(xml_string)
                dict = xmltodict.parse(xml_string)
                print(dict)
                condition_settings = dict["TipConditionLayer"]
                print(condition_settings)        
                
                
            case "autoFab":
                xml = input_data["xml"]
                
                dzdx_ave = None
                dzdy_ave = None
                dzdx = 0
                dzdy = 0
                 
                dict = xmltodict.parse(xml)
                control = dict["ControlGroupLayer"]

                #typecast from 'true'/'false' string to boolean
                test_mode = (control['@testMode'] == 'true')
                print('test_mode: ' + str(test_mode))
                
                print('xml: ' + str(control))

                if test_mode:
                    settle_time_long = 1
                    settle_time_short = 1
                else:
                    settle_time_long = 60
                    settle_time_short = 20
                
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
                resolution = 4
                if bbox is None:
                    print("Failed to read GDS File")
                else:
                    max_x, max_y = bbox[1]
                    max_x = int(max_x)*2*resolution
                    max_y = int(max_y)*2*resolution
                    '''
                    patterned = np.zeros((max_y, max_x, 3), dtype=np.uint8)
                    polygons = top.polygons
                    polys = []
                    for polygon in polygons:
                        x, y = zip(*polygon.points)
                        poly = []
                        for i in range(len(x)):
                            x_vertex = (x[i]*resolution)+max_x/2
                            y_vertex = (y[i]*resolution)+max_y/2
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
                    '''
                    patterned = np.zeros((max_y, max_x, 3), dtype=np.uint8)

                first_scan = 1
                x_offset = 0
                y_offset = 0
                
                scan_settings_list = control["ScanSettingsLayer"]
                print('scan_settings_list: ' + str(scan_settings_list))
                print(len(scan_settings_list))
                if '@controlID' in scan_settings_list:
                    print('only one ScanSettingsLayer')
                    scan_settings_list = [scan_settings_list]
					
                for scan_settings in scan_settings_list:
                    
                    
                    scan_control_ID = scan_settings["@controlID"]
                    xT = float( scan_settings["@x"] )
                    yT = float( scan_settings["@y"] )
                    
                    r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )
                    
                    [x,y,z] = r.apply([xT+x_offset,yT+y_offset,0]) + np.array([x0,y0,0])
                    print("(x,y):")
                    print(x)
                    print(y)
                    
                    if "LithoRasterLayer" not in scan_settings:
                        stm.ref_command(scan_control_ID, 'applyNoThread')
                        #time.sleep(5)
                        
                        condition_settings = scan_settings["TipConditionLayer"]
                        condition_control_ID = condition_settings["@controlID"]
                        
                        print('updating condition_settings for condition control ID: ' + str(condition_control_ID))
                        xml_string = stm.get_xml_for(condition_control_ID)
                        
                        #stm.ref_command(condition_control_ID, 'condition')
                        
                        dict = xmltodict.parse(xml_string)
                        condition_settings = dict["TipConditionLayer"]
                        
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
                        
                        #image_first = bool( condition_settings["@imageFirst"] )
                        image_first = (condition_settings["@imageFirst"] == "true")
                        min_height = float( condition_settings["@minHeight"] )
                        max_height = float( condition_settings["@maxHeight"] )
                        manip_dz = float( condition_settings["@manipDZ"] )
                        manip_V = float( condition_settings["@manipV"] )
                        
                        manip_dz_inc = float( condition_settings["@manipDZinc"] )
                        manip_max_dz = float( condition_settings["@manipMaxDZ"] )
                        manip_V_inc = float( condition_settings["@manipVinc"] )
                        manip_max_V = float( condition_settings["@manipMaxV"] )
                        attempts_per_param = int( condition_settings["@attemptsPerParam"] )
                        
                        '''
                        manip_dz = data["manipDZ"]
    manip_dz_inc = data["manipDZinc"]
    manip_max_dz = data["manipMaxDZ"]
    manip_V = data["manipV"]
    manip_V_inc = data["manipVinc"]
    manip_max_V = data["manipMaxV"]
    attempts_per_param = data["attemptsPerParam"]
                        '''
                        
                        settle_time = float( condition_settings["@settleTime"] )
                        combine = (condition_settings["@combine"] == "true")
                        
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
                            "manipDZinc": manip_dz_inc,
                            "manipMaxDZ": manip_max_dz,
                            "manipV": manip_V,
                            "manipVinc": manip_V_inc,
                            "manipMaxV": manip_max_V,
                            "attemptsPerParam": attempts_per_param,
                            "settleTime": settle_time,
                            "combine": combine
                        }
                        
                        print(input_data)
                        condition_tip(input_data, model, config, test_mode=test_mode)
                        
                        
                        
                        scan_control_ID = prev_scan_settings["@controlID"]
                        xT = float( prev_scan_settings["@x"] )
                        yT = float( prev_scan_settings["@y"] )
                    
                        r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )
                    
                        [x,y,z] = r.apply([xT+x_offset,yT+y_offset,0]) + np.array([x0,y0,0])
                        
                        #move scan region
                        stm.setWindowPosition( (x, y) )
                        #time.sleep(20)
                        for i in tqdm(range(100)):
                            time.sleep(settle_time_short/100.0) 
                                             
                        #apply scan settings
                        stm.ref_command(scan_control_ID, 'applyNoThread')
                        
                        print("settling...")
                        #time.sleep(60)
                        for i in tqdm(range(100)):
                            time.sleep(settle_time_long/100.0) 
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
                        print("dzdx:")
                        print(litho_detect_input["dzdx"])
                        print("dzdy:")
                        print(litho_detect_input["dzdy"])
                        
                        #subtract bg plane
                        #npImg, z_range = subtract_bg_plane(imgInfo[1], litho_detect_input["img_scale_x"], litho_detect_input["img_scale_y"], dzdx, dzdy)
                        #print(npImg.shape)

                        #detect litho: returns img array of where litho is detected & boolean pass/fail
                        if test_mode:
                            pass_litho = True
                            x_offset_curr = 0
                            y_offset_curr = 0
                        else:
                            detected_litho, pass_litho, patterned, x_offset_curr, y_offset_curr = auto_detect_edges(imgInfo[1], litho_detect_input, show_plots=False)
                        #print("detected_litho:")
                        #print(detected_litho)
                        #print()
                        #print("litho_error:")
                        #print(litho_error)
                        #print()
                        print("pass_litho:")
                        print(pass_litho)
                        print("x_offset:")
                        print(x_offset_curr)
                        print("y_offset:")
                        print(y_offset_curr)
                        
                        if ( pass_litho ):
                            x_offset = x_offset - x_offset_curr
                            y_offset = y_offset - y_offset_curr
                        else:
                            print("A large offset is about to occur.")
                            print("    1 - Continue script with automated offset")
                            print("    2 - Continue script without automated offset")
                            print("Else - terminate script to stop [ctrl-Break]")
                            number_input = input("Input: ")
                            number_input_int = int(number_input)
                            if ( number_input_int == 1 ):
                                x_offset = x_offset - x_offset_curr
                                y_offset = y_offset - y_offset_curr
                        
                        
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
                        stm.setWindowPosition( (x, y) )
                        print("settling...")
                        #time.sleep(20)
                        for i in tqdm(range(100)):
                            time.sleep(settle_time_short/100.0) 
                        
                        #apply scan settings
                        stm.ref_command(scan_control_ID, 'applyNoThread')
                        #time.sleep(60)
                        for i in tqdm(range(100)):
                            time.sleep(settle_time_long/100.0) 
                        
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
                            if not test_mode:
                                step_edges = auto_detect_edges(imgInfo[1], step_edge_input, show_plots=False)

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

                            if test_mode:
                                pass_litho = True
                                x_offset_curr = 0
                                y_offset_curr = 0
                            else:
                            	#detect litho: returns img array of where litho is detected & boolean pass/fail
                            	detected_litho, pass_litho, patterned, x_offset_curr, y_offset_curr = auto_detect_edges(imgInfo[1], litho_detect_input, show_plots=False)                      
                            print("pass_litho:")
                            print(pass_litho)
                            print("x_offset:")
                            print(x_offset_curr)
                            print("y_offset:")
                            print(y_offset_curr)
                            
                            if ( pass_litho ):
                                x_offset = x_offset - x_offset_curr
                                y_offset = y_offset - y_offset_curr
                                #move scan region based on offset values
                                xT = float( scan_settings["@x"] )
                                yT = float( scan_settings["@y"] )
                                r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )                  
                                [x,y,z] = r.apply([xT+x_offset,yT+y_offset,0]) + np.array([x0,y0,0])
                                stm.setWindowPosition( (x, y) )
                                print("new x,y:")
                                print(x)
                                print(y)
                                print("settling...")
                                #time.sleep(20)
                                for i in tqdm(range(100)):
                                    time.sleep(settle_time_short/100.0) 
                            else:
                                number_input_int = 0
                                while ((number_input_int != 2) and (number_input_int != 1)):
                                    print("A large offset is about to occur.")
                                    print("    1 - Continue script with automated offset")
                                    print("    2 - Continue script without automated offset")
                                    print("    3 - Change x and y position manually (TODO)")
                                    print("    4 - Re-image and detect litho (TODO)")
                                    print("Else - terminate script to stop [ctrl-Break]")
                                    number_input = input("Input: ")
                                    number_input_int = int(number_input)
                                    if ( number_input_int == 1 ):
                                        x_offset = x_offset - x_offset_curr
                                        y_offset = y_offset - y_offset_curr
                                        xT = float( scan_settings["@x"] )
                                        yT = float( scan_settings["@y"] )
                                        r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )                  
                                        [x,y,z] = r.apply([xT+x_offset,yT+y_offset,0]) + np.array([x0,y0,0])
                                        stm.setWindowPosition( (x, y) )
                                        print("new x,y:")
                                        print(x)
                                        print(y)
                                        print("settling...")
                                        #time.sleep(20)
                                        for i in tqdm(range(100)):
                                            time.sleep(0.2) 
                                    elif ( number_input_int == 3 ):
                                        x_input = input("x position: ")
                                        x_input_int = int(x_input)
                                        y_input = input("y position: ")
                                        y_input_int = int(y_input)
                                        stm.setWindowPosition( (x_input_int, y_input_int) )
                                        print("settling...")
                                        #time.sleep(20)
                                        for i in tqdm(range(100)):
                                            time.sleep(0.2) 
                                    elif ( number_input_int == 4 ):
                                        #apply scan settings
                                        stm.ref_command(scan_control_ID, 'apply')
                                        #time.sleep(20)
                                        for i in tqdm(range(100)):
                                            time.sleep(0.2) 
                                        
                                        #acquire the pre-litho image
                                        imgInfo = util.getNewImage() 
                                        
                                        #detect litho: returns img array of where litho is detected & boolean pass/fail
                                        detected_litho, pass_litho, patterned, x_offset_curr, y_offset_curr = auto_detect_edges(imgInfo[1], litho_detect_input, show_plots=False)                      
                                        print("pass_litho:")
                                        print(pass_litho)
                                        print("x_offset:")
                                        print(x_offset_curr)
                                        print("y_offset:")
                                        print(y_offset_curr)
                                        
                                        if ( pass_litho ):
                                            x_offset = x_offset - x_offset_curr
                                            y_offset = y_offset - y_offset_curr
                                            #move scan region based on offset values
                                            xT = float( scan_settings["@x"] )
                                            yT = float( scan_settings["@y"] )
                                            r = R.from_rotvec( (theta*np.pi/180)*np.array([0.0,0.0,1.0]) )                  
                                            [x,y,z] = r.apply([xT+x_offset,yT+y_offset,0]) + np.array([x0,y0,0])
                                            stm.setWindowPosition( (x, y) )
                                            print("new x,y:")
                                            print(x)
                                            print(y)
                                            print("settling...")
                                            time.sleep(20)
                                            for i in tqdm(range(100)):
                                                time.sleep(0.2) 
                                            number_input = 2
                                        
                        
                        #write the pattern
                        stm.ref_command(litho_control_ID, 'litho')

                        #wait for litho to complete
                        print('performing litho', end='',flush=True)
                        while ( stm.isPerformingLitho() ):
                            time.sleep(0.1)
                            print('.', end='',flush=True)
                        print()
                        print('done with litho')
						
                        #acquire the post-litho image
                        #imgInfo = util.getNewImage()
                        prev_scan_settings = scan_settings
                    
                print('AutoFab complete!')                     
                
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
                print(input_data["y_order"])
                print(input_data["display_hist"])
                                
                img = np.array(input_data["img"])
                img = img.reshape( int(input_data["img_height"]), int(input_data["img_width"]) )
                
                detect_steps_alt(
                    img, 
                    img_width_nm=float(input_data["img_scale_x"]), 
                    img_height_nm=float(input_data["img_scale_x"]),
                    display_hist=bool(input_data["display_hist"]),
                    #display_hist=(input_data["display_hist"] == 'true'),#bool(input_data["display_hist"]),
                    y_order=int(input_data["y_order"]) 
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

