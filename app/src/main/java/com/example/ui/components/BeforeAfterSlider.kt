package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BeforeAfterSlider(
    beforeImageRes: Int,
    afterImageRes: Int,
    modifier: Modifier = Modifier
) {
    // Current slider position from 0.0 (all Before) to 1.0 (all After)
    var sliderPosition by remember { mutableStateOf(0.5f) }
    var containerWidth by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray)
            .onSizeChanged { containerWidth = it.width }
    ) {
        if (containerWidth > 0) {
            // Calculate divider pixel offset
            val dividerX = (containerWidth * sliderPosition).toInt()

            // 1. Draw Before Image (Bottom Layer, always visible except where covered)
            Image(
                painter = painterResource(id = beforeImageRes),
                contentDescription = "Before Redesign",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Label "Before"
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("BEFORE", color = Color.White, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
            }

            // 2. Draw After Image (Top Layer, clipped based on sliderPosition)
            // We only show the after image to the right of dividerX, or left depending on choice.
            // Let's show After on the RIGHT side.
            // This means we clip the After image so only from dividerX to containerWidth is drawn!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CustomRectClip(dividerX, containerWidth))
            ) {
                Image(
                    painter = painterResource(id = afterImageRes),
                    contentDescription = "After Redesign Render",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Label "After AI Render"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("AI REDESIGN", color = Color.White, fontSize = 10.sp, style = MaterialTheme.typography.labelSmall)
                }
            }

            // 3. Sliding Divider Handle Line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { (dividerX).toDp() - 2.dp })
                    .background(Color.White)
            )

            // Circular Drag Handle in center of line
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .offset(
                        x = with(androidx.compose.ui.platform.LocalDensity.current) { (dividerX).toDp() - 20.dp },
                        y = with(androidx.compose.ui.platform.LocalDensity.current) { (maxHeight / 2) - 20.dp }
                    )
                    .background(Color.White, CircleShape)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newXPx = (containerWidth * sliderPosition) + dragAmount.x
                            sliderPosition = (newXPx / containerWidth).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Drag comparison slider",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Custom shape that clips to a rectangular bounds (e.g. from xMin to xMax)
private class CustomRectClip(private val leftPx: Int, private val widthPx: Int) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Rectangle(
            Rect(
                left = leftPx.toFloat(),
                top = 0f,
                right = widthPx.toFloat(),
                bottom = size.height
            )
        )
    }
}
