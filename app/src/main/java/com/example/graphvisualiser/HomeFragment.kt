package com.example.graphvisualiser

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.example.graphvisualiser.queryingapi.Graph
import com.example.graphvisualiser.queryingapi.GraphInput
import com.example.graphvisualiser.queryingapi.RetrieveGraph
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
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
            val bm: InputStream = resources.openRawResource(R.raw.justin_eqn)
            val bufferedInputStream = BufferedInputStream(bm)
            val bmp = BitmapFactory.decodeStream(bufferedInputStream)

            // takePicture()
            // recognizeText(InputImage.fromBitmap(bmp, 0))

            /* wolfram alpha api call */
            val retrieveGraph = @SuppressLint("StaticFieldLeak")
            object : RetrieveGraph(){
                override fun onResponseReceived(result: Any?) {
                    myViewModel.graph.value = result as Graph
                }
            }
            val testInputCoords = arrayOf(Pair(1f, 2f), Pair(2f, 4f), Pair(3f, 6f))
            retrieveGraph.execute(GraphInput(resources.getString(R.string.wolfram_alpha_appID), testInputCoords))

            findNavController().navigate(R.id.action_homeFragment_to_displayGraphFragment)

            //imageView.setImageBitmap(plotImageInput(requireContext(), bmp))   // use for testing to see what input image is fed into the model
            //Log.i("model", "predicted output: ${runModel(requireContext(), myViewModel, processImageInput(requireContext(), bmp))}")
        }

        /*
        myViewModel.graph.observe(viewLifecycleOwner) {
            Log.i("wolfram api response", "linear: ${it.linear}, periodic: ${it.periodic}, logarithmic: ${it.logarithmic}")
        }
        */

        /* check for camera permissions */
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            button.isEnabled = false
            Toast.makeText(requireContext(), "Please enable camera permissions and restart the app", Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProviderFuture.get().unbindAll()
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