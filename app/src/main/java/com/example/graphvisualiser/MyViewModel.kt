package com.example.graphvisualiser

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.graphvisualiser.queryingapi.Graph
import java.io.File

class MyViewModel: ViewModel() {
    val modelFile = MutableLiveData<File?>(null)

    val graph = MutableLiveData<Graph>()

    val coordinates = ArrayList<CoordinateLiveData>()

    fun setCoordinates(data: ArrayList<ArrayList<String>>){
        coordinates.clear()

        for (coordinate in data){
            var coordinateString = ""
            for (character in coordinate){
                coordinateString = coordinateString.plus(character)
            }
            coordinates.add(CoordinateLiveData(coordinateString))
        }
    }

    /*
    val boundingRects = MutableLiveData<ArrayList<Rect>>(null)

    val maxBitmapSizes = MutableLiveData<ArrayList<Int>>(null)
    */
}

