package com.example.gpstracking

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.gpstracking.MainActivity.Companion.showMapFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.properties.Delegates

class GpsTrackingService : Service() {
    // gps를 사용해서 위치를 확인하고 현재 위치를 담아올 변수
    lateinit var fusedLocationClient: FusedLocationProviderClient

    // 실시간 위치 정보 받기
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
        setMinUpdateDistanceMeters(Float.MIN_VALUE) // 최소값 넣어주기
        setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
        setWaitForAccurateLocation(true)
    }.build()

    // 위치 정보가 업데이트 될 때 마다 실행되는 콜백 함수
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            lastLocation?.let {
                // 1초마다 갱신되는 좌표값으로 마커 새로 생성하기

                lastLatLng = lastLocation
                pathPoints.postValue(lastLocation) //postValue: 백그라운드에서도 계속 값의 변화를 인지함
                if (lastLocation.latitude != 0.0 && lastLocation.longitude != 0.0) {
                    initiateLocationRequest = true
                }
                Log.i("미주바보", "change : ${lastLatLng?.latitude}, ${lastLatLng?.longitude}")
            }
        }
    }

    var initiateLocationRequest: Boolean by printDelegate(false)

    // delegate: 실시간으로 갱신되는 값만 기억하는 livedata와 달리 직전값과 바뀐값을 기억함
    private fun printDelegate(init: Boolean) =
        Delegates.observable(init) { _, old, new ->
            if (old != new) { // 계속 false이다가 시작버튼이 눌리면 true바뀌면 실행
                when (new) {
                    true -> { // 바뀐값이 true 이면
                        showMapFragment() // MainActivity의 companion object에 있는 프래그먼트 함수 실행함.
                    }
                    false -> {
                        Log.i(MainActivity.TAG, "not initiate!! Location is Null")
                    }
                }
            }
        }

    companion object {
        var lastLatLng: Location? = null

        val pathPoints = MutableLiveData<Location>() // 값이 변화할 때마다 인식할 수 있는 LiveData 사용

    }

    private val myBinder = MyLocalBinder()

    inner class MyLocalBinder : Binder() {
        fun getService() : GpsTrackingService {
            return this@GpsTrackingService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return myBinder
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("미주바보", "onCreate")
    }

    fun requestFusedClient() {
        try {
            Log.i("미주바보", "requestFusedClient")
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
        } catch (e: SecurityException) {

        }
    }
}