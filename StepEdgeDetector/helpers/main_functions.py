import cv2
import matplotlib.pyplot as plt
import numpy as np
from skimage import filters, feature, morphology, segmentation, restoration
from skimage.metrics import structural_similarity as ssim
from scipy import ndimage as ndi
import random
import gdstk
import datetime
from scipy.optimize import minimize, curve_fit
from scipy.signal import find_peaks, peak_prominences, peak_widths
from scipy.fft import fft2, ifft2
from scipy.ndimage import gaussian_filter

from helpers.histogram_helpers import (
    create_histogram,
    determine_maximas,
    determine_minimum_between_points,
    smooth_histogram,
)

from sympy.logic.boolalg import false
from matplotlib.streamplot import _euler_step
from tensorflow.python.keras.engine.training_arrays_v1 import _get_num_samples_or_steps

import multiprocessing
import itertools
import sys
import time
import io
from contextlib import contextmanager

CV2_WINDOW_OFFSETS = 100, 100
COLORS = [
    (0, 0, 255),  # Red
    (0, 165, 255),  # Orange
    (0, 255, 255),  # Yellow
    (0, 255, 0),  # Green
    (255, 255, 0),  # Cyan
    (255, 0, 255),  # Magenta
]

@contextmanager
def remove_prints():
    old_stdout = sys.stdout
    sys.stdout = io.StringIO()
    try:
        yield
    finally:
        sys.stdout = old_stdout

def find_contours(gray, blur):
    sobel_x = filters.sobel_h(gray)
    sobel_y = filters.sobel_v(gray)
    gradient_magnitude = np.sqrt(sobel_x**2 + sobel_y**2)
    low_threshold = np.percentile(gradient_magnitude, 99)*100
    high_threshold = np.percentile(gradient_magnitude, 100)*100
    print(low_threshold)
    print(high_threshold)
    edges_M = feature.canny(gray, sigma=blur, low_threshold=low_threshold, high_threshold=high_threshold) # Find contours while adding blur
    elevation_map = filters.sobel(edges_M) # Apply Sobel Filter (adds blur to found edges)
    return elevation_map.astype(bool)

