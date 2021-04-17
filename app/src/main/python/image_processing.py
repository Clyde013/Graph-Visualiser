from skimage.io import imread
import numpy as np

from skimage.filters.rank import median
from skimage.morphology import disk

from skimage import img_as_ubyte
from skimage import transform
from skimage import util
from skimage.color import rgb2gray
from skimage.filters import sobel
from skimage.segmentation import watershed

from scipy import ndimage as ndi

import functools
import math


def load_image_as_grayscale(image_as_bytes):
    image = imread(image_as_bytes, plugin='imageio')
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

    subtracted[subtracted > 0.8 * 255] = 255  # set to white
    subtracted[subtracted <= 0.8 * 255] = 0  # set to black

    subtracted = median(subtracted, disk(3))  # remove salt and pepper noise

    return subtracted


def region_segmentation(image):
    # region based segmentation https://scikit-image.org/docs/dev/user_guide/tutorial_segmentation.html
    # assuming we take in the grayscale image as input

    elevation_map = sobel(image)

    markers = np.zeros_like(image)
    markers[image < 100] = 2
    markers[image > 150] = 1

    segmentation = watershed(elevation_map, markers)
    filled_segmentation = ndi.binary_fill_holes(segmentation - 1).astype(np.uint8)

    # label only works on filled segmentation
    labeled_characters, _ = ndi.label(filled_segmentation, structure=ndi.generate_binary_structure(2, 2))

    # parameters
    max_dilat = 5  # dilation (in number of pixels) for a small object
    sz_small = 100  # size of a small object (max dilated)
    sz_big = 10000  # size of a big object (not dilated)
    result = np.zeros_like(labeled_characters)

    # for each detected object
    for obj_id in range(1, _ + 1):
        # creates a binary image with the current object
        obj_img = (labeled_characters == obj_id)

        ys, xs = np.where(obj_img == 1)
        width, height = xs.max() - xs.min() + 1, ys.max() - ys.min() + 1
        # if it is more than 50% of width and height of the image, we assume it is the axes
        if width > math.floor(0.5 * len(image[0])) and height > math.floor(0.5 * len(image)):
            obj_img = remove_axes(obj_img, xs, ys)

        # computes object's area
        area = np.sum(obj_img)
        # dilatation factor inversely proportional to area
        dfac = int(max_dilat * (1 - min(1, (max(0, area - sz_small) / sz_big))))
        # dilates object
        dilat = ndi.binary_dilation(obj_img, iterations=dfac)
        # overlays dilated object onto result
        result += dilat

    labeled, nr_objects = ndi.label(result > 0, structure=ndi.generate_binary_structure(2, 2))

    feature_grouping_map = np.zeros_like(segmentation)  # use this to group coordinates as features

    characters_info = list()
    running_height = list()
    for label in range(1, nr_objects + 1):
        ys, xs = np.where(labeled == label)
        topleft = (xs.min(), ys.min())
        width, height = xs.max() - xs.min() + 1, ys.max() - ys.min() + 1

        comma = False
        running_height.append(height)
        median_height = np.median(running_height)

        diff = abs(height - median_height)
        if diff >= median_height * 1 / 2:
            running_height.pop()
            comma = True

        # instead of np.reshape which will throw error since our values will not be perfectly square shape
        # (e.g. open bracket '(' ) and the number of values that were labelled may be less than shape[0]*shape[1]
        fill = np.zeros(shape=(height, width))
        fill[:height, :width] = segmentation[topleft[1]:topleft[1] + height, topleft[0]: topleft[0] + width] - 1

        # mark character bounding boxes and add padding to the width so they can overlap with neighbouring characters
        feature_grouping_map[topleft[1]:topleft[1] + height, max(topleft[0] - math.ceil(width * 1.3), 0): min(topleft[0] + math.ceil(width * 1.3), len(feature_grouping_map[0]))] = 1

        characters_info.append((topleft[0], topleft[1], fill, comma))

    labeled_coord_groups, number_of_coords = ndi.label(feature_grouping_map)
    characters = [[] for i in range(number_of_coords)]

    for character_info in characters_info:
        x, y, character, comma = character_info
        characters[labeled_coord_groups[y][x]-1].append((x, character, comma))

    result = []
    comma_indices = list()
    index = 0
    for coordinate in characters:
        if coordinate:  # if coordinate not empty
            coordinate.sort(key=functools.cmp_to_key(comparator))
            coordinate_characters = []
            for character in coordinate:
                coordinate_characters.append(character[1])
                if character[2]:
                    comma_indices.append(index)
                index += 1
            result.append(coordinate_characters)

    return result, comma_indices


def comparator(a, b):
    if a[0] < b[0]:   # character a to the left of b
        return -1
    else:
        return 1


def resize_image(image, box_size):  # box size should be 50, consistent with what was used
    return transform.resize(image, (box_size, box_size), anti_aliasing=False, preserve_range=True)


def binarise_grayscale(image):  # works only on ranges 0...1
    image[image > 0.5] = 1  # set to white
    image[image <= 0.5] = 0  # set to black

    return 1 - image  # invert the colour values


def crop_borders(image):
    height = len(image)
    width = len(image[0])

    if height > width:  # pad the image so that it's square, centering the original character
        result = np.zeros((height, height))
        result_center = math.floor(height / 2)
        image_half = math.ceil(width / 2)
        result[:, result_center - image_half:result_center + (width - image_half)] = image
    else:
        result = np.zeros((width, width))
        result_center = math.floor(width / 2)
        image_half = math.ceil(height / 2)
        result[result_center - image_half:result_center + (height - image_half), :] = image

    return result


def remove_axes(axes, xs, ys):
    return np.zeros_like(axes)