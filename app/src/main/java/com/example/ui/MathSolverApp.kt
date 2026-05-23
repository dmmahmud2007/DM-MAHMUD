package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.HistoryItem
import com.example.util.MathExpressionEvaluator
import com.example.util.StatisticsResult
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathSolverApp(
    viewModel: MathSolverViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left-hand Sigma emblem from Geometric Balance theme
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Σ",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = "Axiom Math",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ADVANCED SOLVER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 2.sp
                                )
                            }
                        }

                        // Right header trigger buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { viewModel.setTab(3) }, // Navigate to history tab
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("navigation_bar"),
                containerColor = Color(0xFFF7F2F1)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Solver") },
                    label = { Text("Solver", maxLines = 1) },
                    modifier = Modifier.testTag("tab_solver")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = "Graphing") },
                    label = { Text("Graphing", maxLines = 1) },
                    modifier = Modifier.testTag("tab_graphing")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Statistics") },
                    label = { Text("Stats", maxLines = 1) },
                    modifier = Modifier.testTag("tab_statistics")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", maxLines = 1) },
                    modifier = Modifier.testTag("tab_history")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> SolverScreen(viewModel)
                1 -> GraphingScreen(viewModel)
                2 -> StatisticsScreen(viewModel)
                3 -> HistoryScreen(viewModel)
            }
        }
    }
}

