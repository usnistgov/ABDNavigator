import MatrixPythonAPI as stm
import STMUtils as util

import matplotlib.pyplot as plt
import numpy as np

import time

import cv2

import sys


sys.path.append('../TipDetectorAPI')
from detector_functions.main_functions import detect_tip
from json_utils import convert_to_serializable

abort = False

def set_abort(val):
    global abort
    abort = val
    if abort == True:
        print("aborting")
    
def subtract_bg_plane(img, width_nm, height_nm, dzdx = 0, dzdy = 0):
    nm_px_x = width_nm/(len(img[0])-1)
    nm_px_y = height_nm/(len(img)-1)
    
    img_sub = np.empty((len(img), len(img[0])))
    for x_idx in range(len(img[0])):
        for y_idx in range(len(img)):
            x = nm_px_x*x_idx
            y = nm_px_y*y_idx#(len(img)-1-y_idx)
            dz = x*dzdx + y*dzdy
            img_sub[y_idx][x_idx] = img[y_idx][x_idx] - dz
    
    img = img_sub
    img = (img - np.min(img)) / (np.max(img) - np.min(img)) * 255
    img = np.flipud(img)

    # Convert the image into a cv2 image
    img = np.array(img, dtype=np.uint8)

    # Convert the image to a 3 channel image
    img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
    
    return img
    

def condition_tip(data: dict, model, config):
    print('conditioning tip...')
    print(data)
    
    if "detectionContrast" not in data:
        raise ValueError("detectionContrast is required")
    detection_contrast = data["detectionContrast"]
    
    if "predictionThreshold" not in data:
        raise ValueError("predictionThreshold is required")
    prediction_threshold = data["predictionThreshold"]
    
    if "majorityThreshold" not in data:
        raise ValueError("majorityThreshold is required")
    majority_threshold = data["majorityThreshold"]
    
    if "latticeAngle" not in data:
        raise ValueError("latticeAngle is required")
    lattice_angle = data["latticeAngle"]
    
    if "dzdx" not in data:
        raise ValueError("dzdx is required")
    dzdx = data["dzdx"]
    
    if "dzdy" not in data:
        raise ValueError("dzdy is required")
    dzdy = data["dzdy"]
    
    if "scanX" not in data:
        raise ValueError("scanX is required")
    scan_x = data["scanX"]
    
    if "scanY" not in data:
        raise ValueError("scanY is required")
    scan_y = data["scanY"]
    
    if "conditionX" not in data:
        raise ValueError("conditionX is required")
    condition_x = data["conditionX"]
    
    if "conditionY" not in data:
        raise ValueError("conditionY is required")
    condition_y = data["conditionY"]
    
    #scan_x = 141.0
    #scan_y = -38.0

    width = 50.0
    height = 50.0

    window_width = 0.5*width
    window_height = 0.5*height

    num_x = 8
    num_y = 8

    start_x = 0
    start_y = 0

    #get the ML model and configuration
    #model,config = get_model_and_config()
    
    print(condition_x)
    print(condition_y)
    print(scan_x)
    print(scan_y)
    
    
    #x_idx = 2
    #y_idx = 0
    for y_idx in range(start_y,num_y+1):
        for x_idx in range(start_x,num_x+1):
        
            if abort == True:
                return 
            
            x = window_width*x_idx/num_x - window_width/2
            y = window_height*y_idx/num_y - window_height/2
            
            print("x,y: " + str(x) + "," + str(y))
            print("settle position: " + str(height/2.0))
            #print("scan shift: " + str(-height))
            
            #perform tip conditioning
            stm.setWindowPosition( (condition_x,condition_y) )
            if abort == True:
                return
            time.sleep(20)
            stm.moveTip( (0.0,0.0) )
            if abort == True:
                return
            print("settling...")
            time.sleep(20)
            print("continuing")
            stm.moveTip( (x,y) )
            stm.zPulse(-0.3)
            #imgInfo = util.getNewImage()
            #npImg = imgInfo[1]
            
            #image to check tip condition
            stm.setWindowPosition( (scan_x,scan_y) )
            
            if abort == True:
                return
            
            time.sleep(20)
            
            if abort == True:
                return
            
            stm.moveTip( (0.0,height/2.0) )
            print("settling...")
            time.sleep(20)
            
            if abort == True:
                return
            
            print("continuing")
            imgInfo = util.getNewImage()
            #npImg = imgInfo[1]
            npImg = subtract_bg_plane(imgInfo[1], width, height, dzdx=dzdx, dzdy=dzdy)
            
            #plt.imshow(npImg)
            #plt.gray()
            #plt.show()
            #input("Press Enter to continue...")
            
            print("checking tip quality...")
            
            #check tip condition
            tip_data = detect_tip(
                npImg,
                scan_nm=width,
                roi_nm_size=config["ROI_NM_SIZE"],
                model=model,
                cross_size=config["DETECTOR_CROSS_SIZE"],
                contrast=detection_contrast,
                rotation=lattice_angle,
                scan_debug=False,
                roi_debug=False )

            print(tip_data)
            
            num_sharp = tip_data['sharp']
            num_total = tip_data['total']
            
            stm.reportTipQuality( convert_to_serializable(tip_data) )
            
            if num_sharp/num_total > majority_threshold:
                print('done conditioning tip')
                return
            # Convert numpy types to Python types
            #serializable_data = convert_to_serializable(tip_data)
            

    #stm.setWindowPosition( (0.0,0.0) )
    #time.sleep(20)
    #stm.moveTip( (0.0,height/2.0) )
    #print("settling...")
    #time.sleep(20)
    #print("continuing")
    #imgInfo = util.getNewImage()
    #npImg = imgInfo[1] 



    #plt.imshow(npImg)
    #plt.gray()
    #plt.show()

