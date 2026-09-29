package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.game.RacingCanvasRenderer
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink

@Composable
fun RaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val engine by viewModel.currentGameEngine.collectAsState()
    var showExitDialog by remember { mutableStateOf(false) }
    var activeEventText by remember { mutableStateOf<String?>(null) }

    BackHandler {
        showExitDialog = true
    }

    if (engine == null) {
        LaunchedEffect(Unit) {
            viewModel.navigateTo(AppScreen.HOME)
        }
        return
    }

    // 60 FPS Game Loop
    LaunchedEffect(engine) {
        var lastNanoTime = System.nanoTime()
        while (true) {
            withFrameNanos { currentNanoTime ->
                val dt = (currentNanoTime - lastNanoTime) / 1_000_000_000f
                lastNanoTime = currentNanoTime
                viewModel.updateGameLoop(dt)
            }
        }
    }

    val currentEngine = engine ?: return
    val player = currentEngine.player
    val playerSpeedKmh = (player.speed * 3.6f).toInt()
    val progress = (player.distance / currentEngine.track.lengthMeters).coerceIn(0f, 1f)
    val rankText = when (player.rank) {
        1 -> "1ST"
        2 -> "2ND"
        3 -> "3RD"
        4 -> "4TH"
        5 -> "5TH"
        else -> "6TH"
    }

    val rankColor = when (player.rank) {
        1 -> NeonGold
        2 -> Color(0xFFE0E0E0)
        3 -> Color(0xFFCD7F32)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    // Drag anywhere to steer: normalized across screen width
                    val deltaNormalized = dragAmount.x / size.width
                    viewModel.onSteerInput(deltaNormalized)
                }
            }
            .testTag("race_screen_container")
    ) {
        // 1. The 3D Racing Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            RacingCanvasRenderer.drawRaceScene(
                drawScope = this,
                engine = currentEngine,
                screenWidth = size.width,
                screenHeight = size.height
            )
        }

        // 2. Race Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xCC0B0F19),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, rankColor),
                modifier = Modifier.shadow(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = rankText,
                        color = rankColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Progress Meter
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentEngine.track.name.uppercase(),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NeonCyan,
                    trackColor = Color(0x66333333)
                )
            }

            // Coin counter & Pause button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xCC0B0F19),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coins",
                            tint = NeonGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+${currentEngine.coinsEarnedThisRace}",
                            color = NeonGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showExitDialog = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xCC0B0F19), CircleShape)
                        .testTag("exit_race_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Race",
                        tint = Color.White
                    )
                }
            }
        }

        // 3. Speedometer & Drafting indicator (Bottom Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (player.draftMultiplier > 1.05f) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xCC00E5FF),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "SLIPSTREAM DRAFT!",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xAA0B0F19),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (player.nitroTimeRemaining > 0f) NeonCyan else Color(0x44FFFFFF)
                ),
                modifier = Modifier.shadow(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$playerSpeedKmh",
                        color = if (player.nitroTimeRemaining > 0f) NeonCyan else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "KM/H",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "SWIPE SCREEN TO STEER",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }

        // 4. Nitro Boost Action Button (Bottom Right)
        val canBoost = player.nitroTimeRemaining <= 0f && currentEngine.countdownTimer <= 0f
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 28.dp)
        ) {
            Button(
                onClick = { viewModel.triggerNitro() },
                enabled = canBoost,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (player.nitroTimeRemaining > 0f) NeonCyan else NeonPink,
                    disabledContainerColor = Color(0x55333333)
                ),
                modifier = Modifier
                    .size(68.dp)
                    .shadow(12.dp, CircleShape)
                    .testTag("nitro_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = "Nitro Boost",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 5. Countdown Overlay ("3", "2", "1", "GO!")
        if (currentEngine.countdownTimer > 0f) {
            val countdownNumber = currentEngine.countdownTimer.toInt()
            val displayText = when {
                countdownNumber >= 3 -> "3"
                countdownNumber == 2 -> "2"
                countdownNumber == 1 -> "1"
                else -> "GO!"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = displayText,
                        color = if (displayText == "GO!") NeonLime else NeonCyan,
                        fontSize = 90.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "GET READY!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Exit confirmation dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Quit Race?") },
                text = { Text("Are you sure you want to return to the main menu?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text("Quit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Resume")
                    }
                }
            )
        }
    }
}

val NeonLime = Color(0xFF76FF03)
