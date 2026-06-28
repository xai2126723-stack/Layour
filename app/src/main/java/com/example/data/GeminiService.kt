package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Helper to convert Bitmap to Base64
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeRoomAndGenerateLayout(
        bitmap: Bitmap?,
        style: String,
        budget: String,
        roomTypeInput: String = "Auto-detect"
    ): RoomAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured. Falling back to local design simulation.")
            return@withContext generateLocalMockResult(style, budget, roomTypeInput)
        }

        try {
            // Build the prompt
            val promptText = """
                You are HomeForge AI, a brilliant full-stack interior designer and spatial planner.
                Analyze the uploaded image of the room and generate a redesigned floor plan.
                
                The user wants a redesigned layout in the style: '$style' with a '$budget' budget.
                The room type specified or detected is: '$roomTypeInput'.
                
                Your output must be a perfectly calculated 2D layout layout inside a 400x400 grid space.
                
                Task instructions:
                1. Detect and catalog existing items (sofa, table, chairs, plants, rug, lighting, cabinet, TV, etc.).
                2. Reuse as many of these existing items as possible! Position them strategically.
                3. Mark existing items with status "Keep" (if staying in place) or "Move" (if rearranged). Set isNew = false.
                4. Propose 1-3 highly tasteful new furniture or decor additions only if they fit the budget and style (Low budget: very affordable items under $100; Medium budget: high quality items under $400; High budget: luxurious items over $500). Mark these with isNew = true, status "Keep", estimated price, and mock target purchase url.
                5. Estimate room properties: dominant wallColor (hex string), lighting level, and a notes field explaining the spatial design decisions, flow improvement, and color synergy.
                6. Position all items on a 400x400 Canvas. Give each item valid dimensions (width and depth in DP scale, e.g. Sofa: 120x80, Coffee Table: 60x40, Plant: 35x35) and layout positions: x (0 to 360), y (0 to 360), and rotation (0, 90, 180, 270 degrees). Ensure items DO NOT overlap. Position the Sofa in a central conversation area, Rug underneath, Table in front, TV opposite, Plants in corners, etc.
                
                You MUST return a raw JSON response (without markdown block wrappers or backticks) that strictly matches this schema:
                {
                  "roomType": "string (e.g. Living Room, Bedroom, Office)",
                  "wallColor": "string (hex color format like #E5DFD3)",
                  "lighting": "string (e.g. Soft Ambient, Bright Natural)",
                  "notes": "string (rich design reasoning)",
                  "items": [
                    {
                      "name": "string (descriptive name)",
                      "category": "string (Sofa, Table, Chair, Rug, Plant, Lighting, Cabinet, TV, Decor)",
                      "width": float,
                      "height": float,
                      "x": float,
                      "y": float,
                      "rotation": float,
                      "color": "string (hex format representing item's visual color)",
                      "status": "string (Keep, Move, Remove)",
                      "isNew": boolean,
                      "price": double,
                      "purchaseUrl": "string"
                    }
                  ]
                }
            """.trimIndent()

            // Prepare API JSON request payload
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // Add prompt text part
            val textPart = JSONObject().put("text", promptText)
            partsArray.put(textPart)

            // Add image part if provided
            if (bitmap != null) {
                val imagePart = JSONObject().put("inlineData", JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", bitmap.toBase64())
                })
                partsArray.put(imagePart)
            } else {
                // If no bitmap, we can still analyze, but we'll add text saying we have no photo
                partsArray.put(JSONObject().put("text", "[No image uploaded, generate a standard room template of type $roomTypeInput]"))
            }

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Set JSON response format if possible (supported in gemini-3.5-flash)
            val generationConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.75)
            }
            rootJson.put("generationConfig", generationConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorMsg = response.body?.string() ?: "Unknown API error"
                Log.e(TAG, "Gemini API Request failed with code ${response.code}: $errorMsg")
                return@withContext generateLocalMockResult(style, budget, roomTypeInput)
            }

            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "Empty response body from Gemini API")
                return@withContext generateLocalMockResult(style, budget, roomTypeInput)
            }

            // Parse response
            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

            // Strip any markdown wrapper if model ignored instructions
            rawText = rawText.trim()
            if (rawText.startsWith("```json")) {
                rawText = rawText.removePrefix("```json")
            }
            if (rawText.startsWith("```")) {
                rawText = rawText.removePrefix("```")
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.removeSuffix("```")
            }
            rawText = rawText.trim()

            Log.d(TAG, "Raw Gemini Response Text: $rawText")

            val resultJson = JSONObject(rawText)
            val roomType = resultJson.optString("roomType", roomTypeInput.ifBlank { "Living Room" })
            val wallColor = resultJson.optString("wallColor", "#E5DFD3")
            val lighting = resultJson.optString("lighting", "Natural Daylight")
            val notes = resultJson.optString("notes", "Successfully generated layout.")
            
            val itemsArray = resultJson.optJSONArray("items")
            val itemsList = mutableListOf<FurnitureItemResult>()
            
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(i)
                    itemsList.add(
                        FurnitureItemResult(
                            name = itemObj.optString("name", "Furniture"),
                            category = itemObj.optString("category", "Decor"),
                            width = itemObj.optDouble("width", 60.0).toFloat(),
                            height = itemObj.optDouble("height", 60.0).toFloat(),
                            x = itemObj.optDouble("x", 150.0).toFloat(),
                            y = itemObj.optDouble("y", 150.0).toFloat(),
                            rotation = itemObj.optDouble("rotation", 0.0).toFloat(),
                            color = itemObj.optString("color", "#CCCCCC"),
                            status = itemObj.optString("status", "Keep"),
                            isNew = itemObj.optBoolean("isNew", false),
                            price = itemObj.optDouble("price", 0.0),
                            purchaseUrl = itemObj.optString("purchaseUrl", "")
                        )
                    )
                }
            }

            return@withContext RoomAnalysisResult(
                roomType = roomType,
                wallColor = wallColor,
                lighting = lighting,
                notes = notes,
                items = itemsList
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini analysis: ${e.message}", e)
            return@withContext generateLocalMockResult(style, budget, roomTypeInput)
        }
    }

    private fun generateLocalMockResult(style: String, budget: String, roomTypeInput: String): RoomAnalysisResult {
        val finalRoomType = if (roomTypeInput == "Auto-detect" || roomTypeInput.isBlank()) "Living Room" else roomTypeInput
        Log.d(TAG, "Generating custom local mock design layout for $finalRoomType in style $style ($budget budget)")

        val items = mutableListOf<FurnitureItemResult>()
        var wallColor = "#F0EAE1"
        var lighting = "Soft Natural Warm"
        var notes = ""

        when (finalRoomType) {
            "Living Room" -> {
                wallColor = when (style) {
                    "Japandi" -> "#E8E2D7"
                    "Minimalist" -> "#F7F7F7"
                    "Scandinavian" -> "#EDEDE9"
                    "Cozy Maximalist" -> "#D9C1AC"
                    "Bohemian" -> "#E3D3C4"
                    else -> "#F0EAE1"
                }
                lighting = "Bright Diffuse Light"
                notes = "Arranged with a focus on symmetry and flow. The sofa is positioned centrally to optimize the conversation circle. Reused your existing wooden table as a cozy accent. We added a new modern $style rug and an accent plant to bring warm natural elements into the room without cluttering the walk paths."

                // Existing items
                items.add(FurnitureItemResult("Classic Grey Sofa", "Sofa", 150f, 85f, 125f, 240f, 0f, "#7F8C8D", "Move", false))
                items.add(FurnitureItemResult("Oak Coffee Table", "Table", 80f, 50f, 125f, 150f, 0f, "#B08A61", "Move", false))
                items.add(FurnitureItemResult("Wood Armchair", "Chair", 70f, 70f, 260f, 160f, 270f, "#8C6B53", "Keep", false))
                items.add(FurnitureItemResult("Tall Bookshelf", "Cabinet", 90f, 40f, 330f, 60f, 180f, "#5C4033", "Keep", false))
                items.add(FurnitureItemResult("TV Media Stand", "Cabinet", 110f, 45f, 125f, 45f, 0f, "#404040", "Keep", false))

                // New items depending on style/budget
                val priceFactor = when (budget) {
                    "Low" -> 0.7
                    "High" -> 2.5
                    else -> 1.0
                }
                items.add(FurnitureItemResult("Woven $style Rug", "Rug", 220f, 180f, 125f, 180f, 0f, "#E6DFD3", "Keep", true, 89.0 * priceFactor, "https://www.target.com"))
                items.add(FurnitureItemResult("Fiddle Leaf Fig Plant", "Plant", 45f, 45f, 50f, 50f, 0f, "#2E7D32", "Keep", true, 29.0 * priceFactor, "https://www.homedepot.com"))
                
                if (budget != "Low") {
                    items.add(FurnitureItemResult("Minimalist Brass Floor Lamp", "Lighting", 40f, 40f, 270f, 260f, 0f, "#D4AF37", "Keep", true, 119.0 * priceFactor, "https://www.westelm.com"))
                }
            }
            "Bedroom" -> {
                wallColor = when (style) {
                    "Japandi" -> "#D9C7B6"
                    "Minimalist" -> "#EBEBEB"
                    "Scandinavian" -> "#E3E7E8"
                    "Cozy Maximalist" -> "#A0715C"
                    "Bohemian" -> "#E8D8C8"
                    else -> "#EDEDE9"
                }
                lighting = "Soft Warm Accent"
                notes = "Reoriented the bed against the main accent wall to create a majestic bedroom center-point. We recommend keeping your existing wooden wardrobe in the corner to maximize floor space. Added a plush, custom-colored organic fiber rug and dual sleek nightstands to balance the layout perfectly."

                // Existing Bed
                items.add(FurnitureItemResult("Queen Size Bed", "Sofa", 160f, 200f, 125f, 130f, 0f, "#6D4C41", "Move", false))
                // Existing Wardrobe
                items.add(FurnitureItemResult("Tall Wooden Wardrobe", "Cabinet", 100f, 60f, 330f, 280f, 90f, "#5D4037", "Keep", false))
                // Existing Mirror
                items.add(FurnitureItemResult("Full Length Mirror", "Decor", 50f, 15f, 40f, 280f, 45f, "#BDBDBD", "Keep", false))

                // New additions
                val priceFactor = when (budget) {
                    "Low" -> 0.6
                    "High" -> 2.2
                    else -> 1.0
                }
                items.add(FurnitureItemResult("$style Nightstand", "Table", 45f, 45f, 30f, 80f, 0f, "#E0C39E", "Keep", true, 45.0 * priceFactor, "https://www.ikea.com"))
                items.add(FurnitureItemResult("Warm Textural Area Rug", "Rug", 240f, 160f, 125f, 180f, 0f, "#F5EBE6", "Keep", true, 135.0 * priceFactor, "https://www.wayfair.com"))
                
                if (budget == "High") {
                    items.add(FurnitureItemResult("Designer Pendant Light", "Lighting", 50f, 50f, 125f, 130f, 0f, "#C5A880", "Keep", true, 280.0, "https://www.cb2.com"))
                }
            }
            "Office" -> {
                wallColor = when (style) {
                    "Japandi" -> "#E5E1DA"
                    "Minimalist" -> "#FFFFFF"
                    "Scandinavian" -> "#F0ECE5"
                    "Cozy Maximalist" -> "#1A3636"
                    "Bohemian" -> "#DFD3C3"
                    else -> "#ECEBE4"
                }
                lighting = "Focused Task Lighting"
                notes = "Positioned the desk sideways relative to the window to avoid direct screen glare while keeping natural daylight within sight. Reused your favorite leather office chair. Proposed a sleek metal desk organizer and a cozy geometric rug to suppress hollow acoustic echo."

                // Existing Desk
                items.add(FurnitureItemResult("Writing Desk", "Table", 120f, 60f, 120f, 100f, 0f, "#8B5A2B", "Move", false))
                // Existing Chair
                items.add(FurnitureItemResult("Ergonomic Desk Chair", "Chair", 65f, 65f, 120f, 160f, 180f, "#333333", "Move", false))
                // Existing Shelf
                items.add(FurnitureItemResult("Filing Cabinet", "Cabinet", 45f, 50f, 320f, 80f, 0f, "#7F8C8D", "Keep", false))

                // New additions
                val priceFactor = when (budget) {
                    "Low" -> 0.5
                    "High" -> 2.0
                    else -> 1.0
                }
                items.add(FurnitureItemResult("Sleek LED Desk Lamp", "Lighting", 30f, 30f, 160f, 90f, 0f, "#212121", "Keep", true, 39.0 * priceFactor, "https://www.amazon.com"))
                items.add(FurnitureItemResult("Cozy Geometric Office Rug", "Rug", 180f, 120f, 120f, 140f, 0f, "#D6C7A1", "Keep", true, 79.0 * priceFactor, "https://www.rugsusa.com"))
                items.add(FurnitureItemResult("Desk Accent Plant", "Plant", 30f, 30f, 70f, 90f, 0f, "#4CAF50", "Keep", true, 19.0 * priceFactor, "https://www.homedepot.com"))
            }
            else -> {
                // Fallback / General Space
                wallColor = "#EDEDE9"
                lighting = "Ambient Light"
                notes = "Arranged the space to open up walking corridors. Reused your existing central seating furniture. Added a modern styled rug and lighting."

                items.add(FurnitureItemResult("Main Seating", "Sofa", 140f, 80f, 120f, 180f, 0f, "#555555", "Move", false))
                items.add(FurnitureItemResult("Central Table", "Table", 70f, 70f, 120f, 110f, 0f, "#999999", "Move", false))
                items.add(FurnitureItemResult("Corner Plant", "Plant", 40f, 40f, 40f, 40f, 0f, "#388E3C", "Keep", true, 25.0, ""))
                items.add(FurnitureItemResult("Accent Rug", "Rug", 200f, 150f, 120f, 150f, 0f, "#CCCCCC", "Keep", true, 95.0, ""))
            }
        }

        return RoomAnalysisResult(
            roomType = finalRoomType,
            wallColor = wallColor,
            lighting = lighting,
            notes = notes,
            items = items
        )
    }
}

data class RoomAnalysisResult(
    val roomType: String,
    val wallColor: String,
    val lighting: String,
    val notes: String,
    val items: List<FurnitureItemResult>
)

data class FurnitureItemResult(
    val name: String,
    val category: String,
    val width: Float,
    val height: Float,
    val x: Float,
    val y: Float,
    val rotation: Float,
    val color: String,
    val status: String,
    val isNew: Boolean,
    val price: Double = 0.0,
    val purchaseUrl: String = ""
)
