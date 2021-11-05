package com.example.locationpoc.workers

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.work.*
import com.example.locationpoc.Utils.Util
import com.example.locationpoc.database.LocalRepository
import com.example.locationpoc.database.LocationEntity
import java.util.*
import java.util.concurrent.TimeUnit


class LocationWorker(val context: Context, val params: WorkerParameters) :
    CoroutineWorker(context, params) {

    companion object {
        const val TAG = "LocationWorker"
        fun startPeriodicWorkRequest(context: Context) {
            val workReq = PeriodicWorkRequestBuilder<LocationWorker>(16,TimeUnit.MINUTES,10,TimeUnit.MINUTES)
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
        Log.d(TAG,"Started the Worker !")
        var result = Result.failure()
        if (!checkForLocationPermissions()) {
            Log.d(TAG,"No Permissions, So returing failure of the Worker !")
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

        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG,"GPS Provider Not enabled!")
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
                0f,object :LocationListener {
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
            Log.d(TAG,"Location fetch is success !")
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
}