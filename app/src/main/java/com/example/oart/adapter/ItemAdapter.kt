package com.example.oart.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.iterator
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.models.SlideModel
import com.example.oart.R
import com.example.oart.item.Item

class ItemAdapter(private val context: Context, private val dataset: List<Item>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.userName)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
        val imageSlider: ImageSlider = view.findViewById(R.id.imageSlider)
        val speed: TextView = view.findViewById(R.id.speed)
        val distance: TextView = view.findViewById(R.id.distance)
        val duration: TextView = view.findViewById(R.id.duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.feed_tile, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.userName.text = item.userName
        holder.timestamp.text = item.timestamp
        holder.speed.text = item.speed
        holder.distance.text = item.distance
        holder.duration.text = item.duration

        var images: ArrayList<SlideModel> = ArrayList()
        for(img in item.imageList){
            images.add(SlideModel(img))
        }
        if(item.imageList.isNotEmpty())  holder.imageSlider.setImageList(images)
        else holder.imageSlider.visibility = View.GONE


    }

    override fun getItemCount() = dataset.size
}