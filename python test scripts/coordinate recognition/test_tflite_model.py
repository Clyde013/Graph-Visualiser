import numpy as np
import tensorflow as tf
from image_processing import image_processing
from file_manager import *
import one_hot


def load_image_into_input(images):
    # step -1: crop borders of image to minimise white borders
    # step 0: resize image to fit 50x50
    # step 1: cast 0...255 range to 0...1 range for neural net
    # step 2: flatten into 1d np-array
    # step 3: profit???

    display_images = np.empty((len(images), 50, 50))
    input_images = np.empty((len(images), 50, 50, 1))
    for i in range(len(images)):
        grayscale_image = image_processing.load_image_as_grayscale(images[i])
        subtracted_image = image_processing.background_subtract_grayscale(grayscale_image).astype(
            np.float) / 255  # convert to 0...1 range
        binarised_image = image_processing.binarise_grayscale(subtracted_image, 0.9, True).astype(np.uint8)
        cropped_image = image_processing.crop_borders(binarised_image)
        resized_image = image_processing.resize_image(cropped_image, 50)
        # remove strange in between values that pop up during resizing
        binarised_resized_image = image_processing.binarise_grayscale(resized_image, 0.9, False)
        # reshaped_image = tf.reshape(binarised_resized_image, (2500,))  # flatten
        display_images[i] = binarised_resized_image
        input_images[i] = np.array(binarised_resized_image).reshape((50, 50, 1))

    return display_images, input_images


classes = loadClasses('classes.txt')  # labels for one_hot

# Load the TFLite model and allocate tensors.
interpreter = tf.lite.Interpreter(model_path="model.tflite")
interpreter.allocate_tensors()

# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

display_images, input_images = load_image_into_input(['data/justin_pi.jpg'])

interpreter.set_tensor(input_details[0]['index'], tf.cast(input_images, tf.float32))

interpreter.invoke()

# The function `get_tensor()` returns a copy of the tensor data.
# Use `tensor()` in order to get a pointer to the tensor.
output_data = interpreter.get_tensor(output_details[0]['index'])
print('predicted: ' + classes[np.argmax(output_data[0])])
