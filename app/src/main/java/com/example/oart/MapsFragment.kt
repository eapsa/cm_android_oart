package com.example.oart

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.math.*

class MapsFragment : Fragment() {

    lateinit var outputTextView: TextView
    lateinit var startButton: Button
    lateinit var pauseButton: Button
    lateinit var resumeButton: Button
    lateinit var terminateButton: Button
    lateinit var chronometer: Chronometer

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

    private var timeSpent: Long = 0L

    private var distance: Double = 0.0
    private var speed: Double = 0.0

    val db = Firebase.firestore

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
        chronometer = view.findViewById(R.id.idCMmeter)
        startButton = view.findViewById<Button>(R.id.start_button)
        pauseButton = view.findViewById<Button>(R.id.pause_button)
        resumeButton = view.findViewById<Button>(R.id.resume_button)
        terminateButton = view.findViewById<Button>(R.id.terminate_button)

        startButton.setOnClickListener {
            points = emptyList()
            speed = 0.0
            distance = 0.0
            timeSpent = 0L
            startButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
            startFlag = true
            chronometer.base = SystemClock.elapsedRealtime() + timeSpent
            chronometer.start()
        }

        pauseButton.setOnClickListener {
            pauseButton.visibility = View.GONE
            resumeButton.visibility = View.VISIBLE
            terminateButton.visibility = View.VISIBLE
            startFlag = false
            timeSpent = chronometer.base - SystemClock.elapsedRealtime()
            Log.d("time",timeSpent.toString())
            chronometer.stop()
        }

        resumeButton.setOnClickListener {
            pauseButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            terminateButton.visibility = View.GONE
            startFlag = true
            chronometer.base = SystemClock.elapsedRealtime() + timeSpent
            chronometer.start()
        }
        terminateButton.setOnClickListener {
            startButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            terminateButton.visibility = View.GONE
            startFlag = false
            polyline?.remove()
            chronometer.stop()
            calculateSpeed()
            var builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("Result")
            builder.setMessage("Save workout?")
            builder.setCancelable(true)
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int -> run { resetTimer() }})
            builder.setPositiveButton("OK", DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int -> run { saveWorkout();resetTimer() } }).show()

        }
        return view
    }

    private fun resetTimer(){
        timeSpent = 0L
        chronometer.base = SystemClock.elapsedRealtime() + timeSpent
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
            calculateDistance(local!!, latLng)
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

    private fun calculateDistance(latLng1: LatLng, latLng2: LatLng){
        var lat1 = Math.toRadians(latLng1.latitude)
        var lon1 = Math.toRadians(latLng1.longitude)
        var lat2 = Math.toRadians(latLng2.latitude)
        var lon2 = Math.toRadians(latLng2.longitude)
        var earthRadius = 6378137.0
        distance += 2 * earthRadius *
                asin(
                    sqrt(
                        (sin(lat2 - lat1) / 2).pow(2.0) +
                                cos(lat1) * cos(lat2) * (sin(lon2 - lon1) / 2).pow(2.0)
                    )
                )
    }

    private fun calculateSpeed(){
        if(distance == 0.0) speed = 0.0
        else speed = ((abs(timeSpent)/1000)/60)/(distance/1000)
    }

    private fun saveWorkout(){
        // Create a new user with a first and last name
        val workout = hashMapOf(
            "distance" to distance,
            "speed" to speed,
            "time" to timeSpent.toInt(),
            "cords" to points,
        )

// Add a new document with a generated ID
        db.collection( FirebaseAuth.getInstance().currentUser?.email.toString())
            .add(workout)
            .addOnSuccessListener { documentReference ->
                Log.d("Save", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Save", "Error adding document", e)
            }
    }
}