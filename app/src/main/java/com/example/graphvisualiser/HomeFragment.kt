package com.example.graphvisualiser

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
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
            Log.i("model", "button pressed")
            val bm: InputStream = resources.openRawResource(R.raw.justin_coords)
            val bufferedInputStream = BufferedInputStream(bm)
            var bmp = BitmapFactory.decodeStream(bufferedInputStream)
            //bmp = rotateImage(bmp, 90f)

            // takePicture()

            //recognizeText(InputImage.fromBitmap(bmp, 90), myViewModel)

            /* wolfram alpha api call
            val retrieveGraph = @SuppressLint("StaticFieldLeak")
            object : RetrieveGraph(){
                override fun onResponseReceived(result: Any?) {
                    myViewModel.graph.value = result as Graph
                }
            }
            val testInputCoords = arrayOf(Pair(1f, 2f), Pair(2f, 4f), Pair(3f, 6f))
            retrieveGraph.execute(GraphInput(resources.getString(R.string.wolfram_alpha_appID), testInputCoords))
            */
            findNavController().navigate(R.id.action_homeFragment_to_displayGraphFragment)


            //imageView.setImageBitmap(plotImageInput(requireContext(), bmp))   // use for testing to see what input image is fed into the model

            val output = processImageInput(requireContext(), bmp)
            val characters = output?.first
            val commaIndices = output?.second
            Log.i("model", "${commaIndices?.get(0)}")
            /*
            if (characters != null && commaIndices != null) {
                for (i in 0..characters.size) {
                    Log.i(
                        "model", "predicted output at index $i: ${
                            runModel(
                                requireContext(),
                                myViewModel,
                                characters[i]
                            )
                        }"
                    )
                }
                Log.i("model", "predicted commas: $commaIndices")
            } else {
                Log.i("model", "no characters detected")
            }
            */
        }

        /*
        myViewModel.graph.observe(viewLifecycleOwner) {
            Log.i("wolfram api response", "linear: ${it.linear}, periodic: ${it.periodic}, logarithmic: ${it.logarithmic}")
        }
        */

        /* check for camera permissions */
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            button.isEnabled = false
            Toast.makeText(
                requireContext(),
                "Please enable camera permissions and restart the app",
                Toast.LENGTH_LONG
            ).show()
        }

        /* camera setup */
        /*
        previewView = root.findViewById(R.id.previewView)

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))*/

        return root
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProviderFuture.get().unbindAll()
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
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

        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(requireContext()),
            ImageAnalysis.Analyzer { image ->
                val rotationDegrees = image.imageInfo.rotationDegrees
                // insert your code here.
            })

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(requireView().display.rotation)
            .build()

        var camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageCapture,
            imageAnalysis,
            preview
        )
    }

    private fun takePicture() {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            File(
                requireContext().getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ), "image"
            )
        ).build()
        imageCapture.takePicture(outputFileOptions, this,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
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