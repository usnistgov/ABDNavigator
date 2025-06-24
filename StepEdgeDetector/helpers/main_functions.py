import cv2
import matplotlib.pyplot as plt
import numpy as np
from skimage import filters, feature, morphology, segmentation
from scipy import ndimage as ndi
import random
import gdstk
from scipy.optimize import minimize
from scipy.signal import find_peaks, peak_prominences, peak_widths

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
        

#function to optimize with respect to dz=dzdx,dzdy with the raw image as the fixed argument 
def bg_plane_quality(dz, args):
    img,img_width_nm,img_height_nm = args
    dzdx,dzdy = dz
    
    img_sub = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)
    
    #estimate of the proper vertical range of the image - should be close
    delta_z = np.max(img_sub) - np.min(img_sub)
    
    #set bins to be enough to give a resolution of 0.005 nm
    min_bin_dz = 0.001
    num_bins = int(delta_z/min_bin_dz)
       
    #get the histogram    
    hist, bin_edges = np.histogram(img_sub, bins=num_bins)
    max_idx = np.argmax(hist)  #index of tallest "peak"
    max_val = np.max(hist)  #height of tallest peak
    
    #get full-width half max of tallest peak
    min_val = max_val/2
    
    right_idx = len(hist)-1
    for idx in range(max_idx, len(hist)):
        if hist[idx] < min_val:
            right_idx = idx
            break
    
    left_idx = 0
    for idx in range(max_idx, 0,-1):
        if hist[idx] < min_val:
            left_idx = idx
            break
    
    bin_width = right_idx-left_idx
    width_fract = bin_width*min_bin_dz  #/num_bins
    
    return width_fract


def auto_bg_plane(img, img_width_nm=100, img_height_nm=100):
    #estimate the background plane by getting the average slope
    #resulting in an initial guess for dzdx and dzdy
    dzdx,dzdy = get_bg_plane(img, img_width_nm, img_height_nm)
    
    #minimize the width of the sharpest peak in the image histogram when subtracting off bg plane
    min_plane = minimize(bg_plane_quality, (dzdx,dzdy), args=[img, img_width_nm, img_height_nm], method='Nelder-Mead')
    
    if not min_plane.success:
        print('well that sucks')
        print(min_plane.message)
    
    dzdx,dzdy = min_plane.x
    return [dzdx,dzdy]

def auto_bg(img, img_width_nm=100, img_height_nm=100):
    num_x_px = len(img) #x pixels
    num_y_px = len(img[0]) #y pixels
    
    
    max_px_width = 50
    px_per_x_step = 10
    if num_x_px > max_px_width+2*px_per_x_step:
        x_px_per_win = max_px_width
    else:
        x_px_per_win = num_x_px
        
    win_width_nm = img_width_nm*x_px_per_win/num_x_px
    num_x_steps = 1 + int((num_x_px - x_px_per_win)/px_per_x_step)
    nm_per_x_step = img_width_nm*px_per_x_step/num_x_px  
    
    
    max_px_height = 50
    px_per_y_step = 10
    if num_y_px > max_px_height+2*px_per_y_step:
        y_px_per_win = max_px_height
    else:
        y_px_per_win = num_y_px
        
    win_height_nm = img_height_nm*y_px_per_win/num_y_px
    num_y_steps = 1 + int((num_y_px - y_px_per_win)/px_per_y_step)
    nm_per_y_step = img_height_nm*px_per_y_step/num_y_px
    
    dz_array = []
    
    for x_idx in range(num_x_steps):
        x_start = x_idx*px_per_x_step
        x_end = x_start + x_px_per_win
        x = (x_idx + 0.5)*nm_per_x_step
        
        dz_row = []
        
        for y_idx in range(num_y_steps):
            y_start = y_idx*px_per_y_step
            y_end = y_start + y_px_per_win
            y = (y_idx + 0.5)*nm_per_y_step
            
            win = img[x_start:x_end, y_start:y_end]
            dzdx,dzdy = auto_bg_plane(win, win_width_nm, win_height_nm)
            
            dz_row.append([x, y, dzdx, dzdy])
            
        dz_array.append(dz_row)
        
    print('dz_array' + str(dz_array))
        
    
def detect_steps_alt(img, img_width_nm=100, img_height_nm=100 ):
    
    #run auto_bg
    auto_bg(img, img_width_nm, img_height_nm)
    
    #determine background plane
    dzdx,dzdy = auto_bg_plane(img, img_width_nm, img_height_nm)
    
        

    
    
    '''
    peaks,_ = find_peaks(hist,distance=bin_dist)
    
    
    #use the max peak height to determine height (prominence) cutoffs for other peaks
    cut_fract = 0.1
    max_pk = np.max( peak_prominences(hist, peaks, wlen=bin_dist) )
    height_cut = int(cut_fract*max_pk)
    
    print('height_cut: ' + str(height_cut))
    peaks,pk_info = find_peaks(hist,distance=bin_dist,prominence=height_cut) 
    
    heights = pk_info['prominences']
    right_bases = pk_info['right_bases']
    left_bases = pk_info['left_bases']
    print('heights: ' + str(heights))
    
    #get peak_widths from full width half max
    widths,width_heights,left_ips,right_ips = peak_widths(hist, peaks, rel_height=0.5, prominence_data=(heights,left_bases,right_bases))
    
    print('widths: ' + str(widths))
    plt.plot(peaks, hist[peaks], "x", color='red', label='Detected Peaks')
    '''
    
    '''
    plt.plot(hist)
    plt.axvline(x=left_idx, color='r', linestyle='--')
    plt.axvline(x=right_idx, color='r', linestyle='--')
    plt.show()
    '''
    
    
    #convert to grayscale image
    #img = img_sub
    img = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)
    
    min = np.min(img)
    max = np.max(img)
    gray = ((img - min) / (max - min) * 255).astype(np.uint8)
    
    cv2.namedWindow("gray")
    cv2.imshow("gray", cv2.resize(gray,(400,400)))
    #cv2.imshow("gray", cv2.resize(gray,(400,400)))
    cv2.waitKey(0)
    cv2.destroyAllWindows()


def sub_plane(img0, width_nm, height_nm, dzdx, dzdy):
    img = np.copy(img0)
    num_cols = len(img[0])
    num_rows = len(img)
    
    for xIdx in range(num_rows):
        x = width_nm*xIdx/(num_rows-1)
        
        for yIdx in range(num_cols):
            y = height_nm*yIdx/(num_cols-1)
            
            z = img[xIdx][yIdx] - (dzdx*x + dzdy*y)
            img[xIdx][yIdx] = z
    
    return img
            
    
def get_bg_plane(img, width_nm, height_nm):
    dzdx_ave = 0
    dzdy_ave = 0
    
    num_cols = len(img[0])
    num_rows = len(img)
    
    for yIdx in range(num_cols):
        dzdx_ave += img[num_rows-1][yIdx] - img[0][yIdx]
    
    for xIdx in range(num_rows):
        dzdy_ave += img[xIdx][num_cols-1] - img[xIdx][0]
        
    dzdx_ave /= num_cols*(num_rows-1)
    dzdy_ave /= (num_cols-1)*num_rows
    
    #convert from nm(dz)/px to nm(dz)/nm(dx or dy)
    dzdx_ave *= (num_rows-1)/width_nm
    dzdy_ave *= (num_cols-1)/height_nm
    
    return dzdx_ave, dzdy_ave