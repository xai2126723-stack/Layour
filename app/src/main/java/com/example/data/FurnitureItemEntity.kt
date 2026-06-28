package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "furniture_items",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["roomId"])]
)
data class FurnitureItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roomId: Int,
    val name: String,
    val category: String,
    val width: Float, // in DP units or normalized scale
    val height: Float, // in DP units or normalized scale
    val x: Float, // position on canvas
    val y: Float, // position on canvas
    val rotation: Float = 0f, // rotation in degrees
    val color: String = "#CCCCCC",
    val status: String = "Keep", // "Keep", "Move", "Remove"
    val isNew: Boolean = false, // Proposed addition
    val price: Double = 0.0,
    val purchaseUrl: String = ""
)
