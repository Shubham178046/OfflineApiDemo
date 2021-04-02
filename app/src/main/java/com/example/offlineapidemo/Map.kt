package com.example.offlineapidemo



import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.offlineapidemo.util.AnimationUtils
import com.example.offlineapidemo.util.MapUtils
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.*
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.*
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.offline.OfflineManager.CreateOfflineRegionCallback
import com.mapbox.mapboxsdk.offline.OfflineManager.ListOfflineRegionsCallback
import com.mapbox.mapboxsdk.offline.OfflineRegion.OfflineRegionDeleteCallback
import com.mapbox.mapboxsdk.offline.OfflineRegion.OfflineRegionObserver
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.navigation.ui.route.NavigationMapRoute

import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import kotlinx.android.synthetic.main.activity_map.*
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber


class Map : AppCompatActivity(), PermissionsListener {
    private var grayPolyline: Polyline? = null
    private var blackPolyline: Polyline? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var movingCabMarker: Marker? = null
    private var isEndNotified = false
    private val ROUTE_LAYER_ID = "route-layer-id"
    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ICON_LAYER_ID = "icon-layer-id"
    private val ICON_SOURCE_ID = "icon-source-id"
    private val RED_PIN_ICON_ID = "red-pin-icon-id"
    lateinit var map: MapboxMap
    lateinit var permissionManager: PermissionsManager
    private var progressBar: ProgressBar? = null
    private var offlineManager: OfflineManager? = null
    var locationComponent: LocationComponent? = null
    private var originPosition: Point? = null
    private var dstPosition: Point? = null
    var navigationMapRoute: NavigationMapRoute? = null
    var currentRoute: DirectionsRoute? = null
    private var client: MapboxDirections? = null
    var originLocation: Location? = null
    private var previousLatLng: LatLng? = null
    private var currentLatLng: LatLng? = null
    private var origin: Point? = null
    private var destination: Point? = null
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private val POINTS: ArrayList<MutableList<Point>> = ArrayList()
    private val OUTER_POINTS: ArrayList<Point> = ArrayList()
    init {
        OUTER_POINTS.add(Point.fromLngLat(-122.685699, 45.522585));
        OUTER_POINTS.add(Point.fromLngLat(-122.708873, 45.534611));
        OUTER_POINTS.add(Point.fromLngLat(-122.678833, 45.530883));
        OUTER_POINTS.add(Point.fromLngLat(-122.667503, 45.547115));
        OUTER_POINTS.add(Point.fromLngLat(-122.660121, 45.530643));
        OUTER_POINTS.add(Point.fromLngLat(-122.636260, 45.533529));
        OUTER_POINTS.add(Point.fromLngLat(-122.659091, 45.521743));
        OUTER_POINTS.add(Point.fromLngLat(-122.648792, 45.510677));
        OUTER_POINTS.add(Point.fromLngLat(-122.664070, 45.515008));
        OUTER_POINTS.add(Point.fromLngLat(-122.669048, 45.502496));
        OUTER_POINTS.add(Point.fromLngLat(-122.678489, 45.515369));
        OUTER_POINTS.add(Point.fromLngLat(-122.702007, 45.506346));
        OUTER_POINTS.add(Point.fromLngLat(-122.685699, 45.522585));
        POINTS.add(OUTER_POINTS);
    }
    // JSON encoding/decoding
    val JSON_CHARSET = "UTF-8"
    val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_map)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(object : OnMapReadyCallback {
            override fun onMapReady(mapboxMap: MapboxMap) {
                map = mapboxMap
                enableLocation()
                map.setStyle(Style.OUTDOORS, object : Style.OnStyleLoaded {
                    override fun onStyleLoaded(style: Style) {
                        origin = Point.fromLngLat(73.17539162383034, 22.31018714802519)
                        destination = Point.fromLngLat(73.19586728334912, 22.340470156353586)
                         showDefaultLocationOnMap(LatLng(22.41601901621444, 73.08930216284499))
                        Handler().postDelayed(Runnable {
                            showPath(MapUtils.getListOfLocations())
                            showMovingCab(MapUtils.getListOfLocations())
                        }, 3000)
                        // Set up the OfflineManager
                        offlineManager = OfflineManager.getInstance(this@Map)

                        // Create a bounding box for the offline region
                        val latLngBounds: LatLngBounds = LatLngBounds.Builder()
                            .include(LatLng(22.41601901621444, 73.08930216284499)) // Northeast
                            .include(LatLng(22.30982982239691, 73.17355699298402)) // Southwest
                            .build()

                        // Define the offline region
                        val definition = OfflineTilePyramidRegionDefinition(
                            style.getUri(),
                            latLngBounds,
                            10.0,
                            20.0,
                            this@Map.getResources().getDisplayMetrics().density
                        )

                        // Set the metadata
                        val metadata: ByteArray?
                        metadata = try {
                            val jsonObject = JSONObject()
                            jsonObject.put(JSON_FIELD_REGION_NAME, "Yosemite National Park")
                            val json = jsonObject.toString()
                            json.toByteArray(charset(JSON_CHARSET))
                        } catch (exception: Exception) {
                            Timber.e("Failed to encode metadata: %s", exception.message)
                            null
                        }
                        // Create the region asynchronously
                        if (metadata != null) {
                            offlineManager!!.createOfflineRegion(
                                definition,
                                metadata,
                                object : CreateOfflineRegionCallback {
                                    override fun onCreate(offlineRegion: OfflineRegion) {
                                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)

                                        // Display the download progress bar
                                        progressBar = findViewById(R.id.progress_bar)
                                        startProgress()

                                        // Monitor the download progress using setObserver
                                        offlineRegion.setObserver(object : OfflineRegionObserver {
                                            override fun onStatusChanged(status: OfflineRegionStatus) {

                                                // Calculate the download percentage and update the progress bar
                                                val percentage =
                                                    if (status.requiredResourceCount >= 0) 100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0
                                                if (status.isComplete) {
                                                    // Download complete
                                                    endProgress(getString(R.string.simple_offline_end_progress_success))
                                                } else if (status.isRequiredResourceCountPrecise) {
                                                    // Switch to determinate state
                                                    setPercentage(Math.round(percentage).toInt())
                                                }
                                            }

                                            override fun onError(error: OfflineRegionError) {
                                                // If an error occurs, print to logcat
                                                Timber.e("onError reason: %s", error.reason)
                                                Timber.e("onError message: %s", error.message)
                                            }

                                            override fun mapboxTileCountLimitExceeded(limit: Long) {
                                                // Notify if offline region exceeds maximum tile count
                                                Timber.e(
                                                    "Mapbox tile count limit exceeded: %s",
                                                    limit
                                                )
                                            }
                                        })
                                    }

                                    override fun onError(error: String) {
                                        Timber.e("Error: %s", error)
                                    }
                                })
                        }
                        initLayers(style)
                        initSource(style)
                        getRoute(mapboxMap, origin!!, destination!!);
                        if (!map.markers.isEmpty()) {
                            map.clear()
                        }
                        originPosition = Point.fromLngLat(73.17539162383034, 22.31018714802519)
                        dstPosition = Point.fromLngLat(73.19586728334912, 22.340470156353586)
                        val options = MarkerOptions()
                        options.title("Source")
                        options.position(LatLng(22.31018714802519, 73.17539162383034))

                        val options1 = MarkerOptions()
                        options1.title("Destination")
                        options1.position(LatLng(22.340470156353586, 73.19586728334912))

                        map.addMarker(options);
                        map.addMarker(options1);
                        getRoute(originPosition!!, dstPosition!!)
                        /*startButton.setOnClickListener {
                            val navigationLauncherOptions = NavigationLauncherOptions.builder() //1
                                .directionsRoute(currentRoute) //2
                                .shouldSimulateRoute(true) //3
                                .build()

                            NavigationLauncher.startNavigation(
                                this@Map,
                                navigationLauncherOptions
                            ) //4
                        }*/
                    }
                })
            }
        })
    }

    fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
        } else {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    private fun getRoute(originPoint: Point, endPoint: Point) {
        NavigationRoute.builder(this) //1
            .accessToken(Mapbox.getAccessToken()!!) //2
            .origin(originPoint) //3
            .destination(endPoint) //4
            .build() //5
            .getRoute(object : Callback<DirectionsResponse> { //6
                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.d("MainActivity", t.localizedMessage)
                }

                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (navigationMapRoute != null) {
                        navigationMapRoute?.updateRouteVisibilityTo(false)
                    } else {
                        //navigationMapRoute = NavigationMapRoute(null, mapView, map, R.style.NavigationMapRoute)
                    }

                    currentRoute = response.body()?.routes()?.first()
                    if (currentRoute != null) {
                        navigationMapRoute?.addRoute(currentRoute)
                    }
                    //  mapView.getMapAsync(this@Map)
                    //btnNavigate.isEnabled = true
                }
            })
    }

    fun setCameraPosition(location: Location) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    location.latitude,
                    location.longitude
                ), 30.0
            )
        )
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationComponent?.onStart()
        }

        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        locationComponent?.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (offlineManager != null) {
            offlineManager!!.listOfflineRegions(object : ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>) {
                    if (offlineRegions.size > 0) {
                        // delete the last item in the offlineRegions list which will be yosemite offline map
                        offlineRegions[offlineRegions.size - 1].delete(object :
                            OfflineRegionDeleteCallback {
                            override fun onDelete() {
                                Toast.makeText(
                                    this@Map,
                                    getString(R.string.basic_offline_deleted_toast),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            override fun onError(error: String) {
                                Timber.e("On delete error: %s", error)
                            }
                        })
                    }
                }

                override fun onError(error: String) {
                    Timber.e("onListError: %s", error)
                }
            })
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState!!)
        mapView.onSaveInstanceState(outState)
    }

    // Progress bar methods
    private fun startProgress() {

        // Start and show the progress bar
        isEndNotified = false
        progressBar!!.isIndeterminate = true
        progressBar!!.visibility = View.VISIBLE
    }

    private fun setPercentage(percentage: Int) {
        progressBar!!.isIndeterminate = false
        progressBar!!.progress = percentage
    }

    private fun endProgress(message: String) {
        // Don't notify more than once
        if (isEndNotified) {
            return
        }

        // Stop and hide the progress bar
        isEndNotified = true
        progressBar!!.isIndeterminate = false
        progressBar!!.visibility = View.GONE

        // Show a toast
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    @SuppressLint("MissingPermission")
    private fun checkLocation() {
        if (originLocation == null) {
            map.locationComponent.lastKnownLocation?.run {
                originLocation = this
            }
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(
            this,
            "This app needs location permission to be able to show your location on the map",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocation()
        } else {
            Toast.makeText(this, "User location was not granted", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun showPath(latLngList: ArrayList<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))

        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(5f)
        polylineOptions.addAll(latLngList)
        grayPolyline = map.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.color(Color.BLACK)
        blackPolylineOptions.width(5f)
        blackPolyline = map.addPolyline(blackPolylineOptions)

        originMarker = addOriginDestinationMarkerAndGet(latLngList[0])
        //   originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker = addOriginDestinationMarkerAndGet(latLngList[latLngList.size - 1])
        //    destinationMarker?.setAnchor(0.5f, 0.5f)

        val polylineAnimator = AnimationUtils.polylineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (grayPolyline?.points!!.size) * (percentValue / 100.0f).toInt()
            blackPolyline?.points = grayPolyline?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

    private fun addOriginDestinationMarkerAndGet(latLng: LatLng): Marker {
        val iconFactory = IconFactory.getInstance(this)
        val icon: Icon = iconFactory.fromBitmap(MapUtils.getOriginDestinationMarkerBitmap())
        return map.addMarker(
            MarkerOptions().position(latLng)
                .icon(icon)
        )
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker {
        val iconFactory = IconFactory.getInstance(this)
        val icon: Icon = iconFactory.fromBitmap(MapUtils.getCarBitmap(this))
        return map.addMarker(
            MarkerOptions().position(latLng)
                .icon(icon)
        )
    }

    private fun updateCarLocation(latLng: LatLng) {
        if (movingCabMarker == null) {
            movingCabMarker = addCarMarkerAndGet(latLng)
        }
        if (previousLatLng == null) {
            currentLatLng = latLng
            previousLatLng = currentLatLng
            movingCabMarker?.position = currentLatLng
            //movingCabMarker?.setAnchor(0.5f, 0.5f)
            animateCamera(currentLatLng!!)
        } else {
            previousLatLng = currentLatLng
            currentLatLng = latLng
            val valueAnimator = AnimationUtils.carAnimator()
            valueAnimator.addUpdateListener { va ->
                if (currentLatLng != null && previousLatLng != null) {
                    val multiplier = va.animatedFraction
                    val nextLocation = LatLng(
                        multiplier * currentLatLng!!.latitude + (1 - multiplier) * previousLatLng!!.latitude,
                        multiplier * currentLatLng!!.longitude + (1 - multiplier) * previousLatLng!!.longitude
                    )
                    movingCabMarker?.position = nextLocation
                    val rotation = MapUtils.getRotation(previousLatLng!!, nextLocation)
                    /* if (!rotation.isNaN()) {
                         movingCabMarker?.rotation = rotation
                     }
                     movingCabMarker?.setAnchor(0.5f, 0.5f)*/
                    animateCamera(nextLocation)
                }
            }
            valueAnimator.start()
        }
    }

    private fun showDefaultLocationOnMap(latLng: LatLng) {
        moveCamera(latLng)
        animateCamera(latLng)
    }

    private fun moveCamera(latLng: LatLng) {
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.5).build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun initSource(loadedMapStyle: Style) {
        loadedMapStyle.addSource(GeoJsonSource(ROUTE_SOURCE_ID, Polygon.fromLngLats(POINTS)))
        val iconGeoJsonSource = GeoJsonSource(
            ICON_SOURCE_ID, FeatureCollection.fromFeatures(
                arrayOf<Feature>(
                    Feature.fromGeometry(Point.fromLngLat(origin!!.longitude(), origin!!.latitude())),
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            destination!!.longitude(),
                            destination!!.latitude()
                        )
                    )
                )
            )
        )
        loadedMapStyle.addSource(iconGeoJsonSource)
    }

    /**
     * Add the route and marker icon layers to the map
     */
    private fun initLayers(loadedMapStyle: Style) {
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

// Add the LineLayer to the map. This layer will display the directions route.
        routeLayer.setProperties(
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineWidth(5f),
            lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)

// Add the red marker icon image to the map
        loadedMapStyle.addImage(
            RED_PIN_ICON_ID, MapUtils.getCarBitmap(this)
        )

// Add the red marker icon SymbolLayer to the map
        loadedMapStyle.addLayer(
            SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
                iconImage(RED_PIN_ICON_ID),
                iconIgnorePlacement(true),
                iconAllowOverlap(true),
                iconOffset(arrayOf(0f, -9f))
            )
        )
    }
    /* private fun updateCarLocation(latLng: LatLng) {
         if (movingCabMarker == null) {
             movingCabMarker = addCarMarkerAndGet(latLng)
         }
         if (previousLatLng == null) {
             currentLatLng = latLng
             previousLatLng = currentLatLng
             movingCabMarker?.position = currentLatLng
                 // movingCabMarker?.setAnchor(0.5f, 0.5f)
             animateCamera(currentLatLng!!)
         } else {
             previousLatLng = currentLatLng
             currentLatLng = latLng
             val valueAnimator = AnimationUtils.carAnimator()
             valueAnimator.addUpdateListener { va ->
                 if (currentLatLng != null && previousLatLng != null) {
                     val multiplier = va.animatedFraction
                     val nextLocation =LatLng(
                         multiplier * currentLatLng!!.latitude + (1 - multiplier) * previousLatLng!!.latitude,
                         multiplier * currentLatLng!!.longitude + (1 - multiplier) * previousLatLng!!.longitude
                     )
                     movingCabMarker?.position = nextLocation
                     val rotation = MapUtils.getRotation(previousLatLng!!, nextLocation)
                     if (!rotation.isNaN()) {
                        // movingCabMarker?.rotation = rotation
                     }
                    // movingCabMarker?.setAnchor(0.5f, 0.5f)
                     animateCamera(nextLocation)
                 }
             }
             valueAnimator.start()
         }
     }*/

    private fun showMovingCab(cabLatLngList: ArrayList<LatLng>) {
        handler = Handler()
        var index = 0
        runnable = Runnable {
            run {
                if (index < 10) {
                    updateCarLocation(cabLatLngList[index])
                    handler.postDelayed(runnable, 3000)
                    ++index
                } else {
                    handler.removeCallbacks(runnable)
                    Toast.makeText(this@Map, "Trip Ends", Toast.LENGTH_LONG).show()
                }
            }
        }
        handler.postDelayed(runnable, 5000)
    }
    private fun getRoute(mapboxMap: MapboxMap?, origin: Point, destination: Point) {
        client = MapboxDirections.builder()
            .origin(origin)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(getString(R.string.access_token))
            .build()
        client!!.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
// You can get the generic HTTP info about the response
                Timber.d("Response code: " + response.code())
                if (response.body() == null) {
                    Timber.e("No routes found, make sure you set the right user and access token.")
                    return
                } else if (response.body()!!.routes().size < 1) {
                    Timber.e("No routes found")
                    return
                }

// Get the directions route
                currentRoute = response.body()!!.routes()[0]

// Make a toast which displays the route's distance
                Toast.makeText(
                    this@Map, String.format(
                        getString(R.string.directions_activity_toast_message),
                        currentRoute!!.distance()
                    ), Toast.LENGTH_SHORT
                ).show()
                mapboxMap?.getStyle { style -> // Retrieve and update the source designated for showing the directions route
                    val source = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE_ID)

                    // Create a LineString with the directions route's geometry and
                    // reset the GeoJSON source for the route LineLayer source
                    source?.setGeoJson(
                        LineString.fromPolyline(
                            currentRoute!!.geometry()!!,
                            PRECISION_6
                        )
                    )
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                Timber.e("Error: " + throwable.message)
                Toast.makeText(
                    this@Map, "Error: " + throwable.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}