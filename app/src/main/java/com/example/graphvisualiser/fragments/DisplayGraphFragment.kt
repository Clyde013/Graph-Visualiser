package com.example.graphvisualiser.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.graphvisualiser.MyViewModel
import com.example.graphvisualiser.R
import com.example.graphvisualiser.model.ModelInferenceResultReceiver
import com.example.graphvisualiser.model.ModelInferenceService
import com.example.graphvisualiser.queryingapi.Graph
import com.example.graphvisualiser.queryingapi.GraphInput
import com.example.graphvisualiser.queryingapi.RetrieveGraph
import com.example.graphvisualiser.recyclerview.CoordinateAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import pl.droidsonroids.gif.GifImageView
import java.io.File
import kotlin.math.sqrt

class DisplayGraphFragment: Fragment(), View.OnTouchListener {
    private val myViewModel: MyViewModel by activityViewModels()
    lateinit var graphImageView: ImageView
    lateinit var overlayGraphImageView: ImageView
    lateinit var bottomSheet: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_display_graph, container, false)

        bottomSheet = root.findViewById(R.id.bottomSheet)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 100
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        val coordinateRecyclerView = root.findViewById<RecyclerView>(R.id.coordinateRecyclerView)
        val coordinateLoadingGif = root.findViewById<GifImageView>(R.id.coordinateLoadingGifImageView)
        val graphButton = root.findViewById<Button>(R.id.graphButton)
        coordinateLoadingGif.visibility = View.VISIBLE
        coordinateRecyclerView.visibility = View.GONE
        graphButton.visibility = View.GONE

        val bmpFile = this.arguments?.getSerializable("bmpFile") as File
        val resultReceiver = object : ModelInferenceResultReceiver(Handler()){
            override fun onSuccess(resultData: Bundle?) {
                val predictedCoordinates = Gson().fromJson(resultData?.getString("data"), ArrayList<ArrayList<String>>()::class.java)
                Log.i("model intent service", predictedCoordinates.toString())

                myViewModel.setCoordinates(predictedCoordinates)

                coordinateRecyclerView.setHasFixedSize(true)
                val layoutManager = LinearLayoutManager(requireContext())
                coordinateRecyclerView.layoutManager = layoutManager
                coordinateRecyclerView.itemAnimator = DefaultItemAnimator()
                coordinateRecyclerView.adapter = CoordinateAdapter(myViewModel.coordinates)

                val factor: Float = requireContext().resources.displayMetrics.density
                if (coordinateRecyclerView.layoutParams.height > 300 * factor){     // height > 300dp
                    coordinateRecyclerView.layoutParams.height = (300 * factor).toInt()   // let user use scrollview
                } else {
                    coordinateRecyclerView.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                coordinateLoadingGif.visibility = View.GONE
                coordinateRecyclerView.visibility = View.VISIBLE
                graphButton.visibility = View.VISIBLE
            }

            override fun onFailedNoCharacters(resultData: Bundle?) {
                Log.i("model", "inference failed no characters detected")
                Toast.makeText(requireContext(), "No characters detected, please retry taking a picture", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_displayGraphFragment_to_homeFragment)
            }

            override fun onFailedOperationCancelled(resultData: Bundle?) {
                Log.i("model", "user changed fragments, do nothing")
            }
        }


        val intent = Intent(requireContext(), ModelInferenceService::class.java)
        intent.putExtra("receiver", resultReceiver)
        intent.putExtra("modelFile", myViewModel.modelFile.value)
        intent.putExtra("bmpFile", bmpFile)
        requireActivity().startService(intent)

        /*
        myViewModel.boundingRects.observe(viewLifecycleOwner, Observer {
            if (it != null){
                Log.i("model", myViewModel.boundingRects.value!!::class.java.toString())
                val rectType: Type = object: TypeToken<ArrayList<Rect>>(){}.type
                intent.putExtra("boundingRects", Gson().toJson(myViewModel.boundingRects.value, rectType))
                val bitmapSizeType: Type = object: TypeToken<ArrayList<Int>>(){}.type
                intent.putExtra("maxBitmapSizes", Gson().toJson(myViewModel.maxBitmapSizes.value, bitmapSizeType))

            }
        })*/

        graphImageView = root.findViewById(R.id.graphImageView)
        overlayGraphImageView = root.findViewById(R.id.overlayGraphImageView)
        overlayGraphImageView.visibility = View.GONE
        overlayGraphImageView.setOnTouchListener(this)
        // set the original camera image
        val bmp = rotateImageIfRequired(BitmapFactory.decodeFile(bmpFile.path), bmpFile.toUri())

        graphImageView.setImageBitmap(bmp)

        myViewModel.graph.observe(viewLifecycleOwner, Observer{
            if (it.querySuccessful == true){
                // overlayGraphImageView.setImageBitmap(replaceColor(it.image!!))
                overlayGraphImageView.setImageBitmap(it.image!!)
                overlayGraphImageView.visibility = View.VISIBLE
                graphImageView.alpha = 0.5f
            }
        })

        graphButton.setOnClickListener {
            val retrieveGraph = @SuppressLint("StaticFieldLeak")
            object : RetrieveGraph(){
                override fun onResponseReceived(result: Any?) {
                    if (result != null) {
                        myViewModel.graph.value = result as Graph
                    } else {    // some network error probably?
                        Toast.makeText(requireContext(), "Please check your internet connection!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            try {
                Log.i("model api call", "${myViewModel.coordinatesAsInput().toString()}")
                retrieveGraph.execute(GraphInput(resources.getString(R.string.wolfram_alpha_appID), myViewModel.coordinatesAsInput()))
            } catch (e: Exception){     // threw exception because coord not enclosed in ()
                Toast.makeText(requireContext(), "Please ensure all coordinates are correct!", Toast.LENGTH_SHORT).show()
            }
        }

        return root
    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(requireContext(), ModelInferenceService::class.java)
        requireActivity().stopService(intent)
        Log.i("custom service onStop", "intent service stopped")
    }

    private fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap? {
        val ei = ExifInterface(selectedImage.path!!)
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    // touch gesture things
    private val TAG: String? = "Touch"

    // These matrices will be used to move and zoom image
    var matrix = Matrix()
    var savedMatrix = Matrix()
    var savedMatrix2 = Matrix()

    // We can be in one of these 3 states
    val NONE = 0
    val DRAG = 1
    val ZOOM = 2
    var mode = NONE

    // Remember some things for zooming
    var start: PointF = PointF()
    var mid: PointF = PointF()
    var oldDist = 1f

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val view = v as ImageView

        if (event == null){
            return false
        }

        // Handle touch events here...
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                Log.d(TAG, "mode=DRAG")
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                try{
                    oldDist = spacing(event)
                    Log.d(TAG, "oldDist=$oldDist")
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, event)
                        mode = ZOOM
                        Log.d(TAG, "mode=ZOOM")
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                    Log.d(TAG, "pointer index out of range bug")
                }
            }
            MotionEvent.ACTION_UP -> {
                mode = NONE
                Log.d(TAG, "mode=NONE")
                savedMatrix.set(matrix)
            }
            MotionEvent.ACTION_MOVE -> if (mode === DRAG) {
                // ...
                matrix.set(savedMatrix)
                matrix.postTranslate(event.x - start.x,
                        event.y - start.y)
            } else if (mode === ZOOM) {
                try {
                    val newDist: Float = spacing(event)
                    Log.d(TAG, "newDist=$newDist")
                    if (newDist > 10f) {
                        matrix.set(savedMatrix)
                        val scale: Float = newDist / oldDist
                        matrix.postScale(scale, scale, mid.x, mid.y)
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                    Log.d(TAG, "pointer index out of range bug")
                }
            }
        }

        //fixing()
        view.imageMatrix = matrix
        return true // indicate event was handled
    }

    /** Determine the space between the first two fingers  */
    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    /** Calculate the mid point of the first two fingers  */
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }

    // make sure dragged image doesnt exit ui
    fun fixing() {
        val value = FloatArray(9)
        matrix.getValues(value)
        val savedValue = FloatArray(9)
        savedMatrix2.getValues(savedValue)
        val width: Int = overlayGraphImageView.width
        val height: Int = overlayGraphImageView.height
        val d: Drawable = overlayGraphImageView.drawable ?: return
        val imageWidth: Int = d.intrinsicWidth
        val imageHeight: Int = d.intrinsicHeight
        val scaleWidth = (imageWidth * value[0]).toInt()
        val scaleHeight = (imageHeight * value[4]).toInt()

        // don't let the image go outside
        if (value[2] > width - 1) value[2] = (width - 10).toFloat() else if (value[5] > height - 1) value[5] = (height - 10).toFloat() else if (value[2] < -(scaleWidth - 1)) value[2] = (-(scaleWidth - 10)).toFloat() else if (value[5] < -(scaleHeight - 1)) value[5] = (-(scaleHeight - 10)).toFloat()

        // maximum zoom ratio: MAx
        val MAX_ZOOM = 5f
        if (value[0] > MAX_ZOOM || value[4] > MAX_ZOOM) {
            value[0] = MAX_ZOOM
            value[4] = MAX_ZOOM
            //value[2] = savedValue[2];
            //value[5] = savedValue[5];
        }
        matrix.setValues(value)
        savedMatrix2.set(matrix)
    }
}