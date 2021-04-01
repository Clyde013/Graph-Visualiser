import tensorflow as tf
import argparse

parser = argparse.ArgumentParser(description='Train or test models.')
parser.add_argument('--model_directory', type=String, help='Set directory of the model', default="saved_model"
                                                                                                 "/cnn_model_96.82")
args = parser.parse_args()

model_directory = args.model_directory

# Convert the model
converter = tf.lite.TFLiteConverter.from_saved_model(model_directory) # path to the SavedModel directory
tflite_model = converter.convert()

# Save the model.
with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
