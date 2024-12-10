import cv2
import numpy as np
from tqdm import tqdm

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
    z_range=1
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
    print("detecting tip?")
    if rotation != 0:
        img = rotate_image(img, rotation)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

    contours, img_contrast, edged_contrast = find_contours(img, contrast)
    contours = contours[::-1]

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
            and calculate_black_pixel_ratio(img, (x, y), (x + w, y + h)) > 0
        ):
            contours.pop(i)
            continue
        i += 1
    contours = tuple(contours)

    # Merge overlapping contours
    contours = merge_overlapping_contours(contours, overlap_threshold=0)

    # Tqdm setup
    contour_iterator = (
        contours if scan_debug else tqdm(contours, desc="Processing contours")
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
            cls = 1 if prediction >= SHARP_PREDICTION_THRESHOLD else 0
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
