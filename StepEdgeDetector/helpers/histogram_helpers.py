import numpy as np


def create_histogram(img: np.ndarray) -> np.ndarray:
    """
    Creates a histogram of the image's pixel values.

    Args:
        img (np.ndarray): The image to create the histogram of.

    Returns:
        np.ndarray: The histogram of the image.
    """
    hist, _ = np.histogram(img.ravel(), 256, [0, 256])
    return hist


def smooth_histogram(hist: np.ndarray, smoothing_factor=25) -> np.ndarray:
    """Smooths the histogram.

    Args:
        hist (np.ndarray): The histogram to smooth.
        smoothing_factor (int): The amount of smoothing to apply.

    Returns:
        np.ndarray: The smoothed histogram.
    """
    return np.convolve(hist, np.ones(smoothing_factor) / smoothing_factor, mode="same")


def determine_maximas(graph: np.ndarray, side_increase_min=3) -> list:
    """Determines all the maximas in the histogram.

    Args:
        graph (np.ndarray): The smoothed histogram.
        side_increase_min (int): The amount of x values on each side that must be increasing.

    Returns:
        list: The indexes of the maximas from lowest to highest.
    """
    maximas = []
    for x in range(1, len(graph) - 1):
        if graph[x] > graph[x - 1] and graph[x] > graph[x + 1]:
            if all(graph[x - side_increase_min : x] < graph[x]) and all(
                graph[x + 1 : x + side_increase_min + 1] < graph[x]
            ):
                maximas.append(x)
    return maximas


def determine_minimum_between_points(graph: np.ndarray, x1: int, x2: int) -> int:
    """Determines the minimum between two points.

    Args:
        graph (np.ndarray): The graph to search values in.
        x1 (int): The first point.
        x2 (int): The second point.

    Returns:
        int: The index of the minimum value between the two points.
    """
    min_val = graph[x1]
    x_val = x1
    for x in range(x1, x2):
        if graph[x] < min_val:
            min_val = graph[x]
            x_val = x

    return x_val
