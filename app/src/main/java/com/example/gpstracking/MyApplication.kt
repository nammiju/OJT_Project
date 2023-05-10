package com.example.gpstracking

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.common.util.SharedPreferencesUtils
import java.util.prefs.AbstractPreferences
import java.util.prefs.Preferences

class MyApplication : Application() {
    companion object {
        private const val pref_name = "locationHistory"
        lateinit var sharedPreferences: SharedPreferences
    }

    override fun onCreate() {
        // sharedPreferences 초기화
        sharedPreferences = getSharedPreferences(pref_name, Context.MODE_PRIVATE)
        super.onCreate()
    }

}