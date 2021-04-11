from skimage.filters.rank import median
from skimage.morphology import disk
import numpy as np
import matplotlib.pyplot as plt

from skimage import img_as_ubyte
from skimage import transform
from skimage import util

from skimage import io
from skimage.color import rgb2gray
from skimage.filters import sobel
from skimage.segmentation import watershed

from scipy import ndimage as ndi

import math


def load_image_as_grayscale(filepath):
    image = io.imread(filepath)  # load test image
    grayscale = img_as_ubyte(rgb2gray(image))  # convert to grayscale and shove into unsigned integer array
    return grayscale


def region_segmentation(image):
    # region based segmentation https://scikit-image.org/docs/dev/user_guide/tutorial_segmentation.html
    # assuming we take in the grayscale image as input

    elevation_map = sobel(image)

    markers = np.zeros_like(image)
    markers[image < 100] = 2
    markers[image > 150] = 1

    segmentation = watershed(elevation_map, markers)

    filled_segmentation = ndi.binary_fill_holes(segmentation-1).astype(np.uint8)

    # label only works on filled segmentation
    labeled_characters, _ = ndi.label(filled_segmentation, structure=ndi.generate_binary_structure(2, 2))
    characters = list()

    # parameters
    max_dilat = 10  # dilation (in number of pixels) for a small object
    sz_small = 100  # size of a small object (max dilated)
    sz_big = 10000  # size of a big object (not dilated)

    result = np.zeros_like(labeled_characters)

    # for each detected object
    for obj_id in range(1, labeled_characters.max() + 1):
        # creates a binary image with the current object
        obj_img = (labeled_characters == obj_id)
        # computes object's area
        area = np.sum(obj_img)
        # dilatation factor inversely proportional to area
        dfac = int(max_dilat * (1 - min(1, (max(0, area - sz_small) / sz_big))))
        # dilates object
        dilat = ndi.binary_dilation(obj_img, iterations=dfac)
        # overlays dilated object onto result
        result += dilat

    labeled, nr_objects = ndi.label(result > 0)

    for label in range(1, labeled.max() + 1):
        xs, ys = np.where(labeled == label)
        shape = (len(np.unique(xs)), len(np.unique(ys)))
        topleft = (xs.min(), ys.min())
        width, height = xs.max() - xs.min(), ys.max() - ys.min()

        # instead of np.reshape which will throw error since our values will not be perfectly square shape
        # (e.g. open bracket '(' ) and the number of values that were labelled may be less than shape[0]*shape[1]
        fill = np.zeros(shape=shape)
        fill[:shape[0], :shape[1]] = segmentation[topleft[0]: topleft[0]+width+1, topleft[1]:topleft[1]+height+1] - 1

        characters.append(fill)

    '''
    fig, (ax1, ax2, ax3) = plt.subplots(1, 3, figsize=(10, 5))

    ax1.imshow(characters[0], cmap=plt.cm.gray)
    ax1.axis('off')

    ax2.imshow(characters[1], cmap=plt.cm.gray)
    ax2.axis('off')

    ax3.imshow(characters[2], cmap=plt.cm.gray)
    ax3.axis('off')

    plt.show()
    '''

    return characters


def resize_image(image, box_size):  # box size should be 50, consistent with what was used
    return transform.resize(image, (box_size, box_size), anti_aliasing=False, preserve_range=True)


def binarise_grayscale(image, threshold):  # works only on ranges 0...1
    image[image > 0.5] = 1  # set to white
    image[image <= 0.5] = 0  # set to black

    return 1-image  # invert the colour values


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
