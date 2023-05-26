package com.example.gpstracking

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import com.example.gpstracking.MainActivity.Companion.TAG
import com.example.gpstracking.MainActivity.Companion.gpsData
import com.example.gpstracking.databinding.MapDialogBinding

class MapDialog(context: Context, positiveListener: (MapDialog) -> Unit = {}) : Dialog(context) {
    private val binding by lazy {
        MapDialogBinding.inflate(layoutInflater)
    }
    private var onPositiveButtonListener: ((MapDialog) -> Unit)? = null

    // 인터페이스 연결
    init {
        setContentView(binding.root)
        // 배경 투명 설정
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        onPositiveButtonListener = positiveListener

        binding.startLocationText.text = gpsData.startLocation
        binding.endLocationText.text = gpsData.endLocation

        val totalTime = gpsData.totalTime
        val totalMin = (totalTime / 1000 / 60)
        val totalSec = (totalTime / 1000) % 60

        val a = String.format("%d분 %d초", totalMin, totalSec)
        binding.totalTimeText.text = getContext().resources.getString(R.string.distance_time, totalMin, totalSec)
        binding.totalDistanceText.text = getContext().resources.getString(R.string.distance_meter, gpsData.totalDistance)
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
////        binding = MapDialogBinding.inflate(layoutInflater)
//        setContentView(R.layout.map_dialog)
//
//        // 배경 투명 설정
//        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//    }

    override fun show() {
        binding.closeBtn.setOnClickListener {
            onPositiveButtonListener?.let {
//                binding.startLocationText.text = DialogData().startLocation
                onPositiveButtonListener!!(this)
            }

            this.dismiss()
        }
        super.show()
    }

    //    override fun onClick(view: View?) {
//        when(view?.id){
//            // 확인 버튼이 클릭 되었을 때
//            R.id.closeBtn -> {
//                Log.i(MainActivity.TAG, "다이얼로그 확인버튼 클릭")
//                dialogInterface?.onCloseBtnClicked()
//
//            }
//        }
//    }
}

data class DialogData(
    var startLocation: String = "",
    var endLocation: String = "",
    var totalDistance: Float = 0F,
    var totalTime: Long = 0
)