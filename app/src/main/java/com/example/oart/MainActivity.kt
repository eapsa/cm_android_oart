package com.example.oart

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ActionMenuView
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.fragment.app.Fragment
import androidx.navigation.ActivityNavigator
import androidx.navigation.findNavController
import com.example.oart.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapsFragment = MapsFragment()
        val blankFragment = BlankFragment()
        val blankFragment2 = BlankFragment2()

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> setCurrentFragment(mapsFragment)
                R.id.page_2 -> {
                    setCurrentFragment(blankFragment)
                }
                R.id.page_3 -> setCurrentFragment(blankFragment2)

            }
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bar,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.exit -> { FirebaseAuth.getInstance().signOut();
            startActivity(Intent(this, SignInActivity::class.java))}
            R.id.read_qrcode -> {}
            R.id.scan_qrcode -> {
                scanCode()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setCurrentFragment(fragment: Fragment)=
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragmentContainerView,fragment)
            commit()
        }

    private fun scanCode(){
        var options  = ScanOptions()
        options.setPrompt("Volume up to flash on")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        options.captureActivity = CaptureActivity::class.java
        barLauncher.launch(options)
    }

    private var barLauncher: ActivityResultLauncher<ScanOptions> = registerForActivityResult(
        ScanContract()
    ) {
            result ->
        kotlin.run {
            if(result.contents != null){
                var builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Add friend?")
                builder.setMessage(result.contents)
                builder.setCancelable(false)
                builder.setNegativeButton("Cancel", DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int ->
                })
                builder.setPositiveButton("OK", DialogInterface.OnClickListener(){ dialogInterface: DialogInterface, i: Int -> addFriend(result.contents)
                }).show()
            }
        }
    }

    private fun addFriend(friend:String){
        val db = Firebase.firestore
        db.collection(FirebaseAuth.getInstance().currentUser?.email.toString()+"-friends")
            .add(friend)
            .addOnSuccessListener { documentReference ->
                Log.d("Save", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Save", "Error adding document", e)
            }
    }

}