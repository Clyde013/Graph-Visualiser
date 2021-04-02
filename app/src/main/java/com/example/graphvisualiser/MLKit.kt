package com.example.graphvisualiser

import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader

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