package com.example.gpstracking

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.gpstracking.AreaCode.Companion.getExistAreaCode
import com.example.gpstracking.GpsTrackingService.Companion.pathPoints
import com.example.gpstracking.MainActivity.Companion.TAG
import com.example.gpstracking.MainActivity.Companion.gpsData
import com.example.gpstracking.MyApplication.Companion.sharedPreferences
import com.example.gpstracking.databinding.FragmentMapBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import kotlin.math.round

class FragmentMap : Fragment(), OnMapReadyCallback, OnClickListener {
    // 프레그먼트 뷰바인딩
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var gMap: GoogleMap? = null

    // 폴리라인 변수
    private val lastLocation = mutableListOf<LatLng>()
    var isEndCheck = false
    var isStartCheck = false

    // sharedPreferences
    val gson = GsonBuilder().create()

    var lastLatLng: Location? = null

    // 시간 계산 변수
    var startTime : Long = 0
    var endTime : Long = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
//        val rootView1 = inflater.inflate(R.layout.fragment_map, container, false)
//        mapView = rootView.findViewById(R.id.google_map) as MapView

        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val rootView = binding.root

        mapView = binding.googleMap
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // 버튼 클릭 이벤트
//        val startButton: Button = rootView.findViewById(R.id.startButton)
//        val endButton = rootView.findViewById(R.id.endButton) as Button
//        val historyButton = rootView.findViewById(R.id.historyButton) as Button
        binding.startButton.setOnClickListener(this)
        binding.endButton.setOnClickListener(this)
        binding.historyButton.setOnClickListener(this)

        // 좌표가 바뀔때마다 인지하는 옵저버
        pathPoints.observe(viewLifecycleOwner) {
            lastLatLng = it
            Log.i(TAG, "pathPoints ${it.latitude}, ${it.longitude}, $isEndCheck")
            gMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))

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
            val cameraPosition = CameraPosition.builder().target(point).zoom(20f).build()
            gMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
//            makeMarker(point, "기본 위치", 240.0F)
            try {
                gMap?.isMyLocationEnabled = true // 파랑이
            } catch (e: SecurityException) {
            }
