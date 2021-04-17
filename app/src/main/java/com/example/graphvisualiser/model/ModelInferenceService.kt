package com.example.graphvisualiser.model

import android.app.Service
import android.content.Intent
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.net.toUri
import com.google.gson.Gson
import java.io.*


class ModelInferenceService(): Service() {
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    lateinit var intent: Intent

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            onHandleIntent()

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }
    }

    override fun onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("custom service", "service started")

        this.intent = intent
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_NOT_STICKY
    }


    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("custom service", "service destroyed")
    }


    // if this is similar to intentservice that's because it is and i just made this a service
    companion object Constants{
        val SUCCESS = 1
        val FAILED = 0
    }

    lateinit var resultReceiver: ResultReceiver

    private fun onHandleIntent() {
        Log.i("custom service", "onHandleIntent started")
        resultReceiver = intent.getParcelableExtra("receiver")!!

         /* this was for google text recogniser
        val rectType: Type = object: TypeToken<ArrayList<Rect>>(){}.type
        val boundingRects = Gson().fromJson<ArrayList<Rect>>(intent.getStringExtra("boundingRects"), rectType)
        val bitmapSizeType: Type = object: TypeToken<ArrayList<Int>>(){}.type
        val maxBitmapSizes = Gson().fromJson<ArrayList<Int>>(intent.getStringExtra("maxBitmapSizes"), bitmapSizeType)
         */

        val modelFile = intent.getSerializableExtra("modelFile") as File
        val bmpFile = intent.getSerializableExtra("bmpFile") as File
        val bmp = rotateImageIfRequired(BitmapFactory.decodeFile(bmpFile.path), bmpFile.toUri()) // retrieve bitmap from disk
        val nh = (bmp.height * (2000f / bmp.width)).toInt()  // resize the bitmap
        Log.i("model bitmap resize", "${bmp.height}, ${bmp.width}, ${nh.toString()}")
        val resbmp = Bitmap.createScaledBitmap(bmp, 2000, nh, true)


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

        /*
        val bm: InputStream = resources.openRawResource(R.raw.justin_coords)
        val bufferedInputStream = BufferedInputStream(bm)
        val rawbmp = BitmapFactory.decodeStream(bufferedInputStream)
        val nh = (rawbmp.height * (2000 / rawbmp.width)).toInt()
        val resbmp = Bitmap.createScaledBitmap(rawbmp, 2000, nh, true)*/

        val output = processImageInput(this,  resbmp)   // send through python preprocessing pipeline

        // grouped into array of arrays, each inner array represent a coordinate, containing the imageArrays of characters sorted in order
        val coordinates = output?.first
        val commaIndices = output?.second   // indices of the commas, as manually identified by algorithm

        val predictedCoordinates = arrayListOf<ArrayList<String>>()
        if (coordinates != null && commaIndices != null && coordinates.isNotEmpty()) {
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


    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap {
        val ei = ExifInterface(selectedImage.path!!)
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }
}