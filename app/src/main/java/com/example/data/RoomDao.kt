package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms ORDER BY createdAt DESC")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id")
    fun getRoomById(id: Int): Flow<RoomEntity?>

    @Query("SELECT * FROM furniture_items WHERE roomId = :roomId")
    fun getFurnitureForRoom(roomId: Int): Flow<List<FurnitureItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFurniture(item: FurnitureItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFurnitureList(items: List<FurnitureItemEntity>)

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Update
    suspend fun updateFurniture(item: FurnitureItemEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("DELETE FROM furniture_items WHERE id = :id")
    suspend fun deleteFurnitureById(id: Int)

    @Query("DELETE FROM furniture_items WHERE roomId = :roomId")
    suspend fun deleteFurnitureByRoomId(roomId: Int)
}
