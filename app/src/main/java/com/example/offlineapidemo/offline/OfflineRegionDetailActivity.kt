package com.example.offlineapidemo.offline

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.offlineapidemo.R
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.OfflineManager
import com.mapbox.mapboxsdk.offline.OfflineRegion
import com.mapbox.mapboxsdk.offline.OfflineRegionDefinition
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus
import com.mapbox.mapboxsdk.plugins.offline.model.OfflineDownloadOptions
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflineConstants
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflineDownloadChangeListener
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflinePlugin
import com.mapbox.mapboxsdk.plugins.offline.utils.OfflineUtils
import com.mapbox.mapboxsdk.style.expressions.Expression.*
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property.LINE_CAP_ROUND
import com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import kotlinx.android.synthetic.main.activity_offline_region_detail.*
import timber.log.Timber

class OfflineRegionDetailActivity : AppCompatActivity(), OfflineDownloadChangeListener,
    OnMapReadyCallback,
    MapboxMap.OnMapLongClickListener {
    private var offlinePlugin: OfflinePlugin? = null
    private var offlineRegion: OfflineRegion? = null
    private var isDownloading: Boolean = false

    private val offlineRegionStatusCallback = object : OfflineRegion.OfflineRegionStatusCallback {
        override fun onStatus(status: OfflineRegionStatus) {
            isDownloading = !status.isComplete
            updateFab()
        }

        override fun onError(error: String) {
            Toast.makeText(
                this@OfflineRegionDetailActivity,
                "Error getting offline region state: $error",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val offlineRegionDeleteCallback = object : OfflineRegion.OfflineRegionDeleteCallback {
        override fun onDelete() {
            Toast.makeText(
                this@OfflineRegionDetailActivity,
                "Region deleted.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        override fun onError(error: String) {
            fabDelete.isEnabled = true
            Toast.makeText(
                this@OfflineRegionDetailActivity,
                "Error getting offline region state: $error",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Navigation
    private var mapboxNavigation: MapboxNavigation? = null
    private var mapboxMap: MapboxMap? = null
    private val ORIGIN_COLOR = "#32a852" // Green
    private val DESTINATION_COLOR = "#F84D4D" // Red
    val points: ArrayList<Point> = ArrayList<Point>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_region_detail)
        mapView.onCreate(savedInstanceState)
        offlinePlugin = OfflinePlugin.getInstance(this)

        val bundle = intent.extras
        if (bundle != null) {
            loadOfflineDownload(bundle)
        }

        fabDelete.setOnClickListener { onFabClick(it) }
    }

    private fun loadOfflineDownload(bundle: Bundle) {
        val regionId: Long
        val offlineDownload =
            bundle.getParcelable<OfflineDownloadOptions>(OfflineConstants.KEY_BUNDLE)
        regionId = if (offlineDownload != null) {
            // coming from notification
            offlineDownload.uuid()
        } else {
            // coming from list
            bundle.getLong(KEY_REGION_ID_BUNDLE, -1)
        }

        if (regionId != -1L) {
            loadOfflineRegion(regionId)
        }
    }

    private fun loadOfflineRegion(id: Long) {
        OfflineManager.getInstance(this)
            .listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {

                override fun onList(offlineRegions: Array<OfflineRegion>) {
                    for (region in offlineRegions) {
                        if (region.id == id) {
                            offlineRegion = region
                            val definition = region.definition as OfflineRegionDefinition
                            setupUI(definition)
                            return
                        }
                    }
                }

                override fun onError(error: String) {
                    Timber.e(error)
                }
            })
    }

    private fun updateFab() {
        if (isDownloading) {
            fabDelete.setImageResource(R.drawable.ic_cancel)
            regionState.text = "DOWNLOADING"
        } else {
            fabDelete.setImageResource(R.drawable.ic_delete)
            regionState.text = "DOWNLOADED"
        }
    }

    private fun setupUI(definition: OfflineRegionDefinition) {
        // update map
        mapView?.getMapAsync { mapboxMap ->
            // correct style
            mapboxMap.setOfflineRegionDefinition(definition) { _ ->
                // restrict camera movement
                mapboxMap.setLatLngBoundsForCameraTarget(definition.bounds)

                // update textview data
                offlineRegion?.metadata?.let {
                    regionName.text = OfflineUtils.convertRegionName(it)
                }
                regionStyleUrl.text = definition.styleURL
                regionLatLngBounds.text = definition.bounds.toString()
                regionMinZoom.text = definition.minZoom.toString()
                regionMaxZoom.text = definition.maxZoom.toString()
                offlineRegion?.getStatus(offlineRegionStatusCallback)
            }
        }
        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, resources.getString(R.string.access_token))
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions)
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT).show()
    }

    fun onFabClick(view: View) {
        if (offlineRegion != null) {
            if (!isDownloading) {
                // delete download
                offlineRegion?.delete(offlineRegionDeleteCallback)
            } else {
                // cancel download
                val offlineDownload =
                    offlinePlugin?.getActiveDownloadForOfflineRegion(offlineRegion)
                if (offlineDownload != null) {
                    offlinePlugin?.cancelDownload(offlineDownload)
                    isDownloading = false
                }
            }
            view.visibility = View.GONE
        }
    }

    override fun onCreate(offlineDownload: OfflineDownloadOptions) {
        Timber.e("OfflineDownloadOptions created %s", offlineDownload.hashCode())
    }

    override fun onSuccess(offlineDownload: OfflineDownloadOptions) {
        isDownloading = false
        regionStateProgress.visibility = View.INVISIBLE
        updateFab()
    }

    override fun onCancel(offlineDownload: OfflineDownloadOptions) {
        finish() // nothing to do in this screen, cancel = delete
    }

    override fun onError(offlineDownload: OfflineDownloadOptions, error: String, message: String) {
        regionStateProgress.visibility = View.INVISIBLE
        regionState.text = "ERROR"
        Toast.makeText(this, error + message, Toast.LENGTH_LONG).show()
    }

    override fun onProgress(offlineDownload: OfflineDownloadOptions, progress: Int) {
        if (offlineRegion == null) {
            return
        }

        if (offlineDownload.uuid() == offlineRegion?.id) {
            if (regionStateProgress.visibility != View.VISIBLE) {
                regionStateProgress.visibility = View.VISIBLE
            }
            isDownloading = true
            regionStateProgress.progress = progress
        }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        offlinePlugin?.addOfflineDownloadStateChangeListener(this)
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        offlinePlugin?.removeOfflineDownloadStateChangeListener(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    companion object {
        val KEY_REGION_ID_BUNDLE = "com.mapbox.mapboxsdk.plugins.offline.bundle.id"
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapboxMap.setStyle(Style.LIGHT) {
            this.mapboxMap = mapboxMap

            enableLocationComponent()

// Add the click and route sources
            it.addSource(GeoJsonSource("CLICK_SOURCE"))
            it.addSource(
                GeoJsonSource(
                    "ROUTE_LINE_SOURCE_ID",
                    GeoJsonOptions().withLineMetrics(true)
                )
            )

// Add the destination marker image
            it.addImage(
                "ICON_ID",
                BitmapUtils.getBitmapFromDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.mapbox_marker_icon_default
                    )
                )!!
            )

// Add the LineLayer below the LocationComponent's bottom layer, which is the
// circular accuracy layer. The LineLayer will display the directions route.
            it.addLayerBelow(
                LineLayer("ROUTE_LAYER_ID", "ROUTE_LINE_SOURCE_ID")
                    .withProperties(
                        lineCap(LINE_CAP_ROUND),
                        lineJoin(LINE_JOIN_ROUND),
                        lineWidth(6f),
                        lineGradient(
                            interpolate(
                                linear(),
                                lineProgress(),
                                stop(0f, color(parseColor(ORIGIN_COLOR))),
                                stop(1f, color(parseColor(DESTINATION_COLOR)))
                            )
                        )
                    ),
                "mapbox-location-shadow-layer"
            )

// Add the SymbolLayer to show the destination marker
            it.addLayerAbove(
                SymbolLayer("CLICK_LAYER", "CLICK_SOURCE")
                    .withProperties(
                        iconImage("ICON_ID")
                    ),
                "ROUTE_LAYER_ID"
            )

            mapboxMap.addOnMapLongClickListener(this)
            Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT)
                .show()
        }
    }

    override fun onMapLongClick(latLng: LatLng): Boolean {
        mapboxMap?.getStyle {
            val clickPointSource = it.getSourceAs<GeoJsonSource>("CLICK_SOURCE")
            clickPointSource?.setGeoJson(Point.fromLngLat(latLng.longitude, latLng.latitude))
        }
        mapboxMap?.locationComponent?.lastKnownLocation?.let { originLocation ->
            points.add(Point.fromLngLat(originLocation.longitude, originLocation.latitude))
            mapboxNavigation?.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .accessToken(resources.getString(R.string.access_token))
                    .coordinates(points)
                    .alternatives(true)
                    .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                    .build(),
                routesReqCallback
            )
        }
        return true
    }
    private val routesReqCallback = object : RoutesRequestCallback {
        override fun onRoutesReady(routes: List<DirectionsRoute>) {
            if (routes.isNotEmpty()) {
                Snackbar.make(
                    container,
                    String.format(
                        getString(R.string.steps_in_route),
                        routes[0].legs()?.get(0)?.steps()?.size
                    ),
                    LENGTH_SHORT
                ).show()

// Update a gradient route LineLayer's source with the Maps SDK. This will
// visually add/update the line on the map. All of this is being done
// directly with Maps SDK code and NOT the Navigation UI SDK.
                mapboxMap?.getStyle {
                    val clickPointSource = it.getSourceAs<GeoJsonSource>("ROUTE_LINE_SOURCE_ID")
                    val routeLineString = LineString.fromPolyline(
                        routes[0].geometry()!!,
                        6
                    )
                    clickPointSource?.setGeoJson(routeLineString)
                }
                // route_retrieval_progress_spinner.visibility = INVISIBLE
            } else {
                Snackbar.make(container, R.string.no_routes, LENGTH_SHORT).show()
            }
        }

        override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
            Timber.e("route request failure %s", throwable.toString())
            Snackbar.make(container, R.string.route_request_failed, LENGTH_SHORT).show()
        }

        override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
            Timber.d("route request canceled")
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        mapboxMap?.getStyle {
            mapboxMap?.locationComponent?.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(
                        this@OfflineRegionDetailActivity,
                        it
                    )
                        .build()
                )
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.COMPASS
            }
        }
    }
}