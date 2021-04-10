package com.example.graphvisualiser

import android.graphics.Rect
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.graphvisualiser.queryingapi.Graph
import java.io.File

class MyViewModel: ViewModel() {
    val modelPath = MutableLiveData<File?>(null)

    val graph = MutableLiveData<Graph>()

    val boundingBoxes = MutableLiveData<ArrayList<Rect>>(null)
}