// ----------------- 1. SOLVER SCREEN -----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SolverScreen(viewModel: MathSolverViewModel) {
    val input by viewModel.solverInput.collectAsStateWithLifecycle()
    val uiState by viewModel.solverState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var showScanner by remember { mutableStateOf(false) }
    var selectedScanPresetIdx by remember { mutableStateOf(0) }
    var scanStatus by remember { mutableStateOf("Idle") }
    var scanProgress by remember { mutableStateOf(0f) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var cameraZoom by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()

    val mathSymbols = listOf(
        "√()", "^2", "^", "π", "θ", "∫", "∑", "lim", "d/dx", "log()", "ln()", "sin()", "cos()", "tan()", " ( ", " ) ", "+", "-", "*", "/"
    )

    val inputTemplates = listOf(
        "Algebra" to "solve 3x + 5 = 20",
        "Calculus" to "integrate x^2 from 0 to 3",
        "Derivative" to "derivative of 3x^2 - 5x + 2",
        "Quadratic" to "solve x^2 - 5x + 6 = 0",
        "Matrix" to "inverse of matrix [[2,1],[1,3]]",
        "Limit" to "limit of sin(x)/x as x approaches 0",
        "Trig" to "simplify sin(x)^2 + cos(x)^2"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Input
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Global Mathematics Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Solve anything from algebra to multivariable calculus, trigonometry, statistics, and discrete math.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- PROBLEM SCANNER MODULE ---
        if (!showScanner) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScanner = true }
                        .testTag("btn_open_scanner"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan icon",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Interactive Problem Scanner",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Digitize formulas instantly from live textbook templates using Axiom AI OCR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Scanner",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_viewport_card"),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        // Scanner Viewport Toolbar header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Axiom OCR Camera Viewer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showScanner = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Scanner",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Simulated Lens Viewfinder viewport
                        val scannableEquations = listOf(
                            "solve x^2 - 5x + 6 = 0" to "Algebraic Quadratic Eq",
                            "integrate 3x^2 - 5x + 2" to "Calculus Integration",
                            "derivative of sin(x) * x^2" to "Calculus Product Derivative",
                            "simplify sin(θ)^2 + cos(θ)^2" to "Trigonometric Identity",
                            "limit of sin(x)/x as x approaches 0" to "Limit Evaluation"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color.Black)
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        ) {
                            // Flashlight effect overlay
                            if (isFlashlightOn) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.25f),
                                                    Color.Transparent
                                                ),
                                                radius = 500f
                                            )
                                        )
                                )
                            }

                            // Camera grid lines drawBehind
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                
                                // Draw horizontal camera thirds grid guidelines
                                drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
                                drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), 1f)
                                // Draw vertical grid lines
                                drawLine(Color.White.copy(alpha = 0.15f), Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
                                drawLine(Color.White.copy(alpha = 0.15f), Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), 1f)
                            }

                            // Centered Focus viewfinder target frame box with glowing corners
                            val cropWidth = 260.dp
                            val cropHeight = 110.dp
                            Box(
                                modifier = Modifier
                                    .size(width = cropWidth, height = cropHeight)
                                    .align(Alignment.Center)
                                    .border(
                                        BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                // Draw corner bracket ticks in the crop area
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizeL = 24f
                                    val strokeW = 6f
                                    val neonGreen = Color(0xFF00FFCC)

                                    // Top Left Corner Bracket
                                    drawLine(neonGreen, Offset(0f, 0f), Offset(sizeL, 0f), strokeW)
                                    drawLine(neonGreen, Offset(0f, 0f), Offset(0f, sizeL), strokeW)

                                    // Top Right Corner Bracket
                                    drawLine(neonGreen, Offset(size.width, 0f), Offset(size.width - sizeL, 0f), strokeW)
                                    drawLine(neonGreen, Offset(size.width, 0f), Offset(size.width, sizeL), strokeW)

                                    // Bottom Left Corner Bracket
                                    drawLine(neonGreen, Offset(0f, size.height), Offset(sizeL, size.height), strokeW)
                                    drawLine(neonGreen, Offset(0f, size.height), Offset(0f, size.height - sizeL), strokeW)

                                    // Bottom Right Corner Bracket
                                    drawLine(neonGreen, Offset(size.width, size.height), Offset(size.width - sizeL, size.height), strokeW)
                                    drawLine(neonGreen, Offset(size.width, size.height), Offset(size.width, size.height - sizeL), strokeW)
                                }

                                // Interactive OCR scan target item inside viewfinder
                                val activePair = scannableEquations[selectedScanPresetIdx]
                                val scaleFactor = cameraZoom

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "TEXTBOOK VOL. VII",
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = activePair.first,
                                        color = Color(0xFF00FFCC),
                                        fontSize = (16 * scaleFactor).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "[Tap scan targets below to swap equations]",
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 7.sp
                                    )
                                }
                            }

                            // Glowing continuous scrolling laser bar scanner
                            val infiniteTransition = rememberInfiniteTransition(label = "Laser")
                            val sweepRatio by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 2200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "LaserLine"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.015f)
                                    .offset(y = 220.dp * sweepRatio)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF00FFCC).copy(alpha = 0.1f),
                                                Color(0xFF00FFCC),
                                                Color(0xFF00FFCC).copy(alpha = 0.1f)
                                            )
                                        )
                                    )
                                    .align(Alignment.TopStart)
                            )

                            // Simulated shutter / active scanner capture curtain
                            androidx.compose.animation.AnimatedVisibility(
                                visible = scanStatus == "Scanning",
                                enter = fadeIn(animationSpec = spring()),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.75f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF00FFCC),
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            text = when {
                                                scanProgress < 0.3f -> "ISOLATING REGION OF INTEREST..."
                                                scanProgress < 0.6f -> "DIGITIZING GLYPH CONNECTIONS..."
                                                scanProgress < 0.85f -> "RESOLVING OPERATOR EQUATIONS..."
                                                else -> "COMPILING AXIOM SOLUTION..."
                                            },
                                            color = Color(0xFF00FFCC),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { scanProgress },
                                            modifier = Modifier
                                                .width(160.dp)
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = Color(0xFF00FFCC),
                                            trackColor = Color.White.copy(alpha = 0.2f),
                                        )
                                    }
                                }
                            }

                            // Overlay top right badges (Flash, status info)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopStart)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "LIVE FEED: 1080P",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { isFlashlightOn = !isFlashlightOn },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                            contentDescription = "Torch",
                                            tint = if (isFlashlightOn) Color.Yellow else Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Lens control sliders
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Slider(
                                value = cameraZoom,
                                onValueChange = { cameraZoom = it },
                                valueRange = 0.8f..2.0f,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                            )
                            Text(
                                text = String.format("%.1fx Zoom", cameraZoom),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Divider()

                        // Preset math textbooks choice carousel to easily test it
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Workbook Challenge Previews (Tap to target in Viewfinder):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                itemsIndexed(scannableEquations) { idx, pair ->
                                    val isSelected = selectedScanPresetIdx == idx
                                    AssistChip(
                                        onClick = { selectedScanPresetIdx = idx },
                                        label = {
                                            Column {
                                                Text(pair.second, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                                Text(pair.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            }
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.testTag("scan_preset_$idx")
                                    )
                                }
                            }
                        }

                        // Main action trigger solver
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showScanner = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Dismiss", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        scanStatus = "Scanning"
                                        scanProgress = 0f
                                        // Animate simulated loading progress
                                        repeat(10) { step ->
                                            delay(150)
                                            scanProgress = (step + 1) / 10f
                                        }
                                        val equationToSolve = scannableEquations[selectedScanPresetIdx].first
                                        viewModel.updateSolverInput(equationToSolve)
                                        viewModel.solveCurrentProblem()
                                        scanStatus = "Idle"
                                        showScanner = false
                                        Toast.makeText(context, "Scanning Successful: Scanned '$equationToSolve'!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("btn_trigger_scan"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00BFA5)
                                )
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Scan & Solve ⚡", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Input Field
        item {
            OutlinedTextField(
                value = input,
                onValueChange = { viewModel.updateSolverInput(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("solver_input_field"),
                label = { Text("Enter mathematical query") },
                placeholder = { Text("e.g. solve 2x + 3 = 11, or integrate sin(x)") },
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSolverInput("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear text")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onAny = {
                    keyboardController?.hide()
                    viewModel.solveCurrentProblem()
                }),
                singleLine = false,
                maxLines = 4
            )
        }

        // Quick Math Keyboard
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Math Keyboard",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    mathSymbols.forEach { sym ->
                        SuggestionChip(
                            onClick = {
                                viewModel.appendToSolverInput(sym)
                            },
                            label = {
                                Text(
                                    text = sym,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            },
                            modifier = Modifier.testTag("sym_$sym")
                        )
                    }
                }
            }
        }

        // Templates Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Core Templates",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(inputTemplates) { (category, template) ->
                        ElevatedAssistChip(
                            onClick = { viewModel.updateSolverInput(template) },
                            label = { Text(category) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("template_$category")
                        )
                    }
                }
            }
        }

        // Action Buttons
        item {
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.solveCurrentProblem()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_solve"),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Activate Engine Solver", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // Output Solution Pane
        item {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "SolutionState"
            ) { state ->
                when (state) {
                    is SolverUiState.Idle -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Enter a question above and tap Solve",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is SolverUiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                "Consulting Global Mathematical Knowledge base...",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is SolverUiState.Success -> {
                        StepByStepSolutionCard(solution = state.solution)
                    }
                    is SolverUiState.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Error icon",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepByStepSolutionCard(solution: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solution_card"),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Step-By-Step Solution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Custom Gradient divider to match Geometric Balance theme
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Visual parsing of headings and bold sections
            val paragraphs = solution.split("\n")
            paragraphs.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                } else if (trimmed.startsWith("Step") || trimmed.any { it.isDigit() } && trimmed.contains("Step")) {
                    Text(
                        text = trimmed,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                } else if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                    val cleanText = trimmed.replace("**", "")
                    Text(
                        text = cleanText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // Normal text rendering. Bold inner matches.
                    RenderInlineFormatting(text = trimmed)
                }
            }
        }
    }
}

