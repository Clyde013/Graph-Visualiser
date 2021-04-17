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

import functools
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
        if width > math.floor(0.5 * len(labeled_characters[0])) and height > math.floor(0.5 * len(labeled_characters)):
            obj_img = remove_axes(obj_img, xs, ys)

        '''
        print("new subplot")
        print(xs)
        print(ys)
        fig, (ax1) = plt.subplots(1, 1, figsize=(5, 5))
        ax1.imshow(obj_img, cmap=plt.cm.gray)
        ax1.axis('off')
        plt.show()
        '''

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
    #comma_indices = list()
    running_height = list()
    for label in range(1, nr_objects + 1):
        ys, xs = np.where(labeled == label)
        topleft = (xs.min(), ys.min())
        width, height = xs.max() - xs.min() + 1, ys.max() - ys.min() + 1

        '''
        running_height.append(height)
        median_height = np.median(running_height)

        diff = abs(height - median_height)
        if diff >= median_height * 1 / 2:
            comma_indices.append(label - 1)
            running_height.pop()
        '''
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
        # fill[ys - topleft[1], xs - topleft[0]] = 1
        fill[:height, :width] = segmentation[topleft[1]:topleft[1] + height, topleft[0]: topleft[0] + width] - 1

        # mark character bounding boxes and add padding to the width so they can overlap with neighbouring characters
        feature_grouping_map[topleft[1]:topleft[1] + height,
        max(topleft[0] - math.ceil(width * 1.3), 0): min(topleft[0] + math.ceil(width * 1.3),
                                                         len(feature_grouping_map[0]))] = 1

        # width: 4 - 1 = 3. set width to 3+1 = 4
        # 0 1 2 3 4 5   (size: 6)
        # fill shape = width:   0 1 2 3
        # fill[:width] = fill[:4] = 0 1 2 3
        # 1 2 3 4 [1: 1+width] = [1:5]


        characters_info.append((topleft[0], topleft[1], fill, comma))
        #characters_info.append((topleft[0], topleft[1], fill))

    '''
    labeled_coord_groups, number_of_coords = ndi.label(feature_grouping_map)
    characters = [[] for i in range(number_of_coords)]

    for character_info in characters_info:
        x, y, character = character_info
        characters[labeled_coord_groups[y][x] - 1].append((x, character))

    result = list()
    for coordinate in characters:
        if coordinate:  # if coordinate not empty
            coordinate.sort(key=functools.cmp_to_key(comparator))
            for character in coordinate:
                result.append(character[1])
    '''

    labeled_coord_groups, number_of_coords = ndi.label(feature_grouping_map)
    characters = [[] for i in range(number_of_coords)]

    for character_info in characters_info:
        x, y, character, comma = character_info
        characters[labeled_coord_groups[y][x] - 1].append((x, character, comma))

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
    if a[0] < b[0]:  # character a to the left of b
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

    print(image)

    if height > width:
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


# assuming an axes is passed in, removes the axes without affecting numbers that overlap with the axes
def remove_axes(axes, xs, ys):
    return np.zeros_like(axes)
    ''' subpar algorithm. does not work cleanly. given more time could possibly improve it. but i don't have that time
    print("removing the axes")
    fig, (ax1) = plt.subplots(1, 1, figsize=(5, 5))
    ax1.imshow(axes, cmap=plt.cm.gray)
    ax1.axis('off')
    plt.show()

    # average width/height of the axes. assume the user should be using a consistent pen thickness
    median = list()

    # for x axes
    unique = np.unique(xs)
    print("length of unique:", len(unique))
    for xcoord in range(len(unique)):
        where = np.where(xs == unique[xcoord])[0]
        corresponding_y = ys[where]

        if not median:
            median.append(corresponding_y.max() - corresponding_y.min())

        if corresponding_y.max() - corresponding_y.min() > np.median(median) * 3:  # width more than average = belongs to a number or y axis
            # do nothing
            continue
        else:  # part of x axes. remove. update avg_thickness
            axes[corresponding_y, unique[xcoord]] = 0
            median.append(corresponding_y.max() - corresponding_y.min())

    fig, (ax1) = plt.subplots(1, 1, figsize=(5, 5))
    ax1.imshow(axes, cmap=plt.cm.gray)
    ax1.axis('off')
    plt.show()

    # for y axes
    unique = np.unique(ys)
    for ycoord in range(len(unique)):
        where = np.where(ys == unique[ycoord])[0]
        corresponding_x = xs[where]

        if not median:
            median.append(corresponding_x.max() - corresponding_x.min())

        if corresponding_x.max() - corresponding_x.min() > np.median(median) * 3:  # width more than average = probably belongs to a number
            # do nothing
            continue
        else:  # part of axes. remove
            axes[unique[ycoord], corresponding_x] = 0
            median.append(corresponding_x.max() - corresponding_x.min())

    print("average thickness", np.median(median))
    print(median)

    fig, (ax1) = plt.subplots(1, 1, figsize=(5, 5))
    ax1.imshow(axes, cmap=plt.cm.gray)
    ax1.axis('off')
    plt.show()

    return axes
    '''


'''
fig, (ax1) = plt.subplots(1, 1, figsize=(5, 5))

ax1.imshow(binarise_grayscale(load_image_as_grayscale('test.jpeg')), vmin=0, vmax=255, cmap=plt.cm.gray)
ax1.axis('off')

plt.show()

'''
