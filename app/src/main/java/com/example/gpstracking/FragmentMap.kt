package com.example.gpstracking

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.example.gpstracking.GpsTrackingService.Companion.lastLatLng
import com.example.gpstracking.GpsTrackingService.Companion.pathPoints
import com.example.gpstracking.MainActivity.Companion.TAG
import com.example.gpstracking.MainActivity.Companion.changeLocation
import com.example.gpstracking.databinding.ActivityMainBinding
import com.example.gpstracking.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentMap : Fragment(), OnMapReadyCallback, OnClickListener {

    private lateinit var mapView: MapView
    private lateinit var gMap: GoogleMap

    // 위치 값 요청에 대한 갱신 정보를 받는 변수
    lateinit var locationCallback: LocationCallback

    // 좌표 담는 핸들러
    val addHandler by lazy { Handler(Looper.getMainLooper()) }

    // 폴리라인 변수
    val lastLocation = mutableListOf<LatLng>()

    var isEndCheck = false
    var isStartCheck = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = rootView.findViewById(R.id.google_map) as MapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // 버튼 클릭 이벤트
        val startButton: Button = rootView.findViewById(R.id.startButton)
        val endButton = rootView.findViewById(R.id.endButton) as Button
        startButton.setOnClickListener(this)
        endButton.setOnClickListener(this)

        // 좌표가 바뀔때마다 인지하는 옵저버
        pathPoints.observe(viewLifecycleOwner) {
            Log.i(TAG, "pathPoints ${it.latitude}, ${it.longitude}, $isEndCheck")
            if (!isEndCheck) {
                lastLocation.add(
                    LatLng(it.latitude, it.longitude)
                )
            }
            if (isStartCheck) {
                drawPolyLine()
            }
        }

        return rootView
    }

    // 지도 객체를 사용할 수 있을 때 자동으로 호출되는 함수
    override fun onMapReady(map: GoogleMap) {
        Log.i(TAG, "onMapReady 실행됨.")
        gMap = map
        Log.i(TAG, "onMapReady 실행됨.1 ${lastLatLng?.latitude}, ${lastLatLng?.longitude}")
        lastLatLng?.let {
            Log.i(TAG, "onMapReady ${it.latitude}, ${it.longitude}")
            val point = LatLng(it.latitude, it.longitude)
            map.addMarker(
                MarkerOptions().position(point).title("기본 위치")
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 19f))

            // 실시간 위치 업데이트
            if (point.latitude != it.latitude && point.longitude != it.longitude) {
                val LATLNG = LatLng(it.latitude, it.longitude)

                val markerOptions = MarkerOptions().position(LATLNG).title("현재 위치")
                val cameraPosition = CameraPosition.builder().target(LATLNG).zoom(20f).build()

                gMap.clear()
                gMap.addMarker(markerOptions)
                gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        } ?: run {

        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    @SuppressLint("MissingPermission")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.startButton -> {
                isEndCheck = false
                isStartCheck = true
                val size = lastLocation.size - 1
                val LATLNG = LatLng(lastLocation[size].latitude, lastLocation[size].longitude)

                lastLocation.clear() // 시작 버튼을 누르면 계속해서 위치값을 담던 lastLocation 배열의 데이터가 초기화가 된다

                val markerOptions = MarkerOptions().position(LATLNG).title("시작 위치")
                val cameraPosition = CameraPosition.builder().target(LATLNG).zoom(20f).build()

                gMap.clear()
                gMap.addMarker(markerOptions)
                gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }

            R.id.endButton -> {
                isEndCheck = true
                isStartCheck = false

                if (lastLocation.size > 0) {
                    val size = lastLocation.size - 1
                    val LATLNG = LatLng(
                        lastLocation[size].latitude,
                        lastLocation[size].longitude
                    )

                    val markerOptions = MarkerOptions().position(LATLNG).title("마지막 위치")
                    val cameraPosition = CameraPosition.builder().target(LATLNG).zoom(20f).build()

                    gMap.addMarker(markerOptions)
                    gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
                Log.i(TAG, "end button : $isEndCheck")
            }
        }
    }

    // 폴리라인 그리기
    fun drawPolyLine() {
        CoroutineScope(Dispatchers.Main).launch {
            val polylineOptions = PolylineOptions()
            polylineOptions.color(Color.RED)
            polylineOptions.width(7f)
            lastLocation.forEach { location ->
                polylineOptions.add(location)
                gMap.addPolyline(polylineOptions)
            }
        }
    }

    // 폴리라인 테스트하기
    fun testPolyline() {
//        changeLocation
        val test1 = LatLng(35.866202, 128.592326)
        val test2 = LatLng(35.866353, 128.592330)
        val test3 = LatLng(35.866494, 128.592336)
        val test4 = LatLng(35.866572, 128.592261)
        val test5 = LatLng(35.866573, 128.592150)
        val test6 = LatLng(35.866576, 128.592048)
        val test7 = LatLng(35.866580, 128.591951)
        val test8 = LatLng(35.866579, 128.591844)
        val test9 = LatLng(35.866667, 128.591807)
        val test10 = LatLng(35.866780, 128.591799)

        val locations: MutableList<LatLng> = mutableListOf<LatLng>(
            test1, test2, test3, test4, test5, test6, test7, test8, test9, test10
        )


        CoroutineScope(Dispatchers.Main).launch {
            val polylineOptions = PolylineOptions()
            polylineOptions.color(Color.RED)
            polylineOptions.width(5f)
            locations.forEach { location ->
                polylineOptions.add(location)
                gMap.addPolyline(polylineOptions)
                delay(1000)
            }
        }
    }
}

