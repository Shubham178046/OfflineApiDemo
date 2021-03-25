package com.example.offlineapidemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import kotlinx.android.synthetic.main.activity_offline_map.*

class OfflineMap : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_map)
        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(this, OfflineMap::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.button2).setOnClickListener {
            val intent = Intent(this, OfflineManagerActivity::class.java)
            startActivity(intent)
        }
    }
}