package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FurnitureItemEntity

@Composable
fun FloorPlanCanvas(
    items: List<FurnitureItemEntity>,
    selectedItem: FurnitureItemEntity?,
    onItemSelected: (FurnitureItemEntity?) -> Unit,
    onItemMoved: (FurnitureItemEntity, Float, Float) -> Unit,
    modifier: Modifier = Modifier,
    wallColorHex: String = "#F0EAE1"
) {
    // Canvas is based on a logical coordinate space of 400 x 400
    val logicalWidth = 400f
    val logicalHeight = 400f

    val wallColor = remember(wallColorHex) {
        try {
            Color(android.graphics.Color.parseColor(wallColorHex))
        } catch (e: Exception) {
            Color(0xFFF0EAE1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(16.dp))
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Scaling factors to convert from 400x400 logical space to actual layout space
        val scaleX = widthPx / logicalWidth
        val scaleY = heightPx / logicalHeight

        // Drag tracking state
        var activeDraggingId by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        // Find which item was clicked
        fun findItemAt(offset: Offset): FurnitureItemEntity? {
            // Traverse backward to select items on top first
            for (item in items.asReversed()) {
                if (item.status == "Remove") continue
                
                val itemX = item.x * scaleX
                val itemY = item.y * scaleY
                val itemW = item.width * scaleX
                val itemH = item.height * scaleY

                // Check simple bounding box centered around (itemX, itemY)
                val left = itemX - itemW / 2
                val right = itemX + itemW / 2
                val top = itemY - itemH / 2
                val bottom = itemY + itemH / 2

                if (offset.x in left..right && offset.y in top..bottom) {
                    return item
                }
            }
            return null
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val item = findItemAt(offset)
                            if (item != null) {
                                onItemSelected(item)
                                activeDraggingId = item.id
                                // Store drag anchor relative to item's logical center
                                dragOffset = Offset(
                                    offset.x - (item.x * scaleX),
                                    offset.y - (item.y * scaleY)
                                )
                            } else {
                                onItemSelected(null)
                                activeDraggingId = null
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val draggingId = activeDraggingId
                            if (draggingId != null) {
                                val item = items.find { it.id == draggingId }
                                if (item != null) {
                                    // Calculate new raw coordinates in actual pixels
                                    val currentPointerPos = change.position
                                    val targetPxX = currentPointerPos.x - dragOffset.x
                                    val targetPxY = currentPointerPos.y - dragOffset.y

                                    // Convert back to logical space and clamp to canvas boundaries
                                    val logicalX = (targetPxX / scaleX).coerceIn(
                                        item.width / 2f,
                                        logicalWidth - item.width / 2f
                                    )
                                    val logicalY = (targetPxY / scaleY).coerceIn(
                                        item.height / 2f,
                                        logicalHeight - item.height / 2f
                                    )

                                    onItemMoved(item, logicalX, logicalY)
                                }
                            }
                        },
                        onDragEnd = {
                            activeDraggingId = null
                        },
                        onDragCancel = {
                            activeDraggingId = null
                        }
                    )
                }
        ) {
            // 1. Draw Grid Lines
            val gridCount = 8
            val gridStepX = size.width / gridCount
            val gridStepY = size.height / gridCount
            for (i in 1 until gridCount) {
                // Vertical lines
                drawLine(
                    color = Color(0xFFECE6DB),
                    start = Offset(i * gridStepX, 0f),
                    end = Offset(i * gridStepX, size.height),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                // Horizontal lines
                drawLine(
                    color = Color(0xFFECE6DB),
                    start = Offset(0f, i * gridStepY),
                    end = Offset(size.width, i * gridStepY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // 2. Draw outer walls with current wallColorHex
            val borderPadding = 6f
            drawRoundRect(
                color = wallColor.copy(alpha = 0.15f),
                topLeft = Offset(borderPadding, borderPadding),
                size = Size(size.width - borderPadding * 2, size.height - borderPadding * 2),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = wallColor,
                topLeft = Offset(borderPadding, borderPadding),
                size = Size(size.width - borderPadding * 2, size.height - borderPadding * 2),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 12f)
            )

            // 3. Render all layout furniture items
            for (item in items) {
                if (item.status == "Remove") continue

                val itemX = item.x * scaleX
                val itemY = item.y * scaleY
                val itemW = item.width * scaleX
                val itemH = item.height * scaleY

                // Rotate around the item's center point
                rotate(degrees = item.rotation, pivot = Offset(itemX, itemY)) {
                    val baseColor = try {
                        Color(android.graphics.Color.parseColor(item.color))
                    } catch (e: Exception) {
                        Color(0xFFB0BEC5)
                    }

                    // Adjust design styling based on status and if it's brand new
                    val fillColor = if (item.isNew) {
                        baseColor.copy(alpha = 0.85f)
                    } else if (item.status == "Move") {
                        baseColor.copy(alpha = 0.70f)
                    } else {
                        baseColor.copy(alpha = 0.50f)
                    }

                    // Item Body (Rounded rectangle representing the top-down shape)
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(itemX - itemW / 2, itemY - itemH / 2),
                        size = Size(itemW, itemH),
                        cornerRadius = CornerRadius(16f, 16f)
                    )

                    // Draw special dash/glow outline for selected item
                    if (selectedItem?.id == item.id) {
                        drawRoundRect(
                            color = Color(0xFF5A5A40), // High Density Olive selection accent
                            topLeft = Offset(itemX - itemW / 2 - 4f, itemY - itemH / 2 - 4f),
                            size = Size(itemW + 8f, itemH + 8f),
                            cornerRadius = CornerRadius(20f, 20f),
                            style = Stroke(width = 4f)
                        )
                    } else if (item.isNew) {
                        // Soft green border for AI suggested additions
                        drawRoundRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(itemX - itemW / 2, itemY - itemH / 2),
                            size = Size(itemW, itemH),
                            cornerRadius = CornerRadius(16f, 16f),
                            style = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                    } else {
                        // Substandard slate outline
                        drawRoundRect(
                            color = Color(0xFF5D4037).copy(alpha = 0.3f),
                            topLeft = Offset(itemX - itemW / 2, itemY - itemH / 2),
                            size = Size(itemW, itemH),
                            cornerRadius = CornerRadius(16f, 16f),
                            style = Stroke(width = 2f)
                        )
                    }

                    // Draw category icon representation on Canvas
                    // Text label inside block
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = if (item.color.uppercase() == "#FFFFFF" || item.color.uppercase() == "#F5EBE6") {
                                android.graphics.Color.BLACK
                            } else {
                                android.graphics.Color.WHITE
                            }
                            textSize = if (itemW < 80) 10f * density.density else 12f * density.density
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                        }

                        // Label text (truncated if too wide)
                        val nameToShow = if (item.name.length > 12 && itemW < 90) {
                            item.name.take(10) + ".."
                        } else {
                            item.name
                        }

                        // Center the text
                        drawText(
                            nameToShow,
                            itemX,
                            itemY + (paint.textSize / 3), // vertical center adjustment
                            paint
                        )
                    }
                }
            }
        }

        // Draggable tips label
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (selectedItem != null) "Selected: ${selectedItem.name}. Drag to move / Rotate below" else "Tap and drag any item to rearrange",
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}
