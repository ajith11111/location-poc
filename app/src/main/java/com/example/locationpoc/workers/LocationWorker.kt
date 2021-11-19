package com.example.locationpoc.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.locationpoc.Utils.Util
import com.example.locationpoc.database.LocalRepository
import com.example.locationpoc.database.LocationEntity
import java.util.*
import java.util.concurrent.TimeUnit


class LocationWorker(val context: Context, val params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val CHANNEL_ID = "1001"

    companion object {
        const val TAG = "LocationWorker"
        fun startPeriodicWorkRequest(context: Context) {
            val workReq = PeriodicWorkRequestBuilder<LocationWorker>(
                16,
                TimeUnit.MINUTES,
                10,
                TimeUnit.MINUTES
            )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                workReq
            )
        }
    }

    private fun checkForLocationPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!Util.hasPermissions(
                    context,
//                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                return false
            }
        } else {
            if (!Util.hasPermissions(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                return false
            }
        }
        return true
    }

    override suspend fun doWork(): Result {
        val localDataSource = LocalRepository(context)
        Log.d(TAG, "Started the Worker !")
        var result = Result.failure()
        setForeground(createForegroundInfo("Progress String"))
        if (!checkForLocationPermissions()) {
            Log.d(TAG, "No Permissions, So returing failure of the Worker !")
            localDataSource.addANewLocation(
                LocationEntity(
                    latitude = -2.0,
                    longitude = -2.0,
                    time = Date(),
                    status = "LocationWorker"
                )
            )
            return result
        }
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Checking for GPS to be enabled

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "GPS Provider Not enabled!")
            localDataSource.addANewLocation(
                LocationEntity(
                    latitude = -5.0,
                    longitude = -5.0,
                    time = Date(),
                    status = "LocationWorker"
                )
            )
            return result
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                0,
                0f, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        localDataSource.addANewLocation(
                            LocationEntity(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                time = Date(location.time),
                                status = "LocationWorker"
                            )
                        )
                    }

                }, Looper.getMainLooper()
            )
            result = Result.success()
            Log.d(TAG, "Location fetch is success !")
        } catch (e: SecurityException) {
            localDataSource.addANewLocation(
                LocationEntity(
                    latitude = -7.0,
                    longitude = -7.0,
                    time = Date(),
                    status = "LocationWorker"
                )
            )
            e.printStackTrace()
        }


        return result
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val id = CHANNEL_ID
        val title = "Location Sync"
        val cancel = "CANCEL DOWNLOAD"
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setOngoing(true)
            .setSmallIcon(android.R.drawable.btn_plus)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(1001, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Location Updates"
            val descriptionText = "Testing the location Updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager =
                applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }
}