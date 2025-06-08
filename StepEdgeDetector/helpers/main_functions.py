import cv2
import matplotlib.pyplot as plt
import numpy as np
from skimage import filters, feature, morphology, segmentation
from scipy import ndimage as ndi
import random
import gdstk

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

def find_contours(gray, blur, lowResolution, thickness):
    if lowResolution:
        edges_M = feature.canny(gray, sigma=blur, high_threshold=100) # Find contours while adding blur
        elevation_map = morphology.dilation(edges_M, morphology.disk(thickness, dtype=float))
        #elevation_map = filters.sobel(edges_M, mode='constant', cval=1) # Apply Sobel Filter (adds blur to found edges)
        return elevation_map.astype(bool)
    else:
        edges_M = feature.canny(gray, sigma=blur) # Find contours while adding blur
        elevation_map = filters.sobel(edges_M) # Apply Sobel Filter (adds blur to found edges)
        #filled = ndi.binary_fill_holes(elevation_map) # Fill dangling bonds
        #cleaned = morphology.remove_small_objects(filled, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
        #return cleaned
        elevation_map = morphology.dilation(elevation_map, morphology.disk(thickness, dtype=float))
        return elevation_map.astype(bool)

def white_tophat(contours, max_pxl, post):
    if(post == 1):
        filled = ndi.binary_fill_holes(contours) # Fill dangling bonds
        contours = morphology.remove_small_objects(filled, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
        footprint = morphology.disk(4) # > thickness of step edge
        w_tophat = morphology.white_tophat(contours, footprint) # erodes away step edges, dilates image back to orig, then finds diff
        cleaned2 = morphology.remove_small_objects(w_tophat, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
        cleaned2 = morphology.dilation(cleaned2, morphology.disk(3))
        cleaned2 = morphology.erosion(cleaned2, morphology.disk(3))
    else:
        cleaned2 = contours
        cleaned2 = morphology.dilation(cleaned2, morphology.disk(1.5, dtype=float))
        cleaned2 = morphology.erosion(cleaned2, morphology.disk(1.5, dtype=float))
    return cleaned2

def color_layers(overlay, cleaned, maximas, numbered):
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
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 1:
                    labels[filled] = [255, 0, 0] # red
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 2:
                    labels[filled] = [0, 255, 0] # green
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 3:
                    labels[filled] = [127, 0, 255] # purple
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 4:
                    labels[filled] = [255, 255, 0] # yellow
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 5:
                    labels[filled] = [255, 0, 255] # pink
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 6:
                    labels[filled] = [255, 128, 0] # orange
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 7:
                    labels[filled] = [128, 128, 128] # grey
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 8:
                    labels[filled] = [0, 255, 255] # cyan
                    numbered[filled] = layers
                    layers = layers + 1
                elif layers == 9:
                    labels[filled] = [255, 255, 255] # white
                    numbered[filled] = layers
                    layers = layers + 1
                else:
                    labels[filled] = [random.randrange(0, 255), random.randrange(0, 255), random.randrange(0, 255)]
                    numbered[filled] = layers
                    #print("ERROR: Too many layers, ran out of colors")
                    layers = layers + 1
                    #return labels, numbered
    if layers != maximas:
        print("WARNING: Number of layers does not match local maximas detected. Consider updating blur parameter")
    return labels, numbered

def detect_litho(labels, img, numbered, blur, threshold, method):
    litho = np.zeros_like(labels)
    img = cv2.convertScaleAbs(img, alpha=1.3, beta=0)
    edges = feature.canny(img)
    layers = np.max(numbered)+1
    roughness = [0]*(layers)
    pxl = [0]*(layers)
    #roughness_score = np.sum(edges > 0)
    #print(roughness_score)
    for i in range(len(numbered)):
        for j in range(len(numbered)):
            for k in range(layers):
                if numbered[i,j] == k:
                    pxl[k] = pxl[k]+1
                    if edges[i,j] > 0:
                        roughness[k] = roughness[k] + 1
    if(method == "minorityLitho"):
        roughness_score = roughness
        max_roughness = np.max(roughness_score)
    elif(method == "majorityLitho"):
        roughness_score = roughness
        max_roughness = np.max(roughness_score)
    else:
        roughness_score = np.divide(roughness, pxl)
        max_roughness = np.max(roughness_score)
    print(roughness_score)
    for i in range(len(labels)):
        for j in range(len(labels[1])):
            r, g, b = labels[i,j]
            if r == 0 and g == 0 and b == 0:
                litho[i,j] = [0, 0, 0]
            else:
                layer = numbered[i,j]
                if method == "majorityLitho":
                    if roughness_score[layer]/max_roughness > threshold: 
                        litho[i,j] = [255, 255, 0]
                else:
                    if roughness_score[layer]/max_roughness < threshold: 
                        litho[i,j] = [255, 255, 0]
    return litho

def detect_steps(img, img_width=None, img_height=None, show_plots=False, show_output=False, blur=2, postprocessing =False, max_pxl=400, nm_from_z=1, roughnessThreshold=0.12, lowResolution=False, find_litho="default", thickness=1.0, verifyGDS=False, scan_settings_x=None,
                    scan_settings_y=None, img_rescale_x=None, img_rescale_y=None, scan_settings_angle=None):
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
    img = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)
    #img = cv2.convertScaleAbs(img, alpha=1.3, beta=0)
    #gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
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
    
    #fig, ax = plt.subplots(1, 7, gridspec_kw={'width_ratios': [1, 1, 1, 1, 1, 1, 1]})
    fig, ax = plt.subplots(2, 5, figsize=(15, 5))
    ax[1,0].remove()
    ax[0,0].imshow(gray, cmap='gray')
    ax[0,0].set_title('Original Image')
    ax[0,0].axis("off")
    #plt.show()
    
    contours = find_contours(gray, blur, lowResolution, thickness)
    ax[0,1].imshow(contours, cmap='gray')
    ax[0,1].set_title('Contours')
    ax[0,1].axis("off")
    
    print('after contours')
	
    # White Tophat Postprocessing
    if(postprocessing):
        cleaned = white_tophat(contours, max_pxl, 1)
    else:
        cleaned = white_tophat(contours, max_pxl, 0)
    ax[0,2].imshow(cleaned, cmap='gray')
    ax[0,2].set_title('Post-processing')
    ax[0,2].axis("off")

    print('after postprocessing')
    #plt.show()
    #return
	
    # Create a colored overlay 
    overlay = np.zeros((img_height, img_width, 3), dtype=np.uint8)
    overlay[cleaned] = [0, 255, 0]  # Set the color for True values (green in this case)
    result = cv2.addWeighted(img, 1, overlay, 0.5, 0)
    ax[0,3].imshow(result)
    ax[0,3].set_title('Detected Step Edges')
    ax[0,3].axis("off")

    print('after overlay')

    # Labelling
    numbered = np.zeros_like(gray)
    labels, numbered = color_layers(overlay, cleaned, len(maximas), numbered)
    ax[0,4].imshow(labels)
    ax[0,4].set_title('Color Coded Regions')
    ax[0,4].axis("off")

    # Detect Patterned Areas
    if find_litho == "off": 
        ax[1,1].remove()
    else:
        litho = detect_litho(labels, gray, numbered, blur, roughnessThreshold, find_litho)
        ax[1,1].imshow(litho)
        ax[1,1].set_title('Detected Lithography')
        ax[1,1].axis("off")

    # Verify Pattern with GDS File
    if verifyGDS:
        #lib = gdstk.read_gds("A:\Sample Logbook\W42 Q7 Device LT SET\control\SET_island.gds", 1e-9)
        lib = gdstk.read_gds("A:\Sample Logbook\W42 Q7 Device LT SET\control\SET.gds", 1e-9)
        print("loaded gds file")
        top = lib.top_level()[0]
        bbox = top.bounding_box()
        print(bbox)
        if bbox is None:
            print("Warning: Cell is empty")
        image = np.zeros((img_height, img_width, 3), dtype=np.uint8)
        if bbox is not None:
            polygons = top.polygons
            polys = []
            for polygon in polygons:
                x, y = zip(*polygon.points)
                poly = []
                for i in range(len(x)):
                    x_vertex = x[i]-scan_settings_x
                    y_vertex = y[i]-scan_settings_y
                    xr = int(((x_vertex*np.cos(scan_settings_angle*(np.pi/180))) - (y_vertex*np.sin((scan_settings_angle+180)*(np.pi/180))))*(img_width/(img_rescale_x)) + (img_width/2))
                    yr = int(((x_vertex*np.sin(scan_settings_angle*(np.pi/180))) + (y_vertex*np.cos((scan_settings_angle+180)*(np.pi/180))))*(img_height/(img_rescale_y)) + (img_height/2))
                    poly.append([xr-1, yr-1])
                polys.append(np.array(poly, np.int32))
            print(polys)
            cv2.fillPoly(image, polys, (255, 255, 0))
            print("after fill")
            ax[1,2].imshow(image)
            ax[1,2].set_title('GDS File')
            ax[1,2].axis("off")

            if find_litho == "off":
                ax[1,3].remove()
            else:
                error = np.zeros_like(image)
                total_error = 0
                image = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
                litho = cv2.cvtColor(litho, cv2.COLOR_RGB2GRAY)
                error_gray = np.abs(image - litho)*2
                for i in range(len(error_gray)):
                    for j in range(len(error_gray[0])):
                        if cleaned[i,j]:
                            error[i,j] = (0,0,0)
                        elif error_gray[i,j] == 60:
                            error[i,j] = (150,0,0)
                            total_error = total_error + 1
                        elif error_gray[i,j] == 196:
                            error[i,j] = (255,0,0)
                            total_error = total_error + 1
                ax[1,3].imshow(error)
                ax[1,3].set_title('Error')
                ax[1,3].axis("off")
                error_percent = total_error/(img_width*img_height)
                print(error_percent)
                if(error_percent < 0.05):
                    print("PASS GDS CHECK")
                else:
                    print("FAIL GDS CHECK")
        else:
            print("Failed to read GDS file")
            ax[1,2].remove()
            ax[1,3].remove()
    else:
        ax[1,2].remove()
        ax[1,3].remove()

    ax[1,4].remove()
    print('before show output')
    # Display the output
    if show_output:
        plt.show()
        print('done showing plot')
        cv2.waitKey(0) 
        cv2.destroyAllWindows()
        



def detect_steps_alt(img, img_width=None, img_height=None, show_plots=False, show_each_mask=False, show_output=False, blur = 3, postprocessing = False, max_pxl=500, nm_from_z=1):
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
    
    print('max dz: ' + str(nm_from_z*(max-min)))
    
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

def auto_detect_edges(img, input_data, show_plots=False):
    
    print("Finding Step Edges ...")
    img_width = int( input_data["img_wdith"] )
    img_height = int( input_data["img_height"] )
    scan_settings_x = float( input_data["scan_settings_x"] )
    scan_settings_y = float( input_data["scan_settings_y"] )
    img_scale_x = float( input_data["img_scale_x"] )
    img_scale_y = float( input_data["img_scale_y"] )
    scan_settings_angle = float( input_data["scan_settings_angle"] )
    litho_img = bool( input_data["litho_img"] )
    gds_path = input_data["gds_path"]
                            
    img = np.array(img).reshape(img_height, img_width)
	
    min = np.min(img)
    max = np.max(img)
    
    gray = ((img - min) / (max - min) * 255).astype(np.uint8)
    img = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)


    # Create a smoothed histogram and determine the maximas
    #smooth_hist = smooth_histogram(create_histogram(gray), smoothing_factor=25)
    smooth_hist = smooth_histogram(create_histogram(gray), smoothing_factor=1)
    maximas = determine_maximas(
        smooth_hist, side_increase_min=10
    )  # x values of the maximas

    # Determine the minimas between the maximas for layer thresholding
    minimas = []
    for m in range(len(maximas) - 1):
        minimas.append(
            determine_minimum_between_points(smooth_hist, maximas[m], maximas[m + 1])
        )
    minimas = sorted(minimas)

    #if show_plots:
    #    plt.plot(smooth_hist)
    #    for maxima in maximas:
    #        plt.axvline(x=maxima, color="g", linestyle="--")
    #    for x_val in minimas:
    #        plt.plot(x_val, smooth_hist[x_val], "ro")
    #    plt.show()
    
    # Setting up parameters ¯\_(ツ)_/¯:
    max_pxl = 400
    if( litho_img == False ):
        if ( img_scale_x < 30 and img_scale_y < 30 ):
            blur = 3
            lowResolution = False
            thickness = 1.0
            postprocessing = True
        elif( img_scale_x <= 150 and img_scale_y <= 150 ):
            blur = 4
            lowResolution = False
            thickness = 1.0
            postprocessing = False
        elif ( img_scale_x <= 400 and img_scale_y <= 400 ):
            blur = 3
            lowResolution = False
            thickness = 1.0
            postprocessing = False
        elif ( img_scale_x <= 600 and img_scale_y <= 600 ):
            blur = 2
            lowResolution = False
            thickness = 1.0
            postprocessing = False
        else:
            blur = 1
            lowResolution = True
            thickness = 1.0
            postprocessing = False
        
        contours = find_contours(gray, blur, lowResolution, thickness)
        if(postprocessing):
            cleaned = white_tophat(contours, max_pxl, 1)
        else:
            cleaned = white_tophat(contours, max_pxl, 0)

        overlay = np.zeros((img_height, img_width, 3), dtype=np.uint8)
        overlay[cleaned] = [0, 255, 0]  # Set the color for True values (green in this case)
        result = cv2.addWeighted(img, 1, overlay, 0.5, 0)

        numbered = np.zeros_like(gray)
        labels, numbered = color_layers(overlay, cleaned, len(maximas), numbered)

        if ( show_plots ):
            fig, ax = plt.subplots(1, 5, figsize=(15, 5))
            ax[0,0].imshow(gray, cmap='gray')
            ax[0,0].set_title('Original Image')
            ax[0,0].axis("off")
            ax[0,1].imshow(contours, cmap='gray')
            ax[0,1].set_title('Contours')
            ax[0,1].axis("off")
            ax[0,2].imshow(cleaned, cmap='gray')
            ax[0,2].set_title('Post-processing')
            ax[0,2].axis("off")
            ax[0,3].imshow(result)
            ax[0,3].set_title('Detected Step Edges')
            ax[0,3].axis("off")
            ax[0,4].imshow(labels)
            ax[0,4].set_title('Color Coded Regions')
            ax[0,4].axis("off")
            plt.show()
            cv2.waitKey(0) 
            cv2.destroyAllWindows()
        
        return cleaned

    else: # Litho Image

        # **********************************************************
        # TODO: - REMOVE SMALL REGIONS
        #       - IGNORE REGIONS COMPLETELY OUTSIDE GDS FILE AREA
        #       - TEST AUTOFAB FUNCTION
        #
        # **********************************************************

        # Parameters loop to find lowest error
        i = 0
        num_iter = 12
        prev_error = 100
        for i in range(num_iter):
            if ( img_scale_x < 400 and img_scale_y < 400 ):
                if ( i < 4 ):
                    blur = i + 1
                    lowResolution = False
                    thickness = 1.0
                    roughnessThreshold = 0.12
                    litho_method = "minorityLitho"
                elif ( i < 8 ):
                    blur = i - 3
                    lowResolution = False
                    thickness = 1.0
                    roughnessThreshold = 0.12
                    litho_method = "default"
                elif ( i < 10 ):
                    blur = i - 5
                    lowResolution = False
                    thickness = 1.3
                    roughnessThreshold = 0.01
                    litho_method = "default"
                else:
                    blur = i - 7
                    lowResolution = False
                    thickness = 1.3
                    roughnessThreshold = 0.2
                    litho_method = "default"
            else:
                if ( i < 3 ):
                    blur = int( i + 1 )
                    lowResolution = False
                    thickness = 1.0
                    roughnessThreshold = 0.15
                    litho_method = "default"
                elif ( i < 6 ):
                    blur = int( i - 2 )
                    lowResolution = False
                    thickness = 1.0
                    roughnessThreshold = 0.01
                    litho_method = "default"
                elif ( i < 9 ):
                    blur = int( i - 5 )
                    lowResolution = True
                    thickness = 1.0
                    roughnessThreshold = 0.5
                    litho_method = "default"
                else:
                    blur = int( i - 8 )
                    lowResolution = True
                    thickness = 1.8
                    roughnessThreshold = 0.5
                    litho_method = "default"
        
            curr_contours = find_contours(gray, blur, lowResolution, thickness)
            curr_cleaned = white_tophat(curr_contours, max_pxl, 0)
            curr_overlay = np.zeros((img_height, img_width, 3), dtype=np.uint8)
            curr_overlay[curr_cleaned] = [0, 255, 0]  # Set the color for True values (green in this case)
            curr_result = cv2.addWeighted(img, 1, curr_overlay, 0.5, 0)
            curr_numbered = np.zeros_like(gray)
            curr_labels, curr_numbered = color_layers(curr_overlay, curr_cleaned, len(maximas), curr_numbered)
            
            
            curr_litho = detect_litho(curr_labels, gray, curr_numbered, blur, roughnessThreshold, litho_method)
            lib = gdstk.read_gds(gds_path, 1e-9)
            top = lib.top_level()[0]
            bbox = top.bounding_box()
            if bbox is None:
                print("Detect Litho: Failed to read GDS File")
            curr_image = np.zeros((img_height, img_width, 3), dtype=np.uint8)
            if bbox is not None:
                polygons = top.polygons
                polys = []
                for polygon in polygons:
                    x, y = zip(*polygon.points)
                    poly = []
                    for i in range(len(x)):
                        x_vertex = x[i]-scan_settings_x
                        y_vertex = y[i]-scan_settings_y
                        #xr = int(((x_vertex*np.cos(scan_settings_angle*(np.pi/180))) - (y_vertex*np.sin((scan_settings_angle+180)*(np.pi/180))))*(img_width/(img_rescale_x)) + (img_width/2))
                        #yr = int(((x_vertex*np.sin(scan_settings_angle*(np.pi/180))) + (y_vertex*np.cos((scan_settings_angle+180)*(np.pi/180))))*(img_height/(img_rescale_y)) + (img_height/2))
                        xr = int(((x_vertex*np.cos(scan_settings_angle*(np.pi/180))) - (y_vertex*np.sin((scan_settings_angle+180)*(np.pi/180))))*(img_width/(img_scale_x)) + (img_width/2))
                        yr = int(((x_vertex*np.sin(scan_settings_angle*(np.pi/180))) + (y_vertex*np.cos((scan_settings_angle+180)*(np.pi/180))))*(img_height/(img_scale_y)) + (img_height/2))
                        poly.append([xr-1, yr-1])
                    polys.append(np.array(poly, np.int32))
                cv2.fillPoly(curr_image, polys, (255, 255, 0))
                curr_error = np.zeros_like(curr_image)
                total_error = 0
                curr_image = cv2.cvtColor(curr_image, cv2.COLOR_RGB2GRAY)
                curr_litho = cv2.cvtColor(curr_litho, cv2.COLOR_RGB2GRAY)
                error_gray = np.abs(curr_image - curr_litho)*2
                for i in range(len(error_gray)):
                    for j in range(len(error_gray[0])):
                        if curr_cleaned[i,j]:
                            curr_error[i,j] = (0,0,0)
                        elif error_gray[i,j] == 60:
                            curr_error[i,j] = (150,0,0)
                            total_error = total_error + 1
                        elif error_gray[i,j] == 196:
                            curr_error[i,j] = (255,0,0)
                            total_error = total_error + 1
                curr_error_percent = total_error/(img_width*img_height)

                # Keep results that yield lowest error
                if (curr_error_percent < prev_error):
                    contours = curr_contours
                    cleaned = curr_cleaned
                    result = curr_result
                    labels = curr_labels
                    litho = curr_litho
                    image = curr_image
                    error = curr_error
                    error_percent = curr_error_percent
                
                # update for next loop
                prev_error = curr_error_percent
            
            else:
                print("Failed to read GDS file")    

        if(error_percent < 0.1):
            pf = True
        else:
            pf = False


        if ( show_plots ):
            fig, ax = plt.subplots(1, 5, figsize=(15, 5))
            ax[0,0].imshow(gray, cmap='gray')
            ax[0,0].set_title('Original Image')
            ax[0,0].axis("off")
            ax[0,1].imshow(contours, cmap='gray')
            ax[0,1].set_title('Contours')
            ax[0,1].axis("off")
            ax[0,2].imshow(cleaned, cmap='gray')
            ax[0,2].set_title('Post-processing')
            ax[0,2].axis("off")
            ax[0,3].imshow(result)
            ax[0,3].set_title('Detected Step Edges')
            ax[0,3].axis("off")
            ax[0,4].imshow(labels)
            ax[0,4].set_title('Color Coded Regions')
            ax[0,4].axis("off")
            ax[1,1].imshow(litho)
            ax[1,1].set_title('Detected Lithography')
            ax[1,1].axis("off")
            ax[1,2].imshow(image)
            ax[1,2].set_title('GDS File')
            ax[1,2].axis("off")
            ax[1,3].imshow(error)
            ax[1,3].set_title('Error')
            ax[1,3].axis("off")
            ax[1,4].remove()
            plt.show()
            cv2.waitKey(0) 
            cv2.destroyAllWindows()
        return litho, error, pf

