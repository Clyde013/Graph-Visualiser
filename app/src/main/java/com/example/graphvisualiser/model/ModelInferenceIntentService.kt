package com.example.graphvisualiser.model

import android.app.IntentService
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import androidx.core.net.toUri
import com.example.graphvisualiser.R
import com.example.graphvisualiser.processImageInput
import com.example.graphvisualiser.runModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.*
import java.lang.reflect.Type


class ModelInferenceIntentService(): IntentService("ModelInferenceIntentService") {
    companion object Constants{
        val SUCCESS = 1
        val FAILED = 0
    }

    lateinit var resultReceiver: ResultReceiver

    override fun onHandleIntent(intent: Intent?) {
        resultReceiver = intent!!.getParcelableExtra("receiver")!!

        /* this was for google text recogniser
        val rectType: Type = object: TypeToken<ArrayList<Rect>>(){}.type
        val boundingRects = Gson().fromJson<ArrayList<Rect>>(intent.getStringExtra("boundingRects"), rectType)
        val bitmapSizeType: Type = object: TypeToken<ArrayList<Int>>(){}.type
        val maxBitmapSizes = Gson().fromJson<ArrayList<Int>>(intent.getStringExtra("maxBitmapSizes"), bitmapSizeType)
         */

        val modelFile = intent.getSerializableExtra("modelFile") as File
        val bmpFile = intent.getSerializableExtra("bmpFile") as File
        var bmp = BitmapFactory.decodeFile(bmpFile.path)    // retrieve bitmap from disk

        bmp = rotateImageIfRequired(bmp, bmpFile.toUri())

        /* google text recogniser
        // combine bounded bitmaps into 1 big bitmap
        Log.i("model", "Original bitmap of width ${bmp.width} and height ${bmp.height}")
        val comboBitmap = Bitmap.createBitmap(maxBitmapSizes[0] + 100, (maxBitmapSizes[1] + 50) * boundingRects.size, Bitmap.Config.ARGB_8888)
        Log.i("model", "Created bitmap of width ${comboBitmap.width} and height ${comboBitmap.height}")

        val comboImage = Canvas(comboBitmap)
        val rectPaint = Paint()
        rectPaint.style = Paint.Style.FILL
        rectPaint.color = Color.rgb(255, 255, 255)
        comboImage.drawRect(0f, 0f, comboImage.width.toFloat(), comboImage.height.toFloat(), rectPaint)    // fill the bitmap

        var runningHeight = 0f
        for (boundingRect in boundingRects.indices){
            val boundingBox = boundingRects[boundingRect]
            Log.i("model", "bounding box: ${boundingBox.left}, ${boundingBox.top}, ${boundingBox.bottom}, ${boundingBox.width()}, ${boundingBox.height()}")
            val tempBitmap = Bitmap.createBitmap(bmp, boundingBox.left, boundingBox.top, boundingBox.width(), boundingBox.height())
            comboImage.drawBitmap(tempBitmap, 0f, runningHeight, null)
            runningHeight += boundingBox.height() + 50
        }
        Log.i("model", "combo bitmap final size, width: ${comboBitmap.width}, height: ${comboBitmap.height}")

        val bm: InputStream = resources.openRawResource(R.raw.justin_coords)
        val bufferedInputStream = BufferedInputStream(bm)
        val rawbmp = BitmapFactory.decodeStream(bufferedInputStream)
        val nh = (bmp.height * (512.0 / bmp.width)).toInt()
        val resbmp = Bitmap.createScaledBitmap(rawbmp, 512, nh, true)

        bmp = Bitmap.createScaledBitmap(bmp, 512, nh, true)

        Log.i("model colorspace", "picture bitmap: ${bmp.colorSpace?.getMaxValue(0)}, res bitmap: ${resbmp.colorSpace?.getMaxValue(0)}")
        */

        try {
            Log.i("model filepath", "in service intent ${File(filesDir, "combinedBitmap").path}")
            FileOutputStream(File(filesDir, "combinedBitmap")).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val output = processImageInput(this,  bmp)   // send through python preprocessing pipeline

        // grouped into array of arrays, each inner array represent a coordinate, containing the imageArrays of characters sorted in order
        val coordinates = output?.first
        val commaIndices = output?.second   // indices of the commas, as manually identified by algorithm
        val predictedCoordinates = arrayListOf<ArrayList<String>>()
        if (coordinates != null && commaIndices != null) {
            var index = 0
            for (coordinate in coordinates) {
                val predictedCoordinate = arrayListOf<String>()
                for (character in coordinate.indices) {
                    if (index in commaIndices){     // if identified as a comma
                        predictedCoordinate.add(",")
                    } else {    // unidentified character, run through model
                        predictedCoordinate.add(
                                runModel(
                                        this,
                                        modelFile,
                                        coordinate[character]
                                ) ?: ""
                        )
                    }
                    index++
                }
                predictedCoordinates.add(predictedCoordinate)
            }
        } else {
            Log.i("model", "no characters detected")
            resultReceiver.send(FAILED, Bundle.EMPTY)
        }

        val bundle = Bundle()
        bundle.putString("data", Gson().toJson(predictedCoordinates))
        resultReceiver.send(SUCCESS, bundle)
    }


    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap? {
        val ei = ExifInterface(selectedImage.path!!)
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}