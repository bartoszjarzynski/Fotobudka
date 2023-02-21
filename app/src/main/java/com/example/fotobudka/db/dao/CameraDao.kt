package com.example.fotobudka.db.dao

import androidx.room.*
import com.example.fotobudka.db.entity.Camera

@Dao
interface CameraDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addSettings(settings: Camera)

    @Query("UPDATE camera_table SET amount = :amount, duration = :duration, before = :before")
    fun updateSettings(amount: Int, duration: Int, before: Int)

    @Query("SELECT * FROM camera_table ORDER BY amount ASC")
    fun readAllSettings(): Camera

    @Query("SELECT COUNT(*) FROM camera_table")
    fun ifSettings(): Int
}