import cv2
import numpy as np
import os

from helpers.main_functions import detect_steps

debug_type = 1
CV2_WINDOW_OFFSETS = 100, 100

paths = [
    "examples/default_2017Jul21-230336_STM-STM_AtomManipulation--19_44.Z_flat_10nmx10nm.png",
    "examples/default_2017Jun20-143916_STM-STM_AtomManipulation--4_1.___20nmx20nm___.png",
    "examples/20230920-144206_20211029 W31 P14--STM_AtomManipulation--18_18.png",
    "examples/20231030-232452_20211029 W31 P14--STM_AtomManipulation--1_3.png",
    "examples/deviceImage.png",
]

img = cv2.imread(paths[int(input("Enter the path number: "))])

detect_steps(img, show_plots=False, show_each_mask=False, show_output=True, blur=int(input("Blur amount: ")), postprocessing=True, max_pxl=200)

