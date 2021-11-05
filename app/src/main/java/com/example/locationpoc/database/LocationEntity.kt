package com.example.locationpoc.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import kotlin.random.Random

@Entity(tableName = "LOCATION")
data class LocationEntity(
    @PrimaryKey
    @ColumnInfo(name = "time") val time: Date = Date(),
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "lat") val latitude: Double,
    @ColumnInfo(name = "long") val longitude: Double
)