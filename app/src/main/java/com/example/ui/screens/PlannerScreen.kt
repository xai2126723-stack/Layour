package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.FurnitureItemEntity
import com.example.ui.components.BeforeAfterSlider
import com.example.ui.components.FloorPlanCanvas
import com.example.viewmodel.HomeForgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: HomeForgeViewModel,
    onNavigateBack: () -> Unit
) {
    val activeRoom by viewModel.activeRoom.collectAsState()
    val furnitureItems by viewModel.activeRoomFurniture.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = 2D Plan, 1 = 3D Design
    var selectedItem by remember { mutableStateOf<FurnitureItemEntity?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Calculate total price of new proposed items
    val proposedNewItems = furnitureItems.filter { it.isNew && it.status != "Remove" }
    val totalAdditionsCost = proposedNewItems.sumOf { it.price }

    // Select the "After" image based on the room style
    val afterImageRes = when (activeRoom?.style) {
        "Japandi" -> R.drawable.style_japandi
        "Minimalist" -> R.drawable.style_minimalist
        "Scandinavian" -> R.drawable.style_scandinavian
        "Bohemian" -> R.drawable.style_bohemian
        "Cozy Maximalist" -> R.drawable.style_maximalist
        else -> R.drawable.style_japandi
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeRoom?.name ?: "Interactive Planner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${activeRoom?.roomType} · Style: ${activeRoom?.style}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddItemDialog = true }) {
                        Icon(Icons.Default.AddBox, contentDescription = "Add custom item", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2D Floor Plan", fontWeight = FontWeight.Bold)
                    } }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3D AI Render", fontWeight = FontWeight.Bold)
                    } }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // TAB CONTENTS
                if (selectedTab == 0) {
                    // 2D CANVAS
                    FloorPlanCanvas(
                        items = furnitureItems,
                        selectedItem = selectedItem,
                        onItemSelected = { selectedItem = it },
                        onItemMoved = { item, x, y ->
                            viewModel.updateFurniturePosition(item, x, y)
                        },
                        wallColorHex = activeRoom?.wallColor ?: "#F0EAE1",
                        modifier = Modifier.testTag("floor_plan_canvas")
                    )

                    // Selected item quick controls panel
                    AnimatedVisibility(visible = selectedItem != null) {
                        selectedItem?.let { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${item.category} · ${item.width.toInt()} x ${item.height.toInt()} cm",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteFurnitureItem(item)
                                                selectedItem = null
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Rotate Action
                                        Button(
                                            onClick = { viewModel.rotateFurniture(item) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1EDE6), contentColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rotate 90°", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Status Toggle
                                        val statusLabel = when (item.status) {
                                            "Keep" -> "Action: Keep"
                                            "Move" -> "Action: Move"
                                            "Remove" -> "Action: Remove"
                                            else -> item.status
                                        }
                                        Button(
                                            onClick = {
                                                val nextStatus = when (item.status) {
                                                    "Keep" -> "Move"
                                                    "Move" -> "Remove"
                                                    "Remove" -> "Keep"
                                                    else -> "Keep"
                                                }
                                                viewModel.updateFurnitureStatus(item, nextStatus)
                                                selectedItem = item.copy(status = nextStatus)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = when (item.status) {
                                                    "Keep" -> Color(0xFFE8F5E9)
                                                    "Move" -> Color(0xFFFFF3E0)
                                                    else -> Color(0xFFFFEBEE)
                                                },
                                                contentColor = when (item.status) {
                                                    "Keep" -> Color(0xFF2E7D32)
                                                    "Move" -> Color(0xFFEF6C00)
                                                    else -> Color(0xFFC62828)
                                                }
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1.3f)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                } else {
                    // 3D DESIGN RENDER (Interactive slider comparing Before to the AI-rendered style)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Aesthetic Before & After Comparisons",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Drag the center slider left and right to compare your original space with the AI-generated ${activeRoom?.style} rendering.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        BeforeAfterSlider(
                            beforeImageRes = R.drawable.style_before,
                            afterImageRes = afterImageRes,
                            modifier = Modifier.testTag("before_after_slider")
                        )
                    }
                }

                // Interactive Wall Paint Customizer
                activeRoom?.let { room ->
                    Text(
                        text = "Customize Wall Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Repaint your layout walls digitally to find the perfect accent match:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val paintColors = listOf(
                                Pair("Soft Sage", "#C4D1C2"),
                                Pair("Clay Sand", "#D7A082"),
                                Pair("Warm Linen", "#F3EFE9"),
                                Pair("Charcoal Slate", "#2D3238"),
                                Pair("Golden Ochre", "#DFAC6C")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                paintColors.forEach { (colorName, colorHex) ->
                                    val isSelected = room.wallColor.uppercase() == colorHex.uppercase()
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            viewModel.updateRoomWallColor(room, colorHex)
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(colorHex)))
                                                .border(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(colorName, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Recommendations & Design Notes
                Text(
                    text = "AI Design Rationale",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Designer Commentary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeRoom?.notes ?: "Analyzing design commentary...",
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Shopping List & Additions Budget Tracker
                if (proposedNewItems.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Shopping List & Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Total: $${String.format("%.2f", totalAdditionsCost)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(proposedNewItems) { item ->
                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .testTag("shopping_item_${item.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "$${String.format("%.2f", item.price)}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Button(
                                                onClick = {
                                                    // Mock Open Buy link
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("Buy Now", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Complete Inventory list
                Text(
                    text = "Catalog Inventory (${furnitureItems.size} items)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    furnitureItems.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(item.color)))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            if (item.isNew) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("AI Proposed", color = Color(0xFF2E7D32), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            "${item.category} · ${item.width.toInt()} x ${item.height.toInt()} cm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Status badges
                                    listOf("Keep", "Move", "Remove").forEach { status ->
                                        val isSelected = item.status == status
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (isSelected) {
                                                        when (status) {
                                                            "Keep" -> Color(0xFFE8F5E9)
                                                            "Move" -> Color(0xFFFFF3E0)
                                                            else -> Color(0xFFFFEBEE)
                                                        }
                                                    } else Color(0xFFF1EDE6),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.updateFurnitureStatus(item, status) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = status,
                                                color = if (isSelected) {
                                                    when (status) {
                                                        "Keep" -> Color(0xFF2E7D32)
                                                        "Move" -> Color(0xFFEF6C00)
                                                        else -> Color(0xFFC62828)
                                                    }
                                                } else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Custom Item Dialog
        if (showAddItemDialog) {
            AddCustomItemDialog(
                onDismiss = { showAddItemDialog = false },
                onConfirm = { name, category, width, height, color ->
                    activeRoom?.let { room ->
                        viewModel.addManualFurnitureItem(room.id, name, category, width, height, color)
                    }
                    showAddItemDialog = false
                }
            )
        }
    }
}

@Composable
fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Float, Float, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sofa") }
    var widthStr by remember { mutableStateOf("100") }
    var heightStr by remember { mutableStateOf("80") }
    var selectedColorHex by remember { mutableStateOf("#8B5A2B") }

    val categories = listOf("Sofa", "Table", "Chair", "Cabinet", "Plant", "Lighting", "Rug", "Decor")
    val colors = listOf("#8B5A2B", "#3E2723", "#7F8C8D", "#1E2229", "#E6DFD3", "#2E7D32")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Custom Item",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g. My Wooden Bookcase") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Text("Category", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Dimensions Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Width (cm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightStr,
                        onValueChange = { heightStr = it },
                        label = { Text("Depth (cm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Color Selector
                Text("Render Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (selectedColorHex == hex) 3.dp else 1.dp,
                                    color = if (selectedColorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val w = widthStr.toFloatOrNull() ?: 80f
                            val h = heightStr.toFloatOrNull() ?: 80f
                            onConfirm(name.ifBlank { "Custom $category" }, category, w, h, selectedColorHex)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add to Canvas")
                    }
                }
            }
        }
    }
}
