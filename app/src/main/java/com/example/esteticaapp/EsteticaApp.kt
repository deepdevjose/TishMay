package com.example.esteticaapp

import android.app.Application
import com.cloudinary.android.MediaManager

class EsteticaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val config = mapOf(
            "cloud_name" to "dk6fga5vq",
            "api_key" to "873797274529567",
            "api_secret" to "sttllslm2WJtQPkTsN7Dj1nw-z0"
        )
        MediaManager.init(this, config)
    }
}
