import tensorflow as tf
import numpy as np
from file_manager import *
import one_hot

# feature data already flattened from original box size of 50
test_data = loadPickle('./data/test/test.pickle')
train_data = loadPickle('./data/train/train.pickle')

classes = loadClasses('classes.txt')    # labels for one_hot

model = tf.keras.Sequential([tf.keras.layers.Dense(128, activation='relu'),
                            tf.keras.layers.Dense(len(classes))])    # output layer, same format as one_hot

model.compile(optimizer='adam',
              loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True),
              metrics=['accuracy'])

train_images = np.empty((len(train_data), len(train_data[0]['features'])))
train_labels = np.empty(len(train_data))
for i in range(len(train_data)):
    train_labels[i] = one_hot.decode_index(train_data[i]['label'])
    train_images[i] = train_data[i]['features']

model.fit(train_images, train_labels, epochs=10)

test_images = np.empty((len(test_data), len(test_data[0]['features'])))
test_labels = np.empty(len(test_data))
for i in range(len(test_data)):
    test_labels[i] = one_hot.decode_index(test_data[i]['label'])
    test_images[i] = test_data[i]['features']


test_loss, test_acc = model.evaluate(test_images, test_labels, verbose=2)
print('\nTest accuracy:', test_acc)

model.save('saved_model/model_1')


# saved_model = tf.keras.models.load_model('saved_model/model_1')
