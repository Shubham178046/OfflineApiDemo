package com.example.offlineapidemo.offline

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.offlineapidemo.R
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.offline.OfflineRegionSelector
import com.mapbox.mapboxsdk.plugins.offline.model.NotificationOptions
import com.mapbox.mapboxsdk.plugins.offline.model.RegionSelectionOptions
import com.mapbox.mapboxsdk.plugins.offline.offline.OfflinePlugin
import kotlinx.android.synthetic.main.activity_offline_ui_components.*
import java.util.*

class OfflineUiComponentsActivity : AppCompatActivity() {
    private var permissionsManager: PermissionsManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_ui_components)
        fabRegionSelector.setOnClickListener {
            onOfflineRegionSelectorButtonClicked()
        }
    }

    fun onOfflineRegionSelectorButtonClicked() {
        // Create the offline region selector options
        val options = RegionSelectionOptions.builder()
            .statingCameraPosition(
                CameraPosition.Builder().target(LatLng(22.310276479282578, 73.17538089479986))
                    .zoom(12.0).build()
            ).build()

        val intent = OfflineRegionSelector.IntentBuilder()
            .regionSelectionOptions(options)
            .build(this)
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {
            val builder = NotificationOptions.builder(this)
                .contentTitle("Downloading: ")
                .returnActivity(OfflineUiComponentsActivity::class.java.name)
                .smallIconRes(android.R.drawable.stat_sys_download)

            if (data?.let { OfflineRegionSelector.getRegionName(it) } != null) {
                builder.contentText(OfflineRegionSelector.getRegionName(data))
            }

            val options = OfflineRegionSelector.getOfflineDownloadOptions(data, builder.build())
            OfflinePlugin.getInstance(this).startDownload(options)

            Toast.makeText(this, String.format(Locale.US, "Region name: %s", data?.let {
                OfflineRegionSelector.getRegionName(
                    it
                )
            } ?: "null"), Toast.LENGTH_LONG).show()
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "user canceled out of region selector", Toast.LENGTH_LONG).show()
        }
    }

    companion object {

        private val REQUEST_CODE = 9384
    }

}