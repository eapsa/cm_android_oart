package com.example.oart.item

import android.widget.TextView
import com.denzcoskun.imageslider.ImageSlider
import com.example.oart.R
import com.google.firebase.firestore.QueryDocumentSnapshot

class Item(private val document: QueryDocumentSnapshot, private val user: String) {
    val userName = user
    val timestamp = document.data["timestamp"].toString()
    val speed = document.data["speed"].toString()
    val distance = document.data["distance"].toString()
    val duration = document.data["time"].toString()
    var imageList = emptyList<String>()
    init {
        if(document.data.containsKey("images")){
            imageList = document.data["images"] as List<String>
        }

    }
}