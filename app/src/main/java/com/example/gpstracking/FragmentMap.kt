package com.example.gpstracking

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ToggleButton
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.example.gpstracking.GpsTrackingService.Companion.lastLatLng
import com.example.gpstracking.GpsTrackingService.Companion.pathPoints
import com.example.gpstracking.MainActivity.Companion.TAG
import com.example.gpstracking.MyApplication.Companion.sharedPreferences
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class FragmentMap : Fragment(), OnMapReadyCallback, OnClickListener,
    CompoundButton.OnCheckedChangeListener {

    private lateinit var mapView: MapView
    private lateinit var gMap: GoogleMap

    // 위치 값 요청에 대한 갱신 정보를 받는 변수
    lateinit var locationCallback: LocationCallback

    // 좌표 담는 핸들러
    val addHandler by lazy { Handler(Looper.getMainLooper()) }

    // 마커 변수
    var markerLatLng = LatLng(0.00,0.00)
    var markerTitle = ""

    // 폴리라인 변수
    val lastLocation = mutableListOf<LatLng>()
    var isEndCheck = false
    var isStartCheck = false

    //수신자 변수 선언
    var receiver: BroadcastReceiver? = null

    //gps 온오프
    var locationManager: LocationManager? = null
    var GpsStatus: Boolean = true

    // sharedPreferences
    val gson = GsonBuilder().create()

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
        val historyButton = rootView.findViewById(R.id.historyButton) as Button
        startButton.setOnClickListener(this)
        endButton.setOnClickListener(this)
        historyButton.setOnClickListener(this)

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
            makeMarker(point, "기본 위치")


            // 실시간 위치 업데이트
            if (point.latitude != it.latitude && point.longitude != it.longitude) {
                val LATLNG = LatLng(it.latitude, it.longitude)
                gMap.clear()
                makeMarker(LATLNG, "현재 위치")
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
                gMap.clear()
                makeMarker(LATLNG, "시작 위치")
                lastLocation.clear() // 시작 버튼을 누르면 계속해서 위치값을 담던 lastLocation 배열의 데이터가 초기화가 된다
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
                    makeMarker(LATLNG, "마지막 위치")

                    // sharedPreferences에 좌표값 저장
                    val json = gson.toJson(lastLocation)
                    sharedPreferences.edit().putString("lastLocationList", json).apply()
                    Log.i(TAG, "json형태 리스트 : $json")

                }
                Log.i(TAG, "end button : $isEndCheck")
            }

            R.id.historyButton -> {
                gMap.clear() // 지도 화면 클리어

                val json = sharedPreferences.getString("lastLocationList", null)
                var latLng = LatLng(0.00, 0.00)
                var title = ""

                json?.let {
                    // 폴리라인 생성
                    val polylineOptions = PolylineOptions()
                    polylineOptions.color(Color.RED)
                    polylineOptions.width(5f)

                    val jsonArray = JSONArray(json)
                    for (index in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(index)

                        val latitude = jsonObject.getString("latitude").toDouble()
                        val longitude = jsonObject.getString("longitude").toDouble()

                        polylineOptions.add(LatLng(latitude, longitude))
                        Log.i(TAG, "히스토리 버튼 폴리라인 좌표 : $latitude, test1 : $longitude")

                        if(index == 1 ){
                            latLng = LatLng(latitude, longitude)
                            title = "시작 위치"
                            makeMarker(latLng, title)
                        }
                        if(index == jsonArray.length()-1){
                            latLng = LatLng(latitude, longitude)
                            title = "마지막 위치"
                            makeMarker(latLng, title)
                        }
                    }
                    gMap.addPolyline(polylineOptions)
                }
            }
        }
    }

    // 마커 생성
    fun makeMarker (latLng: LatLng, title:String){
        val markerOptions = MarkerOptions().position(latLng).title(title)
        val cameraPosition = CameraPosition.builder().target(latLng).zoom(20f).build()

        gMap.addMarker(markerOptions)
        gMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
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

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (isChecked) {
            Log.i(TAG, "지피에스 온 누름 ${isChecked}")
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // GPS가 꺼져있는 경우 설정 화면으로 이동
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            Log.i(TAG, "지피에스 오프 누름 ${isChecked}")
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                // GPS가 켜져 있는 경우 설정 화면으로 이동
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
    }
}

