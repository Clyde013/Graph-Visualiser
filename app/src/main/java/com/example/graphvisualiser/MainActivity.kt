package com.example.graphvisualiser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedInputStream
import java.io.InputStream


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bm: InputStream = resources.openRawResource(R.raw.justin_pi)
        val bufferedInputStream = BufferedInputStream(bm)
        var bmp = BitmapFactory.decodeStream(bufferedInputStream)
        val nh = (bmp.height * (512.0 / bmp.width)).toInt()
        bmp = Bitmap.createScaledBitmap(bmp, 512, nh, true)

        processImageInput(this, bmp)
    }
}