package com.example.graphvisualiser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.google.common.util.concurrent.ListenableFuture
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executor

class HomeFragment: Fragment(), Executor {
    private val myViewModel: MyViewModel by activityViewModels()

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val button = root.findViewById<ImageButton>(R.id.photoButton)
        button.setOnClickListener {
            val bm: InputStream = resources.openRawResource(R.raw.justin_pi)
            val bufferedInputStream = BufferedInputStream(bm)
            val bmp = BitmapFactory.decodeStream(bufferedInputStream)

            takePicture()

            //imageView.setImageBitmap(plotImageInput(requireContext(), bmp))   // use for testing to see what input image is fed into the model
            Log.i("model", "predicted output: ${runModel(requireContext(), myViewModel, processImageInput(requireContext(), bmp))}")
        }

        /* camera setup */
        previewView = root.findViewById(R.id.previewView)

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))

        return root
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        val preview : Preview = Preview.Builder()
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext()), ImageAnalysis.Analyzer { image ->
            val rotationDegrees = image.imageInfo.rotationDegrees
            // insert your code here.
        })

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(requireView().display.rotation)
            .build()

        var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageCapture, imageAnalysis, preview)
    }

    private fun takePicture() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image")).build()
        imageCapture.takePicture(outputFileOptions, this,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException)
                {
                    // insert your code here.
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // insert your code here.
                    findNavController().navigate(R.id.action_homeFragment_to_displayGraphFragment)
                }
            })
    }

    override fun execute(command: Runnable?) {
        Thread(command).run()
    }
}