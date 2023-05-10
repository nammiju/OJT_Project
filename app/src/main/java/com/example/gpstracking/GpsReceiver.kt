package com.example.gpstracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import com.example.gpstracking.MainActivity.Companion.TAG

class GpsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        when (intent?.action) {
            "android.location.PROVIDERS_CHANGED" -> {
                val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val gpsState = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    Log.i(TAG, "gpsState : $gpsState")
                if (!gpsState) {
// GPS가 꺼져있는 경우 설정 화면으로 이동
                    val settings = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settings)
                } else {
                    Log.i(TAG, "GPS true")
                }
            }
            else -> {

            }
        }
    }
}