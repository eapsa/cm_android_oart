package com.example.oart.item

import android.widget.TextView
import com.denzcoskun.imageslider.ImageSlider
import com.example.oart.R
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlin.math.roundToInt

class Item(private val document: QueryDocumentSnapshot, private val user: String) {
    val userName = user.split("@")[0]+"'s workout"
    val timestamp = document.data["timestamp"].toString().split("T")[0] +" " + document.data["timestamp"].toString().split("T")[1].split(".")[0]
    val speed = (((document.data["speed"] as Double) * 100).roundToInt() /100).toDouble().toString()
    val distance = (((document.data["distance"] as Double) * 100).roundToInt() /100).toDouble().toString()
    val duration = (((document.data["time"] as Long)/ 1000) / 60).toString().padStart(2,'0') + ":" + (((document.data["time"] as Long) / 1000) % 60).toString().padStart(2,'0')
    var imageList = emptyList<String>()
    init {
        if(document.data.containsKey("images")){
            imageList = document.data["images"] as List<String>
        }
    }

    override fun toString(): String {
        return timestamp
    }
}