def auto_detect_creep(litho_error, input_data):

    img_width = float( input_data["img_wdith"] )
    img_height = float( input_data["img_height"] )
    img_scale_x = float( input_data["img_scale_x"] )
    img_scale_y = float( input_data["img_scale_y"] )

    x_pxl_nm = img_width / img_scale_x
    y_pxl_nm = img_height / img_scale_y
    x_origin = int( img_width/2 )
    y_origin = int( img_height/2 )

    x_offset_ovrwttn = 0
    y_offset_ovrwttn = 0
    x_offset_undrwttn = 0
    y_offset_undrwttn = 0

    for i in range(len(litho_error)):
        for j in range(len(litho_error[0])):
            if litho_error[i,j] == (150,0,0):
                if( i > x_origin ):
                    x_offset_undrwttn += x_pxl_nm
                else:
                    x_offset_undrwttn -= x_pxl_nm
                if ( j > y_origin ):
                    y_offset_undrwttn += y_pxl_nm
                else:
                    y_offset_undrwttn -= y_pxl_nm
            elif litho_error[i,j] == (255,0,0):
                if( i < x_origin ):
                    x_offset_ovrwttn += x_pxl_nm
                else:
                    x_offset_ovrwttn -= x_pxl_nm
                if ( j < y_origin ):
                    y_offset_ovrwttn += y_pxl_nm
                else:
                    y_offset_ovrwttn -= y_pxl_nm
    
    x_offset = (x_offset_ovrwttn + x_offset_undrwttn)/2
    y_offset = (y_offset_ovrwttn + y_offset_undrwttn)/2


    return x_offset, y_offset



