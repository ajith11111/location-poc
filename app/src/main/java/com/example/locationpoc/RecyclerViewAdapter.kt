package com.example.locationpoc

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.locationpoc.database.LocationEntity
import com.example.locationpoc.databinding.LayoutLocationDisplayBinding

class CustomRecyclerViewAdapter(context: Context) : RecyclerView.Adapter<CustomRecyclerViewAdapter.LocationViewHolder>() {
    private lateinit var binding: LayoutLocationDisplayBinding
    private var adapterData:ArrayList<LocationEntity> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        binding =
            LayoutLocationDisplayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LocationViewHolder(binding)
    }

    fun setData(list: List<LocationEntity>) {
        adapterData.clear()
        adapterData.addAll(list)
        notifyDataSetChanged()
    }
    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.set(adapterData[position],position)
    }

    override fun getItemCount(): Int  = adapterData.size

    inner class LocationViewHolder(binding: LayoutLocationDisplayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun set(data:LocationEntity,position: Int) {
            binding.apply {
                tvId.text = position.toString()
                tvStatus.text = data.status
                tvTime.text = data.time.toString()
                tvLat.text = data.latitude.toString()
                tvLon.text = data.longitude.toString()
            }
        }
    }
}