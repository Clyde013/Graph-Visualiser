from skimage.filters.rank import median
from skimage.morphology import disk
import numpy as np
import matplotlib.pyplot as plt

from skimage import img_as_ubyte
from skimage import transform
from skimage import util

from skimage import io
from skimage.color import rgb2gray

import math


def load_image_as_grayscale(filepath):
    image = io.imread(filepath)  # load test image
    grayscale = img_as_ubyte(rgb2gray(image))  # convert to grayscale and shove into unsigned integer array
    return grayscale


def background_subtract_grayscale(grayscale):
    rankFiltered = median(grayscale, disk(160))  # rank filtering at 160th maximum pixel

    # background subtraction. using np.clip to keep unsigned pixel values within 0 and 255 otherwise negative values
    # and unsigned integers have aneurysm
    subtracted = np.subtract(rankFiltered.astype(np.int16), grayscale).clip(0, 255).astype(np.uint8)
    # can be commented out depending on whether we want to invert the colours of the subtracted output
    subtracted = util.invert(subtracted)
    # result is black = 0 and white = 255 (because img_as_ubyte scales values)
    return subtracted


def resize_image(image, box_size):  # box size should be 50, consistent with what was used
    return transform.resize(image, (box_size, box_size), anti_aliasing=False, preserve_range=True)


def binarise_grayscale(image, threshold, rank_filter):  # works only on ranges 0...1
    image[image > threshold] = 1  # set to white
    image[image <= threshold] = 0  # set to black
    if rank_filter:
        image = median(image, disk(1))  # remove grainy individual black pixels through rank filtering
    return image


def crop_borders(image):
    topborder = -1
    bottomborder = -1
    leftborder = -1
    rightborder = -1
    for row in range(len(image)):
        for col in range(len(image[row])):
            if image[row][col] == 0:  # 0 is black
                if topborder == -1:
                    topborder = row
                bottomborder = row
                if col < leftborder or leftborder == -1:
                    leftborder = col
                if col > rightborder:
                    rightborder = col

    height = bottomborder - topborder
    width = rightborder - leftborder
    if height > width:
        diff = (height - width) / 2
        diff = math.floor(diff)
        leftborder = leftborder - diff
        rightborder = rightborder + diff
    else:
        diff = (width - height) / 2
        diff = math.floor(diff)
        topborder = topborder - diff
        bottomborder = bottomborder + diff

    cropped_image = image[max(topborder, 0):min(bottomborder, len(image)),
                    max(leftborder, 0):min(rightborder, len(image[0]))]

    return cropped_image
    # will pad and center image
    shape = cropped_image.shape

    if shape[0] > shape[1]:     # height > width
        return np.pad(cropped_image, [(shape[0]-shape[1], shape[0]-shape[1]), (0, 0)], mode='constant', constant_values=1)
    else:   # width > height
        return np.pad(cropped_image, [(0, 0), (shape[1]-shape[0], shape[1]-shape[0])], mode='constant', constant_values=1)


'''
fig, (ax1) = plt.subplots(1, 1, figsize=(5, 5))

ax1.imshow(binarise_grayscale(load_image_as_grayscale('test.jpeg')), vmin=0, vmax=255, cmap=plt.cm.gray)
ax1.axis('off')

plt.show()

'''
