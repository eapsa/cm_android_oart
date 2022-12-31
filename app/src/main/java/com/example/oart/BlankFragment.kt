package com.example.oart

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.oart.adapter.ItemAdapter
import com.example.oart.item.Item
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

private const val TAG = "BlankFragment"
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34

class BlankFragment : Fragment(){
    var data: List<Item> = emptyList()
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view: View = inflater.inflate(R.layout.fragment_blank, container, false)
        recyclerView = view.findViewById<RecyclerView>(R.id.feed)
        fillDataset()
        return view
    }

    private fun fillDataset(){
        recyclerView.layoutManager = LinearLayoutManager(requireActivity())
        data = emptyList()
        val db = Firebase.firestore
        db.collection(FirebaseAuth.getInstance().currentUser?.email.toString())
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    data += Item(document, FirebaseAuth.getInstance().currentUser?.email.toString())
                    Log.d(TAG, "${document.id} => ${document.data}")
                }

                val adapter = ItemAdapter(requireActivity(),data)
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }
}