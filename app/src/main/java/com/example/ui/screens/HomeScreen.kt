package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarCatalog
import com.example.model.TrackCatalog
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val cars by viewModel.cars.collectAsState()
    val selectedTrackId by viewModel.selectedTrackId.collectAsState()

    val currentCarModel = CarCatalog.getCar(profile.selectedCarId)
    val currentCarStatus = cars.find { it.carId == profile.selectedCarId }
    val currentTrack = TrackCatalog.getTrack(selectedTrackId)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isTablet = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = if (isTablet) 32.dp else 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Bar: Title, Coins, Trophies, Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EX DRIVER",
                        fontSize = if (isTablet) 32.sp else 26.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "ARCADE 3D RACING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coins Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = NeonGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${profile.coins}",
                                color = NeonGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Trophies Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophies",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${profile.trophies}",
                                color = NeonCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkSurface, CircleShape)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Responsive Content (Side-by-side on tablet, stacked on phone)
            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        CarShowcaseCard(
                            carModel = currentCarModel,
                            carStatus = currentCarStatus,
                            onTuneClick = { viewModel.navigateTo(AppScreen.GARAGE) }
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TrackPreviewCard(
                            track = currentTrack,
                            onSelectTrack = { viewModel.navigateTo(AppScreen.TRACK_SELECT) }
                        )
                        MainActionButtons(
                            onRaceClick = { viewModel.startRace() },
                            onGarageClick = { viewModel.navigateTo(AppScreen.GARAGE) },
                            onTracksClick = { viewModel.navigateTo(AppScreen.TRACK_SELECT) }
                        )
                    }
                }
            } else {
                CarShowcaseCard(
                    carModel = currentCarModel,
                    carStatus = currentCarStatus,
                    onTuneClick = { viewModel.navigateTo(AppScreen.GARAGE) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TrackPreviewCard(
                    track = currentTrack,
                    onSelectTrack = { viewModel.navigateTo(AppScreen.TRACK_SELECT) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                MainActionButtons(
                    onRaceClick = { viewModel.startRace() },
                    onGarageClick = { viewModel.navigateTo(AppScreen.GARAGE) },
                    onTracksClick = { viewModel.navigateTo(AppScreen.TRACK_SELECT) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CarShowcaseCard(
    carModel: com.example.model.CarModel,
    carStatus: com.example.data.CarStatus?,
    onTuneClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.5.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = carModel.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = carModel.subtitle,
                        fontSize = 13.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonPink.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink),
                    modifier = Modifier.clickable { onTuneClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Tune",
                            tint = NeonPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "GARAGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPink
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3D Car Render Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF0F1420), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                val paintColor = carStatus?.paintColor ?: carModel.defaultPaint
                val neonColor = carStatus?.neonColor ?: carModel.defaultNeon

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height * 0.65f
                    val w = 180f
                    val h = 85f

                    // Neon Underglow
                    if (neonColor != 0L) {
                        drawOval(
                            color = Color(neonColor).copy(alpha = 0.45f),
                            topLeft = Offset(cx - w * 0.7f, cy - 18f),
                            size = Size(w * 1.4f, 40f)
                        )
                    }

                    // Shadow
                    drawOval(
                        color = Color.Black.copy(alpha = 0.6f),
                        topLeft = Offset(cx - w * 0.6f, cy - 8f),
                        size = Size(w * 1.2f, 25f)
                    )

                    // Tires
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx - w * 0.5f, cy - 35f),
                        size = Size(24f, 45f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(cx + w * 0.5f - 24f, cy - 35f),
                        size = Size(24f, 45f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )

                    // Chassis Body
                    val body = Path().apply {
                        moveTo(cx - w * 0.45f, cy)
                        lineTo(cx + w * 0.45f, cy)
                        lineTo(cx + w * 0.48f, cy - h * 0.5f)
                        lineTo(cx + w * 0.36f, cy - h * 0.95f)
                        lineTo(cx - w * 0.36f, cy - h * 0.95f)
                        lineTo(cx - w * 0.48f, cy - h * 0.5f)
                        close()
                    }
                    drawPath(body, color = Color(paintColor))

                    // Windshield
                    val glass = Path().apply {
                        moveTo(cx - w * 0.28f, cy - h * 0.45f)
                        lineTo(cx + w * 0.28f, cy - h * 0.45f)
                        lineTo(cx + w * 0.22f, cy - h * 0.88f)
                        lineTo(cx - w * 0.22f, cy - h * 0.88f)
                        close()
                    }
                    drawPath(glass, color = Color(0xFF0F172A))

                    // Spoiler
                    drawRoundRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(cx - w * 0.45f, cy - h * 1.05f),
                        size = Size(w * 0.9f, 10f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )

                    // Taillights
                    drawRoundRect(
                        color = Color(0xFFFF1744),
                        topLeft = Offset(cx - w * 0.4f, cy - h * 0.35f),
                        size = Size(32f, 8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = Color(0xFFFF1744),
                        topLeft = Offset(cx + w * 0.4f - 32f, cy - h * 0.35f),
                        size = Size(32f, 8f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat bars
            val speedLvl = carStatus?.speedLevel ?: 1
            val handlingLvl = carStatus?.handlingLevel ?: 1
            val topSpeedKmh = (carModel.baseTopSpeed + (speedLvl - 1) * 12f).toInt()

            StatRow(label = "TOP SPEED", value = "$topSpeedKmh KM/H", progress = speedLvl / 5f, color = NeonGold)
            Spacer(modifier = Modifier.height(8.dp))
            StatRow(label = "HANDLING", value = "LVL $handlingLvl", progress = handlingLvl / 5f, color = NeonCyan)
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    progress: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DarkSurfaceVariant
        )
    }
}

@Composable
private fun TrackPreviewCard(
    track: com.example.model.TrackData,
    onSelectTrack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .clickable { onSelectTrack() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(track.theme.skyColorBottom),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Track",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "SELECTED TRACK",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = track.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${track.lengthMeters.toInt()}m • ${track.difficulty}",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceVariant
            ) {
                Text(
                    text = "CHANGE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MainActionButtons(
    onRaceClick: () -> Unit,
    onGarageClick: () -> Unit,
    onTracksClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // RACE NOW (Primary)
        Button(
            onClick = onRaceClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .shadow(16.dp, RoundedCornerShape(16.dp))
                .testTag("race_now_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RACE NOW",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onGarageClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("garage_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Garage",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GARAGE",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Button(
                onClick = onTracksClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .border(1.dp, NeonGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("tracks_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Tracks",
                    tint = NeonGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TRACKS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGold
                )
            }
        }
    }
}
