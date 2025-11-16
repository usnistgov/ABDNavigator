import cv2
import numpy as np
from tqdm import tqdm
import matplotlib.pyplot as plt
from scipy.signal import correlate2d

import sys
sys.path.append('../StepEdgeDetector')
from helpers.main_functions import auto_flatten

from detector_functions.image_helpers import (
    calculate_black_pixel_ratio,
    cross_check_prediction,
    extract_roi,
    find_contours,
    locate_brightest_pixel,
    merge_overlapping_contours,
    rotate_image,
)

# Constants
CONTOUR_MIN_SIZE = (
    0.9,
    0.9,
)  # Minimum size of the contour to pass (width, height) in nm
SHARP_PREDICTION_THRESHOLD = 0.5  # Prediction threshold for sharpness - Greater than or equal to this value is sharp, otherwise dull
CLASS_NAMES = {
    0: "Dull",
    1: "Sharp",
}

# Colors
RED = (50, 50, 255)
GREEN = (0, 255, 0)
BLUE = (255, 200, 0)
YELLOW = (0, 255, 255)


def detect_tip(
    img: np.ndarray,
    scan_nm,
    model,
    roi_nm_size=2,
    cross_size=0,
    contrast=1,
    rotation=45,
    scan_debug=False,
    roi_debug=False,
    z_range=1,
    min_height=0,
    max_height=0.2,
    sharp_prediction_threshold=0.5
) -> dict:
    """Detects the tip in the given image.

    Parameters:
        img (ndarray): Image to process
        scan_nm (float): Scan size in nm
        model: Prediction model
        roi_nm_size (int, optional): Size of the region of interest in nm.
        cross_size (int, optional): Size of the cross for roi cross check.
        contrast (int, optional): Contrast adjustment factor.
        rotation (float, optional): Rotation angle for the image.
        scan_debug (bool, optional): Enable debugging for finished scans.
        roi_debug (bool, optional): Enable debugging for each region of interest.

    Returns:
        dict: Dictionary containing the detection results
    """
    print("detecting tip")
    
    #flatten the image
    print('flattening image...')
    img = auto_flatten(img, img_width_nm=scan_nm, img_height_nm=scan_nm, display_hist=False, y_order=6 )
    
    #rotate the image so the lattice is straight
    print('rotating image...')
    if rotation != 0:
        img = rotate_image(img, rotation)
        
    #switch data to a grayscale cv2 image
    print('getting z range...')
    z_range = np.max(img) - np.min(img)
    print('z_range: ' + str(z_range))
    
    print('converting image to grayscale uint8...')
    gray = (img - np.min(img)) / z_range * 255
    gray = gray.astype(int)
    gray = np.array(gray, dtype=np.uint8)
    #gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    '''
    cv2.namedWindow("gray0")
    cv2.imshow("gray0", cv2.resize(gray,(400,400)))
    cv2.waitKey(0)
    cv2.destroyAllWindows()
    '''
    
    max_gray0 = np.max(gray)
    min_gray0 = np.min(gray)
    
    #get rid of dips below the plane
    hist, bin_edges = np.histogram(gray, bins=256)
    max_idx = np.argmax(hist)
    
    gray = np.clip(gray, a_min=max_idx, a_max=None)
    gray = gray.astype(np.uint8)
    
    #max_gray = np.max(gray)
    #min_gray = np.min(gray)
    
    '''
    autocorr_image = correlate2d(gray, gray, mode='same')
    
    
    autocorr_image = (autocorr_image - np.min(autocorr_image)) / (np.max(autocorr_image) - np.min(autocorr_image)) * 255
    
    autocorr_image = autocorr_image.astype(np.uint8)
    
    
    
    cv2.namedWindow("gray")
    cv2.imshow("gray", cv2.resize(gray,(400,400)))
    cv2.namedWindow("autocorr_image")
    cv2.imshow("autocorr_image", cv2.resize(autocorr_image,(400,400)))
        
    cv2.waitKey(0)
    cv2.destroyAllWindows()
    '''
    
    
    
    #threshold the image to find all the dangling bonds
    max_gray = np.max(gray)
    min_gray = np.min(gray)
    
    z_thresh = min_gray + (max_gray0-min_gray0)*min_height/z_range
    
    _, thresh = cv2.threshold(gray, int(z_thresh), 255, cv2.THRESH_BINARY)
    
    '''
    cv2.namedWindow("gray")
    cv2.imshow("gray", cv2.resize(gray,(400,400)))
    
    cv2.namedWindow("thresh")
    cv2.imshow("thresh", cv2.resize(thresh,(400,400)))
    
    cv2.waitKey(0)
    cv2.destroyAllWindows()
    '''
    
    #get the connected components, which should be individual features
    num_labels, labels, stats, centroids = cv2.connectedComponentsWithStats(thresh, connectivity=8)
    
    #only need bounding boxes from the connected components
    boxes = []
    for i in range(num_labels):
        x = stats[i, cv2.CC_STAT_LEFT]
        y = stats[i, cv2.CC_STAT_TOP]
        w = stats[i, cv2.CC_STAT_WIDTH]
        h = stats[i, cv2.CC_STAT_HEIGHT]
        boxes.append([x,y,w,h])
    
    #no dangling bond should be taller than max_height nm, so clip colors "taller" than that and scale image appropriately
    heights = z_range*(gray - min_gray)/(max_gray - min_gray)  #array of the actual heights
    
    #remove features that are too tall or too short
    
    i = 0
    boxes = list(boxes)
    print('num boxes: ' + str(len(boxes)))
    
    tall_idxs = []
    
    while i < len(boxes):
        [x, y, w, h] = boxes[i]
        check_heights = heights[y : y + w, x : x + h]
        h_diff = np.max(check_heights)#-np.min(check_heights)
        
        #if it's too tall, record that
        if (h_diff > max_height):
            tall_idxs.append(i)
        
        #if (h_diff > max_height):
        #    boxes.pop(i)
        #if it's too short, just remove it
        if (h_diff < min_height):
            boxes.pop(i)
        else:
            i += 1
    
    print('new num boxes: ' + str(len(boxes)))
    
    #if there are no features left, then we need to return
    if (len(boxes) <= 1):
        print('contour length is 0')
        output = {
            "sharp": 0,
            "dull": -1,
            "total": -1,
            "roi_data": {
                "constants": {"nm_size": 0, "pixel_size": 0},
                "locations": [],
            },
        }

        print('output is: ')
        print(output)
        return output
    
    print('more than 0 features')
    
    total_bonds = 0
    total_cls = {0: 0, 1: 0}
    nm_p_pixel = scan_nm / img.shape[0]  # Calculate using height
    
    print('CONTOUR_MIN_SIZE (nm): ' + str(CONTOUR_MIN_SIZE[0]) + '  ' + str(CONTOUR_MIN_SIZE[1]))
    print('CONTOUR_MIN_SIZE (nm): ' + str(CONTOUR_MIN_SIZE[0]/ nm_p_pixel) + '  ' + str(CONTOUR_MIN_SIZE[1]/ nm_p_pixel))
    
    i = 0
    brightest_locations = set()
    roi_locations = []
    for box in boxes:
        [x, y, w, h] = box
        if (True
            #w >= CONTOUR_MIN_SIZE[0] / nm_p_pixel
            #and h >= CONTOUR_MIN_SIZE[1] / nm_p_pixel
        ):
            # Extract the ROI and resize it to a square
            roi, x_roi, y_roi, _ = extract_roi(gray, x, y, x + w, y + h)
            new_size = int(roi_nm_size / nm_p_pixel)
            
            # Remove nearby duplicates from brightness centering
            x_b, y_b = locate_brightest_pixel(roi)
            
            
            area_check = False
            for x_test in range(-cross_size * 2, cross_size * 2 + 1):
                if area_check:
                    break
                for y_test in range(-cross_size * 2, cross_size * 2 + 1):
                    if (
                        x_b + x_roi + x_test,
                        y_b + y_roi + y_test,
                    ) in brightest_locations:
                        area_check = True
                        break
            if area_check:
                continue
            
            
            brightest_locations.add((x_b + x_roi, y_b + y_roi))
            
            # Perform the cross check
            prediction, new_x, new_y, roi_preprocessed = cross_check_prediction(
                model,
                gray,
                x_roi,
                y_roi,
                x_b,
                y_b,
                new_size,
                cross_size,
                scan_debug,
            )
            
            #throw out items that are too tall, regardless of prediction value by setting prediction to 0
            if (i in tall_idxs):
                prediction = 0.0
                
            i += 1
                
            cls = 1 if prediction >= sharp_prediction_threshold else 0
            #cls = 1 if prediction >= SHARP_PREDICTION_THRESHOLD else 0
            if scan_debug:
                print(f"Class: {CLASS_NAMES[cls]}, Prediction: {prediction}")

            # Count the number of contours
            total_bonds += 1
            total_cls[cls] += 1

            # Store the additional information
            roi_locations.append(
                {
                    "x": new_x,
                    "y": new_y,
                    "prediction": prediction,
                }
            )

            if scan_debug:
                # Draw colored bounding boxes
                cv2.rectangle(
                    img,
                    (new_x, new_y),
                    (new_x + new_size, new_y + new_size),
                    GREEN if cls else RED,
                    0,
                )
                # Show a live preview
                cv2.imshow("Scan", img)
                cv2.imshow("Contrast", img_contrast)
                cv2.imshow("Edges", edged_contrast)
                cv2.waitKey(1)

            if roi_debug:
                cv2.imshow("Scan", img)
                cv2.imshow("ROI2", roi_preprocessed[0])
                cv2.waitKey(0)
                cv2.destroyAllWindows()

    if scan_debug:
        decision = 0 if total_cls[0] > total_cls[1] else 1
        percent = total_cls[1] / total_bonds * 100 if total_bonds > 0 else 0
        print(f"Total bonds: {total_bonds}")
        print(f"Total sharp: {total_cls[1]}")
        print(f"Total dull: {total_cls[0]}")
        print(f"Overall Decision: {CLASS_NAMES[decision]}")
        print(f"Sharp Percentage: {percent:.4f}%")

        cv2.imshow("Scan", img)
        cv2.imshow("Contrast", img_contrast)
        cv2.imshow("Edges", edged_contrast)
        cv2.waitKey(0)
        cv2.destroyAllWindows()

    if total_bonds == 0:
        print('No bonds detected in image.  But who cares?')
        output = {
            "sharp": 0,
            "dull": -1,
            "total": -1,
            "roi_data": {
                "constants": {"nm_size": 0, "pixel_size": 0},
                "locations": [],
            },
        }
        #raise ValueError("No bonds detected in the image.")
        return output
    
    print('num roi locations: ' + str(len(roi_locations)))
    output = {
        "sharp": total_cls[1],
        "dull": total_cls[0],
        "total": total_cls[0] + total_cls[1],
        "roi_data": {
            "constants": {"nm_size": float(roi_nm_size), "pixel_size": new_size},
            "locations": roi_locations,
        },
    }

    return output
    
    

def detect_tip_using_contours(
    img: np.ndarray,
    scan_nm,
    model,
    roi_nm_size=2,
    cross_size=0,
    contrast=1,
    rotation=45,
    scan_debug=False,
    roi_debug=False,
    z_range=1,
    min_height=0,
    max_height=0.2,
    sharp_prediction_threshold=0.5
) -> dict:
    """Detects the tip in the given image.

    Parameters:
        img (ndarray): Image to process
        scan_nm (float): Scan size in nm
        model: Prediction model
        roi_nm_size (int, optional): Size of the region of interest in nm.
        cross_size (int, optional): Size of the cross for roi cross check.
        contrast (int, optional): Contrast adjustment factor.
        rotation (float, optional): Rotation angle for the image.
        scan_debug (bool, optional): Enable debugging for finished scans.
        roi_debug (bool, optional): Enable debugging for each region of interest.

    Returns:
        dict: Dictionary containing the detection results
    """
    print("detecting tip")
    print("z_range:")
    print(z_range)
    
    if rotation != 0:
        img = rotate_image(img, rotation)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
        
    #no dangling bond should be taller than max_height nm, so clip colors "taller" than that and scale image appropriately
    max_gray = np.max(gray)
    min_gray = np.min(gray)
    heights = z_range*(gray - min_gray)/(max_gray - min_gray)  #array of the actual heights
    
    z_cut = max_height/z_range
    print('z_cut: ')
    print(z_cut)
    if z_cut <= 1:
        grayPre = gray
        
        gray = np.clip(gray, 0, int(z_cut*max_gray))
        
        gray = (gray - np.min(gray)) / (np.max(gray) - np.min(gray)) * 255
        gray = np.array(gray, dtype=np.uint8)
               
        
        print(gray)
        print(np.max(gray))
        
        #cv2.namedWindow("grayPre")
        #cv2.imshow("grayPre", cv2.resize(grayPre,(400,400)))
        
        #cv2.namedWindow("gray")
        #cv2.imshow("gray", cv2.resize(gray,(400,400)))
        
        #cv2.waitKey(0)
        #cv2.destroyAllWindows()
        
        img = cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)

    print('getting contours')
    
    contours, img_contrast, edged_contrast = find_contours(img, contrast)
    contours = contours[::-1]
    
    print(len(contours))

    # Remove the contours caused by the rotation
    i = 0
    contours = list(contours)
    while i < len(contours):
        # Draw bounding box for each contour
        x, y, w, h = cv2.boundingRect(contours[i])
        cv2.rectangle(img_contrast, (x, y), (x + w, y + h), YELLOW, 0)

        # Calculate the mode color ratio for each contour
        if (
            rotation != 0
            #and calculate_black_pixel_ratio(gray, (x, y), (x + w, y + h)) > 0
            and calculate_black_pixel_ratio(img, (x, y), (x + w, y + h)) > 0
        ):
            contours.pop(i)
            continue
        i += 1
    
    #if there are no contours left, then we need to return
    if (len(contours) <= 1):
        print('contour length is 0')
        output = {
            "sharp": 0,
            "dull": -1,
            "total": -1,
            "roi_data": {
                "constants": {"nm_size": 0, "pixel_size": 0},
                "locations": [],
            },
        }

        print('output is: ')
        print(output)
        return output
    
    print('more than 0 contours')
    contours = tuple(contours)

    # Merge overlapping contours
    contours = merge_overlapping_contours(contours, overlap_threshold=0)
    
    
    #remove contours that are too tall or too short
    i = 0
    contours = list(contours)
    while i < len(contours):
        x, y, w, h = cv2.boundingRect(contours[i])
        check_heights = heights[y : y + w, x : x + h]
        h_diff = np.max(check_heights)#-np.min(check_heights)
        
        if (h_diff > max_height):
            contours.pop(i)
        elif (h_diff < min_height):
            contours.pop(i)
        
        i += 1
    contours = tuple(contours)
    
    
    # Tqdm setup
    contour_iterator = (
        contours if scan_debug else tqdm(contours, desc="Processing contours ")
    )

    total_bonds = 0
    total_cls = {0: 0, 1: 0}
    nm_p_pixel = scan_nm / img.shape[0]  # Calculate using height
    brightest_locations = set()
    roi_locations = []
    for cnt in contour_iterator:
        x, y, w, h = cv2.boundingRect(cnt)
        if (
            w >= CONTOUR_MIN_SIZE[0] / nm_p_pixel
            and h >= CONTOUR_MIN_SIZE[1] / nm_p_pixel
        ):
            # Extract the ROI and resize it to a square
            roi, x_roi, y_roi, _ = extract_roi(gray, x, y, x + w, y + h)
            new_size = int(roi_nm_size / nm_p_pixel)

            # Remove nearby duplicates from brightness centering
            x_b, y_b = locate_brightest_pixel(roi)
            area_check = False
            for x_test in range(-cross_size * 2, cross_size * 2 + 1):
                if area_check:
                    break
                for y_test in range(-cross_size * 2, cross_size * 2 + 1):
                    if (
                        x_b + x_roi + x_test,
                        y_b + y_roi + y_test,
                    ) in brightest_locations:
                        area_check = True
                        break
            if area_check:
                continue
            brightest_locations.add((x_b + x_roi, y_b + y_roi))

            # Perform the cross check
            prediction, new_x, new_y, roi_preprocessed = cross_check_prediction(
                model,
                gray,
                x_roi,
                y_roi,
                x_b,
                y_b,
                new_size,
                cross_size,
                scan_debug,
            )
            cls = 1 if prediction >= sharp_prediction_threshold else 0
            #cls = 1 if prediction >= SHARP_PREDICTION_THRESHOLD else 0
            if scan_debug:
                print(f"Class: {CLASS_NAMES[cls]}, Prediction: {prediction}")

            # Count the number of contours
            total_bonds += 1
            total_cls[cls] += 1

            # Store the additional information
            roi_locations.append(
                {
                    "x": new_x,
                    "y": new_y,
                    "prediction": prediction,
                }
            )

            if scan_debug:
                # Draw colored bounding boxes
                cv2.rectangle(
                    img,
                    (new_x, new_y),
                    (new_x + new_size, new_y + new_size),
                    GREEN if cls else RED,
                    0,
                )
                # Show a live preview
                cv2.imshow("Scan", img)
                cv2.imshow("Contrast", img_contrast)
                cv2.imshow("Edges", edged_contrast)
                cv2.waitKey(1)

            if roi_debug:
                cv2.imshow("Scan", img)
                cv2.imshow("ROI2", roi_preprocessed[0])
                cv2.waitKey(0)
                cv2.destroyAllWindows()

    if scan_debug:
        decision = 0 if total_cls[0] > total_cls[1] else 1
        percent = total_cls[1] / total_bonds * 100 if total_bonds > 0 else 0
        print(f"Total bonds: {total_bonds}")
        print(f"Total sharp: {total_cls[1]}")
        print(f"Total dull: {total_cls[0]}")
        print(f"Overall Decision: {CLASS_NAMES[decision]}")
        print(f"Sharp Percentage: {percent:.4f}%")

        cv2.imshow("Scan", img)
        cv2.imshow("Contrast", img_contrast)
        cv2.imshow("Edges", edged_contrast)
        cv2.waitKey(0)
        cv2.destroyAllWindows()

    if total_bonds == 0:
        raise ValueError("No bonds detected in the image.")

    output = {
        "sharp": total_cls[1],
        "dull": total_cls[0],
        "total": total_cls[0] + total_cls[1],
        "roi_data": {
            "constants": {"nm_size": float(roi_nm_size), "pixel_size": new_size},
            "locations": roi_locations,
        },
    }

    return output
