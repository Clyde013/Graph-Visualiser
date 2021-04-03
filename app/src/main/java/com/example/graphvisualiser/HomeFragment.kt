package com.example.graphvisualiser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import java.io.BufferedInputStream
import java.io.InputStream

class HomeFragment: Fragment() {
    private val myViewModel: MyViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        val imageView = root.findViewById<ImageView>(R.id.imageView3)
        val button = root.findViewById<ImageButton>(R.id.photoButton)
        button.setOnClickListener {
            val bm: InputStream = resources.openRawResource(R.raw.justin_pi)
            val bufferedInputStream = BufferedInputStream(bm)
            val bmp = BitmapFactory.decodeStream(bufferedInputStream)

            //imageView.setImageBitmap(plotImageInput(requireContext(), bmp))
            Log.i("model", "predicted output: ${runModel(requireContext(), myViewModel, processImageInput(requireContext(), bmp))}")
        }

        return root
    }
}