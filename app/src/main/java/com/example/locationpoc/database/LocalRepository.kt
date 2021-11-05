package com.example.locationpoc.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room

class LocalRepository(context: Context) {

    private val INSTANCE : LocationDatabase = LocationDatabase.getInstance(context)!!

    fun addANewLocation(location: LocationEntity) {
        INSTANCE.getLocationEntityDao().insertAll(location)
    }

    fun getAllLoctions(): LiveData<List<LocationEntity>> {
        return INSTANCE.getLocationEntityDao().getAll()
    }

    fun deleteAllLocations() {
        INSTANCE.getLocationEntityDao().deleteAll()
    }
}