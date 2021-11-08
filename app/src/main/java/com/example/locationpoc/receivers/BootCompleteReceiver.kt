package com.example.locationpoc.receivers

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.locationpoc.Application.App
import com.example.locationpoc.Utils.Util
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit

class BootCompleteReceiver : BroadcastReceiver() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(App.get(), LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(App.get(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    private val locationRequest: LocationRequest = LocationRequest().apply {
        interval = TimeUnit.SECONDS.toMillis(150)

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        fastestInterval = TimeUnit.SECONDS.toMillis(30)

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        maxWaitTime = TimeUnit.MINUTES.toMillis(3)

        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        startReceivingLocationUpdates()
    }

    private fun startReceivingLocationUpdates() {

        if (!Util.hasPermissions(App.get(), Manifest.permission.ACCESS_FINE_LOCATION)) return

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(App.get() as Context)

        try {

            fusedLocationClient.requestLocationUpdates(locationRequest, locationUpdatePendingIntent)
        } catch (permissionRevoked: SecurityException) {
            // Exception only occurs if the user revokes the FINE location permission before
            // requestLocationUpdates() is finished executing (very rare).
            Log.d("BootCompleteReceiver", "Location permission revoked; details: $permissionRevoked")
            permissionRevoked.printStackTrace()
        }


    }
}