def post_processing(contours, max_pxl, tophat, thickness):
    if(tophat == 1):
        filled = ndi.binary_fill_holes(contours) # Fill dangling bonds
        cleaned = morphology.remove_small_objects(filled, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
        footprint = morphology.disk(3) # > thickness of step edge
        w_tophat = morphology.white_tophat(cleaned, footprint) # erodes away step edges, dilates image back to orig, then finds diff
        cleaned2 = morphology.remove_small_objects(w_tophat, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
        cleaned2 = morphology.dilation(cleaned2, morphology.disk(thickness+1))
        cleaned2 = morphology.erosion(cleaned2, morphology.disk(thickness+1))
    else:
        cleaned = np.array(contours, dtype=bool)       
        cleaned = morphology.dilation(cleaned, morphology.disk(thickness, dtype=float))
        cleaned = morphology.erosion(cleaned, morphology.disk(thickness, dtype=float))
        cleaned2 = morphology.remove_small_objects(cleaned, min_size = max_pxl) # remove small objects (i.e. dangling bonds)
    return cleaned2

def color_layers(overlay, cleaned, numbered):
    labels = np.zeros_like(overlay)
    layers = 0
    error_msg = 0
    for i in range(len(cleaned)):
        for j in range(len(cleaned[0])):
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
                    layers = layers + 1
                    if (layers == 256):
                        layers = 255
                        error_msg = 1
    if error_msg == 1:
        print("WARNING: Too many layers")
    
    return labels, numbered

def detect_litho(labels, img, numbered, threshold, method, neg, contrast, thickness, cleaned, max_pxl):
    litho_clr = np.zeros_like(labels)
    litho = np.zeros_like(img)
    contrast_img = cv2.convertScaleAbs(img, alpha=contrast, beta=0)   
    edges = filters.sobel(contrast_img)
    edges = (edges >= np.percentile(edges, 60)).astype(int)
    layers = np.max(numbered)+1
    roughness = [0]*(layers)
    pxl = [0]*(layers)
    height = [0]*(layers)
    height = np.array(height, dtype=np.float64)
    avg_height = [0]*(layers)
    for i in range(len(numbered)):
        for j in range(len(numbered[0])):
            for k in range(layers):
                if numbered[i,j] == k:
                    pxl[k] = pxl[k]+1
                    height[k] = height[k] + img[i,j]
                    if edges[i,j] > 0:
                        roughness[k] = roughness[k] + 1
    print(roughness)
    if(method == "minorityLitho"):
        roughness_score = roughness
        max_roughness = np.max(roughness_score)
        for k in range(layers):
            avg_height[k] = height[k]/pxl[k] 
            if pxl[k] < 50:
                roughness_score[k] = 100*max_roughness
    elif(method == "majorityLitho"):
        roughness_score = roughness
        max_roughness = np.max(roughness_score)  
        for k in range(layers):
            avg_height[k] = height[k]/pxl[k] 
            if pxl[k] < 50:
                roughness_score[k] = 0
    else:
        roughness_score = np.divide(roughness, pxl)       
        max_roughness = np.max(roughness_score)
        for k in range(layers):
            avg_height[k] = height[k]/pxl[k] 
            if pxl[k] < 50:
                if(method == "rough"):
                    roughness_score[k] = 0
                else:
                    roughness_score[k] = 100*max_roughness
    print(roughness_score)
    min_height_avg = np.min(avg_height)
    max_height_avg = np.max(avg_height)
    for i in range(len(labels)):
        for j in range(len(labels[0])):
            r, g, b = labels[i,j]
            if r == 0 and g == 0 and b == 0:
                litho[i,j] = 0
            else:
                layer = numbered[i,j]
                if (method == "majorityLitho" or method == "rough"):
                    if roughness_score[layer]/max_roughness > threshold:
                        if(neg == 0):
                            if (avg_height[layer] != min_height_avg):
                                litho[i,j] = 1
                        else:
                            if (avg_height[layer] != max_height_avg):
                                litho[i,j] = 1
                else:
                    if roughness_score[layer]/max_roughness < threshold:
                        if(neg == 0):
                            if (avg_height[layer] != min_height_avg):
                                litho[i,j] = 1
                        else:
                            if (avg_height[layer] != max_height_avg):
                                litho[i,j] = 1
                
    litho = np.array(litho, dtype=bool)       
    litho = morphology.dilation(litho, morphology.disk(thickness, dtype=float))
    litho_clr[litho] = [255, 255, 0]
    litho_clr_rstr = restoration.inpaint_biharmonic(litho_clr, cleaned, channel_axis=-1)
    litho_clr = (litho_clr_rstr*255).astype(np.uint8)
    litho_gray = cv2.cvtColor(litho_clr, cv2.COLOR_BGR2GRAY)
    litho_mask = (litho_gray >= 166).astype(bool)
    height, width = img.shape
    border_width = 6
    cleaned[border_width:height-border_width, border_width:width-border_width] = 0
    cleaned = morphology.remove_small_objects(cleaned, min_size = max_pxl) 
    litho_mask[cleaned] = 0
    litho_mask = ndi.binary_fill_holes(litho_mask)
    litho_clr.fill(0)
    litho_clr[litho_mask] = [255, 255, 0]
    
    return litho_clr, edges, litho_mask
    
def detect_dual_litho(labels, img, numbered, threshold, method):
    litho = np.zeros_like(labels)
    litho_grayscale = detect_litho(labels, img, numbered, threshold, method, 0)
    neg_img = 255 - img
    litho_neg_scale = detect_litho(labels, neg_img, numbered, threshold, method, 1)
    for i in range(len(litho)):
        for j in range(len(litho[0])):
            if ((litho_grayscale[i,j,0] == 255) or (litho_neg_scale[i,j,0] == 255)):
                litho[i,j] = [255, 255, 0]
                
    return litho

def auto_detect_creep(litho, curr_image, img_width, img_height, img_scale_x, img_scale_y, scan_settings_angle, overlap):
    # loop and count index for every nonzero pixel and then divide by width/height
    litho_x_pxl_offset = 0
    litho_x_pxl_tot = 0
    litho_y_pxl_offset = 0
    litho_y_pxl_tot = 0
    gds_x_pxl_offset = 0
    gds_x_pxl_tot = 0
    gds_y_pxl_offset = 0
    gds_y_pxl_tot = 0
    max_y_gds = 0
    max_y_litho = 0
    max_x_gds = 0
    max_x_litho = 0
    min_y_litho = 0
    min_x_litho = 0
    min_y_gds = 0
    min_x_gds = 0
    curr_max_y_litho = 0
    curr_max_x_litho = 0
    curr_min_y_litho = 0
    curr_min_x_litho = 0
    litho_min_skip = 0
    gds_min_skip = 0
        
    for i in range(len(litho)):
        litho_y_skip = 0
        gds_y_skip = 0
        for j in range(len(litho[0])):
            if(litho[i,j] != 0 and litho_y_skip == 0):
                litho_y_pxl_offset += (i+1-img_height/2)
                litho_y_pxl_tot += 1
                litho_y_skip = 1
                curr_max_y_litho = i
                if(litho_min_skip == 0):
                    curr_min_y_litho = i
                    litho_min_skip = 1
                if(i == img_height - 1):
                    max_y_litho = 1
                if(i == 0):
                    min_y_litho = 1
            if(curr_image[i,j] != 0 and gds_y_skip == 0):
                gds_y_pxl_offset += (i+1-img_height/2)
                gds_y_pxl_tot += 1
                gds_y_skip = 1
                if(gds_min_skip == 0):
                    curr_min_y_gds = i
                    gds_min_skip = 1
                if(i == img_height - 1):
                    max_y_gds = 1
                if(i == 0):
                    min_y_gds = 1
    litho_min_skip = 0
    gds_min_skip = 0
    for j in range(len(litho[0])):
        litho_x_skip = 0
        gds_x_skip = 0
        for i in range(len(litho)):
            if(litho[i,j] != 0 and litho_x_skip == 0):
                litho_x_pxl_offset += (j+1-img_width/2)
                litho_x_pxl_tot += 1
                litho_x_skip = 1
                curr_max_x_litho = j
                if(litho_min_skip == 0):
                    curr_min_x_litho = j
                    litho_min_skip = 1
                if(j == img_width - 1):
                    max_x_litho = 1
                if(j == 0):
                    min_x_litho = 1
            if(curr_image[i,j] != 0 and gds_x_skip == 0):
                gds_x_pxl_offset += (j+1-img_width/2)
                gds_x_pxl_tot += 1
                gds_x_skip = 1
                if(gds_min_skip == 0):
                    curr_min_x_gds = j
                    gds_min_skip = 1
                if(j == img_width - 1):
                    max_y_gds = 1
                if(j == 0):
                    min_x_gds = 1
    if ( gds_x_pxl_tot != 0 and gds_y_pxl_tot != 0 ):
        gds_x = (gds_x_pxl_offset/gds_x_pxl_tot)*(img_scale_x/img_width)
        gds_y = (gds_y_pxl_offset/gds_y_pxl_tot)*(img_scale_y/img_height)
    else:
        gds_x = 0
        gds_y = 0
    if ( litho_x_pxl_tot != 0 and litho_y_pxl_tot != 0 ):
        litho_x = (litho_x_pxl_offset/litho_x_pxl_tot)*(img_scale_x/img_width)
        litho_y = (litho_y_pxl_offset/litho_y_pxl_tot)*(img_scale_y/img_height)
    else:
        litho_x = gds_x
        litho_y = gds_y
    
    x_offset_0 = gds_x - litho_x
    y_offset_0 = gds_y - litho_y
    
    
    if ( (max_y_gds == 1) and (max_y_litho == 0) ):
        y_offset_0 = (curr_max_y_litho - img_height - 1)*(img_scale_y/img_height)
    if ( (max_x_gds == 1) and (max_x_litho == 0) ):
        x_offset_0 = (curr_max_x_litho - img_width - 1)*(img_scale_x/img_width)
    if ( (min_y_gds == 1) and (min_y_litho == 0) ):
        y_offset_0 = -(curr_min_y_litho)*(img_scale_y/img_height)
    if ( (min_x_gds == 1) and (min_x_litho == 0) ):
        x_offset_0 = -(curr_min_x_litho)*(img_scale_x/img_width)
    
    
    zero_score = 0
    if (overlap):
        if ( (max_y_gds == 1) and (max_y_litho != 1) ):
            zero_score = 1
        elif ( (min_y_gds == 1) and (min_y_litho != 1) ):
            zero_score = 1
        
    x_offset = (x_offset_0*np.cos(scan_settings_angle*(np.pi/180))) - (y_offset_0*np.sin((scan_settings_angle)*(np.pi/180)))
    y_offset = (x_offset_0*np.sin(scan_settings_angle*(np.pi/180))) + (y_offset_0*np.cos((scan_settings_angle)*(np.pi/180)))

    return x_offset, y_offset, x_offset_0, y_offset_0, zero_score
 
def auto_detect_creep_fft(litho, curr_image, img_width, img_height, img_scale_x, img_scale_y, scan_settings_angle, overlap):
   
    ideal_litho = np.fft.fftshift(fft2(curr_image))
    detected_litho = np.fft.fftshift(fft2(litho))
    cross_spectrum = ideal_litho * np.conj(detected_litho)
    correlation = np.fft.ifft2(cross_spectrum).real
    peaks, _ = find_peaks(correlation.flatten())
    if not peaks.size:
        return 0,0,0,0,1
        
    peak_index = peaks[np.argmax(correlation.flatten()[peaks])]
    y_shift, x_shift = np.unravel_index(peak_index, correlation.shape)
    x_offset_0 = -(x_shift/2 + 1 - img_width/2)*(img_scale_x/img_width)
    y_offset_0 = -(y_shift/2 + 1 - img_height/2)*(img_scale_y/img_height)
    
    x_offset = (x_offset_0*np.cos(scan_settings_angle*(np.pi/180))) - (y_offset_0*np.sin((scan_settings_angle)*(np.pi/180)))
    y_offset = (x_offset_0*np.sin(scan_settings_angle*(np.pi/180))) + (y_offset_0*np.cos((scan_settings_angle)*(np.pi/180)))
    
    zero_score = 0
    

    return x_offset, y_offset, x_offset_0, y_offset_0, zero_score

def creep_correct(curr_litho, curr_x_pxl_offset, curr_y_pxl_offset, img_width, img_height):
    curr_litho_offset = np.zeros_like(curr_litho)
    for i in range(len(curr_litho)):
        for j in range(len(curr_litho[0])):
            if(curr_litho[i,j] != 0):
                if( (j + curr_x_pxl_offset < img_width) and (i + curr_y_pxl_offset < img_height) and (j + curr_x_pxl_offset > 0) and (i + curr_y_pxl_offset > 0) ):
                    curr_litho_offset[i + curr_y_pxl_offset, j + curr_x_pxl_offset] = curr_litho[i,j]
    return curr_litho_offset

def error_score(error_gray, curr_image_color, img_width, img_height, zero_score):
    total_error = 0
    curr_error = np.zeros_like(curr_image_color)
    for i in range(len(error_gray)):
        for j in range(len(error_gray[0])):                      
            if error_gray[i,j] == 60:
                curr_error[i,j] = (150,0,0)
                total_error = total_error + 1
            elif error_gray[i,j] == 196:
                curr_error[i,j] = (255,0,0)
                total_error = total_error + 1                     
    curr_error_percent = total_error/(img_width*img_height)
    if (zero_score == 1):
        curr_error_percent = 1
    curr_score = (1-curr_error_percent)
    return curr_score, curr_error

def auto_score(params):
    other_params, blurParam, roughParam, contrastParam = params
    with remove_prints():
        similarityScores, litho, curr_x_offset, curr_y_offset = auto_detect_litho_score(other_params, blur=blurParam, tophat=False, roughnessThreshold=roughParam, contrast=contrastParam, save_output=True)
    return similarityScores

def auto_detect_litho(params):
    other_params, blurParam, roughParam, contrastParam = params
    with remove_prints():
        similarityScores, litho, curr_x_offset, curr_y_offset = auto_detect_litho_score(other_params, blur=blurParam, tophat=False, roughnessThreshold=roughParam, contrast=contrastParam, save_output=True)
    return litho, curr_x_offset, curr_y_offset

def auto_detect_litho_score(other_params, blur=3, tophat=False, roughnessThreshold=0.15, contrast=2, save_output=False):    
    img, ideal_litho, img_scale_x, img_scale_y, scan_settings_angle, overlap = other_params
    
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    height, width = gray.shape
    thickness = (height*width)/(100000.0)
    if thickness < 1:
        thickness = 1
    max_pxl = round((height+width)/4)

    if(save_output):
        fig, ax = plt.subplots(3, 3)
        ax[0,0].imshow(gray, cmap='gray')
        ax[0,0].set_title('Original Image')
        ax[0,0].axis("off")
    
    cleaned = find_contours(gray, blur)
    
    # White Tophat Postprocessing
    if(tophat):
        cleaned = post_processing(cleaned, max_pxl*2, tophat, thickness)
    else:
        cleaned = post_processing(cleaned, max_pxl, tophat, thickness)

    # Create a colored overlay 
    overlay = np.zeros_like(img)
    cleaned = np.array(cleaned, dtype=bool)
    overlay[cleaned] = [0, 255, 0]  # Set the color for True values (green in this case)
    img = cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)
    result = cv2.addWeighted(img, 1, overlay, 0.5, 0)
    
    if(save_output):
        ax[0,1].imshow(result)
        ax[0,1].set_title('Detected Step Edges')
        ax[0,1].axis("off")

    # Labelling
    numbered = np.zeros_like(gray)
    labels, numbered = color_layers(overlay, cleaned, numbered)
    if(save_output):
        ax[0,2].imshow(labels)
        ax[0,2].set_title('Color Coded Regions')
        ax[0,2].axis("off")

    # Detect Litho
    litho_method = "default"
    litho, edges, litho_mask = detect_litho(labels, gray, numbered, roughnessThreshold, litho_method, 0, contrast, thickness, cleaned, max_pxl)
    result = cv2.addWeighted(img, 0.5, litho, 0.5, 0)
    if(save_output):
        ax[1,1].imshow(edges, cmap='gray')
        ax[1,1].set_title('Contours 2')
        ax[1,1].axis("off")
        ax[1,2].imshow(result)
        ax[1,2].set_title('Detected Litho')
        ax[1,2].axis("off")
    image1 = "tmp/image1.jpg"
    image2 = "tmp/image2.jpg"
    if np.all(~litho_mask):
        score = 0
        litho = 0
    else:
        curr_litho = cv2.cvtColor(litho, cv2.COLOR_RGB2GRAY)  
        cv2.imwrite(image2, curr_litho)
        img1 = cv2.imread(image1, cv2.IMREAD_GRAYSCALE)
        img2 = cv2.imread(image2, cv2.IMREAD_GRAYSCALE)
        if img1.shape != img2.shape:
            # Resize img2 to match img1 dimensions if necessary
            img2 = cv2.resize(img2, (img1.shape[1], img1.shape[0]))
        # Calculate SSIM
        ssim_score, diff = ssim(img1, img2, full=True)
        
        #Compare to GDSII
        gds = cv2.cvtColor(ideal_litho, cv2.COLOR_RGB2GRAY)
        error_gray = np.abs(gds - curr_litho)*2 
        curr_score, curr_error = error_score(error_gray, ideal_litho, width, height)
        
        curr_x_offset, curr_y_offset, curr_x_offset_0, curr_y_offset_0, zero_score = auto_detect_creep(curr_litho, gds, width, height, img_scale_x, img_scale_y, scan_settings_angle, overlap)
        curr_x_pxl_offset = int(curr_x_offset_0*(width/img_scale_x))
        curr_y_pxl_offset = int(-1*curr_y_offset_0*(height/img_scale_y))
        curr_litho_offset = creep_correct(curr_litho, curr_x_pxl_offset, curr_y_pxl_offset)         
        error_gray_corr = np.abs(gds - curr_litho_offset)*2  
        curr_score_corr, curr_error_corr = error_score(error_gray_corr, ideal_litho, width, height)
        print(f'ssim score: {ssim_score}')
        print(f'pxl comp score: {curr_score}')
        print(f'creep corrected score: {curr_score_corr}')
        
        score = ssim_score*0.5 + curr_score_corr*0.5
    
    if(save_output):
        ax[2,0].imshow(ideal_litho)
        ax[2,0].set_title('Ideal Lithograhy')
        ax[2,0].axis("off")
        ax[2,1].imshow(curr_error)
        ax[2,1].set_title('Error')
        ax[2,1].axis("off")
        ax[2,2].imshow(curr_error_corr)
        ax[2,2].set_title('Creep Corrected Error')
        ax[2,2].axis("off")
        text_subplot = ax[1,0]
        ax[1,0].axis("off")
        text_subplot.text(0.5, 0.5, f'\u03C3 = {blur}\nTopHat = {tophat}\nThreshold = {roughnessThreshold}\nContrast = {contrast}\nSSIM: {score:.3f}\npxl comp score: {curr_score:.3f}\n creep corrected score: {curr_score_corr:.3f}',
            horizontalalignment='center',
            verticalalignment='center',
            fontsize=12,
            color='black')
        plt.tight_layout()
        plt.savefig(f'tmp/litho_{blur}_{roughnessThreshold}_{contrast}.png') 
    return score, litho, curr_x_offset, curr_y_offset

def auto_detect_edges(img, input_data, show_plots=False):
    
    print("Finding Step Edges ...")
    img_width = int( input_data["img_width"] )
    img_height = int( input_data["img_height"] )
    scan_settings_x = float( input_data["scan_settings_x"] )
    scan_settings_y = float( input_data["scan_settings_y"] )
    img_scale_x = float( input_data["img_scale_x"] )
    img_scale_y = float( input_data["img_scale_y"] )
    scan_settings_angle = float( input_data["scan_settings_angle"] )
    litho_img = bool( input_data["litho_img"] )
    gds_path = input_data["gds_path"]
    patterned = input_data["patterned"]
    dzdx = float( input_data["dzdx"] )
    dzdy = float( input_data["dzdy"] )
    overlap = bool ( input_data["overlap"] )
    
    # *** Start plane subtract
    dzdx,dzdy = get_bg_plane(img, img_scale_x, img_scale_y)
    
    nm_px_x = img_scale_x/(len(img[0])-1)
    nm_px_y = img_scale_y/(len(img)-1)
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
    
    img = np.array(img).reshape(img_height, img_width)
    orig_img = img
	
    min = np.min(img)
    max = np.max(img)  
    
    gray = ((img - min) / (max - min) * 255).astype(np.uint8)
    orig = gray
    img = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)

    if( litho_img == False ):
        if ( img_scale_x < 15 and img_scale_y < 15 ):
            blur = 6
            tophat = True
        elif( img_scale_x < 50 and img_scale_y < 50 ):
            blur = 3
            tophat = True
        elif( img_scale_x < 100 and img_scale_y < 100 ):
            blur = 3
            tophat = False
        elif( img_scale_x < 500 and img_scale_y < 500 ):
            blur = 2
            tophat = False
        else:
            blur = 1
            tophat = False
        
        with remove_prints():
            edges = detect_steps(img, img_width=img_width, img_height=img_height, show_plots=show_plots, save_output=True, blur=blur, tophat=tophat, detectLitho=False, roughnessThreshold=0, scan_settings_x=scan_settings_x,
                scan_settings_y=scan_settings_y, img_scale_x=img_scale_x, img_scale_y=img_scale_y, scan_settings_angle=scan_settings_angle, contrast=0)
         
        print("done detecting edges")
        return edges

    else: # Litho Image

        image1 = "tmp/image1.jpg"
        image2 = "tmp/image2.jpg"
        
        lib = gdstk.read_gds(gds_path, 1e-9)
        top = lib.top_level()[0]
        bbox = top.bounding_box()
        if bbox is not None:
            max_x, max_y = bbox[1]
            max_x = int(max_x)*2
            max_y = int(max_y)*2
        else:
            print("Detect Litho: Failed to read GDS File")
        
        if ( not overlap ):         
            curr_image_color = np.zeros((img_height, img_width, 3), dtype=np.uint8)
            if bbox is not None:
                polygons = top.polygons
                polys = []
                for polygon in polygons:
                    x, y = zip(*polygon.points)
                    poly = []          
                    for i in range(len(x)):
                        x_vertex = x[i]-scan_settings_x
                        y_vertex = y[i]-scan_settings_y
                        xr = int(((x_vertex*np.cos(scan_settings_angle*(np.pi/180)*(-1))) - (y_vertex*np.sin((scan_settings_angle)*(np.pi/180)*(-1))))*(img_width/(img_scale_x)) + (img_width/2))
                        yr = int(((x_vertex*np.sin(scan_settings_angle*(np.pi/180)*(-1))) + (y_vertex*np.cos((scan_settings_angle)*(np.pi/180))*(-1)))*(img_height/(img_scale_y)) + (img_height/2))
                        poly.append([xr-1, yr-1])
                    polys.append(np.array(poly, np.int32))
                cv2.fillPoly(curr_image_color, polys, (255, 255, 0))       
                curr_image = cv2.cvtColor(curr_image_color, cv2.COLOR_RGB2GRAY)  
                cv2.imwrite("tmp/image1.jpg", curr_image)

                resolution = 4
                for i in range(len(curr_image)):
                    for j in range(len(curr_image[0])):
                        if(curr_image_color[i,j,0] == 255):
                            x_shift = (j + 1 - (img_width/2))*(img_scale_x/img_width)
                            y_shift = (i + 1 - (img_height/2))*(img_scale_y/img_height)
                            x_index = x_shift*np.cos((scan_settings_angle)*(np.pi/180))-y_shift*np.sin((scan_settings_angle)*(np.pi/180))
                            x_index = int((x_index + (max_x/2) + scan_settings_x)*resolution)
                            y_index = x_shift*np.sin((scan_settings_angle)*(np.pi/180))+y_shift*np.cos((scan_settings_angle)*(np.pi/180))
                            y_index = int((y_index + (max_y/2) + scan_settings_y)*resolution)
                            patterned[y_index, x_index, 0] = 0
                            patterned[y_index, x_index, 1] = 255                             
                            patterned[y_index, x_index-1, 0] = 0
                            patterned[y_index, x_index-1, 1] = 255
                            patterned[y_index-1, x_index-1, 0] = 0
                            patterned[y_index-1, x_index-1, 1] = 255   
                            patterned[y_index, x_index-2, 0] = 0
                            patterned[y_index, x_index-2, 1] = 255
                            patterned[y_index-2, x_index-2, 0] = 0
                            patterned[y_index-2, x_index-2, 1] = 255
                            patterned[y_index, x_index-3, 0] = 0
                            patterned[y_index, x_index-3, 1] = 255  
                            patterned[y_index-3, x_index-3, 0] = 0
                            patterned[y_index-3, x_index-3, 1] = 255         
        
        else: # overlapping region 
            curr_image_color = np.zeros((img_height, img_width, 3), dtype=np.uint8) 
            resolution = 4
            for i in range(len(patterned)):
                for j in range(len(patterned[0])):
                    if(patterned[i,j,1] == 255):
                        x_vertex = (j/resolution) - 1 - scan_settings_x - (max_x/2)
                        y_vertex = (i/resolution) - 1 - scan_settings_y - (max_y/2)
                        xr = int(((x_vertex*np.cos(scan_settings_angle*(np.pi/180)*(-1))) - (y_vertex*np.sin((scan_settings_angle)*(np.pi/180)*(-1))))*(img_width/(img_scale_x)) + (img_width/2))
                        yr = int(((x_vertex*np.sin(scan_settings_angle*(np.pi/180)*(-1))) + (y_vertex*np.cos((scan_settings_angle)*(np.pi/180)*(-1))))*(img_height/(img_scale_y)) + (img_height/2))
                        if (xr <= img_width and xr > 0):
                            if (yr <= img_height and yr > 0):
                                curr_image_color[yr-1, xr-1, 0] = 255
                                curr_image_color[yr-1, xr-1, 1] = 255
    
            curr_image = cv2.cvtColor(curr_image_color, cv2.COLOR_RGB2GRAY)  
            cv2.imwrite("tmp/image1.jpg", curr_image) 
        
        # Parameters loop to find lowest error
        other_params = [(img, curr_image_color, img_scale_x, img_scale_y, scan_settings_angle, overlap)]
        blur = [1, 2, 3, 4, 5, 6]
        roughnessThreshold = [0.1, 0.15, 0.2, 0.25, 0.3]
        contrast = [1, 2, 3]
        parameter_combinations = list(itertools.product(other_params, blur, roughnessThreshold, contrast))
        num_processes = multiprocessing.cpu_count()
        print(f'Number of cores detected: {num_processes}')
        start_time = time.time()
        with multiprocessing.Pool(processes=num_processes) as pool:
            results = pool.map(auto_score, parameter_combinations)
        end_time = time.time()
        print(f'elapsed time to test parameters: {end_time-start_time}')
        max_score = np.argmax(results)
        misc, blur_p, roughnessThreshold_p, contrast_p = parameter_combinations[max_score]
        print(f'Chosen Params:\nblur = {blur_p}\nroughnessThreshold = {roughnessThreshold_p}\ncontrast = {contrast_p}')
        print(f'\nScore: {results[max_score]}\n')
        detectedLithoImg = cv2.imread(f'tmp/litho_{blur_p}_{roughnessThreshold_p}_{contrast_p}.png')
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"detectedLithoImages/detectedLitho_{timestamp}.png"
        cv2.imwrite(filename, detectedLithoImg) 

        if ( results[max_score] < 0.5 ):
            pf = False
        else:
            pf = True

        litho, x_offset, y_offset = auto_detect_litho(parameter_combinations[max_score])

        print("done detecting litho")
        return litho, pf, patterned, x_offset, y_offset

