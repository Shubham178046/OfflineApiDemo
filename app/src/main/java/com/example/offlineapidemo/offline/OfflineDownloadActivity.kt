package com.example.offlineapidemo.offline

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import com.example.offlineapidemo.R
import com.mapbox.mapboxsdk.constants.MapboxConstants
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition
import com.mapbox.mapboxsdk.plugins.offline.model.NotificationOptions
import com.mapbox.mapboxsdk.plugins.offline.model.OfflineDownloadOptions
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflinePlugin
import com.mapbox.mapboxsdk.plugins.offline.utils.OfflineUtils
import kotlinx.android.synthetic.main.activity_offline_download.*
import java.util.ArrayList

class OfflineDownloadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_download)
        initUi()
        initSeekbarListeners()
        fabStartDownload.setOnClickListener {
            if (seekbarMaxZoom.progress.toFloat() > seekbarMinZoom.progress.toFloat())
                onDownloadRegion() else Toast.makeText(this,
                "Please make sure that the Max zoom value is larger" +
                        " than the Min zoom level", Toast.LENGTH_SHORT).show()
        }
    }
    private fun initUi() {
        initEditTexts()
        initSeekbars()
        initSpinner()
        initZoomLevelTextviews()
    }

    private fun initEditTexts() {
        editTextRegionName.setText("Region name")
        editTextLatNorth.setText("22.310696")
        editTextLonEast.setText("73.192635")
        editTextLatSouth.setText("22.29941")
        editTextLonWest.setText("73.20812")
    }

    private fun initSeekbars() {
        val maxZoom = MapboxConstants.MAXIMUM_ZOOM.toInt()
        seekbarMinZoom.max = maxZoom
        seekbarMinZoom.progress = 16
        seekbarMaxZoom.max = maxZoom
        seekbarMaxZoom.progress = 19
    }

    private fun initSpinner() {
        val styles = ArrayList<String>()
        styles.add(Style.MAPBOX_STREETS)
        styles.add(Style.DARK)
        styles.add(Style.LIGHT)
        styles.add(Style.OUTDOORS)
        val spinnerArrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, styles)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStyleUrl.adapter = spinnerArrayAdapter
    }

    private fun initZoomLevelTextviews() {
        textViewMaxText.text = getString(R.string.max_zoom_textview, seekbarMaxZoom.progress)
        textViewMinText.text = getString(R.string.min_zoom_textview, seekbarMinZoom.progress)
    }

    private fun initSeekbarListeners() {
        seekbarMaxZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textViewMaxText.text = getString(R.string.max_zoom_textview, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Empty on purpose
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Empty on purpose
            }
        })

        seekbarMinZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                textViewMinText.text = getString(R.string.min_zoom_textview, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Empty on purpose
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Empty on purpose
            }
        })
    }

    fun onDownloadRegion() {
        // get data from UI
        val regionName = editTextRegionName.text.toString()
        val latitudeNorth = editTextLatNorth.text.toString().toDouble()
        val longitudeEast = editTextLonEast.text.toString().toDouble()
        val latitudeSouth = editTextLatSouth.text.toString().toDouble()
        val longitudeWest = editTextLonWest.text.toString().toDouble()
        val styleUrl = spinnerStyleUrl.selectedItem as String
        val maxZoom = seekbarMaxZoom.progress.toFloat()
        val minZoom = seekbarMinZoom.progress.toFloat()

        if (!validCoordinates(latitudeNorth, longitudeEast, latitudeSouth, longitudeWest)) {
            Toast.makeText(this, "coordinates need to be in valid range", Toast.LENGTH_LONG).show()
            return
        }

        // create offline definition from data
        val definition = OfflineTilePyramidRegionDefinition(
            styleUrl,
            LatLngBounds.Builder()
                .include(LatLng(latitudeNorth, longitudeEast))
                .include(LatLng(latitudeSouth, longitudeWest))
                .build(),
            minZoom.toDouble(),
            maxZoom.toDouble(),
            resources.displayMetrics.density
        )

        // customize notification appearance
        val notificationOptions = NotificationOptions.builder(this)
            .smallIconRes(R.drawable.mapbox_logo_icon)
            .returnActivity(OfflineRegionDetailActivity::class.java.name)
            .build()

        // start offline download
        OfflinePlugin.getInstance(this).startDownload(
            OfflineDownloadOptions.builder()
                .definition(definition)
                .metadata(OfflineUtils.convertRegionName(regionName))
                .notificationOptions(notificationOptions)
                .build()
        )
    }

    private fun validCoordinates(
        latitudeNorth: Double,
        longitudeEast: Double,
        latitudeSouth: Double,
        longitudeWest: Double
    ): Boolean {
        if (latitudeNorth < -90 || latitudeNorth > 90) {
            return false
        } else if (longitudeEast < -180 || longitudeEast > 180) {
            return false
        } else if (latitudeSouth < -90 || latitudeSouth > 90) {
            return false
        } else if (longitudeWest < -180 || longitudeWest > 180) {
            return false
        }
        return true
    }
}