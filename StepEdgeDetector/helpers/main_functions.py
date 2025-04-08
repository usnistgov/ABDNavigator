import cv2
import matplotlib.pyplot as plt
import numpy as np
from skimage import filters, feature, morphology, segmentation
from scipy import ndimage as ndi

from helpers.histogram_helpers import (
    create_histogram,
    determine_maximas,
    determine_minimum_between_points,
    smooth_histogram,
)

CV2_WINDOW_OFFSETS = 100, 100
COLORS = [
    (0, 0, 255),  # Red
    (0, 165, 255),  # Orange
    (0, 255, 255),  # Yellow
    (0, 255, 0),  # Green
    (255, 255, 0),  # Cyan
    (255, 0, 255),  # Magenta
]

def find_contours(gray, blur, max_pxl):
    edges_M = feature.canny(gray, sigma=blur) # Find contours while adding blur
    elevation_map = filters.sobel(edges_M) # Apply Sobel Filter (adds blur to found edges)
    filled = ndi.binary_fill_holes(elevation_map) # Fill dangling bonds
    cleaned = morphology.remove_small_objects(filled, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
    return cleaned

def white_tophat(cleaned, max_pxl):
    footprint = morphology.disk(3) # > thickness of step edge
    w_tophat = morphology.white_tophat(cleaned, footprint) # erodes away step edges, dilates image back to orig, then finds diff
    cleaned2 = morphology.remove_small_objects(w_tophat, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
    cleaned2 = morphology.dilation(cleaned2, morphology.disk(3))
    cleaned2 = morphology.erosion(cleaned2, morphology.disk(3))
    return cleaned2

def color_layers(overlay, cleaned, maximas):
    labels = np.zeros_like(overlay)
    layers = 0
    for i in range(len(cleaned)):
        for j in range(len(cleaned[1])):
            r, g, b = labels[i,j]
            if r == 0 and g == 0 and b == 0 and cleaned[i,j] == False:
                filled = segmentation.flood_fill(cleaned, (i,j), 127)
                filled = filled ^ cleaned
                if layers == 0:
                    labels[filled] = [0, 0, 255] # blue
                    layers = layers + 1
                elif layers == 1:
                    labels[filled] = [255, 0, 0] # red
                    layers = layers + 1
                elif layers == 2:
                    labels[filled] = [0, 255, 0] # green
                    layers = layers + 1
                elif layers == 3:
                    labels[filled] = [127, 0, 255] # purple
                    layers = layers + 1
                elif layers == 4:
                    labels[filled] = [255, 255, 0] # yellow
                    layers = layers + 1
                elif layers == 5:
                    labels[filled] = [255, 0, 255] # pink
                    layers = layers + 1
                elif layers == 6:
                    labels[filled] = [255, 128, 0] # orange
                    layers = layers + 1
                elif layers == 7:
                    labels[filled] = [128, 128, 128] # grey
                    layers = layers + 1
                elif layers == 8:
                    labels[filled] = [0, 255, 255] # cyan
                    layers = layers + 1
                elif layers == 9:
                    labels[filled] = [255, 255, 255] # white
                    layers = layers + 1
                else:
                    print("ERROR: Too many layers, ran out of colors")
                    layers = layers + 1
    if layers != maximas:
        print("WARNING: Number of layers does not match local maximas detected. Consider updating blur parameter")
    return labels

def detect_steps(img, img_width=None, img_height=None, show_plots=False, show_each_mask=False, show_output=False, blur = 3, postprocessing = False, max_pxl=500):
    #print('it\'s working!')
    #return
    
    #gray = ((img - min) / (max - min) * 255)
    #gray = ((img - min) / (max - min))
	
    
    #return
	#steps_deteced_img = img.copy()
	
    if img_width is None:
        img_width = int( np.sqrt( len(img) ) )
        img_height = img_width
        print(img_height)
	
    print(img_width)
    img = np.array(img).reshape(img_height, img_width)
	
    min = np.min(img)
    max = np.max(img)
    
    gray = ((img - min) / (max - min) * 255).astype(np.uint8)
    print(gray.shape)
    print(gray)
    #gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    #print(np.array(gray))

    # Create a smoothed histogram and determine the maximas
    #smooth_hist = smooth_histogram(create_histogram(gray), smoothing_factor=25)
    smooth_hist = smooth_histogram(create_histogram(gray), smoothing_factor=1)
    maximas = determine_maximas(
        smooth_hist, side_increase_min=10
    )  # x values of the maximas
    print(f"Maximas/Layers Detected: {len(maximas)}")

    # Determine the minimas between the maximas for layer thresholding
    minimas = []
    for m in range(len(maximas) - 1):
        minimas.append(
            determine_minimum_between_points(smooth_hist, maximas[m], maximas[m + 1])
        )
    minimas = sorted(minimas)

    if show_plots:
        plt.plot(smooth_hist)
        for maxima in maximas:
            plt.axvline(x=maxima, color="g", linestyle="--")
        for x_val in minimas:
            plt.plot(x_val, smooth_hist[x_val], "ro")
        plt.show()

    # Create a mask based on the threshold values
    #masked_areas = None
    #masks = []
    #for color_val in minimas + [256]:
    #    mask = np.zeros_like(gray)
    #    mask[gray < color_val] = 255

        # Subtract the previous masked areas from the current mask to display only the current layer
    #    if masked_areas is not None:
    #        mask = cv2.bitwise_and(mask, cv2.bitwise_not(masked_areas))
    #        masked_areas = cv2.bitwise_or(masked_areas, mask)
    #    else:
    #        masked_areas = mask

    #    masks.append(mask)

        # Apply edge detection
    #    blur = cv2.GaussianBlur(mask, (5, 5), 25)
    #    edges = cv2.Canny(blur, 50, 150)
   #     contours, _ = cv2.findContours(
   #         edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
   #     )
   #     contours, _ = cv2.findContours(
   #         edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
   #     )


        # Show the mask and edges
    #    if show_each_mask:
    #        cv2.imshow("mask", mask)
    #        cv2.imshow("contours", edges)
    #        cv2.moveWindow("mask", CV2_WINDOW_OFFSETS[0], CV2_WINDOW_OFFSETS[1])
    #        cv2.moveWindow(
    #            "contours", CV2_WINDOW_OFFSETS[0] + mask.shape[1], CV2_WINDOW_OFFSETS[1]
    #        )
    #        cv2.waitKey(0)
    #        cv2.destroyAllWindows()

        # Draw line over the detected edge
    #    cv2.drawContours(steps_deteced_img, contours, -1, (25, 255, 25), 2)
    print('before plot stuff')
    
    fig, ax = plt.subplots(1, 5, figsize=(15, 5))
    #fig, ax = plt.subplots(1, 1, figsize=(15, 5))
    ax[0].imshow(gray, cmap='gray')
    ax[0].set_title('Original Image')
    ax[0].axis("off")
    #plt.show()
    
    cleaned = find_contours(gray, blur, max_pxl)
    ax[1].imshow(cleaned, cmap='gray')
    ax[1].set_title('Contours')
    ax[1].axis("off")
    
    print('after contours')
	
    # White Tophat Postprocessing
    if(postprocessing):
        cleaned = white_tophat(cleaned, max_pxl)
        ax[2].imshow(cleaned, cmap='gray')
        ax[2].set_title('Post-processing')
        ax[2].axis("off")
    else:
        ax[2].remove()

    print('after postprocessing')
    plt.show()
    return
	
    # Create a colored overlay 
    overlay = np.zeros_like(img)
    overlay[cleaned] = [0, 255, 0]  # Set the color for True values (green in this case)
    result = cv2.addWeighted(img, 1, overlay, 0.5, 0)
    ax[3].imshow(result)
    ax[3].set_title('Detected Step Edges')
    ax[3].axis("off")

    # Labelling
    labels = color_layers(overlay, cleaned, len(maximas))
    ax[4].imshow(labels)
    ax[4].set_title('Color Coded Regions')
    ax[4].axis("off")

    print('before show output')
    # Display the output
    if show_output:
        plt.show()
        print('done showing plot')
        cv2.waitKey(0) 
        cv2.destroyAllWindows()
        
        # Create a debug mask to display the combined masks
        #debug_mask = np.zeros_like(img)
        #for i, mask in enumerate(masks):
        #    debug_mask[mask == 255] = COLORS[i % len(COLORS)]

        # Display the images in a looped window
        #curr = 0
        #windows = [img, debug_mask, steps_deteced_img]
        #cv2.imshow("image", windows[curr])
        #cv2.moveWindow("image", CV2_WINDOW_OFFSETS[0], CV2_WINDOW_OFFSETS[1])
        #while True:
        #    cv2.imshow("image", windows[curr])
        #    key = cv2.waitKey(0)
        #    if key == ord("q"):
        #        break
        #    # If a key is pressed, increment the current window
        #    elif key == ord("a"):
        #        curr = (curr - 1) % len(windows)
        #    elif key == ord("d"):
        #        curr = (curr + 1) % len(windows)

       # cv2.destroyAllWindows()