def detect_steps(img, img_width=None, img_height=None, show_plots=False, save_output=False, blur=3, tophat=False, detectLitho=False, roughnessThreshold=0.15, scan_settings_x=0,
                scan_settings_y=0, img_scale_x=50, img_scale_y=50, scan_settings_angle=0, contrast=2):
    
    if img_width is None:
        img_width = int( np.sqrt( len(img) ) )
        img_height = img_width
        print(img_height)
	
    print(img_width)
    img = np.array(img).reshape(img_height, img_width)
    
    # *** Start plane subtract
    dzdx,dzdy = get_bg_plane(img, img_scale_x, img_scale_y)
    
    nm_px_x = img_scale_x/(len(img[0])-1)
    nm_px_y = img_scale_y/(len(img)-1)
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
	
    min = np.min(img)
    max = np.max(img)
    
    gray = ((img - min) / (max - min) * 255).astype(np.uint8)
    img = cv2.cvtColor(gray, cv2.COLOR_GRAY2RGB)
  
    height, width = gray.shape
    thickness = (height*width)/(100000.0)
    if thickness < 1:
        thickness = 1
    max_pxl = round((height+width)/4)
    
    fig, ax = plt.subplots(2, 4, figsize=(15, 5))
    ax[0,0].imshow(gray, cmap='gray')
    ax[0,0].set_title('Original Image')
    ax[0,0].axis("off")
    
    contours = find_contours(gray, blur)
    ax[0,1].imshow(contours, cmap='gray')
    ax[0,1].set_title('Contours')
    ax[0,1].axis("off")
	
    # White Tophat Postprocessing
    if(tophat):
        cleaned = post_processing(contours, max_pxl*2, tophat, thickness)
    else:
        cleaned = post_processing(contours, max_pxl, tophat, thickness)
    ax[0,2].imshow(cleaned, cmap='gray')
    ax[0,2].set_title('Post-processing')
    ax[0,2].axis("off")
	
    # Create a colored overlay 
    overlay = np.zeros((img_height, img_width, 3), dtype=np.uint8)
    overlay[cleaned] = [0, 255, 0]  # Set the color for True values (green in this case)
    result = cv2.addWeighted(img, 1, overlay, 0.5, 0)
    ax[0,3].imshow(result)
    ax[0,3].set_title('Detected Step Edges')
    ax[0,3].axis("off")

    # Labelling
    numbered = np.zeros_like(gray)
    labels, numbered = color_layers(overlay, cleaned, numbered)
    ax[1,1].imshow(labels)
    ax[1,1].set_title('Color Coded Regions')
    ax[1,1].axis("off")

    text_subplot = ax[1,0]
    ax[1,0].axis("off")

    # Detect Patterned Areas
    if detectLitho: 
        litho_method = "default"
        litho, edges, litho_mask = detect_litho(labels, gray, numbered, roughnessThreshold, litho_method, 0, contrast, thickness, cleaned, max_pxl)
        result = cv2.addWeighted(img, 0.5, litho, 0.5, 0)
        ax[1,2].imshow(edges)
        ax[1,2].set_title('Contours 2')
        ax[1,2].axis("off")
        ax[1,3].imshow(result)
        ax[1,3].set_title('Detected Lithography')
        ax[1,3].axis("off")
        text_subplot.text(0.5, 0.5, f'\u03C3 = {blur}\nTopHat = {tophat}\nThreshold = {roughnessThreshold}\nContrast = {contrast}',
            horizontalalignment='center',
            verticalalignment='center',
            fontsize=12,
            color='black')
    else:
        ax[1,2].remove()
        ax[1,3].remove()
        text_subplot.text(0.5, 0.5, f'\u03C3 = {blur}\nTopHat = {tophat}',
            horizontalalignment='center',
            verticalalignment='center',
            fontsize=12,
            color='black')
        
    plt.tight_layout()
    if(save_output):
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        filename = f"detectedLithoImages/detectedSteps_{timestamp}.png"
        plt.savefig(filename) 

    # Display the output
    if show_plots:
        plt.show()
        cv2.waitKey(0) 
        cv2.destroyAllWindows()

    return cleaned
        
