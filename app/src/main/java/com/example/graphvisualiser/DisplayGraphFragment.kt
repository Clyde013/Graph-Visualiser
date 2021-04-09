package com.example.graphvisualiser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe

class DisplayGraphFragment: Fragment() {
    private val myViewModel: MyViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_display_graph, container, false)

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