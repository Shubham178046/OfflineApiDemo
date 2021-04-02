package com.example.offlineapidemo

import android.annotation.SuppressLint
import android.graphics.Color.parseColor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
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
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import kotlinx.android.synthetic.main.activity_basic_nav_sdk_only.*
import timber.log.Timber

class BasicNavSdkOnlyActivity : AppCompatActivity(), OnMapReadyCallback,
    MapboxMap.OnMapLongClickListener {
    private var mapboxNavigation: MapboxNavigation? = null
    private var mapboxMap: MapboxMap? = null
    private val ORIGIN_COLOR = "#32a852" // Green
    private val DESTINATION_COLOR = "#F84D4D" // Red

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_nav_sdk_only)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val mapboxNavigationOptions = MapboxNavigation
            .defaultNavigationOptionsBuilder(this, getString(R.string.access_token))
            .build()

        mapboxNavigation = MapboxNavigation(mapboxNavigationOptions)
        Snackbar.make(container, R.string.msg_long_press_map_to_place_waypoint, LENGTH_SHORT).show()
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
        var point = Point.fromLngLat(latLng.longitude, latLng.latitude)
        route_retrieval_progress_spinner.visibility = VISIBLE
// Place the destination marker at the map long click location
        mapboxMap?.getStyle {
            val clickPointSource = it.getSourceAs<GeoJsonSource>("CLICK_SOURCE")
            clickPointSource?.setGeoJson(Point.fromLngLat(latLng.longitude, latLng.latitude))
        }
        mapboxMap?.locationComponent?.lastKnownLocation?.let { originLocation ->
            var origin = Point.fromLngLat(
                originLocation.longitude,
                originLocation.latitude
            )
            mapboxNavigation?.requestRoutes(
                RouteOptions.builder().applyDefaultParams()
                    .accessToken(getString(R.string.access_token))
                    .coordinates(listOf(origin, point))
                    .alternatives(true)
                    .profile(DirectionsCriteria.PROFILE_DRIVING)
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
                route_retrieval_progress_spinner.visibility = INVISIBLE
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

    /**
     * Enable the Maps SDK's LocationComponent
     */
    @SuppressLint("MissingPermission")
    private fun enableLocationComponent() {
        mapboxMap?.getStyle {
            mapboxMap?.locationComponent?.apply {
                activateLocationComponent(
                    LocationComponentActivationOptions.builder(
                        this@BasicNavSdkOnlyActivity,
                        it
                    )
                        .build()
                )
                isLocationComponentEnabled = true
                cameraMode = CameraMode.TRACKING_GPS
                renderMode = RenderMode.GPS
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    public override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}