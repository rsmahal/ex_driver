package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CarCatalog
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonLime
import com.example.ui.theme.NeonPink

@Composable
fun GarageScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val profile by viewModel.profile.collectAsState()
    val cars by viewModel.cars.collectAsState()

    var selectedIndex by remember {
        val initialIdx = CarCatalog.cars.indexOfFirst { it.id == profile.selectedCarId }
        mutableIntStateOf(if (initialIdx >= 0) initialIdx else 0)
    }

    val currentModel = CarCatalog.cars[selectedIndex]
    val currentStatus = cars.find { it.carId == currentModel.id }
    val isUnlocked = currentStatus?.isUnlocked == true
    val isSelected = profile.selectedCarId == currentModel.id

    val paintColor = currentStatus?.paintColor ?: currentModel.defaultPaint
    val neonColor = currentStatus?.neonColor ?: currentModel.defaultNeon

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
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkSurface, CircleShape)
                            .testTag("garage_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "GARAGE & TUNING",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                // Coins
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Car Showcase Viewer with Left/Right Selectors
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, DarkCardBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedIndex > 0) selectedIndex--
                                else selectedIndex = CarCatalog.cars.size - 1
                            },
                            modifier = Modifier.background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Car",
                                tint = Color.White
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentModel.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = currentModel.subtitle,
                                fontSize = 13.sp,
                                color = NeonCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = {
                                if (selectedIndex < CarCatalog.cars.size - 1) selectedIndex++
                                else selectedIndex = 0
                            },
                            modifier = Modifier.background(DarkSurfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Car",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Car Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Color(0xFF0F1420), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height * 0.65f
                            val w = 210f
                            val h = 100f

                            if (neonColor != 0L) {
                                drawOval(
                                    color = Color(neonColor).copy(alpha = 0.5f),
                                    topLeft = Offset(cx - w * 0.7f, cy - 20f),
                                    size = Size(w * 1.4f, 48f)
                                )
                            }

                            drawOval(
                                color = Color.Black.copy(alpha = 0.6f),
                                topLeft = Offset(cx - w * 0.6f, cy - 10f),
                                size = Size(w * 1.2f, 30f)
                            )

                            // Tires
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(cx - w * 0.5f, cy - 40f),
                                size = Size(26f, 50f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                            drawRoundRect(
                                color = Color(0xFF1E293B),
                                topLeft = Offset(cx + w * 0.5f - 26f, cy - 40f),
                                size = Size(26f, 50f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )

                            // Body
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

                            // Cockpit
                            val glass = Path().apply {
                                moveTo(cx - w * 0.28f, cy - h * 0.45f)
                                lineTo(cx + w * 0.28f, cy - h * 0.45f)
                                lineTo(cx + w * 0.22f, cy - h * 0.88f)
                                lineTo(cx - w * 0.22f, cy - h * 0.88f)
                                close()
                            }
                            drawPath(glass, color = Color(0xFF0F172A))

                            // Wing
                            drawRoundRect(
                                color = Color(0xFF0F172A),
                                topLeft = Offset(cx - w * 0.45f, cy - h * 1.05f),
                                size = Size(w * 0.9f, 12f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                            )

                            // Taillights
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(cx - w * 0.4f, cy - h * 0.35f),
                                size = Size(36f, 9f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                            )
                            drawRoundRect(
                                color = Color(0xFFFF1744),
                                topLeft = Offset(cx + w * 0.4f - 36f, cy - h * 0.35f),
                                size = Size(36f, 9f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Unlock / Select Button
                    if (!isUnlocked) {
                        Button(
                            onClick = { viewModel.purchaseCar(currentModel.id) },
                            enabled = profile.coins >= currentModel.unlockPrice,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_car_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Unlock",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UNLOCK FOR ${currentModel.unlockPrice} COINS",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else if (isSelected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonLime.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonLime),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = NeonLime,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CURRENT RACER",
                                        color = NeonLime,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { viewModel.selectCar(currentModel.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("select_car_button")
                        ) {
                            Text(
                                text = "EQUIP CAR",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Upgrades Section (Only if car is unlocked)
            if (currentStatus != null && currentStatus.isUnlocked) {
                Text(
                    text = "PERFORMANCE UPGRADES",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                UpgradeStatCard(
                    title = "TOP SPEED (ENGINE)",
                    level = currentStatus.speedLevel,
                    icon = Icons.Default.Speed,
                    tint = NeonGold,
                    playerCoins = profile.coins,
                    onUpgrade = { viewModel.upgradeStat(currentModel.id, "speed") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                UpgradeStatCard(
                    title = "HANDLING (STEERING)",
                    level = currentStatus.handlingLevel,
                    icon = Icons.Default.TurnRight,
                    tint = NeonCyan,
                    playerCoins = profile.coins,
                    onUpgrade = { viewModel.upgradeStat(currentModel.id, "handling") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                UpgradeStatCard(
                    title = "ARMOR (RAMMING POWER)",
                    level = currentStatus.armorLevel,
                    icon = Icons.Default.Shield,
                    tint = NeonLime,
                    playerCoins = profile.coins,
                    onUpgrade = { viewModel.upgradeStat(currentModel.id, "armor") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                UpgradeStatCard(
                    title = "NITRO (TURBO BOOST)",
                    level = currentStatus.nitroLevel,
                    icon = Icons.Default.ElectricBolt,
                    tint = NeonPink,
                    playerCoins = profile.coins,
                    onUpgrade = { viewModel.upgradeStat(currentModel.id, "nitro") }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Paint Color Palette
                Text(
                    text = "CUSTOM PAINT COLORS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CarCatalog.paintPalette) { colorVal ->
                        val isColorSelected = currentStatus.paintColor == colorVal
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setPaintColor(currentModel.id, colorVal)
                                }
                        ) {
                            if (isColorSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (colorVal == 0xFFFFFFFFL) Color.Black else Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Custom Neon Underglow Palette
                Text(
                    text = "NEON UNDERGLOW",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CarCatalog.neonPalette) { neonVal ->
                        val isNeonSelected = currentStatus.neonColor == neonVal
                        val displayColor = if (neonVal == 0L) Color(0xFF222222) else Color(neonVal)

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(displayColor)
                                .border(
                                    width = if (isNeonSelected) 3.dp else 1.dp,
                                    color = if (isNeonSelected) Color.White else Color(0x44FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setNeonColor(currentModel.id, neonVal)
                                }
                        ) {
                            if (isNeonSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun UpgradeStatCard(
    title: String,
    level: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    playerCoins: Int,
    onUpgrade: () -> Unit
) {
    val isMax = level >= 5
    val cost = CarCatalog.getUpgradeCost(level)
    val canAfford = playerCoins >= cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tint.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Level pip indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..5) {
                            Box(
                                modifier = Modifier
                                    .size(width = 16.dp, height = 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (i <= level) tint else DarkSurfaceVariant)
                            )
                        }
                    }
                }
            }

            if (isMax) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant
                ) {
                    Text(
                        text = "MAX",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            } else {
                Button(
                    onClick = onUpgrade,
                    enabled = canAfford,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tint,
                        disabledContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text(
                        text = "$cost COINS",
                        color = if (canAfford) Color.Black else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
