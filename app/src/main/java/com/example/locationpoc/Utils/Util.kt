package com.example.locationpoc.Utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object Util {

    fun hasPermissions(context: Context?, vararg permissions: String?): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context!!,
                    permission!!
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

}