#function to optimize with respect to dz=dzdx,dzdy with the raw image as the fixed argument 
def bg_plane_quality(dz, args):
    img,img_width_nm,img_height_nm = args
    dzdx,dzdy = dz
    
    img_sub = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)
    
    #return quality(img_sub, img_width_nm, img_height_nm)
    return quality_prev(img_sub, img_width_nm, img_height_nm)
    
    '''
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
    '''

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
    return dzdx,dzdy

def peak_func(x,A,sigma,x0):
    return A*np.exp( -((x-x0)**2)/(2*sigma**2) )

def quality(img, img_width_nm, img_height_nm, max_fract=0.5, display_hist=False, name=''):
    #estimate of the proper vertical range of the image - should be close
    delta_z = np.max(img) - np.min(img)
    
    #set bins to be enough to give a resolution of 0.001 nm
    min_bin_dz = 0.001
    num_bins = int(delta_z/min_bin_dz)
    bin_dz = delta_z/num_bins
       
    #get the histogram    
    hist, bin_edges = np.histogram(img, bins=num_bins)
    max_idx = np.argmax(hist)  #index of tallest "peak"
    max_val = np.max(hist)  #height of tallest peak
    
    #determine where other peak locations should be relative to the max peak based on known step height
    dz_step_nm = 0.08
    r_step_nm = 0.01
    r_step = int(r_step_nm/bin_dz)
    
    peak_idxs = [max_idx]
    peak_idx = max_idx
    L_idx = max(peak_idx - r_step, 0)
    R_idx = min(peak_idx + r_step, len(hist))
    L_idxs = [L_idx]
    R_idxs = [R_idx]
    
    #figure out what bins each peak should be at
    n = 1
    while peak_idx > 0:
        dist = n*dz_step_nm
        peak_idx = int(max_idx - dist/bin_dz)
        if peak_idx > 0:
            peak_idxs.insert(0,peak_idx)
        
            L_idx = max(peak_idx - r_step, 0)
            R_idx = min(peak_idx + r_step, len(hist))
            L_idxs.insert(0, L_idx)
            R_idxs.insert(0, R_idx)
        n += 1
    
    n = 1
    while peak_idx < len(hist):
        dist = n*dz_step_nm
        peak_idx = int(max_idx + dist/bin_dz)
        if peak_idx < len(hist):
            peak_idxs.append(peak_idx)
            
            L_idx = max(peak_idx - r_step, 0)
            R_idx = min(peak_idx + r_step, len(hist))
            L_idxs.append(L_idx)
            R_idxs.append(R_idx)
        n += 1
        
    #integrate the regions between peaks
    sum = 0
    
    start_idx = 0
    end_idx = L_idxs[0]
    for i in range(start_idx,end_idx):
        sum += hist[i]
        
    for idx in range(len(peak_idxs)-1):
        start_idx = R_idxs[idx]
        end_idx = L_idxs[idx+1]
        for i in range(start_idx,end_idx):
            sum += hist[i]
    
    start_idx = R_idxs[-1]
    end_idx = len(hist)
    for i in range(start_idx,end_idx):
        sum += hist[i]
    
    sum *= bin_dz
    
    if display_hist:
        plt.plot(hist)
        plt.title(name)
        plt.show()
                    
    return sum

