package com.android.cast.dlna.demo.renderer

import android.app.Application

class RendererApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // jUPnP logs are routed to Android logcat via slf4j-android (no manual config needed)
        com.android.cast.dlna.core.Logger.printThread = true
        com.android.cast.dlna.core.Logger.enabled = true
        com.android.cast.dlna.core.Logger.level = com.android.cast.dlna.core.Level.D
        com.android.cast.dlna.core.Logger.create("RendererApplication").i("Application onCreate.")
    }
}