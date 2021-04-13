import numpy as np
import image_processing as rs
import io
import matplotlib.pyplot as plt


def load_image_into_input(image_bytes):  # passed in as bytearray
    # this function can take a single image and extract all individual characters
    image = bytes(image_bytes)
    img = rs.load_image_as_grayscale(image)

    characters, comma_indices = rs.region_segmentation(img)

    input_images = [[np.empty((50, 50, 1)) for j in range(len(characters[i]))] for i in range(len(characters))]

    for i in range(len(characters)):
        for j in range(len(characters[i])):
            cropped_image = rs.crop_borders(characters[i][j])
            resized_image = rs.resize_image(cropped_image, 50)
            # remove strange in between values that pop up during resizing
            binarised_resized_image = rs.binarise_grayscale(resized_image)

            input_images[i][j] = np.array(binarised_resized_image, dtype=np.byte).reshape((50, 50, 1))

    return input_images, comma_indices


def plot_image(image_bytes):
    image = bytes(image_bytes)

    grayscale_image = rs.load_image_as_grayscale(image)
    subtracted_image = rs.background_subtract_grayscale(grayscale_image).astype(
        np.float) / 255  # convert to 0...1 range
    binarised_image = rs.binarise_grayscale(subtracted_image, 0.9, True).astype(np.uint8)
    cropped_image = rs.crop_borders(binarised_image)
    resized_image = rs.resize_image(cropped_image, 50)
    # remove strange in between values that pop up during resizing
    binarised_resized_image = rs.binarise_grayscale(resized_image, 0.9, False)
    input_image = np.array(binarised_resized_image, dtype=np.byte).reshape((50, 50))

    plt.imshow(input_image)

    f = io.BytesIO()
    plt.savefig(f, format='png')
    return f.getvalue()