def quality_alt(img, img_width_nm, img_height_nm, max_fract=0.5, display_hist=False, name=''):
    #estimate of the proper vertical range of the image - should be close
    delta_z = np.max(img) - np.min(img)
    
    #set bins to be enough to give a resolution of 0.001 nm
    min_bin_dz = 0.001
    num_bins = int(delta_z/min_bin_dz)
       
    #get the histogram    
    hist, bin_edges = np.histogram(img, bins=num_bins)
    max_idx = np.argmax(hist)  #index of tallest "peak"
    max_val = np.max(hist)  #height of tallest peak
    
    #get full-width at fractional max of tallest peak by fitting a gaussian to it
    #we consider the data in a window whose width is about half a step edge height on either side of the peak
    window_rad_nm = 0.05
    window_rad_bins = int(window_rad_nm/min_bin_dz)
    
    left_idx = max(0,max_idx-window_rad_bins)
    right_idx = min(len(hist)-1,max_idx+window_rad_bins)+1
    peak_signal = hist[left_idx:right_idx]
    
    N = len(peak_signal)
    params, pcov = curve_fit(peak_func, range(N), peak_signal, p0=[max_val,N/4,N/2])
    peak_width = params[1]*min_bin_dz
    
    if display_hist:
        print('height: ' + str(max_val))
        print('gaussian params: ' + str(params))
    
        g_x_vals = range(N)
        g_y_vals = []
        for x in g_x_vals:
            g_y_vals.append(peak_func(x,params[0],params[1],params[2]))
    
        plt.plot(hist)
        plt.plot(peak_signal)
        plt.plot(g_y_vals)
        plt.title(name)
        plt.show()
                    
    return peak_width

