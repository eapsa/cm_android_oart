package com.example.oart

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.oart.adapter.ItemAdapter
import com.example.oart.item.Item
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "BlankFragment2"

class BlankFragment2 : Fragment() {

    var data: List<Item> = emptyList()
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view: View = inflater.inflate(R.layout.fragment_blank, container, false)
        refreshLayout = view.findViewById(R.id.layout)
        refreshLayout.setOnRefreshListener {
            refreshLayout.isRefreshing = true
            fillDataset()
            refreshLayout.isRefreshing = false
        }

        recyclerView = view.findViewById(R.id.feed)
        fillDataset()
        return view
    }

    private fun fillDataset(){

        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        val adapter = ItemAdapter(requireActivity(),data)
        recyclerView.adapter = adapter
        data = emptyList()
        val db = Firebase.firestore
        db.collection(FirebaseAuth.getInstance().currentUser?.email.toString())
            .get()
            .addOnSuccessListener { result ->
                if(!result.isEmpty){
                    var duration = Item(result.first(), FirebaseAuth.getInstance().currentUser?.email.toString())
                    var distance = Item(result.first(), FirebaseAuth.getInstance().currentUser?.email.toString())
                    var speed = Item(result.first(), FirebaseAuth.getInstance().currentUser?.email.toString())


                    for (document in result) {
                        val r = Item(document, FirebaseAuth.getInstance().currentUser?.email.toString())
                        if(r.duration > duration!!.duration) duration = r
                        if(r.distance > distance!!.distance) distance = r
                        if(r.speed > speed!!.speed) speed = r
                    }
                    duration.userName = "Biggest duration workout"
                    distance.userName = "Longest distance workout"
                    speed.userName = "Top average speed workout"
                    data += duration
                    data += distance
                    data += speed
                    Log.d(TAG,data.size.toString())
                    data.sortedBy { it.timestamp }
                    recyclerView.adapter = ItemAdapter(requireActivity(),data)
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }
}
