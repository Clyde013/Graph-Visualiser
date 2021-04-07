package com.example.graphvisualiser.queryingapi

import android.graphics.Bitmap

data class Graph(var querySuccessful: Boolean? = false,
                 var image: Bitmap? = null,
                 var linear: String? = null,
                 var periodic: String? = null,
                 var logarithmic: String? = null)