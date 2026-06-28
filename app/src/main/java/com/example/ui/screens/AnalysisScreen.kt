package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.viewmodel.HomeForgeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: HomeForgeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPlanner: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val roomType by viewModel.roomTypeInput.collectAsState()
    val selectedStyle by viewModel.styleInput.collectAsState()
    val selectedBudget by viewModel.budgetInput.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisError by viewModel.analysisError.collectAsState()

    // Launcher for selecting an image from the gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri).use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    viewModel.setSelectedImage(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Design style data class
    data class DesignStyle(
        val name: String,
        val imageRes: Int,
        val desc: String
    )

    val designStyles = remember {
        listOf(
            DesignStyle("Japandi", R.drawable.style_japandi, "Blends Japanese minimalism with Scandinavian coziness."),
            DesignStyle("Scandinavian", R.drawable.style_scandinavian, "Airy, functional, characterized by light woods and pale tones."),
            DesignStyle("Minimalist", R.drawable.style_minimalist, "Focused on clean lines, spacious negative area, and raw function."),
            DesignStyle("Bohemian", R.drawable.style_bohemian, "Rich texture, rattan elements, warm clay tones, and plant synergy."),
            DesignStyle("Cozy Maximalist", R.drawable.style_maximalist, "Rich jewel tones, gallery walls, and comfortable layered decor.")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyze Space", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (isAnalyzing) {
            LoadingScannerOverlay(roomType, selectedStyle, selectedBudget)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Upload Section
                Text(
                    text = "1. Upload Room Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable { imagePickerLauncher.launch("image/*") }
                        .testTag("upload_image_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    if (selectedImage != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = selectedImage!!.asImageBitmap(),
                                contentDescription = "Selected living space",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Re-upload badge overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Photo", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to upload space photo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PNG, JPG up to 10MB (Wide-angle works best)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Sample buttons fallback
                            Text(
                                text = "OR USE A SAMPLE ROOM:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SampleRoomChip(label = "Living Room") {
                                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.style_before)
                                    viewModel.setSelectedImage(bitmap)
                                    viewModel.setRoomType("Living Room")
                                }
                                SampleRoomChip(label = "Bedroom") {
                                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.style_before)
                                    viewModel.setSelectedImage(bitmap)
                                    viewModel.setRoomType("Bedroom")
                                }
                                SampleRoomChip(label = "Home Office") {
                                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.style_before)
                                    viewModel.setSelectedImage(bitmap)
                                    viewModel.setRoomType("Office")
                                }
                            }
                        }
                    }
                }

                // 2. Room Type Selector
                Text(
                    text = "2. Specify Room Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Living Room", "Bedroom", "Office").forEach { type ->
                        val isSelected = roomType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setRoomType(type) },
                            label = { Text(type, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Style Preference Horizontal Scroll
                Text(
                    text = "3. Select Redesign Style",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(designStyles) { style ->
                        val isSelected = selectedStyle == style.name
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable { viewModel.setStyle(style.name) }
                                .testTag("style_card_${style.name}"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                Image(
                                    painter = painterResource(id = style.imageRes),
                                    contentDescription = style.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = style.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = style.desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Budget Slider
                Text(
                    text = "4. Budget Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { budget ->
                            val isSelected = selectedBudget == budget
                            Button(
                                onClick = { viewModel.setBudget(budget) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1EDE6),
                                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = when (budget) {
                                        "Low" -> "Low ($)"
                                        "Medium" -> "Medium ($$)"
                                        "High" -> "High ($$$)"
                                        else -> budget
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                if (analysisError != null) {
                    Text(
                        text = analysisError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action CTA
                Button(
                    onClick = {
                        viewModel.analyzeAndGenerateLayout { roomId ->
                            onNavigateToPlanner(roomId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("run_analysis_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate AI Layout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SampleRoomChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(Color(0xFFEFECE6), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun LoadingScannerOverlay(
    roomType: String,
    style: String,
    budget: String
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Animation for scanning bar
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Dynamic scanning log lines
    var currentLog by remember { mutableStateOf("Initializing Room Analysis Engine...") }

    LaunchedEffect(Unit) {
        val logs = listOf(
            "Initializing Room Analysis Engine...",
            "Decoding uploaded image matrices...",
            "Detecting physical spatial boundaries...",
            "Cataloging furniture items...",
            "Segmenting existing sofas, chairs, tables...",
            "Running Gemini Vision spatial analysis...",
            "Fusing custom '$style' style elements...",
            "Arranging layout using design rules...",
            "Balancing lighting and pedestrian traffic flows...",
            "Sourcing $budget budget additions...",
            "Generating 2D floor blueprint..."
        )
        var index = 0
        while (true) {
            currentLog = logs[index]
            index = (index + 1) % logs.size
            delay(1800)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Scanning Visual Box
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
            ) {
                // Background placeholder photo representation
                Image(
                    painter = painterResource(id = R.drawable.style_before),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Laser Scanning Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .offset(y = 240.dp * scanY)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), MaterialTheme.colorScheme.primary)
                            )
                        )
                )

                // Laser glow effect
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .offset(y = (240.dp * scanY) - 15.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "HomeForge AI is Forging",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Redesigning $roomType into a $style space...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                modifier = Modifier
                    .width(180.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading logs text
            Text(
                text = currentLog,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
