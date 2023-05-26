package com.example.gpstracking

import android.util.Log
import com.example.gpstracking.MainActivity.Companion.TAG

/**
 * <p>
 *
 * </p>
 */
enum class AreaCode(var value: String) {
    AREA_DEAGU("대한민국 대구광역시"),
    AREA_BUSAN("대한민국 부산광역시"),
    AREA_INCHON("대한민국 인천광역시"),
    AREA_GWANGJU("대한민국 광주광역시"),
    AREA_DAEJEON("대한민국 대전광역시"),
    AREA_ULSAN("대한민국 울산광역시"),
    AREA_UNKNOWN("대한민국");

    companion object {
        fun getExistAreaCode(containArea: String): AreaCode {
            for (area in values()) {
                if (containArea.contains(area.value)) {
                    return area
                }
            }
            return AREA_UNKNOWN
        }
    }
}