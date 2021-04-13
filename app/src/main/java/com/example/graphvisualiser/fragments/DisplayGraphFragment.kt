package com.example.graphvisualiser.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.graphvisualiser.MyViewModel
import com.example.graphvisualiser.R
import com.example.graphvisualiser.model.ModelInferenceIntentService
import com.example.graphvisualiser.model.ModelInferenceResultReceiver
import com.google.gson.Gson
import java.io.File

class DisplayGraphFragment: Fragment() {
    private val myViewModel: MyViewModel by activityViewModels()

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
            }

            override fun onFailed(resultData: Bundle?) {
                TODO("Not yet implemented")
            }
        }

        val intent = Intent(requireContext(), ModelInferenceIntentService::class.java)
        intent.putExtra("receiver", resultReceiver)
        intent.putExtra("modelFile", myViewModel.modelFile.value)
        intent.putExtra("bmpFile", bmpFile)

        myViewModel.boundingBoxes.observe(viewLifecycleOwner, Observer {
            if (it != null){
                requireActivity().startService(intent)
            }
        })

        val graphImageView = root.findViewById<ImageView>(R.id.graphImageView)

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