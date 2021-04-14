package com.example.graphvisualiser.fragments

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.graphvisualiser.MyViewModel
import com.example.graphvisualiser.R
import com.example.graphvisualiser.model.ModelInferenceIntentService
import com.example.graphvisualiser.model.ModelInferenceResultReceiver
import com.example.graphvisualiser.recognizeText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.lang.reflect.Type

class DisplayGraphFragment: Fragment() {
    private val myViewModel: MyViewModel by activityViewModels()
    lateinit var graphImageView: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_display_graph, container, false)

        val bmpFile = this.arguments?.getSerializable("bmpFile") as File
        val resultReceiver = object : ModelInferenceResultReceiver(Handler()){
            override fun onSuccess(resultData: Bundle?) {
                val predictedCoordinates = Gson().fromJson(resultData?.getString("data"), ArrayList<ArrayList<String>>()::class.java)
                Log.i("model intent service", predictedCoordinates.toString())

                Log.i("model filepath", "in main ${File(requireContext().filesDir,"combinedBitmap").path}")
                val bmp = BitmapFactory.decodeFile(File(requireContext().filesDir, "combinedBitmap").path)
                graphImageView.setImageBitmap(bmp)
            }

            override fun onFailed(resultData: Bundle?) {
                TODO("Not yet implemented")
            }
        }

        recognizeText(InputImage.fromFilePath(requireContext(), bmpFile.toUri()), myViewModel)

        val intent = Intent(requireContext(), ModelInferenceIntentService::class.java)
        intent.putExtra("receiver", resultReceiver)
        intent.putExtra("modelFile", myViewModel.modelFile.value)
        intent.putExtra("bmpFile", bmpFile)

        myViewModel.boundingRects.observe(viewLifecycleOwner, Observer {
            if (it != null){
                Log.i("model", myViewModel.boundingRects.value!!::class.java.toString())
                val rectType: Type = object: TypeToken<ArrayList<Rect>>(){}.type
                intent.putExtra("boundingRects", Gson().toJson(myViewModel.boundingRects.value, rectType))
                val bitmapSizeType: Type = object: TypeToken<ArrayList<Int>>(){}.type
                intent.putExtra("maxBitmapSizes", Gson().toJson(myViewModel.maxBitmapSizes.value, bitmapSizeType))
                requireActivity().startService(intent)
            }
        })

        graphImageView = root.findViewById(R.id.graphImageView)

        if (myViewModel.graph.value != null) {
            graphImageView.setImageBitmap(myViewModel.graph.value!!.image)
        }

        myViewModel.graph.observe(viewLifecycleOwner, Observer{
            if (it.querySuccessful == true){
                graphImageView.setImageBitmap(it.image)
            }
        })

        return root
    }
}