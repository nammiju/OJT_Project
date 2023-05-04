package com.example.gpstracking

import android.location.Location

interface LocationInterface {
    fun onLocationChanged(location: Location?)

    fun getLastKnownLocation(location: Location?)
}