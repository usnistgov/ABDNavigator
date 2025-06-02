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
    z_range = np.max(img) - np.min(img)
    img = (img - np.min(img)) / z_range * 255
    img = np.flipud(img)

    # Convert the image into a cv2 image
    img = np.array(img, dtype=np.uint8)

    # Convert the image to a 3 channel image
    img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
    
    return img, z_range
    

def condition_tip(data: dict, model, config):
    print('conditioning tip...')
    print(data)
    
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
    
    print('scales:')
    print(scan_scale_x)
    print(scan_scale_y)
    print(condition_scale_x)
    print(condition_scale_y)
    
    width = scan_scale_x#50.0
    height = scan_scale_y#50.0

    window_width = condition_scale_x#0.5*width
    window_height = condition_scale_y#0.5*height

    num_x = 8
    num_y = 8

    start_x = 0
    start_y = 0

        
    print(condition_x)
    print(condition_y)
    print(scan_x)
    print(scan_y)
    
    
   
    for y_idx in range(start_y,num_y+1):
        for x_idx in range(start_x,num_x+1):
        
            if abort == True:
                return 
            
            x = window_width*x_idx/num_x - window_width/2
            y = window_height*y_idx/num_y - window_height/2
            
            print("x,y: " + str(x) + "," + str(y))
            print("settle position: " + str(height/2.0))
            #print("scan shift: " + str(-height))
            
            #the first pulse happens before the first image, unless imageFirst is selected
            print(y_idx)
            print(start_y)
            print(image_first)
            if y_idx > start_y or x_idx > start_x or image_first == False:
                #perform tip conditioning
                stm.setWindowPosition( (condition_x,condition_y) )
                print("setting window size")
                stm.setWindowSize( (condition_scale_x, condition_scale_y) )
                if abort == True:
                    return
                time.sleep(settle_time)
                stm.moveTip( (0.0,0.0) )
                if abort == True:
                    return
                print("settling...")
                time.sleep(settle_time)
                print("continuing")
                stm.moveTip( (x,y) )
                print("manipulating")
                stm.manip(manip_dz,manip_V)
                #imgInfo = util.getNewImage()
                #npImg = imgInfo[1]
            
            #image to check tip condition
            print('imaging to check tip condition')
            stm.setWindowPosition( (scan_x,scan_y) )
            print("setting window size")
            stm.setWindowSize( (scan_scale_x, scan_scale_y) )
            
            if abort == True:
                return
            
            time.sleep(settle_time)
            
            if abort == True:
                return
            
            stm.moveTip( (0.0,height/2.0) )
            print("settling...")
            time.sleep(settle_time)
            
            if abort == True:
                return
            
            print("continuing")
            imgInfo = util.getNewImage()
            #npImg = imgInfo[1]
            
            print('initial z range:')
            print(np.max(imgInfo[1]) - np.min(imgInfo[1]))
    
    
            npImg,z_range = subtract_bg_plane(imgInfo[1], width, height, dzdx=dzdx, dzdy=dzdy)
            
            print('conditioning z_range: ')
            print(z_range)
            
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
                roi_debug=False,
                z_range=z_range,
                min_height=min_height,
                max_height=max_height,
                sharp_prediction_threshold=prediction_threshold )

            print('done detecting tip')
            print(tip_data)
            
            num_sharp = tip_data['sharp']
            num_total = tip_data['total']
            
            stm.reportTipQuality( convert_to_serializable(tip_data) )
            
            if num_sharp/num_total > majority_threshold:
                print('done conditioning tip')
                return
            
            #if image_first == True:
            #    return
                
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

