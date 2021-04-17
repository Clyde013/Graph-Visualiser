package com.example.graphvisualiser.recyclerview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.graphvisualiser.CoordinateLiveData
import com.example.graphvisualiser.R
import com.example.graphvisualiser.databinding.CoordinateCardLayoutBinding
import com.example.graphvisualiser.BR.coordinate

class CoordinateAdapter(private var dataSet: ArrayList<CoordinateLiveData>) : RecyclerView.Adapter<CoordinateAdapter.ViewHolder>(){

    inner class ViewHolder(val binding: CoordinateCardLayoutBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val binding = DataBindingUtil.inflate<CoordinateCardLayoutBinding>(LayoutInflater.from(context), R.layout.coordinate_card_layout, parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.setVariable(coordinate, dataSet[position])
    }

    fun setCoordinates(data: ArrayList<CoordinateLiveData>){
        dataSet = data
    }
}