def quality_peak_widths(img, img_width_nm, img_height_nm, max_fract=0.5, display_hist=False, name=''):
    #estimate of the proper vertical range of the image - should be close
    delta_z = np.max(img) - np.min(img)
    
    #set bins to be enough to give a resolution of 0.005 nm
    min_bin_dz = 0.001
    num_bins = int(delta_z/min_bin_dz)
    bin_dz = delta_z/num_bins
       
    #get the histogram    
    hist, bin_edges = np.histogram(img, bins=num_bins)
    max_idx = np.argmax(hist)  #index of tallest "peak"
    #determine where other peak locations should be relative to the max peak based on known step height
    dz_step_nm = 0.08
    r_step_nm = 0.02
    r_step = int(r_step_nm/bin_dz)
    width_step_nm = 0.04
    width_step = int(width_step_nm/bin_dz) 
    
    peak_idxs = [max_idx]
    peak_idx = max_idx
    L_idx = max(peak_idx - r_step, 0)
    R_idx = min(peak_idx + r_step, len(hist))
    L_idxs = [L_idx]
    R_idxs = [R_idx]
    
    #figure out what bins each peak should be at
    n = 1
    while peak_idx > 0:
        dist = n*dz_step_nm
        peak_idx = int(max_idx - dist/bin_dz)
        if peak_idx > 0:
            L_idx = max(peak_idx - r_step, 0)
            R_idx = min(peak_idx + r_step, len(hist))
            
            sub_hist = hist[L_idx:R_idx]
            peak_idx = np.argmax(sub_hist) + L_idx
            peak_idxs.insert(0,peak_idx)
            
            L_idx = max(peak_idx - width_step, 0)
            R_idx = min(peak_idx + width_step, len(hist))
            
            L_idxs.insert(0, L_idx)
            R_idxs.insert(0, R_idx)
        n += 1
    
    n = 1
    while peak_idx < len(hist):
        dist = n*dz_step_nm
        peak_idx = int(max_idx + dist/bin_dz)
        if peak_idx < len(hist):
            L_idx = max(peak_idx - r_step, 0)
            R_idx = min(peak_idx + r_step, len(hist))
            
            sub_hist = hist[L_idx:R_idx]
            peak_idx = np.argmax(sub_hist) + L_idx
            peak_idxs.append(peak_idx)
            
            L_idx = max(peak_idx - width_step, 0)
            R_idx = min(peak_idx + width_step, len(hist))
            
            L_idxs.append(L_idx)
            R_idxs.append(R_idx)
        n += 1
    
    width_sum = 0
    for peak_idx in peak_idxs:
        width_sum += full_width_fract_max(hist, peak_idx, max_fract=max_fract)
        
    width_fract = width_sum*min_bin_dz
    
    
    if display_hist:
        
        plt.plot(hist)
        plt.title(name)
        
        for peak_idx in peak_idxs:
            plt.axvline(x=peak_idx, linestyle='--', color='red', linewidth=2)
        
        plt.show()
        
        
    return width_fract

def full_width_fract_max(hist, peak_idx, max_fract=0.5):
    max_val = hist[peak_idx]  #height of tallest peak
    
    #get full-width at fractional max of tallest peak
    min_val = max_val*max_fract
    
    right_idx = len(hist)-1
    for idx in range(peak_idx, len(hist)):
        if hist[idx] < min_val:
            right_idx = idx
            break
    
    left_idx = 0
    for idx in range(peak_idx, 0,-1):
        if hist[idx] < min_val:
            left_idx = idx
            break
    
    bin_width = right_idx-left_idx
    return bin_width
    
def quality_prev(img, img_width_nm, img_height_nm, max_fract=0.5, display_hist=False, name=''):
    #estimate of the proper vertical range of the image - should be close
    delta_z = np.max(img) - np.min(img)
    
    #set bins to be enough to give a resolution of 0.005 nm
    min_bin_dz = 0.001
    num_bins = int(delta_z/min_bin_dz)
       
    #get the histogram    
    hist, bin_edges = np.histogram(img, bins=num_bins)
    max_idx = np.argmax(hist)  #index of tallest "peak"
    bin_width = full_width_fract_max(hist, max_idx, max_fract=max_fract)
    '''
    #max_val = np.max(hist)  #height of tallest peak
    
    #get full-width at fractional max of tallest peak
    min_val = max_val*max_fract
    
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
    '''
    width_fract = bin_width*min_bin_dz  #/num_bins
    
    if display_hist:
        plt.plot(hist)
        plt.title(name)
        plt.show()
    
    return width_fract
    
def bg_poly_quality(params, args):
    img,img_width_nm,img_height_nm,N_x,N_y = args
    x_coefs = params[0:N_x]
    y_coefs = params[N_x:N_x+N_y]
    
    img_sub = sub_poly_bg(img, img_width_nm, img_height_nm, x_coefs, y_coefs)
    
    return quality(img_sub, img_width_nm, img_height_nm, max_fract=0.5)

def bg_poly_step(img, img_width_nm=100, img_height_nm=100, x0_coefs=[], y0_coefs=[]):
    N_x = len(x0_coefs)
    N_y = len(y0_coefs)
    
    params = x0_coefs + y0_coefs
    print('params: ' + str(params))
    
    #minimize the width of the sharpest peak in the image histogram when subtracting off background
    min_plane = minimize(bg_poly_quality, params, args=[img, img_width_nm, img_height_nm,N_x,N_y], method='Nelder-Mead')
    
    if not min_plane.success:
        print('well that sucks')
        print(min_plane.message)
    
    params = min_plane.x
    print(' out params: ', str(params))
    x_coefs = params[0:N_x]
    y_coefs = params[N_x:N_x+N_y]
    
    return [x_coefs.tolist(), y_coefs.tolist()]

def auto_bg_poly(img, img_width_nm=100, img_height_nm=100, N_x=1, N_y=1):
    #initial guess for dzdx and dzdy
    #dzdx,dzdy = auto_bg_plane(img, img_width_nm, img_height_nm) 
    #get_bg_plane(img, img_width_nm, img_height_nm)
     
    #dzdx,dzdy = auto_bg_slopes(img, img_width_nm, img_height_nm)    
    #img = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)
    
    x_coefs = [dzdx]
    y_coefs = [dzdy]
    
    for n in range(N_x-1):
        x_coefs.append(0)
        x_coefs,y_coefs = bg_poly_step(img, img_width_nm, img_height_nm, x0_coefs=x_coefs, y0_coefs=y_coefs)
        
    for n in range(N_y-1):
        y_coefs.append(0)
        x_coefs,y_coefs = bg_poly_step(img, img_width_nm, img_height_nm, x0_coefs=x_coefs, y0_coefs=y_coefs)
    
    #x_coefs,y_coefs = bg_poly_step(img, img_width_nm, img_height_nm, x0_coefs=x_coefs, y0_coefs=y_coefs)
    
    return [x_coefs, y_coefs]#bg_poly_step(img, img_width_nm=100, img_height_nm=100, x0_coefs=x_coefs, y0_coefs=y_coefs)

