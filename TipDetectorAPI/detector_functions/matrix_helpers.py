import access2thematrix
import cv2
import numpy as np


def matrix_to_img_array(
    matrix_path: str, trace: int, plane_slopes: tuple = None
) -> np.ndarray:
    """Extracts the image from a matrix file and returns the image as a numpy array.

    Parameters:
        matrix_path (str): Path to the matrix file.
        trace (int): Trace number to extract the image from.
        plane_slopes (tuple): Tuple of the plane slopes (dz_dx, dz_dy) to subtract from the image (default: None).

    Returns:
        ndarray: Image as a numpy array.
    """
    mtrx_data = access2thematrix.MtrxData()
    traces, _ = mtrx_data.open(matrix_path)

    # Check if the dictionary is empty
    if not traces:
        return None

    # Select the first image
    im, _ = mtrx_data.select_image(traces[trace])

    # Normalize the data to 0-255 and reflect over the x-axis (not sure why it's like this)
    if plane_slopes:
        img = subtract_plane(im.data, (im.height, im.width), plane_slopes)
    else:
        img = im.data
    img = (img - np.min(img)) / (np.max(img) - np.min(img)) * 255
    img = np.flipud(img)

    # Convert the image into a cv2 image
    img = np.array(img, dtype=np.uint8)

    # Convert the image to a 3 channel image
    img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)

    return img

def matrix_to_img_array_with_z_range(matrix_path: str, trace: int, plane_slopes: tuple = None):
    mtrx_data = access2thematrix.MtrxData()
    traces, _ = mtrx_data.open(matrix_path)

    # Check if the dictionary is empty
    if not traces:
        return None

    # Select the first image
    im, _ = mtrx_data.select_image(traces[trace])

    # Normalize the data to 0-255 and reflect over the x-axis (not sure why it's like this)
    if plane_slopes:
        img = subtract_plane(im.data, (im.height, im.width), plane_slopes)
    else:
        img = im.data
    z_range = np.max(img)-np.min(img)
    img = (img - np.min(img)) / z_range * 255
    img = np.flipud(img)

    # Convert the image into a cv2 image
    img = np.array(img, dtype=np.uint8)

    # Convert the image to a 3 channel image
    img = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)

    return img,z_range*1e9 #convert z_range from m to nm
    

def get_nm_from_matrix(matrix_path: str) -> float:
    """Extracts the nm from the matrix path.

    Parameters:
        matrix_path (str): Path of the matrix file.

    Returns:
        float: The height of the scan in nm.
    """
    mtrx_data = access2thematrix.MtrxData()
    traces, _ = mtrx_data.open(matrix_path)

    # Check if the dictionary is empty
    if not traces:
        return None

    # Select the first image
    im, _ = mtrx_data.select_image(traces[0])

    # Extract the nm from the matrix path
    return im.height * 1e9


def subtract_plane(img: np.ndarray, img_nm: tuple, plane_slopes: tuple) -> np.ndarray:
    """
    Subtracts a plane from the image with a scaling factor, adjusted for micro-scale data.

    Parameters:
        img (ndarray): Image as a numpy array.
        img_nm (tuple): Tuple of the image dimensions in nm (height, width).
        plane_slopes (tuple): Tuple of the plane slopes (dz_dx, dz_dy) in nm.

    Returns:
        ndarray: Image with the scaled plane subtracted, preserving original scale.
    """
    dz_dx, dz_dy = plane_slopes
    height_nm, width_nm = img_nm

    height_px = img.shape[0]
    width_px = img.shape[1]

    for y in range(img.shape[0]):
        for x in range(img.shape[1]):
            # Convert the pixel coordinates to nm
            x_nm = x * width_nm / width_px
            y_nm = y * height_nm / height_px

            img[y, x] -= dz_dx * x_nm + dz_dy * y_nm

    return img
