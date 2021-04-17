package com.example.graphvisualiser.model

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

abstract class ModelInferenceResultReceiver(handler: Handler): ResultReceiver(handler) {
    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
        when (resultCode){
            ModelInferenceService.Constants.SUCCESS -> {
                onSuccess(resultData)
            }

            ModelInferenceService.Constants.FAILED -> {
                onFailed(resultData)
            }
        }
    }

    abstract fun onSuccess(resultData: Bundle?)

    abstract fun onFailed(resultData: Bundle?)
}