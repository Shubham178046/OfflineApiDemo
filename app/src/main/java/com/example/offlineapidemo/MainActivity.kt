package com.example.offlineapidemo

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_main.*

/*
class MainActivity : AppCompatActivity(), PermissionsListener, LocationEngineListener,
    OnMapReadyCallback, MapboxMap.OnMapClickListener {
    val REQUEST_CHECK_SETTINGS = 1
    var settingsClient: SettingsClient? = null

    //2
    lateinit var map: MapboxMap
    lateinit var permissionManager: PermissionsManager
    var originLocation: Location? = null

    var locationEngine: LocationEngine? = null
    var locationComponent: LocationComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_main)
        mapbox.onCreate(savedInstanceState)
        mapbox.getMapAsync(this)
        settingsClient = LocationServices.getSettingsClient(this)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                101
            )
        }
    }

    fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationComponent()
            initializeLocationEngine()
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }
    @SuppressWarnings("MissingPermission")
    private fun initializeLocationComponent() {
        locationComponent = map.locationComponent
        locationComponent?.activateLocationComponent(this)
        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.TRACKING
    }
    @SuppressWarnings("MissingPermission")
    fun initializeLocationEngine() {
        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.priority = LocationEnginePriority.HIGH_ACCURACY
        locationEngine?.activate()
        locationEngine?.addLocationEngineListener(this)

        val lastLocation = locationEngine?.lastLocation
        if(lastLocation != null)
        {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        }
        else{
            locationEngine?.addLocationEngineListener(this)
        }
    }

    fun setCameraPosition(location: Location?) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location!!.latitude,location.longitude),30.0))
    }
    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        if(PermissionsManager.areLocationPermissionsGranted(this))
        {
            locationEngine?.requestLocationUpdates()
            locationComponent?.onStart()
        }
        mapbox.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapbox.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapbox.onPause()
    }
    @SuppressWarnings("MissingPermission")
    override fun onStop() {
        super.onStop()
        locationEngine?.requestLocationUpdates()
        locationComponent?.onStop()
        mapbox.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationEngine?.deactivate()
        mapbox.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapbox.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapbox.onSaveInstanceState(outState)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "This app needs location permission to be able to show your location on the map", Toast.LENGTH_LONG).show()
    }

    override fun onPermissionResult(granted: Boolean) {
      if(granted)
      {
          enableLocation()
      }
        else{
          Toast.makeText(this, "User location was not granted", Toast.LENGTH_LONG).show()
          finish()
      }
    }
    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
       locationEngine?.requestLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
       location?.run {
           originLocation = this
           setCameraPosition(this)
       }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    override fun onMapReady(mapboxMap: MapboxMap?) {
        map = mapboxMap ?: return
        val locationRequestBuilder = LocationSettingsRequest.Builder().addLocationRequest(
            LocationRequest().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        )
        val locationRequest = locationRequestBuilder.build()
        settingsClient?.checkLocationSettings(locationRequest)?.run {
            addOnSuccessListener {
                enableLocation()
            }
            addOnFailureListener {
                val statusCode = (it as ApiException).statusCode
                if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                    val resolvableApiException = it as? ResolvableApiException
                    resolvableApiException?.startResolutionForResult(
                        this@MainActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                enableLocation()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        }
    }

    override fun onMapClick(point: LatLng) {
        TODO("Not yet implemented")
    }
}*/
