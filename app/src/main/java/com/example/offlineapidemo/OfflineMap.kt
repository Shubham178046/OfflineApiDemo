package com.example.offlineapidemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.offlineapidemo.offline.OfflineDownloadActivity
import com.example.offlineapidemo.offline.OfflineRegionListActivity
import com.example.offlineapidemo.offline.OfflineUiComponentsActivity
import kotlinx.android.synthetic.main.activity_offline_map.*

class OfflineMap : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_map)
        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(this, Map::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.button2).setOnClickListener {
            val intent = Intent(this, OfflineManagerActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.buttonUIComponent).setOnClickListener {
            val intent = Intent(this, OfflineUiComponentsActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.buttonListRegion).setOnClickListener {
            val intent = Intent(this, OfflineRegionListActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.buttonCreateRegion).setOnClickListener {
            val intent = Intent(this, OfflineDownloadActivity::class.java)
            startActivity(intent)
        }
    }
}