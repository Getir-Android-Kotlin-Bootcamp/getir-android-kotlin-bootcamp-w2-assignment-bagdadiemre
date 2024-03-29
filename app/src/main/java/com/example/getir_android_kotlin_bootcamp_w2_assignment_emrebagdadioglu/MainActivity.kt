package com.example.getir_android_kotlin_bootcamp_w2_assignment_emrebagdadioglu

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mGoogleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var autoCompleteFragment: AutocompleteSupportFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //enableEdgeToEdge()
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        autoCompleteFragment = supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        val info : CardView = findViewById(R.id.infoCardView)
        autoCompleteFragment.setHint("Where is your location ?")
        autoCompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG))
        autoCompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng
                if (latLng != null) {
                    zoomOnMap(latLng)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@MainActivity, "An error occurred: $status", Toast.LENGTH_SHORT).show()
            }
        })

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun zoomOnMap(latLng: LatLng){
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
        mGoogleMap?.moveCamera(newLatLngZoom)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSIONS_REQUEST_LOCATION
            )
            return
        }

        // Get current location
        val locationTask: Task<android.location.Location> = fusedLocationClient.lastLocation
        locationTask.addOnSuccessListener(OnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                // Add marker at current location
                val markerOptions = MarkerOptions()
                    .position(currentLatLng)
                    .title("Current Location")
                mGoogleMap?.addMarker(markerOptions)
            } else {
                Toast.makeText(this@MainActivity, "Current location is not available", Toast.LENGTH_SHORT).show()
            }
        })
    }
    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 100
    }
}