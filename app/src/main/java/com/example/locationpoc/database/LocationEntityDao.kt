package com.example.locationpoc.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LocationEntityDao {

    @Query("SELECT * FROM LOCATION")
    fun getAll(): LiveData<List<LocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg location: LocationEntity)

    @Query("DELETE FROM LOCATION")
    fun deleteAll()
}