def bg_model_function(x, *p0):
    val = 0
    for n in range(len(p0)):
        val += (n+1)*p0[n]*x**n
    return val

def sub_poly_bg(img0, width_nm, height_nm, x_coefs, y_coefs):
    img = np.copy(img0)
    
    num_cols = len(img[0])
    num_rows = len(img)
    
    for xIdx in range(num_cols):
        x = width_nm*xIdx/(num_cols-1)
        
        for yIdx in range(num_rows):
            y = height_nm*yIdx/(num_rows-1)
            
            z = img[yIdx][xIdx]
            for n in range(len(x_coefs)):
                z -= x_coefs[n]*x**(n+1)
            for n in range(len(y_coefs)):
                z -= y_coefs[n]*y**(n+1)
            
            img[yIdx][xIdx] = z
    
    return img            

def sub_line_by_line(img0):
    img = np.copy(img0)
    num_rows = len(img)
    num_cols = len(img[0])
    
    for y_idx in range(num_rows - 1):
        diffs = []
        for x_idx in range(num_cols):
            diffs.append( img[y_idx+1][x_idx] - img[y_idx][x_idx] )
        
        median = np.median(diffs)
        
        for x_idx in range(num_cols):
            img[y_idx+1][x_idx] -= median
    
    return img

def sub_plane(img0, width_nm, height_nm, dzdx, dzdy):
    img = np.copy(img0)
    num_cols = len(img[0])
    num_rows = len(img)
    
    for xIdx in range(num_cols):
        x = width_nm*xIdx/(num_cols-1)
        
        for yIdx in range(num_rows):
            y = height_nm*yIdx/(num_rows-1)
            
            z = img[yIdx][xIdx] - (dzdx*x + dzdy*y)
            img[yIdx][xIdx] = z
    
    return img

def get_bg_plane(img, width_nm, height_nm):
    dzdx_ave = 0
    dzdy_ave = 0
    
    num_cols = len(img[0])
    num_rows = len(img)
    
    for yIdx in range(num_rows):
        dzdx_ave += img[yIdx][num_cols-1] - img[yIdx][0]
    
    for xIdx in range(num_cols):
        dzdy_ave += img[num_rows-1][xIdx] - img[0][xIdx]
        
    dzdx_ave /= num_rows*(num_cols-1)
    dzdy_ave /= (num_rows-1)*num_cols
    
    #convert from nm(dz)/px to nm(dz)/nm(dx or dy)
    dzdx_ave *= (num_cols-1)/width_nm
    dzdy_ave *= (num_rows-1)/height_nm
    
    return dzdx_ave, dzdy_ave

def dz_from_diffs(diffs, num_px, size_nm, num_bins=256, display_hist=False):
    hist, bin_edges = np.histogram(diffs, bins=num_bins)
    max_idx = np.argmax(hist)
    
    min = np.min(diffs)
    max = np.max(diffs)
    idx_fract = max_idx/(num_bins-1)
    dz_idx = min + idx_fract*(max - min)
    nm_per_idx = size_nm/(num_px-1)
    dz_nm = dz_idx/nm_per_idx
    
    if display_hist:
        
        plt.plot(hist)
        plt.show()
        
    return dz_nm
    
def auto_bg_slopes(img, img_width_nm, img_height_nm, display_hist=True):
    num_cols = len(img[0])
    num_rows = len(img)
    
    nm_per_x_px = img_width_nm/(num_cols-1)
    nm_per_y_px = img_height_nm/(num_rows-1)
    
    smear_nm = 4
    
    sigma_x = smear_nm/nm_per_x_px
    sigma_y = smear_nm/nm_per_y_px
    
    #avoid image borders where gaussian blur will misrepresent the slopes
    skip_x = 2*int(sigma_x)
    skip_y = 2*int(sigma_y)
    
    img = np.copy(img)
    img = gaussian_filter(img, sigma=[sigma_y,sigma_x])
    
    dzdx_diffs = []
    dzdy_diffs = []
    
    for yIdx in range(num_rows):
        for xIdx in range(num_cols-1):
            if xIdx > skip_x and yIdx > skip_y and xIdx < (num_cols-1 - skip_x) and yIdx < (num_rows-1 - skip_y):
                dzdx_diffs.append(img[yIdx,xIdx+1] - img[yIdx,xIdx])

    for xIdx in range(num_cols):
        for yIdx in range(num_rows-1):
            if xIdx > skip_x and yIdx > skip_y and xIdx < (num_cols-1 - skip_x) and yIdx < (num_rows-1 - skip_y):
                dzdy_diffs.append(img[yIdx+1,xIdx] - img[yIdx,xIdx])
    
    
    #dzdx_ave *= (num_cols-1)/width_nm
    #dzdy_ave *= (num_rows-1)/height_nm
    
    dzdx = dz_from_diffs(dzdx_diffs, num_cols, img_width_nm, num_bins=256, display_hist=display_hist)
    dzdy = dz_from_diffs(dzdy_diffs, num_rows, img_height_nm, num_bins=256, display_hist=display_hist)
    
    print('dzdx,dzdy: ' + str([dzdx,dzdy]))
    
    if display_hist:
        dzdx_blur,dzdy_blur = get_bg_plane(img, img_width_nm, img_height_nm)
        img = sub_plane(img, img_width_nm, img_height_nm, dzdx_blur, dzdy_blur)
        
        #if xIdx > skip_x and yIdx > skip_y and xIdx < (num_cols-1 - skip_x) and yIdx < (num_rows-1 - skip_y):
        img = img[skip_y:(num_rows-1-skip_y),skip_x:(num_cols-1-skip_x)]    
    
        min = np.min(img)
        max = np.max(img)
        gray = ((img - min) / (max - min) * 255).astype(np.uint8)
        cv2.namedWindow("gauss")
        cv2.imshow("gauss", cv2.resize(gray,(400,400)))
                
        cv2.waitKey(0)
        cv2.destroyAllWindows()

    
    return dzdx,dzdy

def auto_flatten(img, img_width_nm=100, img_height_nm=100, line_by_line_flatten=True, display_hist=True, y_order=5 ):
    img0 = img
    #if line_by_line_flatten:
    #do line by line flattening
    img = sub_line_by_line(img)
    
    #get bg plane for comparison
    #dzdx,dzdy = auto_bg_plane(img, img_width_nm, img_height_nm)
    #img_plane = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)
    #quality(img_plane, img_width_nm, img_height_nm, display_hist=True, name='plane')
    
    dzdx,dzdy = auto_bg_slopes(img, img_width_nm, img_height_nm, display_hist=display_hist)    
    img = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)

    img_diff = img - img0
    lin_z_sub = []

    num_cols = len(img[0])
    num_rows = len(img)

    indep_data = range(num_rows)
    for y_idx in indep_data:
        lin_z_sub.append( np.mean(img_diff[y_idx]) )

    initial_vals = [0]*y_order
    initial_vals[0] = 1
    y_params, pcov = curve_fit(bg_model_function, np.array(indep_data), np.array(lin_z_sub), p0=tuple(initial_vals))#(1,0,0,0,0))

    fit_z = []
    diff_z = []
    for y_idx in indep_data:
        fit_z.append( bg_model_function(y_idx, *y_params) )#y_params[0],y_params[1],y_params[2],y_params[3],y_params[4]) )
        diff_z.append( fit_z[y_idx]-lin_z_sub[y_idx] )
        img[y_idx] += diff_z[y_idx]


    if display_hist:

        plt.plot(lin_z_sub)
        plt.plot(fit_z)
        plt.show()

    dzdx,dzdy = auto_bg_slopes(img, img_width_nm, img_height_nm, display_hist=display_hist)    
    img = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)

    if display_hist:
        min = np.min(img)
        max = np.max(img)
        gray = ((img - min) / (max - min) * 255).astype(np.uint8)
        cv2.namedWindow("gray")
        cv2.imshow("gray", cv2.resize(gray,(400,400)))

        cv2.waitKey(0)
        cv2.destroyAllWindows()

    return img

