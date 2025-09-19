import json

import cv2
import numpy as np

# Load config file
with open("config.json") as f:
    config = json.load(f)

SQUARE_PIXEL_SIZE = config["SQUARE_PIXEL_SIZE"]


def preprocess_image(roi: np.ndarray) -> np.ndarray:
    """Preprocess the image before feeding it to the model.

    Parameters:
        roi (ndarray): Region of interest to preprocess

    Returns:
        ndarray: Preprocessed region of interest
    """
    if roi.shape[0] != SQUARE_PIXEL_SIZE or roi.shape[1] != SQUARE_PIXEL_SIZE:
        roi = cv2.resize(
            roi, (SQUARE_PIXEL_SIZE, SQUARE_PIXEL_SIZE), interpolation=cv2.INTER_LINEAR
        )
    roi = cv2.normalize(roi, None, 0, 255, cv2.NORM_MINMAX)
    roi = roi.astype("float32") / 255.0
    roi = np.expand_dims(roi, axis=-1)
    roi = np.expand_dims(roi, axis=0)
    return roi


def find_contours(
    img: np.ndarray, alpha: float = 1.0
) -> tuple[list[np.ndarray], np.ndarray, np.ndarray]:
    """Processes the image and finds the contours.

    Parameters:
        img (ndarray): Image to find the contours in
        alpha (float): Contrast adjustment factor

    Returns:
        list: Contours found in the image
        ndarray: Image with contrast adjustment applied
        ndarray: Edges found in the image
    """
    img_contrast = cv2.convertScaleAbs(img, alpha=alpha, beta=0)
    gray = cv2.cvtColor(img_contrast, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray, (5, 5), 0)
    edged = cv2.Canny(blurred, 50, 150)
    contours, _ = cv2.findContours(edged, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

    return contours, img_contrast, edged


def extract_roi(
    img: np.ndarray, x1: int, y1: int, x2: int, y2: int
) -> tuple[np.ndarray, int, int, int]:
    """Extracts the region of interest from the image.

    Parameters:
        img (ndarray): Image to extract the region of interest from
        x1 (int): X coordinate of the top left corner of the square
        y1 (int): Y coordinate of the top left corner of the square
        x2 (int): X coordinate of the bottom right corner of the square
        y2 (int): Y coordinate of the bottom right corner of the square

    Returns:
        ndarray: Region of interest
        int: X coordinate of the top left corner of the square
        int: Y coordinate of the top left corner of the square
        int: Size of the square
    """
    # Calculate the size of the square
    square_size = max(x2 - x1, y2 - y1)

    # Calculate the coordinates for the square
    x = x1 + (x2 - x1) // 2 - square_size // 2
    y = y1 + (y2 - y1) // 2 - square_size // 2
    x = max(x, 0)
    y = max(y, 0)

    # Extract the square region of interest
    return img[y : y + square_size, x : x + square_size], x, y, square_size


def resize_roi(
    img: np.ndarray, x: int, y: int, square_size: int, new_size: int
) -> tuple[np.ndarray, int, int, int]:
    """Expands the region of interest to the new size.

    Parameters:
        img (ndarray): Image to extract the region of interest from
        x (int): X coordinate of the top left corner of the original square
        y (int): Y coordinate of the top left corner of the original square
        square_size (int): Current size of the square
        new_size (int): New size of the square

    Returns:
        ndarray: Region of interest
        int: X coordinate of the top left corner of the square
        int: Y coordinate of the top left corner of the square
        int: Size of the square
    """
    x_new = x - (new_size - square_size) // 2
    y_new = y - (new_size - square_size) // 2
    x_new = max(x_new, 0)
    y_new = max(y_new, 0)
    return (
        img[y_new : y_new + new_size, x_new : x_new + new_size],
        x_new,
        y_new,
        new_size,
    )


def locate_brightest_pixel(img: np.ndarray) -> tuple[int, int]:
    """Locates the brightest pixel in the image.

    Finds the average location if there are multiple pixels with the same brightness.

    Parameters:
        img (ndarray): Image to locate the brightest pixel in

    Returns:
        int: X coordinate of the brightest pixel
        int: Y coordinate of the brightest pixel
    """
    max_value = np.max(img)
    y, x = np.where(img == max_value)
    return int(np.mean(x)), int(np.mean(y))


def rotate_image(img: np.ndarray, angle: float) -> np.ndarray:
    """Rotates the image by the specified angle.

    Parameters:
        img (ndarray): Image to rotate
        angle (float): Angle to rotate the image by clockwise

    Returns:
        ndarray: Rotated image
    """
    if len(img.shape) == 3:
        rows, cols, _ = img.shape
    else:
        rows, cols = img.shape
    M = cv2.getRotationMatrix2D((cols / 2, rows / 2), angle, 1)

    borderVal = np.min(img)
    
    # Apply the rotation
    return cv2.warpAffine(
        img,
        M,
        (cols, rows),
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=borderVal
        #borderValue=(0, 0, 0),
    )


def merge_overlapping_contours(
    contours: tuple[np.ndarray], overlap_threshold: float = 0
) -> list[np.ndarray]:
    """Merge contours that overlap by more than a certain threshold.

    Parameters:
        contours (tuple): Contours to merge
        overlap_threshold (float): Threshold for merging the contours

    Returns:
        list: Merged contours
    """
    merged_contours = []
    contours = list(contours)  # Convert tuple to list
    while contours:
        base = contours.pop(0)
        base_rect = cv2.boundingRect(base)
        base_x, base_y, base_w, base_h = base_rect
        merged = False
        i = 0
        while i < len(contours):
            cnt = contours[i]
            cnt_rect = cv2.boundingRect(cnt)
            cnt_x, cnt_y, cnt_w, cnt_h = cnt_rect
            if (
                max(base_x, cnt_x) < min(base_x + base_w, cnt_x + cnt_w)
                and max(base_y, cnt_y) < min(base_y + base_h, cnt_y + cnt_h)
                and (
                    (min(base_x + base_w, cnt_x + cnt_w) - max(base_x, cnt_x))
                    * (min(base_y + base_h, cnt_y + cnt_h) - max(base_y, cnt_y))
                    >= overlap_threshold * base_w * base_h
                )
            ):
                merged_contour = np.vstack((base, cnt))
                contours.pop(i)
                contours.insert(0, merged_contour)
                merged = True
                break
            i += 1
        if not merged:
            merged_contours.append(base)
    return merged_contours


def calculate_black_pixel_ratio(
    img: np.ndarray, pt1: tuple[int, int], pt2: tuple[int, int]
) -> float:
    """Calculate the ratio of black to the total number of pixels in the region.

    Parameters:
        img (ndarray): Image to calculate the black pixel ratio in
        pt1 (tuple): Coordinates of the top left corner of the region
        pt2 (tuple): Coordinates of the bottom right corner of the region

    Returns:
        float: Ratio of black pixels to the total number of pixels
    """
    x1, y1 = pt1
    x2, y2 = pt2
    num_black = np.sum(img[y1:y2, x1:x2] == 0)

    # Divide by 3 if the image is in color
    if len(img.shape) == 3:
        num_black /= 3

    return num_black / ((x2 - x1) * (y2 - y1))


def cross_check_prediction(
    model: object,
    gray: np.ndarray,
    x_roi: int,
    y_roi: int,
    x_b: int,
    y_b: int,
    new_size: int,
    cross_size: int,
    scan_debug: bool,
) -> tuple[float, int, int, int, np.ndarray]:
    """Cross check the prediction by scanning the region in different directions.

    Moves the region of interest in the x and y directions and uses the maximum prediction as the final prediction.

    Parameters:
        model: Prediction model.
        gray (ndarray): Grayscale image.
        x_roi (int): X coordinate of the top left corner of the region of interest.
        y_roi (int): Y coordinate of the top left corner of the region of interest.
        x_b (int): X coordinate of the brightest pixel.
        y_b (int): Y coordinate of the brightest pixel.
        new_size (int): New size of the region of interest.
        cross_size (int): Size of the cross for roi cross check.
        scan_debug (bool): Enable debugging for finished scans.

    Returns:
        float: Maximum prediction.
        int: X coordinate of the top left corner of the region of interest.
        int: Y coordinate of the top left corner of the region of interest.
        ndarray: Preprocessed region of interest.
    """
    cross_predictions = []
    new_x = x_roi + x_b - new_size // 2
    new_y = y_roi + y_b - new_size // 2
    for direction in range(2):
        for shift in range(-cross_size, cross_size + 1):
            roi, _, _, _ = resize_roi(
                gray,
                new_x + shift * direction,
                new_y + shift * (1 - direction),
                new_size,
                new_size,
            )
            if roi.shape[0] == 0 or roi.shape[1] == 0:
                continue
            roi_preprocessed = preprocess_image(roi)
            cross_predictions.append(
                model.predict(roi_preprocessed, verbose=1 if scan_debug else 0)[0][0]
            )
    return np.max(cross_predictions), new_x, new_y, roi_preprocessed
