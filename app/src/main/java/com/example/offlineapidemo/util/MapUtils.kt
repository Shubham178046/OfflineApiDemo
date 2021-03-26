package com.example.offlineapidemo.util

import android.content.Context
import android.graphics.*
import com.example.offlineapidemo.R
import com.mapbox.mapboxsdk.geometry.LatLng
import kotlin.math.abs
import kotlin.math.atan

object MapUtils {

    fun getCarBitmap(context: Context): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
        return Bitmap.createScaledBitmap(bitmap, 50, 100, false)
    }

    fun getOriginDestinationMarkerBitmap(): Bitmap {
        val height = 20
        val width = 20
        val bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
        canvas.drawRect(0F, 0F, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    fun getRotation(start: LatLng, end: LatLng): Float {
        val latDifference: Double = abs(start.latitude - end.latitude)
        val lngDifference: Double = abs(start.longitude - end.longitude)
        var rotation = -1F
        when {
            start.latitude < end.latitude && start.longitude < end.longitude -> {
                rotation = Math.toDegrees(atan(lngDifference / latDifference)).toFloat()
            }
            start.latitude >= end.latitude && start.longitude < end.longitude -> {
                rotation = (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 90).toFloat()
            }
            start.latitude >= end.latitude && start.longitude >= end.longitude -> {
                rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 180).toFloat()
            }
            start.latitude < end.latitude && start.longitude >= end.longitude -> {
                rotation =
                    (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 270).toFloat()
            }
        }
        return rotation
    }

    /**
     * This function returns the list of locations of Car during the trip i.e. from Origin to Destination
     */
    fun getListOfLocations(): ArrayList<LatLng> {
        val locationList = ArrayList<LatLng>()
        locationList.add(LatLng(22.309499941673824, 73.18797260551804))
        locationList.add(LatLng(22.318086659951703, 73.18755444754866))
        locationList.add(LatLng(22.324258770847287, 73.19051331018937))
        locationList.add(LatLng(22.328784360017046, 73.19686478072173))
        locationList.add(LatLng(22.333944940431735, 73.1943756909185))
        locationList.add(LatLng(22.33696180669229, 73.19257324657825))
        locationList.add(LatLng(22.338807319588838, 73.19262069187847))
        locationList.add(LatLng(22.341094754672675, 73.19276975105983))
        locationList.add(LatLng(22.344800250192385, 73.19075385945777))
        locationList.add(LatLng(22.34607472903016, 73.19139179982498))
        return locationList
    }

}