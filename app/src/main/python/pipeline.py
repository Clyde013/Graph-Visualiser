import numpy as np
import image_processing
import io
import matplotlib.pyplot as plt

def load_image_into_input(image_bytes):  # passed in as bytearray
    # step -1: crop borders of image to minimise white borders
    # step 0: resize image to fit 50x50
    # step 1: cast 0...255 range to 0...1 range for neural net
    # step 2: flatten into 1d np-array
    # step 3: profit???

    image = bytes(image_bytes)

    grayscale_image = image_processing.load_image_as_grayscale(image)
    subtracted_image = image_processing.background_subtract_grayscale(grayscale_image).astype(
        np.float) / 255  # convert to 0...1 range
    binarised_image = image_processing.binarise_grayscale(subtracted_image, 0.9, True).astype(np.uint8)
    cropped_image = image_processing.crop_borders(binarised_image)
    resized_image = image_processing.resize_image(cropped_image, 50)
    # remove strange in between values that pop up during resizing
    binarised_resized_image = image_processing.binarise_grayscale(resized_image, 0.9, False)
    input_image = np.array(binarised_resized_image, dtype=np.byte).reshape((50, 50, 1))

    return input_image

def plot_image(image_bytes):
    image = bytes(image_bytes)

    grayscale_image = image_processing.load_image_as_grayscale(image)
    subtracted_image = image_processing.background_subtract_grayscale(grayscale_image).astype(
        np.float) / 255  # convert to 0...1 range
    binarised_image = image_processing.binarise_grayscale(subtracted_image, 0.9, True).astype(np.uint8)
    cropped_image = image_processing.crop_borders(binarised_image)
    resized_image = image_processing.resize_image(cropped_image, 50)
    # remove strange in between values that pop up during resizing
    binarised_resized_image = image_processing.binarise_grayscale(resized_image, 0.9, False)
    input_image = np.array(binarised_resized_image, dtype=np.byte).reshape((50, 50))

    plt.imshow(input_image)

    f = io.BytesIO()
    plt.savefig(f, format='png')
    return f.getvalue()
