package com.example.locationpoc

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.locationpoc.Utils.Util
import com.example.locationpoc.database.LocalRepository
import com.example.locationpoc.database.LocationEntity
import com.example.locationpoc.databinding.ActivityMainBinding
import com.example.locationpoc.receivers.LocationUpdatesBroadcastReceiver
import com.example.locationpoc.workers.LocationWorker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.simpleName
    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: LocalRepository
    private var size:Int = 1
    private var listAdapter : CustomRecyclerViewAdapter? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, LocationUpdatesBroadcastReceiver::class.java)
        intent.action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        repo = LocalRepository(applicationContext)
        Log.d("PERM", "Starting to ask Permissions !")
        initViews()
        initRecyclerView()
        initListeners()
        LocationWorker.startPeriodicWorkRequest(applicationContext)
//        registerListenerForLocationUpdates()
        checkAndAskForLocationPermissions()

    }

    private fun registerListenerForLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()")

        if (!Util.hasPermissions(this,Manifest.permission.ACCESS_FINE_LOCATION)) return

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {

            fusedLocationClient.requestLocationUpdates(locationRequest, locationUpdatePendingIntent)
        } catch (permissionRevoked: SecurityException) {
            // Exception only occurs if the user revokes the FINE location permission before
            // requestLocationUpdates() is finished executing (very rare).
            Log.d(TAG, "Location permission revoked; details: $permissionRevoked")
            permissionRevoked.printStackTrace()
        }
    }

    private fun checkForLocationPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!Util.hasPermissions(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                return false
            }
        } else {
            if (!Util.hasPermissions(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                return false
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        initViews()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1001) {
            if(grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_DENIED) {

                Snackbar.make(
                    binding.root,
                    "Permission is denied, Please enable in App Settings !",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("GRANT \nNOW") {
                        // Build intent that displays the App settings screen.
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "package",
                            BuildConfig.APPLICATION_ID,
                            null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }.show()
            }
        }
    }

    private fun initRecyclerView() {
        listAdapter = CustomRecyclerViewAdapter(this)
        binding.rvLocations.apply {
            setHasFixedSize(true)
            adapter = listAdapter
            layoutManager = LinearLayoutManager(context)
            isNestedScrollingEnabled = false
        }
    }

    private fun initListeners() {
        repo.getAllLoctions().observe(this, {
            listAdapter?.setData(it)
        })
    }

    private fun initViews() {
        binding.apply {
            btnAddLocation.setOnClickListener {
                repo.addANewLocation(
                    LocationEntity(
                        latitude = size * 7.0,
                        longitude = size * 7.0,
                        status =  "Dummy"
                    )
                )
                size++
            }
            btnDeleteAll.setOnClickListener{
                repo.deleteAllLocations()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !checkForLocationPermissions()) {
                btnEnableBl.visibility = View.VISIBLE
                btnEnableBl.setOnClickListener {
                    continueToAskBackgroundLocPermission()
                }
            }
            if(!checkPowerOptimizationEnabled()) {
                btnPowerOpt.visibility = View.VISIBLE
                btnPowerOpt.setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent()
                        val packageName = packageName
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun checkPowerOptimizationEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }

    private fun checkAndAskForLocationPermissions() {
        if (!Util.hasPermissions(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {

            Dexter.withContext(this).withPermissions(
                getPermissionsList()
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    if (report?.isAnyPermissionPermanentlyDenied == true) {
                        Toast.makeText(
                            applicationContext,
                            "Please Grant the Required Permissions !",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (report?.areAllPermissionsGranted() == true) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                            continueToAskBackgroundLocPermission()
                        registerListenerForLocationUpdates()
                    }

                }

                override fun onPermissionRationaleShouldBeShown(
                    list: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }

            }).check()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun continueToAskBackgroundLocPermission() {
//        AlertDialog.Builder(this)
//            .setTitle("Background Location Permission Required !")
//            .setMessage("Please Grant the background location permissions")
//            .setPositiveButton(
//                "ALLOW"
//            ) { dialog, which ->
//                run {
//                    requestPermissions(
//                        listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION).toTypedArray(),
//                        1001
//                    )
//                }
//            }
//            .setNegativeButton("NO") { dialog, which ->
//                run {
//                    dialog.dismiss()
//                }
//            }
//            .setCancelable(false)
//            .create()
//            .show()
        Snackbar.make(
            binding.root,
            "Please Grant the background location permissions",
            Snackbar.LENGTH_LONG
        )
            .setAction("GRANT \n NOW") {
                requestPermissions(
                    listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION).toTypedArray(),
                    1001
                )
            }.show()

    }

    private fun getPermissionsList(): List<String> {
        return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }
}