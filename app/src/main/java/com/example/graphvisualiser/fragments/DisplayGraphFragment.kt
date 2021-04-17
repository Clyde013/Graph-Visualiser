package com.example.graphvisualiser.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
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

class DisplayGraphFragment: Fragment() {
    private val myViewModel: MyViewModel by activityViewModels()
    lateinit var graphImageView: ImageView
    lateinit var overlayGraphImageView: ImageView
    lateinit var bottomSheet: ScrollView
    lateinit var intent: Intent

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

            override fun onFailed(resultData: Bundle?) {
                Log.i("model", "inference failed")
            }
        }


        intent = Intent(requireContext(), ModelInferenceService::class.java)
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
        // set the original camera image
        val bmp = rotateImageIfRequired(BitmapFactory.decodeFile(bmpFile.path), bmpFile.toUri())

        /*
        val bm: InputStream = resources.openRawResource(R.raw.justin_coords)
        val bufferedInputStream = BufferedInputStream(bm)
        val rawbmp = BitmapFactory.decodeStream(bufferedInputStream)
        val nh = (rawbmp.height * (512.0 / rawbmp.width)).toInt()
        val resbmp = Bitmap.createScaledBitmap(rawbmp, 512, nh, true)*/

        graphImageView.setImageBitmap(bmp)

        myViewModel.graph.observe(viewLifecycleOwner, Observer{
            if (it.querySuccessful == true){
                overlayGraphImageView.setImageBitmap(replaceColor(it.image!!))
                overlayGraphImageView.visibility = View.VISIBLE
            }
        })

        graphButton.setOnClickListener {
            val retrieveGraph = @SuppressLint("StaticFieldLeak")
            object : RetrieveGraph(){
                override fun onResponseReceived(result: Any?) {
                    myViewModel.graph.value = result as Graph
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

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().stopService(intent)
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

    private fun replaceColor(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, 1 * width, 0, 0, width, height)
        for (x in pixels.indices) {
            //    pixels[x] = ~(pixels[x] << 8 & 0xFF000000) & Color.BLACK;
            if (pixels[x] == Color.WHITE) pixels[x] = 0
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}