//            // 실시간 위치 업데이트
//            if (point.latitude != it.latitude && point.longitude != it.longitude) {
//                val currentLatLng = LatLng(it.latitude, it.longitude)
//                gMap.clear()
//                val currentCameraPosition = CameraPosition.builder().target(currentLatLng).zoom(20f).build()
//                gMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentCameraPosition))
//            }
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
        _binding = null
    }

    private fun getGeoAddress(fromLatLng: LatLng): String {
        // 좌표로 주소 알아내기
        val geocoder = Geocoder(requireContext())
        val geoLocation = geocoder.getFromLocation(fromLatLng.latitude, fromLatLng.longitude, 1)
        val locationAddress = geoLocation?.get(0)?.getAddressLine(0).toString()
        Log.i(TAG, "좌표 주소1 : ${geoLocation}")
        Log.i(TAG, "좌표 주소1 : ${geoLocation?.get(0)?.adminArea.toString()}"+" "+ geoLocation?.get(0)?.thoroughfare.toString() + " " + geoLocation?.get(0)?.featureName.toString())
        Log.i(TAG, "좌표 주소2 : $locationAddress")

        return locationAddress
    }

    private fun setLastLocationMarker(markerTitle: String, markerColor: Float): LatLng {
        var listLatLng = LatLng(lastLatLng?.latitude ?: 0.0, lastLatLng?.longitude ?: 0.0)
        if (lastLocation.isNotEmpty()) {
            listLatLng = lastLocation.last()
            when (markerTitle) {
                "시작 위치" -> {
                    lastLocation.clear() // 시작 버튼을 누르면 계속해서 위치값을 담던 lastLocation 배열의 데이터가 초기화가 된다
                }
            }
            makeMarker(listLatLng, markerTitle, markerColor)
        }

        return listLatLng
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.startButton -> {
                // 시작 시작 측정
                startTime = System.currentTimeMillis()

                isEndCheck = false
                isStartCheck = true

                gMap?.clear()
                val startLatLng = setLastLocationMarker("시작 위치", 240.0F)


//                // 좌표로 주소 알아내기
                val geocoderAddress = getGeoAddress(startLatLng)
                binding.startLocationText.text = geocoderAddress
                binding.endLocationText.text = "종료 주소"
                // 데이타 클래스에 값 담기
//                gpsData = gpsData.copy(startLocation = startLocation?.get(0)?.adminArea.toString() +" "+ startLocation?.get(0)?.thoroughfare.toString() + " " + startLocation?.get(0)?.featureName.toString())
                val areaCode = getExistAreaCode(geocoderAddress)
                val areaAddress = geocoderAddress.replace(areaCode.value, "")

                gpsData = gpsData.copy(startLocation = areaAddress)
            }

            R.id.endButton -> {
                // 종료 시작 측정
                endTime = System.currentTimeMillis()

                if (lastLocation.size > 0) {
                    isEndCheck = true
                    isStartCheck = false

                    val lastLatLng = setLastLocationMarker("마지막 위치", 0.0F)

                    // sharedPreferences에 좌표값 저장
                    val json = gson.toJson(lastLocation)
                    sharedPreferences.edit().putString("lastLocationList", json).apply()
                    Log.i(TAG, "json형태 리스트 : $json")

                    // 좌표로 주소 알아내기
                    val geocoderAddress = getGeoAddress(lastLatLng)
                    binding.endLocationText.text = geocoderAddress

                    Log.i(TAG, "데이터클래스 :  ${gpsData.toString()}")

                    var totalDist = 0F
                    for (i in 0 until lastLocation.size - 1) {
                        val aa = calculateDistance(lastLocation, i)

                        totalDist += aa//calculateDistance(lastLocation, i)

                        Log.i(TAG, "미주 test : $totalDist, aa : $aa")
                    }

                    Log.i(TAG, "총 거리 : $totalDist")

                    val totalTime = endTime - startTime
                    Log.i(TAG, "총 시간 : $totalTime")

                    val areaCode = getExistAreaCode(geocoderAddress)
                    val areaAddress = geocoderAddress.replace(areaCode.value, "")
                    Log.i(TAG, "총 거리 = $totalDist")

                    gpsData = gpsData.copy(endLocation = areaAddress, totalDistance = totalDist, totalTime = totalTime)

                    // 다이얼로그 띄우기
                    val mapDialog = MapDialog(requireContext())
                    mapDialog.show()

                }
                Log.i(TAG, "end button : $isEndCheck, lastLocation size : ${lastLocation.size}")
            }

            R.id.historyButton -> {
                gMap?.clear() // 지도 화면 클리어

                val json = sharedPreferences.getString("lastLocationList", null)
                var latLng: LatLng
                var title: String

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

                        when (index) {
                            1 -> {
                                latLng = LatLng(latitude, longitude)
                                title = "시작 위치"
                                makeMarker(latLng, title, 240.0F)
                            }

                            jsonArray.length() - 1 -> {
                                latLng = LatLng(latitude, longitude)
                                title = "마지막 위치"
                                makeMarker(latLng, title,0.0F)

                            }
                        }
                    }
                    gMap?.addPolyline(polylineOptions)
                }
            }
        }
    }

    // 마커 생성
    private fun makeMarker(latLng: LatLng, title: String, markerColor: Float) {
        val markerOptions = MarkerOptions().position(latLng).title(title).alpha(0.7f).icon(
            BitmapDescriptorFactory.defaultMarker(markerColor))
        val cameraPosition = CameraPosition.builder().target(latLng).zoom(20f).build()

        gMap?.addMarker(markerOptions)
        gMap?.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    // 폴리라인 그리기
    private fun drawPolyLine() {
        CoroutineScope(Dispatchers.Main).launch {
            val polylineOptions = PolylineOptions()
            polylineOptions.color(Color.RED)
            polylineOptions.width(7f)
            lastLocation.forEach { location ->
                polylineOptions.add(location)
                gMap?.addPolyline(polylineOptions)
            }
        }
    }

    // 두 좌표간 거리 계산
    private fun calculateDistance(listLatLng: MutableList<LatLng>, index: Int): Float {
        val myLoc = Location(LocationManager.NETWORK_PROVIDER)
        val targetLoc = Location(LocationManager.NETWORK_PROVIDER)

        myLoc.latitude= listLatLng[index].latitude
        myLoc.longitude = listLatLng[index].longitude

        targetLoc.latitude= listLatLng[index+1].latitude
        targetLoc.longitude = listLatLng[index+1].longitude

        Log.i(TAG, "좌표간 거리 = ${myLoc.distanceTo(targetLoc)}")
        return myLoc.distanceTo(targetLoc)
    }
}

