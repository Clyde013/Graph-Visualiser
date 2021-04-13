package com.example.graphvisualiser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executor


val input_shape = arrayListOf<Int>(50, 50, 1)   // 50x50 single colour channel grayscale image
val output_shape = arrayListOf<Int>(27)     // array of probabilities

/* custom model */
fun downloadModel(viewModel: MyViewModel?) {
    val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()

    FirebaseModelDownloader.getInstance()
            .getModel("Alphanumeric-Classifier", DownloadType.LOCAL_MODEL, conditions)
            .addOnCompleteListener { customModel ->
                customModel.addOnSuccessListener {
                    // Download complete or already downloaded. Enable ML features
                    viewModel?.modelFile?.postValue(it.file)
                }
            }
}

fun processImageInput(context: Context, image: Bitmap): Pair<Array<Array<Array<Array<FloatArray>>>>, Array<Int>>?{    // start python pipeline
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
        val output = mainModule.callAttr("load_image_into_input", byteArray).asList()
        val imageArray = output[0].toJava(Array<Array<Array<Array<FloatArray>>>>::class.java)   // for some reason it returns the np array as dtype float although i specified np.byte. No harm done it reduces conversions in the long run
        val commaIndices = output[1].toJava(Array<Int>::class.java)
        Pair(imageArray, commaIndices)
    }catch(e: PyException){
        Toast.makeText(context, "something went wrong processing image input :(", Toast.LENGTH_SHORT).show()
        null
    }
}

fun plotImageInput(context: Context, image: Bitmap): Bitmap?{    // start python pipeline
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
        val bytes = mainModule.callAttr("plot_image", byteArray).toJava(ByteArray::class.java)
        Log.i("model plot", "plotted image")
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }catch(e: PyException){
        e.printStackTrace()
        null
    }
}

fun runModel(context:Context, modelFile: File, imageArray: Array<Array<FloatArray>>?): String?{
    if (imageArray == null){
        return null
    }

    val interpreter = Interpreter(modelFile)
    val input = ByteBuffer.allocateDirect(input_shape[0] * input_shape[1] * input_shape[2] * 4).order(ByteOrder.nativeOrder())   // *4 because using inputtype float32

    for (row in imageArray){
        for (col in row){
            for (float in col){  // processing outputs a byte array of 1 and 0
                input.putFloat(float)
            }
        }
    }

    val bufferSize = output_shape[0] * java.lang.Float.SIZE / java.lang.Byte.SIZE
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

    return null
}

/* google provided model for text recognition */
fun recognizeText(image: InputImage, viewModel: MyViewModel) {

    // [START get_detector_default]
    val recognizer = TextRecognition.getClient()
    // [END get_detector_default]

    // [START run_detector]
    val result: Task<Text> = recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                // [START_EXCLUDE]
                // [START get_text]
                val boundingBitmaps = ArrayList<Bitmap>()
                for (block in visionText.textBlocks) {
                    Log.i("google model", "identified text block")
                    for (line in block.lines) {
                        for (element in line.elements) {
                            val boundingBox: Rect = element.boundingBox!!
                            val cornerPoints: Array<Point> = element.cornerPoints!!
                            val text = element.text
                            Log.i("google model", "identified text $text at ${cornerPoints.size} corner points. first corner: ${cornerPoints[0].toString()}")
                            boundingBitmaps.add(Bitmap.createBitmap(image.bitmapInternal!!, boundingBox.left, boundingBox.top, boundingBox.width(), boundingBox.height()))
                        }
                    }
                }
                viewModel.boundingBoxes.value = boundingBitmaps
                Log.i("google model", "all text identified")
                // [END get_text]
                // [END_EXCLUDE]
            }
            .addOnFailureListener {
                // Task failed with an exception
                // ...
                Log.e("google sucks", "text recogniser failed")
            }
    // [END run_detector]
}