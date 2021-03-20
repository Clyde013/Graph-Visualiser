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
                                 tf.keras.layers.Dense(64, activation='relu'),
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
img_path = 'data/justin2.jpg'
threshold = 0.75  # threshold for determining pixel as white or black
model_path = 'saved_model/model_85.07'

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

# step 0: resize image to fit 50x50
# step 1: cast 0...255 range to 0...1 range for neural net
# step 2: flatten into 1d np-array
# step 3: profit???

grayscale_image = image_processing.load_image_as_grayscale(img_path)
binarised_image = image_processing.binarise_grayscale(grayscale_image)
# for some reason resizing changes the value range from 0-255 to 0-1. But hey I'm not complaining, works to my advantage
resized_image = image_processing.resize_image(binarised_image, 50)

resized_image[resized_image > threshold] = 1  # set to white
resized_image[resized_image <= threshold] = 0  # set to black
resized_image = resized_image.astype(np.uint8)

reshaped_image = tf.reshape(resized_image, (2500,))  # flatten

# expand dimensions because predict works on batches, while we only have single input
prediction = saved_model.predict(np.expand_dims(reshaped_image, 0))

plt.figure(figsize=(5, 5))
plt.imshow(resized_image, vmin=0, vmax=1, cmap=plt.cm.gray)
plt.xlabel('predicted output: ' + classes[np.argmax(prediction[0])].decode('utf-8'))    # decode from bytes object
plt.show()

'''
def plot_image(i, predictions_array, true_label, img):
    true_label, img = true_label[i].astype(int), img[i]
    plt.grid(False)
    plt.xticks([])
    plt.yticks([])

    img = tf.reshape(img, (50, 50))
    plt.imshow(img, cmap=plt.cm.gray)

    predicted_label = np.argmax(predictions_array)
    if predicted_label == true_label:
        color = 'blue'
    else:
        color = 'red'
    plt.xlabel("{} {:2.0f}% ({})".format(classes[predicted_label],
                                         100 * np.max(predictions_array),
                                         classes[true_label]),
               color=color)


def plot_value_array(i, predictions_array, true_label):
    true_label = true_label[i].astype(int)
    plt.grid(False)
    plt.xticks(range(len(predictions_array)))
    plt.yticks([])
    thisplot = plt.bar(range(len(predictions_array)), predictions_array, color="#777777")
    plt.ylim([0, 1])
    predicted_label = np.argmax(predictions_array)

    thisplot[predicted_label].set_color('red')
    thisplot[true_label].set_color('blue')



predictions = saved_model.predict(test_images)
# Plot the first X test images, their predicted labels, and the true labels.
# Color correct predictions in blue and incorrect predictions in red.
num_rows = 5
num_cols = 3
num_images = num_rows * num_cols
plt.figure(figsize=(2 * 2 * num_cols, 2 * num_rows))
for i in range(num_images):
    plt.subplot(num_rows, 2 * num_cols, 2 * i + 1)
    plot_image(i, predictions[i], test_labels, test_images)
    plt.subplot(num_rows, 2 * num_cols, 2 * i + 2)
    plot_value_array(i, predictions[i], test_labels)
plt.tight_layout()
plt.show()
'''
