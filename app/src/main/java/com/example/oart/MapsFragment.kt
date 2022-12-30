package com.example.oart

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.tabs.TabLayout.TabGravity

class MapsFragment : Fragment() {

    lateinit var outputTextView: TextView
    lateinit var startButton: Button
    lateinit var pauseButton: Button
    lateinit var resumeButton: Button
    lateinit var terminateButton: Button

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

    internal var mCurrLocationMarker: Marker? = null
    private var mMap: GoogleMap? = null

    private var polyline: Polyline? = null
    private var local: LatLng? = null

    private var startFlag: Boolean = false

    private var points: List<LatLng> = emptyList()

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        mMap = googleMap

        val sydney = LatLng(-34.0, 151.0)
        googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view : View = inflater.inflate(R.layout.fragment_maps, container, false)

//        foregroundOnlyLocationButton = view.findViewById<Button>(R.id.start_button)
//
//        client = LocationServices.getFusedLocationProviderClient(activity as MainActivity)
//
//        foregroundOnlyLocationButton.setOnClickListener {
//            if(!flag) {
//                if (ContextCompat.checkSelfPermission(
//                        activity as MainActivity,
//                        android.Manifest.permission.ACCESS_FINE_LOCATION
//                    ) == PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(
//                        activity as MainActivity,
//                        android.Manifest.permission.ACCESS_COARSE_LOCATION
//                    ) == PackageManager.PERMISSION_GRANTED
//                ) {
////                    getCurrentLocation()
//                    startLocationUpdates()
//                } else {
//                    ActivityCompat.requestPermissions(
//                        activity as MainActivity,
//                        arrayOf(
//                            android.Manifest.permission.ACCESS_FINE_LOCATION,
//                            android.Manifest.permission.ACCESS_COARSE_LOCATION
//                        ), 100
//                    )
//                }
//                flag = !flag
//                updateButtonState(flag)
//            }else{
//                stopLocationUpdates()
//                flag = !flag
//                updateButtonState(flag)
//            }
//
//        }

        startButton = view.findViewById<Button>(R.id.start_button)
        pauseButton = view.findViewById<Button>(R.id.pause_button)
        resumeButton = view.findViewById<Button>(R.id.resume_button)
        terminateButton = view.findViewById<Button>(R.id.terminate_button)

        startButton.setOnClickListener {
            startButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            startFlag = true
        }

        pauseButton.setOnClickListener {
            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.VISIBLE
            terminateButton.visibility = View.VISIBLE
            startFlag = false
        }

        resumeButton.setOnClickListener {
            pauseButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            terminateButton.visibility = View.GONE
            startFlag = true
        }
        terminateButton.setOnClickListener {
            startButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            terminateButton.visibility = View.GONE
            startFlag = false
            polyline?.remove()
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    override fun onStart() {
        super.onStart()
        client = LocationServices.getFusedLocationProviderClient(activity as MainActivity)

        if (ContextCompat.checkSelfPermission(
                activity as MainActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                activity as MainActivity,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
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
    }

    override fun onStop() {
        super.onStop()
        polyline?.remove()
        stopLocationUpdates()
    }

    fun onlocationchange(location: Location){
//        outputTextView.text = location1.toString()
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker!!.remove()
        }
        val latLng = LatLng(location.latitude, location.longitude)
        val markerOptions = MarkerOptions()
        markerOptions.position(latLng)
        markerOptions.title("Current Position")
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        mCurrLocationMarker = mMap!!.addMarker(markerOptions)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(18f))
        if(local == null){
            local = latLng
            points += latLng
        }
        if(local != null && latLng != null && startFlag){
            Log.d("po","pole")
            points += latLng
            polyline?.remove()
            polyline = mMap!!.addPolyline(
                PolylineOptions()
                    .width(16F).color(ContextCompat.getColor(requireActivity(), R.color.red))
                    .clickable(true)
                    .addAll(points))
//            polyline!!.points.add(latLng)
            local = latLng

        }

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        client.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        client.removeLocationUpdates(locationCallback)
    }
}