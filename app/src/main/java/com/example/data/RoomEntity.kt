package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val roomType: String,
    val style: String,
    val budget: String,
    val wallColor: String = "#F0EAE1",
    val lighting: String = "Natural Daylight",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
