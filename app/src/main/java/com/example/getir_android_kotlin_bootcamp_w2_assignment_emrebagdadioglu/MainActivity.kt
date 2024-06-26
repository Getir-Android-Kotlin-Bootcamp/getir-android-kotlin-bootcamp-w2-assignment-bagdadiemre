package com.example.getir_android_kotlin_bootcamp_w2_assignment_emrebagdadioglu

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import  com.example.getir_android_kotlin_bootcamp_w2_assignment_emrebagdadioglu.R
import com.google.android.libraries.places.R as placesR
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException


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

        autoCompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment



        autoCompleteFragment.getView()
        val info: CardView = findViewById(R.id.infoCardView)
        autoCompleteFragment.setHint("Where is your location ?")
        autoCompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID, Place.Field.ADDRESS, Place.Field.LAT_LNG
            )
        )

        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val latLng = place.latLng
                place.address?.let {
                    val address = it
                    val addressTextView: TextView = findViewById(R.id.locationText)
                    addressTextView.text = address
                    info.visibility = CardView.VISIBLE
                }
                if (latLng != null) {
                    val markerOptions =
                        MarkerOptions().position(latLng).title("Current Location").icon(
                            bitmapDescriptorFromVector(
                                this@MainActivity, R.drawable.ic_marker
                            )
                        ).anchor(0.5f, 0.5f)

                    val marker = mGoogleMap?.addMarker(markerOptions)

// Add a circle around the marker
                    val circle1 = mGoogleMap?.addCircle(
                        CircleOptions().center(latLng).radius(130.0) // radius in meters
                            .strokeColor(Color.TRANSPARENT) // "#f6dee0"
                            .fillColor(Color.parseColor("#80D61355")) // fill color, transparent in this case
                    )

                    val circle2 = mGoogleMap?.addCircle(
                        CircleOptions().center(latLng).radius(230.0) // radius in meters
                            .strokeColor(Color.TRANSPARENT) // "#f6dee0"
                            .fillColor(Color.parseColor("#80f5d3d5")) // fill color, transparent in this case
                    )

// Associate the circle with the marker for management purposes
                    marker?.tag = Pair(circle1, circle2)

                    mGoogleMap?.setOnCameraIdleListener {
                        val zoomLevel = mGoogleMap?.cameraPosition?.zoom ?: 0f

                        val newRadius1 = calculateRadius(
                            zoomLevel, 250
                        ) // Calculate the new radius based on the zoom level
                        circle1?.radius = newRadius1

                        val newRadius2 = calculateRadius(
                            zoomLevel, 100
                        ) // Calculate the new radius based on the zoom level
                        circle2?.radius = newRadius2

                    }
                    zoomOnMap(latLng)


                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Toast.makeText(this@MainActivity, "An error occurred: $status", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun zoomOnMap(latLng: LatLng) {
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(latLng, 12f)
        mGoogleMap?.moveCamera(newLatLngZoom)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), PERMISSIONS_REQUEST_LOCATION
            )
            return
        }




        try {

            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map
                )
            )

            if (!success) {
                Log.e("MapsActivity", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "Can't find style. Error: ", e)
        }


        // Get current location
        val locationTask: Task<android.location.Location> = fusedLocationClient.lastLocation
        locationTask.addOnSuccessListener(OnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)

                mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))


                val addressTextView: TextView = findViewById(R.id.locationText)
                addressTextView.text = getAddressFromLatLng(this, currentLatLng)

                // Add marker at current location
                val markerOptions =
                    MarkerOptions().position(currentLatLng).title("Current Location")
                        .icon(bitmapDescriptorFromVector(this@MainActivity, R.drawable.ic_marker))
                        .anchor(0.5f, 0.5f)

                val marker = mGoogleMap?.addMarker(markerOptions)

// Add a circle around the marker
                val circle1 = mGoogleMap?.addCircle(
                    CircleOptions().center(currentLatLng).radius(130.0) // radius in meters
                        .strokeColor(Color.TRANSPARENT) // "#f6dee0"
                        .fillColor(Color.parseColor("#80D61355")) // fill color, transparent in this case
                )

                val circle2 = mGoogleMap?.addCircle(
                    CircleOptions().center(currentLatLng).radius(230.0) // radius in meters
                        .strokeColor(Color.TRANSPARENT) // "#f6dee0"
                        .fillColor(Color.parseColor("#80f5d3d5")) // fill color, transparent in this case
                )

// Associate the circle with the marker for management purposes
                marker?.tag = Pair(circle1, circle2)

                mGoogleMap?.setOnCameraIdleListener {
                    val zoomLevel = mGoogleMap?.cameraPosition?.zoom ?: 0f

                    val newRadius1 = calculateRadius(
                        zoomLevel, 250
                    ) // Calculate the new radius based on the zoom level
                    circle1?.radius = newRadius1

                    val newRadius2 = calculateRadius(
                        zoomLevel, 100
                    ) // Calculate the new radius based on the zoom level
                    circle2?.radius = newRadius2

                }


            } else {
                Toast.makeText(
                    this@MainActivity, "Current location is not available", Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateRadius(zoomLevel: Float, factor: Int): Double {
        return 1000 * Math.pow(2.0, (20 - zoomLevel).toDouble()) / factor
    }


    companion object {
        private const val PERMISSIONS_REQUEST_LOCATION = 100
    }

    private fun bitmapDescriptorFromVector(
        context: Context, @DrawableRes vectorResId: Int
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0, 0, vectorDrawable!!.intrinsicWidth, vectorDrawable!!.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable!!.intrinsicWidth,
            vectorDrawable!!.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable!!.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun getAddressFromLatLng(context: Context, latLng: LatLng): String {
        val geocoder = Geocoder(context)
        var addressText = ""
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    addressText = address.getAddressLine(0)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return addressText
    }

}