@Composable
fun RenderInlineFormatting(text: String) {
    // Basic bold highlighting replacement within line
    if (text.contains("**")) {
        val parts = text.split("**")
        androidx.compose.foundation.text.BasicText(
            text = androidx.compose.ui.text.buildAnnotatedString {
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        append(part)
                        pop()
                    } else {
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.onSurface))
                        append(part)
                        pop()
                    }
                }
            },
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            modifier = Modifier.padding(vertical = 2.dp)
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 2.dp),
            lineHeight = 22.sp
        )
    }
}


// ----------------- 2. GRAPHING SCREEN -----------------

@Composable
fun GraphingScreen(viewModel: MathSolverViewModel) {
    val formula by viewModel.graphInput.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()
    val trackedPoint by viewModel.selectedGraphPoint.collectAsStateWithLifecycle()
    val panOffsetX by viewModel.panOffsetX.collectAsStateWithLifecycle()
    val panOffsetY by viewModel.panOffsetY.collectAsStateWithLifecycle()
    val dragMode by viewModel.graphDragMode.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val sampleEquations = listOf(
        "Parabola" to "y = x^2 - 4",
        "Sine Wave" to "y = 3 * sin(x)",
        "Cosine Wave" to "y = 2 * cos(2x)",
        "Cubic Shape" to "y = x^3 - 3x",
        "Absolute Val" to "y = abs(x) - 3",
        "Linear Grid" to "y = 0.5x + 1",
        "Exponential" to "y = exp(0.2x) - 2"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Interactive Vector Grapher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Enter any function in terms of x and trace coordinates dynamically on the canvas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Formula Field
        item {
            OutlinedTextField(
                value = formula,
                onValueChange = { viewModel.updateGraphInput(it) },
                label = { Text("Formula y = f(x)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("graph_formula_field"),
                placeholder = { Text("e.g. y = x^2 - 2x") },
                trailingIcon = {
                    if (formula.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateGraphInput("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                }),
                singleLine = true
            )
        }

        // Templates Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(sampleEquations) { (label, value) ->
                    FilterChip(
                        selected = formula == value,
                        onClick = { viewModel.updateGraphInput(value) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("graph_preset_$label")
                    )
                }
            }
        }

        // Interactive Mode Selector (Pan vs Trace Cursor)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control mode:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                FilterChip(
                    selected = dragMode == 0,
                    onClick = { viewModel.setDragMode(0) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Trace Cursor")
                        }
                    },
                    modifier = Modifier.testTag("mode_trace")
                )
                FilterChip(
                    selected = dragMode == 1,
                    onClick = { viewModel.setDragMode(1) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OpenWith, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Pan Viewport")
                        }
                    },
                    modifier = Modifier.testTag("mode_pan")
                )
            }
        }

        // Interactive Graph Canvas Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .testTag("graph_canvas_card"),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val lineColor = MaterialTheme.colorScheme.primary
                    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

                    val evaluator = remember { MathExpressionEvaluator() }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface)
                            .pointerInput(formula, zoomLevel, panOffsetX, panOffsetY) {
                                detectTapGestures { offset ->
                                    val width = size.width
                                    val height = size.height
                                    val centerX = width / 2f + panOffsetX
                                    val centerY = height / 2f + panOffsetY
                                    val scaleX = width / (2f * zoomLevel)
                                    val scaleY = height / (2f * zoomLevel)

                                    // map touch cursor X to math space
                                    val xMath = (offset.x - centerX) / scaleX
                                    val yMath = evaluator.evaluate(formula, xMath.toDouble()).toFloat()

                                    if (yMath.isFinite() && !yMath.isNaN()) {
                                        viewModel.setGraphPoint(Pair(xMath, yMath))
                                    }
                                }
                            }
                            .pointerInput(formula, zoomLevel, dragMode, panOffsetX, panOffsetY) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    if (dragMode == 1) {
                                        viewModel.pan(dragAmount.x, dragAmount.y)
                                    } else {
                                        val width = size.width
                                        val height = size.height
                                        val centerX = width / 2f + panOffsetX
                                        val centerY = height / 2f + panOffsetY
                                        val scaleX = width / (2f * zoomLevel)
                                        val scaleY = height / (2f * zoomLevel)

                                        val xMath = (change.position.x - centerX) / scaleX
                                        val yMath = evaluator.evaluate(formula, xMath.toDouble()).toFloat()

                                        if (yMath.isFinite() && !yMath.isNaN()) {
                                            viewModel.setGraphPoint(Pair(xMath, yMath))
                                        }
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val centerX = width / 2f + panOffsetX
                        val centerY = height / 2f + panOffsetY

                        val scaleX = width / (2f * zoomLevel)
                        val scaleY = height / (2f * zoomLevel)

                        // 1. Draw Grid Lines & scale ticks
                        val gridSpacing = if (zoomLevel <= 10f) 1f else if (zoomLevel <= 30f) 5f else 10f

                        // Draw X ticks and grid vertical lines
                        var xMark = -zoomLevel + (zoomLevel % gridSpacing)
                        while (xMark <= zoomLevel) {
                            val pixelX = centerX + xMark * scaleX
                            // Draw Grid Line
                            drawLine(
                                color = gridColor,
                                start = Offset(pixelX, 0f),
                                end = Offset(pixelX, height),
                                strokeWidth = 1f
                            )
                            xMark += gridSpacing
                        }

                        // Draw Y ticks and grid horizontal lines
                        var yMark = -zoomLevel + (zoomLevel % gridSpacing)
                        while (yMark <= zoomLevel) {
                            val pixelY = centerY - yMark * scaleY
                            // Draw Grid Line
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, pixelY),
                                end = Offset(width, pixelY),
                                strokeWidth = 1f
                            )
                            yMark += gridSpacing
                        }

                        // 2. Draw Principal Axes
                        // X axis
                        drawLine(
                            color = axisColor,
                            start = Offset(0f, centerY),
                            end = Offset(width, centerY),
                            strokeWidth = 2.5f
                        )
                        // Y axis
                        drawLine(
                            color = axisColor,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, height),
                            strokeWidth = 2.5f
                        )

                        // 3. Evaluate and plot math formula path
                        val path = Path()
                        var first = true
                        val steps = 200
                        val stepSize = (2f * zoomLevel) / steps

                        for (i in 0..steps) {
                            val xMath = -zoomLevel + i * stepSize
                            val yMath = evaluator.evaluate(formula, xMath.toDouble()).toFloat()

                            if (yMath.isFinite() && !yMath.isNaN()) {
                                val pixelX = centerX + xMath * scaleX
                                val pixelY = centerY - yMath * scaleY

                                // clips inside a relative range for fluid bezier mapping
                                if (pixelX.isFinite() && pixelY.isFinite() && pixelY >= -500f && pixelY <= height + 500f) {
                                    if (first) {
                                        path.moveTo(pixelX, pixelY)
                                        first = false
                                    } else {
                                        path.lineTo(pixelX, pixelY)
                                    }
                                }
                            }
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 4f, cap = StrokeCap.Round)
                        )

                        // 4. Render interactive cursor traced highlight dot
                        trackedPoint?.let { (xT, yT) ->
                            val dotX = centerX + xT * scaleX
                            val dotY = centerY - yT * scaleY

                            if (dotX in 0f..width && dotY in 0f..height) {
                                // Draw horizontal & vertical reference tracking crosshairs lines
                                drawLine(
                                    color = lineColor.copy(alpha = 0.4f),
                                    start = Offset(dotX, 0f),
                                    end = Offset(dotX, height),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                                drawLine(
                                    color = lineColor.copy(alpha = 0.4f),
                                    start = Offset(0f, dotY),
                                    end = Offset(width, dotY),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )

                                // Draw anchor point circle
                                drawCircle(
                                    color = lineColor,
                                    radius = 7.dp.toPx(),
                                    center = Offset(dotX, dotY)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 3.dp.toPx(),
                                    center = Offset(dotX, dotY)
                                )
                            }
                        }
                    }

                    // On-Canvas UI Overlays: Floating Zoom Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.setZoom(zoomLevel - 3f) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_zoom_in"),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                        }
                        FloatingActionButton(
                            onClick = { viewModel.setZoom(zoomLevel + 5f) },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_zoom_out"),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                        }
                        FloatingActionButton(
                            onClick = {
                                viewModel.setZoom(10f)
                                viewModel.setGraphPoint(null)
                                viewModel.resetPan()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("btn_zoom_reset"),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Zoom", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Coordinate Tracing Label Floating Panel
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "f(x) = ${formula.replace("y=", "").replace("f(x)=", "")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(2.dp))
                            trackedPoint?.let { (xT, yT) ->
                                Text(
                                    text = "x: ${String.format("%.3f", xT)}\ny: ${String.format("%.3f", yT)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } ?: Text(
                                text = "Tap canvas to trace",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Scale bounds overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Bounds: ±${String.format("%.0f", zoomLevel)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// ----------------- 3. STATISTICS SCREEN -----------------

@Composable
fun StatisticsScreen(viewModel: MathSolverViewModel) {
    val inputX by viewModel.statsInput.collectAsStateWithLifecycle()
    val inputY by viewModel.statsYInput.collectAsStateWithLifecycle()
    val statsX by viewModel.statsResult.collectAsStateWithLifecycle()
    val statsY by viewModel.statsYResult.collectAsStateWithLifecycle()
    val regression by viewModel.regressionResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedSubTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Unified Analytic & Regression Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Input X and Y variables to view detailed descriptive statistics and perform simple linear regression and trendline analysis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Dual Dataset Inputs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = inputX,
                    onValueChange = { viewModel.updateStatsInput(it) },
                    label = { Text("Independent Variable (X Dataset)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_input_field"),
                    placeholder = { Text("e.g. 1, 2, 3, 4, 5, 6, 7") },
                    trailingIcon = {
                        if (inputX.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateStatsInput("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear X")
                            }
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = inputY,
                    onValueChange = { viewModel.updateStatsYInput(it) },
                    label = { Text("Dependent Variable (Y Dataset)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_y_input_field"),
                    placeholder = { Text("e.g. 2.1, 3.9, 6.1, 8.0, 9.9, 12.1, 14.2") },
                    trailingIcon = {
                        if (inputY.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateStatsYInput("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear Y")
                            }
                        }
                    },
                    singleLine = true
                )
            }
        }

        // Subnavigation control chips for clean segmenting
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = { Text("X Metrics") },
                    modifier = Modifier.testTag("tab_stats_x")
                )
                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = { Text("Y Metrics") },
                    enabled = statsY != null,
                    modifier = Modifier.testTag("tab_stats_y")
                )
                FilterChip(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    label = { Text("Regression Model") },
                    enabled = regression != null,
                    modifier = Modifier.testTag("tab_regression")
                )
            }
        }

        if (selectedSubTab == 0) {
            statsX?.let { s ->
                item {
                    Text(
                        text = "Arithmetic Metrics: X (${s.count} values)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Arithmetic Mean",
                                value = String.format("%.4f", s.mean),
                                description = "Central mean value",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Central Median",
                                value = String.format("%.2f", s.median),
                                description = "Middle value",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Sample SD (s)",
                                value = String.format("%.4f", s.sampleStandardDeviation),
                                description = "Spread dispersion",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Bounds Range",
                                value = String.format("%.2f", s.range),
                                description = "Min: ${s.min} | Max: ${s.max}",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Sample Variance (s²)",
                                value = String.format("%.4f", s.sampleVariance),
                                description = "Variance squared",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Sum total",
                                value = String.format("%.2f", s.sum),
                                description = "Cumulative sum",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "X Data Distribution Graph",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                .padding(16.dp)
                        ) {
                            val barColor = MaterialTheme.colorScheme.primary
                            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                val maxVal = s.sortedData.maxOrNull() ?: 1.0
                                val minVal = s.sortedData.minOrNull() ?: 0.0
                                val rangeVal = if (maxVal == minVal) 1.0 else (maxVal - minVal)

                                val paddingX = 8f
                                val drawableWidth = w - paddingX * 2
                                val barCount = s.sortedData.size
                                val barWidth = (drawableWidth / barCount) * 0.82f
                                val barSpacing = (drawableWidth / barCount) * 0.18f

                                s.sortedData.forEachIndexed { idx, valItem ->
                                    val rawRatio = if (maxVal == 0.0) 0.0 else (valItem / maxVal)
                                    val normalisedHeight = (rawRatio * h * 0.8f).toFloat()

                                    val startX = paddingX + idx * (barWidth + barSpacing)
                                    val startY = h - normalisedHeight

                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(startX, startY),
                                        size = androidx.compose.ui.geometry.Size(barWidth, normalisedHeight),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }

                                drawLine(
                                    color = labelColor.copy(alpha = 0.3f),
                                    start = Offset(0f, h),
                                    end = Offset(w, h),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sorted Dataset (X):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = s.sortedData.joinToString(", "),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } ?: item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Please enter valid numbers for dataset X.")
                }
            }
        }

        if (selectedSubTab == 1) {
            statsY?.let { s ->
                item {
                    Text(
                        text = "Arithmetic Metrics: Y (${s.count} values)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Arithmetic Mean",
                                value = String.format("%.4f", s.mean),
                                description = "Central mean value",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Central Median",
                                value = String.format("%.2f", s.median),
                                description = "Middle value",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Sample SD (s)",
                                value = String.format("%.4f", s.sampleStandardDeviation),
                                description = "Spread dispersion",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Bounds Range",
                                value = String.format("%.2f", s.range),
                                description = "Min: ${s.min} | Max: ${s.max}",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricCard(
                                label = "Sample Variance (s²)",
                                value = String.format("%.4f", s.sampleVariance),
                                description = "Variance squared",
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                label = "Sum total",
                                value = String.format("%.2f", s.sum),
                                description = "Cumulative sum",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Y Data Distribution Graph",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                .padding(16.dp)
                        ) {
                            val barColor = MaterialTheme.colorScheme.primary
                            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                val maxVal = s.sortedData.maxOrNull() ?: 1.0
                                val minVal = s.sortedData.minOrNull() ?: 0.0
                                val rangeVal = if (maxVal == minVal) 1.0 else (maxVal - minVal)

                                val paddingX = 8f
                                val drawableWidth = w - paddingX * 2
                                val barCount = s.sortedData.size
                                val barWidth = (drawableWidth / barCount) * 0.82f
                                val barSpacing = (drawableWidth / barCount) * 0.18f

                                s.sortedData.forEachIndexed { idx, valItem ->
                                    val rawRatio = if (maxVal == 0.0) 0.0 else (valItem / maxVal)
                                    val normalisedHeight = (rawRatio * h * 0.8f).toFloat()

                                    val startX = paddingX + idx * (barWidth + barSpacing)
                                    val startY = h - normalisedHeight

                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(startX, startY),
                                        size = androidx.compose.ui.geometry.Size(barWidth, normalisedHeight),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }

                                drawLine(
                                    color = labelColor.copy(alpha = 0.3f),
                                    start = Offset(0f, h),
                                    end = Offset(w, h),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sorted Dataset (Y):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = s.sortedData.joinToString(", "),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 2) {
            regression?.let { r ->
                item {
                    Text(
                        text = "Trend & Linear Fit Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Best Fit Regression Model:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = r.formula,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                            Spacer(Modifier.height(10.dp))

                            val correlationDescription = when {
                                r.r >= 0.8f -> "Strong Positive correlation"
                                r.r <= -0.8f -> "Strong Negative correlation"
                                r.r >= 0.5f -> "Moderate Positive correlation"
                                r.r <= -0.5f -> "Moderate Negative correlation"
                                r.r >= 0.2f -> "Weak Positive correlation"
                                r.r <= -0.2f -> "Weak Negative correlation"
                                else -> "Essentially no linear correlation"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Pearson Coefficient (r)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("%.4f", r.r),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = correlationDescription,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Coeff of Determination (R²)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("%.4f", r.rSquared),
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = String.format("%.1f%% variance described", r.rSquared * 100),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Interactive-looking canvas scatter with trend fit line
                item {
                    Text(
                        text = "Bivariate Scatter Plot & Trendline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            val trendColor = MaterialTheme.colorScheme.primary
                            val pointColor = Color(0xFF7D5260)
                            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val margin = 40f

                                val pairs = r.pairs
                                val xs = pairs.map { it.first }
                                val ys = pairs.map { it.second }

                                val minX = xs.minOrNull() ?: 0.0
                                val maxX = xs.maxOrNull() ?: 1.0
                                val minY = ys.minOrNull() ?: 0.0
                                val maxY = ys.maxOrNull() ?: 1.0

                                val deltaX = if (maxX == minX) 1.0 else (maxX - minX)
                                val deltaY = if (maxY == minY) 1.0 else (maxY - minY)

                                fun mapPoint(xVal: Double, yVal: Double): Offset {
                                    val px = margin + ((xVal - minX) / deltaX) * (w - 2 * margin)
                                    val py = (h - margin) - ((yVal - minY) / deltaY) * (h - 2 * margin)
                                    return Offset(px.toFloat(), py.toFloat())
                                }

                                // Draw plot axes bounds
                                drawRect(
                                    color = gridColor,
                                    topLeft = Offset(margin, margin),
                                    size = androidx.compose.ui.geometry.Size(w - 2 * margin, h - 2 * margin),
                                    style = Stroke(width = 2f)
                                )

                                // Draw calculated linear trend line
                                val yStart = r.slope * minX + r.intercept
                                val yEnd = r.slope * maxX + r.intercept

                                val lineStart = mapPoint(minX, yStart)
                                val lineEnd = mapPoint(maxX, yEnd)

                                drawLine(
                                    color = trendColor,
                                    start = lineStart,
                                    end = lineEnd,
                                    strokeWidth = 6f
                                )

                                // Draw scatter data coordinate points
                                pairs.forEach { (px, py) ->
                                    val center = mapPoint(px, py)
                                    drawCircle(
                                        color = pointColor,
                                        radius = 12f,
                                        center = center
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5f,
                                        center = center
                                    )
                                }
                            }
                        }
                    }
                }

                // Fitted predicted tables listing
                item {
                    Text(
                        text = "Fitted Predictions Table:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("X", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                                Text("Observed Y", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1.5f))
                                Text("Predicted Ŷ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1.5f))
                                Text("Residual", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1.2f))
                            }
                            Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            r.pairs.forEach { (xVal, yVal) ->
                                val predictedY = r.slope * xVal + r.intercept
                                val residual = yVal - predictedY

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(String.format("%.2f", xVal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                    Text(String.format("%.2f", yVal), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                    Text(String.format("%.2f", predictedY), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                                    Text(String.format("%+.2f", residual), fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1.2f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


// ----------------- 4. HISTORY SCREEN -----------------

@Composable
fun HistoryScreen(viewModel: MathSolverViewModel) {
    val history by viewModel.historyList.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Math Solver History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllHistory() },
                    modifier = Modifier.testTag("btn_clear_history")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear All")
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "No mathematical history yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("history_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onLoad = {
                            viewModel.updateSolverInput(item.question)
                            viewModel.setTab(0)
                            Toast.makeText(context, "Loaded into Solver", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.deleteHistoryItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    item: HistoryItem,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("history_item_${item.id}"),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.category.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onLoad,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Load into Solver",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete item",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = item.question,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Animated expansion for Step-By-Step Answer
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cached Solution:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.answer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!expanded) {
                Text(
                    text = "Tap to view cached steps...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}