def detect_steps_alt(img, img_width_nm=100, img_height_nm=100, line_by_line_flatten=True, display_hist=True, y_order=5 ):
    img = auto_flatten(img, img_width_nm, img_height_nm, line_by_line_flatten, display_hist, y_order)

    hist, bin_edges = np.histogram(img, bins=256)
    corr = np.correlate(hist,hist,mode='full')

    if display_hist:
        plt.plot(hist)
        #plt.show()

        s = (np.max(hist)/np.max(corr))
        for idx in range( len(corr) ):
            corr[idx] *= s

        plt.plot(corr[256:])
        plt.show()
    
    #run auto_bg
    #x_coefs, y_coefs = auto_bg_poly(img, img_width_nm, img_height_nm, N_x=2, N_y=3)
    #x_coefs, y_coefs = auto_bg_prev(img, img_width_nm, img_height_nm)
    #img = sub_poly_bg(img, img_width_nm, img_height_nm, x_coefs, y_coefs)
    #quality(img, img_width_nm, img_height_nm, display_hist=True, name='poly')
    
    
    
    #min = np.min(img_plane)
    #max = np.max(img_plane)
    #gray_plane = ((img_plane - min) / (max - min) * 255).astype(np.uint8)
    #cv2.namedWindow("gray_plane")
    #cv2.imshow("gray_plane", cv2.resize(gray_plane,(400,400)))

    '''
    min = np.min(img)
    max = np.max(img)
    gray = ((img - min) / (max - min) * 255).astype(np.uint8)
    cv2.namedWindow("gray")
    cv2.imshow("gray", cv2.resize(gray,(400,400)))
    
    
    cv2.waitKey(0)
    cv2.destroyAllWindows()
	'''

def auto_bg_prev(img, img_width_nm=100, img_height_nm=100):
    num_x_px = len(img[0]) #x pixels
    num_y_px = len(img) #y pixels
    
    x_nm_per_px = img_width_nm/num_x_px
    y_nm_per_px = img_height_nm/num_y_px
    
    
    #create a grid of dzdx(x,y) and dzdy(x,y) values to fit a polynomial background to
    max_nm_width = 100
    max_px_width = int( max_nm_width/x_nm_per_px )
    px_per_x_step = 10
    if num_x_px > max_px_width+2*px_per_x_step:
        x_px_per_win = max_px_width
    else:
        x_px_per_win = num_x_px
        
    win_width_nm = x_nm_per_px*x_px_per_win#img_width_nm*x_px_per_win/num_x_px
    num_x_steps = 1 + int((num_x_px - x_px_per_win)/px_per_x_step)
    nm_per_x_step = px_per_x_step*x_nm_per_px  
    
    
    max_nm_height = 100
    max_px_height = int( max_nm_height/y_nm_per_px )
    px_per_y_step = 10
    if num_y_px > max_px_height+2*px_per_y_step:
        y_px_per_win = max_px_height
    else:
        y_px_per_win = num_y_px
        
    win_height_nm = y_nm_per_px*y_px_per_win#img_height_nm*y_px_per_win/num_y_px
    num_y_steps = 1 + int((num_y_px - y_px_per_win)/px_per_y_step)
    nm_per_y_step = px_per_y_step*y_nm_per_px
    
    dz_array = []
    x_data = []
    y_data = []
    
    for x_idx in range(num_x_steps):
        x_start = x_idx*px_per_x_step
        x_end = x_start + x_px_per_win
        x = (x_idx + 0.5)*nm_per_x_step
        x_data.append(x)
        
        dz_row = []
        
        for y_idx in range(num_y_steps):
            y_start = y_idx*px_per_y_step
            y_end = y_start + y_px_per_win
            y = (y_idx + 0.5)*nm_per_y_step
            
            win = img[y_start:y_end,x_start:x_end]
            #dzdx,dzdy = auto_bg_plane(win, win_width_nm, win_height_nm)
            #[dzdx,dummy_x],[dzdy,dummy_y] = auto_bg_poly(win, win_width_nm, win_height_nm, N_x=2, N_y=2)
            dzdx,dzdy = auto_bg_slopes(win, win_width_nm, win_height_nm,display_hist=False)    
            #img = sub_plane(img, img_width_nm, img_height_nm, dzdx, dzdy)  
            
            dz_row.append([dzdx, dzdy])
            #sub_plane(img0, width_nm, height_nm, dzdx, dzdy)
            img_sub = sub_plane(win, win_width_nm, win_height_nm, dzdx, dzdy)    
            min = np.min(img_sub)
            max = np.max(img_sub)
            gray_plane = ((img_sub - min) / (max - min) * 255).astype(np.uint8)
            win_name = str("gray_plane " + str(x) + ',' + str(y))
            #cv2.namedWindow(win_name)
            #cv2.imshow(win_name, cv2.resize(gray_plane,(400,400)))
            
            
            if x_idx == 0:
                y_data.append(y)
            
        dz_array.append(dz_row)     
        
    dz_array = np.array(dz_array)
    #print('dz_array' + str(dz_array))
    
    #dzdx should (ideally) not depend on y, and dzdy should not depend on x
    #so we'll average out that dependence from the data, and format the arrays for curve fitting
    dzdx_data = np.zeros(len(x_data))
    dzdy_data = np.zeros(len(y_data))
    
    for y_idx in range(len(y_data)):
        for x_idx in range(len(x_data)):
            dzdx_data[x_idx] += dz_array[x_idx,y_idx,0]
            dzdy_data[y_idx] += dz_array[x_idx,y_idx,1]
    dzdx_data /= len(y_data)
    dzdy_data /= len(x_data)
    
    print('x_data: ' + str(x_data))
    print('dzdx_data: ' + str(dzdy_data))
    print('y_data: ' + str(y_data))
    print('dzdy_data: ' + str(dzdy_data))

    #now fit the data
    x_params, pcov = curve_fit(bg_model_function, np.array(x_data), dzdx_data,p0=(1,0))
    print('x_params: ' + str(x_params))
    
    y_params, pcov = curve_fit(bg_model_function, np.array(y_data), dzdy_data,p0=(1,0))
    print('y_params: ' + str(y_params))
    
    
    #cv2.waitKey(0)
    #cv2.destroyAllWindows()

    
    '''
    #print('y_data length: ' + str(len(y_data)))
    #print('dzdy_data length: ' + str(len(dzdy_data)))
    
    x_pts = np.linspace(x_data[0],x_data[-1],100)
    y_pts = np.linspace(y_data[0],y_data[-1],100)
        
    fig, axs = plt.subplots(1, 2)
    
    axs[0].scatter(x_data,dzdx_data)
    axs[0].plot(x_pts, bg_model_function(x_pts,x_params))
    axs[1].scatter(y_data,dzdy_data)
    axs[1].plot(y_pts, bg_model_function(y_pts,y_params))
    
    plt.tight_layout()
    plt.show()
    '''
    
    return [x_params,y_params]   
