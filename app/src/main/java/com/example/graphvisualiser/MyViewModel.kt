package com.example.graphvisualiser

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class MyViewModel: ViewModel() {
    val modelPath = MutableLiveData<File?>(null)
}