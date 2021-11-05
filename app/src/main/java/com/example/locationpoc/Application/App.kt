package com.example.locationpoc.Application

import android.app.Application
import android.content.res.Configuration

class App: Application() {

    companion object {
        private var instance:App? = null
        fun get():App? {
            return instance
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        scheduleTasks()
    }

    private fun scheduleTasks() {
        // Start a work req using work manager
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}