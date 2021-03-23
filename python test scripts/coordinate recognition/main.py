import tensorflow as tf
import numpy as np
from file_manager import *
from image_processing import image_processing
import one_hot
import matplotlib.pyplot as plt
import argparse

# feature data already flattened from original box size of 50
test_data = loadPickle('./data/test/test.pickle')
train_data = loadPickle('./data/train/train.pickle')
# loaded images are in cmap='gray' default where black is 0 and white is 1


classes = loadClasses('classes.txt')  # labels for one_hot

train_images = np.empty((len(train_data), len(train_data[0]['features'])))
train_labels = np.empty(len(train_data))
for i in range(len(train_data)):
    train_labels[i] = one_hot.decode_index(train_data[i]['label'])
    train_images[i] = train_data[i]['features']

test_images = np.empty((len(test_data), len(test_data[0]['features'])))
test_labels = np.empty(len(test_data))
for i in range(len(test_data)):
    test_labels[i] = one_hot.decode_index(test_data[i]['label'])
    test_images[i] = test_data[i]['features']


def train_model():
    model = tf.keras.Sequential([tf.keras.Input(shape=(2500,)),  # set input shape as it is 50x50 flattened image
                                 tf.keras.layers.Dense(128, activation='relu'),
                                 tf.keras.layers.Dense(84, activation='relu'),
                                 tf.keras.layers.Dropout(0.1),
                                 tf.keras.layers.Dense(128, activation='relu'),
                                 tf.keras.layers.Dense(84, activation='relu'),
                                 tf.keras.layers.Dense(len(classes))])  # output layer, same format as one_hot

    model.compile(optimizer='adam',
                  loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
                  metrics=['accuracy'])

    model.fit(train_images, train_labels, epochs=10)

    test_loss, test_acc = model.evaluate(test_images, test_labels, verbose=2)
    print('\nTest accuracy:', test_acc, '\n')

    # output probabilities instead with softmax output layer
    probability_model = tf.keras.Sequential([tf.keras.Input(shape=(2500,)), model, tf.keras.layers.Softmax()])
    # tf needs us to restate the input layer shape for some reason, otherwise it won't save
    probability_model.save('saved_model/model_{:.2f}'.format(test_acc * 100))


# default values
img_path = 'data/justin_pi.jpg'
threshold = 0.9  # threshold for determining pixel as white or black
model_path = 'saved_model/model_85.20'

parser = argparse.ArgumentParser(description='Train or test models.')
parser.add_argument('--train', type=bool, help='Set true to retrain a new model', default=False)
parser.add_argument('--threshold', type=float, help='Threshold used to determine whether a pixel is white or black',
                    default=threshold)
parser.add_argument('--img_path', type=str, help='Filepath to image to run model on. Default is "data/justin2.jpg"',
                    default=img_path)
parser.add_argument('--model_path', type=str, help='Filepath to model to run. Default is "saved_model/model_85.07"',
                    default=model_path)

args = parser.parse_args()

if args.train:
    print('- - - - - - - - - - - - - - - - - - - - TRAINING MODEL - - - - - - - - - - - - - - - - - - - -')
    train_model()

threshold = args.threshold
img_path = args.img_path
model_path = args.model_path

# load model
saved_model = tf.keras.models.load_model(model_path, compile=False)
saved_model.summary()   # print loaded model summary

# step -1: crop borders of image to minimise white borders
# step 0: resize image to fit 50x50
# step 1: cast 0...255 range to 0...1 range for neural net
# step 2: flatten into 1d np-array
# step 3: profit???

grayscale_image = image_processing.load_image_as_grayscale(img_path)
subtracted_image = image_processing.background_subtract_grayscale(grayscale_image).astype(np.float) / 255  # convert to 0...1 range
binarised_image = image_processing.binarise_grayscale(subtracted_image, threshold, True).astype(np.uint8)
cropped_image = image_processing.crop_borders(binarised_image)
resized_image = image_processing.resize_image(cropped_image, 50)
binarised_resized_image = image_processing.binarise_grayscale(resized_image, threshold, False)  # remove strange in between values that pop up during resizing
reshaped_image = tf.reshape(resized_image, (2500,))  # flatten

# expand dimensions because predict works on batches, while we only have single input
prediction = saved_model.predict(np.expand_dims(reshaped_image, 0))

plt.figure(figsize=(5, 5))
plt.imshow(resized_image, vmin=0, vmax=1, cmap=plt.cm.gray)
plt.xlabel('predicted output: ' + classes[np.argmax(prediction[0])].decode('utf-8'))    # decode from bytes object
plt.show()