package com.example.graphvisualiser

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import java.io.ByteArrayOutputStream

val input_shape = arrayListOf<Int>(50, 50, 1)   // 50x50 single colour channel grayscale image
val output_shape = arrayListOf<Int>(27)     // array of probabilities

fun downloadModel(viewModel: MyViewModel) {
    val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()
            .build()

    FirebaseModelDownloader.getInstance()
            .getModel("Alphanumeric-Classifier", DownloadType.LOCAL_MODEL, conditions)
            .addOnSuccessListener {
                // Download complete or already downloaded. Enable ML features
                viewModel.modelAvailable.postValue(it.file)

            }
}

fun processImageInput(context: Context, image: Bitmap): ArrayList<Int>?{    // start python pipeline
    if (!Python.isStarted()){
        Python.start(AndroidPlatform(context))
    }

    val py = Python.getInstance()
    val mainModule = py.getModule("pipeline")

    val stream = ByteArrayOutputStream()
    image.compress(Bitmap.CompressFormat.PNG, 100, stream)
    val byteArray: ByteArray = stream.toByteArray()
    image.recycle()

    try{
        val imageDimensions = Array(50) {Array(50) { ByteArray(1) {0} } }    // init 3d bytearray (50x50 dims and 1 grayscale channel)
        val imageArray = mainModule.callAttr("load_image_into_input", byteArray).toJava(imageDimensions.javaClass)
        Log.i("python", imageArray[0][0][0].toString())
        return null
    }catch(e: PyException){
        Toast.makeText(context, "something went wrong processing image input :(", Toast.LENGTH_SHORT).show()
        return null
    }
}