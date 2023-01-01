package com.example.oart

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.time.LocalDateTime
import kotlin.math.*

class MapsFragment : Fragment() {

    lateinit var startButton: Button
    lateinit var pauseButton: Button
    lateinit var resumeButton: Button
    lateinit var terminateButton: Button
    lateinit var cameraButton: FloatingActionButton
    lateinit var chronometer: Chronometer
    lateinit var distanceText: TextView

    lateinit var client: FusedLocationProviderClient

    private var locationCallback: LocationCallback = object: LocationCallback(){
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            val location1: Location = p0.lastLocation
            onlocationchange(location1)
        }
    }

    var locationRequest: LocationRequest = LocationRequest()
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        .setInterval(10000)
        .setFastestInterval(1000)

    private var mCurrLocationMarker: Marker? = null
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
        startButton = view.findViewById(R.id.start_button)
        pauseButton = view.findViewById(R.id.pause_button)
        resumeButton = view.findViewById(R.id.resume_button)
        terminateButton = view.findViewById(R.id.terminate_button)
        cameraButton = view.findViewById(R.id.cameraButton)
        distanceText = view.findViewById(R.id.textView2)

        cameraButton.setOnClickListener {
            requestCameraPermission()
        }

        startButton.setOnClickListener {
            points = emptyList()
            speed = 0.0
            distance = 0.0
            timeSpent = 0L
            imageUriList = emptyList()
            imageUri = null
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
            cameraButton.visibility = View.VISIBLE
            startFlag = false
            timeSpent = chronometer.base - SystemClock.elapsedRealtime()
            Log.d("time",timeSpent.toString())
            chronometer.stop()
        }

        resumeButton.setOnClickListener {
            pauseButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            terminateButton.visibility = View.GONE
            cameraButton.visibility = View.GONE
            startFlag = true
            chronometer.base = SystemClock.elapsedRealtime() + timeSpent
            chronometer.start()
        }
        terminateButton.setOnClickListener {
            startButton.visibility = View.VISIBLE
            resumeButton.visibility = View.GONE
            terminateButton.visibility = View.GONE
            cameraButton.visibility = View.GONE
            startFlag = false
            polyline?.remove()
            chronometer.stop()
            distanceText.text = "0.0"
            calculateSpeed()
            var builder = AlertDialog.Builder(requireActivity())
            builder.setTitle("Result")
            builder.setMessage("Save workout?")
            builder.setCancelable(false)
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int -> run { resetTimer() }})
            builder.setPositiveButton("OK", DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int -> run { saveWorkout(); resetTimer() } }).show()

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
            distanceText.text = (((distance /1000)*100).roundToInt().toDouble()/100).toString()
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
            "timestamp" to LocalDateTime.now().toString(),
            "distance" to distance,
            "speed" to speed,
            "time" to timeSpent.toInt()*-1,
            "cords" to points,
        )

// Add a new document with a generated ID

        if(imageUriList.isEmpty()){
            db.collection( FirebaseAuth.getInstance().currentUser?.email.toString())
                .add(workout)
                .addOnSuccessListener { documentReference ->
                    Log.d("Save", "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w("Save", "Error adding document", e)
                }
        }
        else {
            var listImages : List<String> = emptyList()
            var last = imageUriList.size
            var count = 0
            var storagecount = 0
            var storagecount2 = 0
            var storageRef: List<StorageReference> = emptyList()
            for (imageUri in imageUriList) {
                Log.d("IMAGEDATA",imageUri.toString())

                storageRef += FirebaseStorage.getInstance().reference.child("Images/" + System.currentTimeMillis().toString())
                storagecount += 1
                imageUri?.let {
                    storageRef[storagecount-1].putFile(it).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            storagecount2 += 1
                            storageRef[storagecount2-1].downloadUrl.addOnSuccessListener { uri ->
                                Log.d("URI", uri.toString())
                                listImages += uri.toString()
                                count += 1
                                if(last == count){
                                    workout["images"] = listImages
                                    db.collection(FirebaseAuth.getInstance().currentUser?.email.toString())
                                        .add(workout)
                                        .addOnSuccessListener { documentReference ->
                                            Log.d("Save", "DocumentSnapshot added with ID: ${documentReference.id}")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w("Save", "Error adding document", e)
                                        }
                                }
                            }
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                task.exception?.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        }
        imageUriList = emptyList()
        imageUri = null
    }

    val CAMERA_PERMISSION_CODE = 1000;
    private fun requestCameraPermission(): Boolean {
        var permissionGranted = false
// If system os is Marshmallow or Above, we need to request runtime permission
            val cameraPermissionNotGranted = checkSelfPermission(activity as MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
            if (cameraPermissionNotGranted){
                val permission = arrayOf(Manifest.permission.CAMERA)
                // Display permission dialog
                requestPermissions(permission, CAMERA_PERMISSION_CODE)
            }
            else{
                // Permission already granted
                permissionGranted = true
                openCameraInterface()
            }

        return permissionGranted
    }

    private val IMAGE_CAPTURE_CODE = 1001
    private var imageUriList: List<Uri?> = emptyList()
    private var imageUri: Uri? = null
    private fun openCameraInterface() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Take a picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "take_picture_description")
        imageUri = activity?.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode === CAMERA_PERMISSION_CODE) {
            if (grantResults.size === 1 && grantResults[0] ==    PackageManager.PERMISSION_GRANTED){
                openCameraInterface()
            }
            else{
                showAlert("Camera permission was denied. Unable to take a picture.");
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
//            showAlert( imageUri.toString())
            imageUriList += imageUri
        }
        else {
            showAlert("Failed to take camera picture")
        }
    }

    private fun showAlert(message: String) {
        val builder = AlertDialog.Builder(activity as MainActivity)
        builder.setMessage(message)
        builder.setPositiveButton("OK", null)
        val dialog = builder.create()
        dialog.show()
    }


}