package com.example.graphvisualiser

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class MyViewModel: ViewModel() {
    val modelAvailable = MutableLiveData<File?>(null)
}