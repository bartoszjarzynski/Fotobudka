package com.example.fotobudka.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "camera_table")
data class Camera(
    @PrimaryKey
    val amount: Int,
    val duration: Int,
    val before: Int
)