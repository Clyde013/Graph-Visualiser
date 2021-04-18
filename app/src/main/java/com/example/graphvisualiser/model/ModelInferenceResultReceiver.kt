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

            ModelInferenceService.Constants.FAILED_NO_CHARACTERS -> {
                onFailedNoCharacters(resultData)
            }

            ModelInferenceService.Constants.FAILED_OPERATION_CANCELLED -> {
                onFailedOperationCancelled(resultData)
            }
        }
    }

    abstract fun onSuccess(resultData: Bundle?)

    abstract fun onFailedNoCharacters(resultData: Bundle?)

    abstract fun onFailedOperationCancelled(resultData: Bundle?)    // user changed fragments
}