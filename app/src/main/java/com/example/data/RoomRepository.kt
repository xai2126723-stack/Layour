package com.example.data

import kotlinx.coroutines.flow.Flow

class RoomRepository(private val roomDao: RoomDao) {
    val allRooms: Flow<List<RoomEntity>> = roomDao.getAllRooms()

    fun getRoomById(id: Int): Flow<RoomEntity?> = roomDao.getRoomById(id)

    fun getFurnitureForRoom(roomId: Int): Flow<List<FurnitureItemEntity>> = 
        roomDao.getFurnitureForRoom(roomId)

    suspend fun insertRoom(room: RoomEntity): Long = roomDao.insertRoom(room)

    suspend fun insertFurniture(item: FurnitureItemEntity): Long = roomDao.insertFurniture(item)

    suspend fun insertFurnitureList(items: List<FurnitureItemEntity>) = 
        roomDao.insertFurnitureList(items)

    suspend fun updateRoom(room: RoomEntity) = roomDao.updateRoom(room)

    suspend fun updateFurniture(item: FurnitureItemEntity) = roomDao.updateFurniture(item)

    suspend fun deleteRoom(room: RoomEntity) = roomDao.deleteRoom(room)

    suspend fun deleteFurnitureById(id: Int) = roomDao.deleteFurnitureById(id)

    suspend fun deleteFurnitureByRoomId(roomId: Int) = roomDao.deleteFurnitureByRoomId(roomId)
}
