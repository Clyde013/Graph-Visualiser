import tensorflow as tf
import numpy as np
from file_manager import *
from image_processing import image_processing_rank_filter as rf
import one_hot
import matplotlib.pyplot as plt
import argparse
import math
from image_processing import image_processing_region_segmentation as rs

gpus = tf.config.experimental.list_physical_devices('GPU')
if gpus:
    try:
        # Currently, memory growth needs to be the same across GPUs
        for gpu in gpus:
            tf.config.experimental.set_memory_growth(gpu, True)
        logical_gpus = tf.config.experimental.list_logical_devices('GPU')
        print(len(gpus), "Physical GPUs,", len(logical_gpus), "Logical GPUs")
    except RuntimeError as e:
        # Memory growth must be set before GPUs have been initialized
        print(e)

# feature data already flattened from original box size of 50
test_data = loadPickle('./data/test/test.pickle')
train_data = loadPickle('./data/train/train.pickle')
# loaded images are in cmap='gray' default where black is 0 and white is 1

classes = loadClasses('classes.txt')  # labels for one_hot

train_images = np.empty((len(train_data), 50, 50, 1))
train_labels = np.empty(len(train_data))

for i in range(len(train_data)):
    train_labels[i] = one_hot.decode_index(train_data[i]['label'])
    train_images[i] = np.array(train_data[i]['features']).reshape((50, 50, 1))

test_images = np.empty((len(test_data), 50, 50, 1))
test_labels = np.empty(len(test_data))

for i in range(len(test_data)):
    test_labels[i] = one_hot.decode_index(test_data[i]['label'])
    test_images[i] = np.array(test_data[i]['features']).reshape((50, 50, 1))


def train_model():
    model = tf.keras.Sequential([tf.keras.layers.Conv2D(32, (3, 3), activation='relu', input_shape=(50, 50, 1)),
                                 tf.keras.layers.MaxPooling2D((2, 2)),
                                 tf.keras.layers.Conv2D(64, (3, 3), activation='relu'),
                                 tf.keras.layers.MaxPooling2D((2, 2)),
                                 tf.keras.layers.Conv2D(64, (3, 3), activation='relu'),
                                 tf.keras.layers.Flatten(),
                                 tf.keras.layers.Dense(64, activation='relu'),
                                 tf.keras.layers.Dense(len(classes))])  # output layer, same format as one_hot

    model.compile(optimizer='adam',
                  loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
                  metrics=['accuracy'])

    model.fit(train_images, train_labels, epochs=5)

    test_loss, test_acc = model.evaluate(test_images, test_labels, verbose=2)
    print('\nTest accuracy:', test_acc, '\n')

    # output probabilities instead with softmax output layer
    probability_model = tf.keras.Sequential([tf.keras.Input(shape=(50, 50, 1)), model, tf.keras.layers.Softmax()])
    # tf needs us to restate the input layer shape for some reason, otherwise it won't save
    probability_model.save('saved_model/cnn_model_{:.2f}'.format(test_acc * 100))
    return 'saved_model/cnn_model_{:.2f}'.format(test_acc * 100)


# default values
img_path = 'data/justin_coords_cropped.jpg'
threshold = 0.9  # threshold for determining pixel as white or black
model_path = 'saved_model/cnn_model_96.82'
cols = 4

parser = argparse.ArgumentParser(description='Train or test models.')
parser.add_argument('--train', type=bool, help='Set true to retrain a new model', default=False)
parser.add_argument('--threshold', type=float, help='Threshold used to determine whether a pixel is white or black',
                    default=threshold)
parser.add_argument('--img_path', type=str, help='Filepath to image to run model on. Default is "data/justin_coords_cropped.jpg"',
                    default=img_path)
parser.add_argument('--model_path', type=str, help='Filepath to model to run. Default is "saved_model/cnn_model_96.82"',
                    default=model_path)
parser.add_argument('--cols', type=int, help='Number of columns used to display inference results.',
                    default=cols)


args = parser.parse_args()

threshold = args.threshold
img_path = args.img_path
model_path = args.model_path
cols = args.cols

if args.train:
    print('- - - - - - - - - - - - - - - - - - - - TRAINING MODEL - - - - - - - - - - - - - - - - - - - -')
    model_path = train_model()

# load model
saved_model = tf.keras.models.load_model(model_path, compile=False)
saved_model.summary()  # print loaded model summary


def load_image_into_input_rank_filter(images):
    # step -1: crop borders of image to minimise white borders
    # step 0: resize image to fit 50x50
    # step 1: cast 0...255 range to 0...1 range for neural net
    # step 2: flatten into 1d np-array
    # step 3: profit???

    display_images = np.empty((len(images), 50, 50))
    input_images = np.empty((len(images), 50, 50, 1))
    for i in range(len(images)):
        grayscale_image = rf.load_image_as_grayscale(images[i])
        subtracted_image = rf.background_subtract_grayscale(grayscale_image).astype(
            np.float) / 255  # convert to 0...1 range
        binarised_image = rf.binarise_grayscale(subtracted_image, threshold, True).astype(np.uint8)
        cropped_image = rf.crop_borders(binarised_image)
        resized_image = rf.resize_image(cropped_image, 50)
        # remove strange in between values that pop up during resizing
        binarised_resized_image = rf.binarise_grayscale(resized_image, threshold, False)
        # reshaped_image = tf.reshape(binarised_resized_image, (2500,))  # flatten
        display_images[i] = binarised_resized_image
        input_images[i] = np.array(binarised_resized_image).reshape((50, 50, 1))

    return display_images, input_images


def load_image_into_input_region_segmentation(image_filepath):
    # this function can take a single image and extract all individual characters
    img = rs.load_image_as_grayscale(image_filepath)
    characters = rs.region_segmentation(img)

    display_images = np.empty((len(characters), 50, 50))
    input_images = np.empty((len(characters), 50, 50, 1))

    for i in range(len(characters)):
        cropped_image = rs.crop_borders(characters[i])
        resized_image = rs.resize_image(cropped_image, 50)
        # remove strange in between values that pop up during resizing
        binarised_resized_image = rs.binarise_grayscale(resized_image, threshold)

        display_images[i] = binarised_resized_image
        input_images[i] = np.array(binarised_resized_image).astype(np.float32).reshape((50, 50, 1))

    return display_images, input_images

'''
display_images, input_images = load_image_into_input_rank_filter([img_path, 'data/justin_1.jpg', 'data/justin_2.jpg',
                                                      'data/justin_3.jpg', 'data/another_justin_4.jpg',
                                                      'data/justin_5.jpg', 'data/justin_8.jpg',
                                                      'data/another_justin_9.jpg', 'data/justin_0.jpg',
                                                      'data/justin_beta.jpg'])
'''

display_images, input_images = load_image_into_input_region_segmentation(img_path)
print(display_images)

predictions = saved_model.predict(input_images)
rows = math.ceil(len(display_images) / cols)
fig, axes = plt.subplots(rows, cols)

fig.subplots_adjust(hspace=0.2)

for i, ax in enumerate(axes.flatten()):
    if i < len(display_images):
        ax.imshow(display_images[i], vmin=0, vmax=1, cmap=plt.cm.gray)
        ax.set_title('predicted: ' + classes[np.argmax(predictions[i])])  # decode from bytes object
        ax.axis('off')

plt.show()
