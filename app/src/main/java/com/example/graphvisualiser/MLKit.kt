package com.example.graphvisualiser

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

// for human reference
val input_shape = arrayListOf<Int>(50, 50, 1)   // 50x50 single colour channel grayscale image
val output_shape = arrayListOf<Int>(27)     // array of probabilities

fun downloadModel(viewModel: MyViewModel?) {
    val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()

    FirebaseModelDownloader.getInstance()
            .getModel("Alphanumeric-Classifier", DownloadType.LOCAL_MODEL, conditions)
            .addOnCompleteListener { customModel ->
                customModel.addOnSuccessListener {
                    // Download complete or already downloaded. Enable ML features
                    viewModel?.modelPath?.postValue(it.file)
                }
            }
}

fun processImageInput(context: Context, image: Bitmap): Array<Array<ByteArray>>?{    // start python pipeline
    if (!Python.isStarted()){
        Python.start(AndroidPlatform(context))
    }

    val py = Python.getInstance()
    val mainModule = py.getModule("pipeline")

    val stream = ByteArrayOutputStream()
    image.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray: ByteArray = stream.toByteArray()
    image.recycle()

    return try{
        val imageDimensions = Array(50) {Array(50) { ByteArray(1) {0} } }    // init 3d bytearray (50x50 dims and 1 grayscale channel)
        val imageArray = mainModule.callAttr("load_image_into_input", byteArray).toJava(imageDimensions.javaClass)
        imageArray
    }catch(e: PyException){
        Toast.makeText(context, "something went wrong processing image input :(", Toast.LENGTH_SHORT).show()
        null
    }
}

fun runModel(context:Context, viewModel: MyViewModel, imageArray: Array<Array<ByteArray>>?): String?{
    if (imageArray == null){
        return null
    }

    if (viewModel.modelPath.value != null){
        val interpreter = Interpreter(viewModel.modelPath.value!!)
        val input = ByteBuffer.allocateDirect(50 * 50 * 1 * 4).order(ByteOrder.nativeOrder())   // *4 because using inputtype float32

        for (row in imageArray){
            for (col in row){
                for (byte in col){
                    input.put(byte)
                }
            }
        }

        val bufferSize = 27 * java.lang.Float.SIZE / java.lang.Byte.SIZE
        val modelOutput = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
        interpreter.run(input, modelOutput)

        modelOutput.rewind()
        val probabilities = modelOutput.asFloatBuffer()
        try {
            val reader = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.classes)))
            var highestProbability = -1f
            var predictedLabel = ""
            for (i in 0 until probabilities.capacity()) {
                val label: String = reader.readLine()
                val probability = probabilities.get(i)
                Log.i("model predictions", "$label: $probability")
                if (probability > highestProbability){
                    highestProbability = probability
                    predictedLabel = label
                }
            }
            return predictedLabel
        } catch (e: IOException){
            Log.e("running inference model error", "class tags not found")
            return null
        }
    }

    return null
}