package com.example.graphvisualiser.model

import android.app.IntentService
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import com.example.graphvisualiser.processImageInput
import com.example.graphvisualiser.runModel
import com.google.gson.Gson
import java.io.File


class ModelInferenceIntentService(): IntentService("ModelInferenceIntentService") {
    companion object Constants{
        val SUCCESS = 1
        val FAILED = 0
    }

    lateinit var resultReceiver: ResultReceiver

    override fun onHandleIntent(intent: Intent?) {
        resultReceiver = intent!!.getParcelableExtra("receiver")!!
        val modelFile = intent.getSerializableExtra("modelFile") as File
        val bmpFile = intent.getSerializableExtra("bmpFile") as File
        val bmp = BitmapFactory.decodeFile(bmpFile.path)    // retrieve bitmap from disk

        val output = processImageInput(this, bmp)   // send through python preprocessing pipeline

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

}