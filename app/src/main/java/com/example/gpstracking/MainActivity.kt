package com.example.gpstracking

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.example.gpstracking.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

//1. main -> service
//2. main -> permission check
//3. service-> onCreate : notification background service, fusedLocationClient, callback
//4. fragment -> Map, Gps 좌표만, 버튼만, save
class MainActivity : AppCompatActivity() {

    // 뷰 바인딩
    private lateinit var binding: ActivityMainBinding

    // 권한 정의
    private val PERMISSION_REQUEST_CODE = 100

    companion object {
        var TAG = "미주바보"
        var changeLocation: Location? = null // 마지막으로 찍히는 위치값을 담을 변수
        var activity: MainActivity? = null

        // fragment 띄우기
        fun showMapFragment() {
            activity?.binding?.mainViewGroup?.visibility = View.GONE
//            activity?.binding?.progressBarText?.text = activity?.getText(R.string.miju_text)
            val fragment: Fragment = FragmentMap()
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.frame_layout, fragment)?.commit()
        }

        const val START_FOREGROUND = "start foreground"
        const val STOP_FOREGROUND = "stop foreground"
    }

    // 서비스 변수
    var gpsTrackingService: GpsTrackingService? = null
    var isBound = false

    // MainActivity가 실행되자마자(앱이 실행되자마자)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
        activity = this // MainActivity를 담아줌

        // 위치 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 거부된 경우
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // 권한 허용 시 Service bind 실행
            val intent = Intent(this, GpsTrackingService::class.java)
            bindService(intent, myConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private val myConnection = object : ServiceConnection {
        // 서비스가 바인딩 되어 연결되면
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as GpsTrackingService.MyLocalBinder
            gpsTrackingService = binder.getService()
            isBound = true
            gpsTrackingService?.requestFusedClient()  // 서비스 안에 있는 requestFusedClient 실행함
        }

        // 서비스가 연결되지 않았을때
        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    // 권한 실행에 관한 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허용 시 Service bind 실행
                    val intent = Intent(this, GpsTrackingService::class.java)
                    bindService(intent, myConnection, Context.BIND_AUTO_CREATE)

                    // 권한이 허용된 경우 fragment 실행
//                    val fragment: Fragment = FragmentMap()
//                    supportFragmentManager
//                        .beginTransaction().replace(R.id.frame_layout, fragment)
//                        .commit()
                } else {
                    // 권한이 거부된 경우 메세지 출력
                    Toast.makeText(this, "위치 권한을 허용해야 합니다.", Toast.LENGTH_SHORT).show()

                    // 권한을 허용하도록 설정으로 유도하기
                }
            }
        }
    }
}