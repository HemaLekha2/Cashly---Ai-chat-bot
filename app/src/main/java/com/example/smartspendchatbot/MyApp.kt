package com.example.smartspendchatbot

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application(){
    override fun onCreate() {
        super.onCreate()
        // Initialize your app here if needed
    }
}
