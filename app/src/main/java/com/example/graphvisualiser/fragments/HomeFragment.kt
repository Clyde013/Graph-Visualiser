package com.example.graphvisualiser.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.example.graphvisualiser.*
import com.example.graphvisualiser.R
import com.google.common.util.concurrent.ListenableFuture
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit


class HomeFragment: Fragment(), Executor {
    private val myViewModel: MyViewModel by activityViewModels()

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture

    // compensating for landscape pictures orientation to display correctly
    // this only works if the phone is held upright so include that in the onboarding
    private val orientationEventListener by lazy {
        object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                // Monitors orientation values to determine the target rotation value
                val rotation = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270

                    in 135..224 -> Surface.ROTATION_180

                    in 225..314 -> Surface.ROTATION_90

                    else -> Surface.ROTATION_0
                }

                imageCapture.targetRotation = rotation
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val root = inflater.inflate(R.layout.fragment_home, container, false)

            val button = root.findViewById<ImageButton>(R.id.photoButton)
            button.setOnClickListener {
                Log.i("model", "button pressed")

                takePicture()

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

                //imageView.setImageBitmap(plotImageInput(requireContext(), bmp))   // use for testing to see what input image is fed into the model
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

    @SuppressLint("UnsafeExperimentalUsageError", "ClickableViewAccessibility")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview : Preview = Preview.Builder()
            .build()

        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview.setSurfaceProvider(previewView.surfaceProvider)

        imageCapture = ImageCapture.Builder().build()

        var camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageCapture,
            preview
        )

        previewView.afterMeasured {
            previewView.setOnTouchListener { _, event ->
                return@setOnTouchListener when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                previewView.width.toFloat(), previewView.height.toFloat()
                        )
                        val autoFocusPoint = factory.createPoint(event.x, event.y)
                        try {
                            camera.cameraControl.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                            autoFocusPoint,
                                            FocusMeteringAction.FLAG_AF
                                    ).apply {
                                        //focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                            )
                        } catch (e: CameraInfoUnavailableException) {
                            Log.d("ERROR", "cannot access camera", e)
                        }
                        true
                    }
                    else -> false // Unhandled event.
                }
            }
        }
    }

    private fun takePicture() {
        val outputFile = File(
            requireContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            ), "image"
        )
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(outputFileOptions, this,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    // insert your code here.
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Something went wrong taking a picture :(",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    requireActivity().runOnUiThread {
                        Log.i("model", outputFile.path)
                        val bundle = bundleOf("bmpFile" to outputFile)
                        findNavController().navigate(
                            R.id.action_homeFragment_to_displayGraphFragment,
                            bundle
                        )
                    }
                }
            })
    }

    override fun execute(command: Runnable?) {
        Thread(command).run()
    }

    override fun onStart(){
        super.onStart()
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }
    }

    override fun onStop(){
        super.onStop()
        orientationEventListener.disable()
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }
}