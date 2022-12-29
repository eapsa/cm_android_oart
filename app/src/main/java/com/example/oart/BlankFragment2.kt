package com.example.oart

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.audiofx.Equalizer.Settings
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import java.util.jar.Manifest

class BlankFragment2 : Fragment() {

    lateinit var outputTextView: TextView
    lateinit var foregroundOnlyLocationButton: Button

    lateinit var client: FusedLocationProviderClient

    var locationCallback: LocationCallback = object: LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            var location1: Location = p0.lastLocation
            onlocationchange(location1)
        }
    }

    var locationRequest: LocationRequest = LocationRequest()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(10000)
        .setFastestInterval(1000)

    var flag: Boolean = false

    fun onlocationchange(location1:Location){
        outputTextView.text = location1.toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view : View = inflater.inflate(R.layout.fragment_blank2, container, false)

        foregroundOnlyLocationButton = view.findViewById<Button>(R.id.foreground_only_location_button)
        outputTextView = view.findViewById(R.id.output_text_view)

        client = LocationServices.getFusedLocationProviderClient(activity as MainActivity)

        foregroundOnlyLocationButton.setOnClickListener {
            if(!flag) {
                if (ContextCompat.checkSelfPermission(
                        activity as MainActivity,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        activity as MainActivity,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
//                    getCurrentLocation()
                    startLocationUpdates()
                } else {
                    ActivityCompat.requestPermissions(
                        activity as MainActivity,
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ), 100
                    )
                }
                flag = !flag
                updateButtonState(flag)
            }else{
                stopLocationUpdates()
                flag = !flag
                updateButtonState(flag)
            }

        }
        return view
    }

//    @SuppressLint("MissingPermission")
//    private fun getCurrentLocation() {
//        var locationManager : LocationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
//                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
//            client.lastLocation.addOnCompleteListener { task ->
//                var location: Location = task.result
//                if(location!=null) {
//                    outputTextView.text = location.toString()
//                }else{
//                    var locationRequest: LocationRequest = LocationRequest()
//                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                        .setInterval(10000)
//                        .setFastestInterval(1000)
//                        .setNumUpdates(1)
//
//                    Looper.myLooper()?.let {
//                        client.requestLocationUpdates(locationRequest,locationCallback, it)
//                    }
//                }
//            }
//        }else{
//            startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
//        }
//    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        client.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        client.removeLocationUpdates(locationCallback)
    }

    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            foregroundOnlyLocationButton.text = getString(R.string.stop_location_updates_button_text)
        } else {
            foregroundOnlyLocationButton.text = getString(R.string.start_location_updates_button_text)
        }
    }
}
