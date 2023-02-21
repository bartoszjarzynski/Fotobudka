package com.example.fotobudka.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fotobudka.db.dao.CameraDao
import com.example.fotobudka.db.entity.Camera

@Database(entities = [Camera::class], version = 2)
abstract class CameraDatabase: RoomDatabase() {

    abstract fun cameraDao(): CameraDao

    companion object {
        @Volatile
        private var INSTANCE: CameraDatabase? = null

        fun getInstance(context: Context): CameraDatabase {
            synchronized(this) {
                return INSTANCE?: Room.databaseBuilder(
                    context.applicationContext,
                    CameraDatabase::class.java,
                    "database"
                ).allowMainThreadQueries().fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
        }
    }
}