package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeForgeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "HomeForgeViewModel"
    private val repository: RoomRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RoomRepository(database.roomDao())
    }

    // List of all saved rooms/layouts in the DB
    val allRooms: StateFlow<List<RoomEntity>> = repository.allRooms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current inputs for a new design run
    private val _roomTypeInput = MutableStateFlow("Living Room")
    val roomTypeInput: StateFlow<String> = _roomTypeInput.asStateFlow()

    private val _styleInput = MutableStateFlow("Japandi")
    val styleInput: StateFlow<String> = _styleInput.asStateFlow()

    private val _budgetInput = MutableStateFlow("Medium")
    val budgetInput: StateFlow<String> = _budgetInput.asStateFlow()

    private val _selectedImage = MutableStateFlow<Bitmap?>(null)
    val selectedImage: StateFlow<Bitmap?> = _selectedImage.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // Currently active room/layout selected for the 2D editor/planner
    private val _activeRoomId = MutableStateFlow<Int?>(null)
    val activeRoomId: StateFlow<Int?> = _activeRoomId.asStateFlow()

    // Observe active room entity dynamically
    val activeRoom: StateFlow<RoomEntity?> = _activeRoomId
        .flatMapLatest { id ->
            if (id != null) repository.getRoomById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Observe active room's furniture items
    val activeRoomFurniture: StateFlow<List<FurnitureItemEntity>> = _activeRoomId
        .flatMapLatest { id ->
            if (id != null) repository.getFurnitureForRoom(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Setters
    fun setRoomType(type: String) {
        _roomTypeInput.value = type
    }

    fun setStyle(style: String) {
        _styleInput.value = style
    }

    fun setBudget(budget: String) {
        _budgetInput.value = budget
    }

    fun setSelectedImage(bitmap: Bitmap?) {
        _selectedImage.value = bitmap
        _analysisError.value = null
    }

    fun setActiveRoomId(id: Int?) {
        _activeRoomId.value = id
    }

    // Trigger AI space analysis & layout generation
    fun analyzeAndGenerateLayout(onSuccess: (roomId: Int) -> Unit) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisError.value = null
            try {
                val image = _selectedImage.value
                val style = _styleInput.value
                val budget = _budgetInput.value
                val roomType = _roomTypeInput.value

                Log.d(TAG, "Starting space analysis with style=$style, budget=$budget, roomType=$roomType")
                
                // Call Gemini REST API
                val result = GeminiService.analyzeRoomAndGenerateLayout(image, style, budget, roomType)

                // Save results in database
                val roomEntity = RoomEntity(
                    name = "${result.roomType} - $style Design",
                    roomType = result.roomType,
                    style = style,
                    budget = budget,
                    wallColor = result.wallColor,
                    lighting = result.lighting,
                    notes = result.notes
                )
                
                val newRoomId = repository.insertRoom(roomEntity).toInt()
                Log.d(TAG, "Room saved with ID: $newRoomId")

                // Map Gemini items result to DB entities
                val dbItems = result.items.map { itemResult ->
                    FurnitureItemEntity(
                        roomId = newRoomId,
                        name = itemResult.name,
                        category = itemResult.category,
                        width = itemResult.width,
                        height = itemResult.height,
                        x = itemResult.x,
                        y = itemResult.y,
                        rotation = itemResult.rotation,
                        color = itemResult.color,
                        status = itemResult.status,
                        isNew = itemResult.isNew,
                        price = itemResult.price,
                        purchaseUrl = itemResult.purchaseUrl
                    )
                }

                repository.insertFurnitureList(dbItems)
                Log.d(TAG, "Furniture list saved with ${dbItems.size} items.")

                // Set active room and callback
                _activeRoomId.value = newRoomId
                onSuccess(newRoomId)

                // Clear input image for next time
                _selectedImage.value = null

            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed: ${e.message}", e)
                _analysisError.value = "Failed to analyze layout: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // Database updates for 2D planning interactive operations
    fun updateFurniturePosition(item: FurnitureItemEntity, newX: Float, newY: Float) {
        viewModelScope.launch {
            repository.updateFurniture(item.copy(x = newX, y = newY))
        }
    }

    fun rotateFurniture(item: FurnitureItemEntity) {
        viewModelScope.launch {
            val nextRotation = (item.rotation + 90f) % 360f
            repository.updateFurniture(item.copy(rotation = nextRotation))
        }
    }

    fun updateFurnitureStatus(item: FurnitureItemEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateFurniture(item.copy(status = newStatus))
        }
    }

    fun deleteFurnitureItem(item: FurnitureItemEntity) {
        viewModelScope.launch {
            repository.deleteFurnitureById(item.id)
        }
    }

    fun addManualFurnitureItem(
        roomId: Int,
        name: String,
        category: String,
        width: Float,
        height: Float,
        color: String
    ) {
        viewModelScope.launch {
            val newItem = FurnitureItemEntity(
                roomId = roomId,
                name = name,
                category = category,
                width = width,
                height = height,
                x = 180f, // Center of canvas
                y = 180f,
                rotation = 0f,
                color = color,
                status = "Keep",
                isNew = false
            )
            repository.insertFurniture(newItem)
        }
    }

    fun updateRoomWallColor(room: RoomEntity, newColorHex: String) {
        viewModelScope.launch {
            repository.updateRoom(room.copy(wallColor = newColorHex))
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch {
            repository.deleteRoom(room)
            if (_activeRoomId.value == room.id) {
                _activeRoomId.value = null
            }
        }
    }
}
