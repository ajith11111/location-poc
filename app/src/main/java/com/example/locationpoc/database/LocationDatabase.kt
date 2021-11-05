package com.example.locationpoc.database

import android.content.Context
import androidx.room.*

@Database(version = 1, entities = [LocationEntity::class])
@TypeConverters(LocationTypeConverters::class)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun getLocationEntityDao(): LocationEntityDao

    companion object {
        private var INSTANCE: LocationDatabase? = null

        fun getInstance(context: Context): LocationDatabase? {
            if (INSTANCE == null) {
                synchronized(LocationDatabase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        LocationDatabase::class.java, LocationDatabase::class.java.simpleName+".db")
                        .allowMainThreadQueries()
                        .fallbackToDestructiveMigration()
                        .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }
}