package com.example.graphvisualiser.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.graphvisualiser.CoordinateLiveData
import com.example.graphvisualiser.R
import com.example.graphvisualiser.databinding.CoordinateCardLayoutBinding
import com.example.graphvisualiser.BR.coordinate
import com.example.graphvisualiser.MyViewModel

class CoordinateAdapter(private var dataSet: ArrayList<CoordinateLiveData>, private val viewModel: MyViewModel) : RecyclerView.Adapter<CoordinateAdapter.ViewHolder>(){

    inner class ViewHolder(val binding: CoordinateCardLayoutBinding): RecyclerView.ViewHolder(binding.root){
        val removeCoordinateButton = binding.removeCoordinateButton
    }

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

        holder.removeCoordinateButton.setOnClickListener{   // remove the clicked item
            viewModel.removeCoordinates(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, dataSet.size - position)
        }
    }

    fun setCoordinates(data: ArrayList<CoordinateLiveData>){
        dataSet = data
    }
}