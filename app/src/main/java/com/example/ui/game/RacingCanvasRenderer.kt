package com.example.ui.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.game.ActiveObstacle
import com.example.game.GameEngine
import com.example.game.RacerState
import com.example.model.HorizonStyle
import com.example.model.ObstacleKind
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object RacingCanvasRenderer {

    fun drawRaceScene(
        drawScope: DrawScope,
        engine: GameEngine,
        screenWidth: Float,
        screenHeight: Float
    ) {
        val player = engine.player
        val track = engine.track
        val theme = track.theme

        val horizonY = screenHeight * 0.40f
        val centerX = screenWidth * 0.5f

        // 1. Calculate camera and curve shift
        val cameraZ = player.distance - 6.5f
        val cameraY = 2.6f + (player.jumpHeight * 0.35f)

        // Find active curve
        var curveSum = 0f
        for (seg in track.curveProfile) {
            if (player.distance in seg.startDistance..seg.endDistance) {
                curveSum = seg.curveIntensity
                break
            }
        }
        val horizonCurveX = centerX + (curveSum * screenWidth * 0.28f)

        // 2. Draw Sky Background
        drawSky(drawScope, theme.skyColorTop, theme.skyColorBottom, screenWidth, horizonY)

        // 3. Draw Horizon Elements (City, Mountains, Toys, etc.)
        drawHorizonScenery(drawScope, theme.horizonElementKind, screenWidth, horizonY, curveSum, player.distance)

        // 4. Draw Ground / Road Grass / Borders
        drawGroundPlane(drawScope, theme.roadColorDark, screenWidth, screenHeight, horizonY)

        // 5. Draw Road Segments (Stripes for speed sensation)
        drawRoad(
            drawScope = drawScope,
            engine = engine,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            horizonY = horizonY,
            centerX = centerX,
            cameraZ = cameraZ,
            cameraY = cameraY,
            curveSum = curveSum
        )

        // 6. Draw Finish Line Arch if near end
        if (track.lengthMeters - cameraZ < 180f) {
            drawFinishLineArch(
                drawScope = drawScope,
                finishDist = track.lengthMeters,
                cameraZ = cameraZ,
                cameraY = cameraY,
                centerX = centerX,
                horizonY = horizonY,
                screenHeight = screenHeight
            )
        }

        // 7. Depth-sorted draw list for AI cars and Obstacles
        val renderableItems = mutableListOf<RenderableEntity>()

        // Add Active Obstacles ahead of camera
        for (obs in engine.activeObstacles) {
            val dZ = obs.currentDistance - cameraZ
            if (dZ in 1.2f..130f) {
                renderableItems.add(
                    RenderableEntity(
                        distance = obs.currentDistance,
                        dZ = dZ,
                        isCar = false,
                        obstacle = obs
                    )
                )
            }
        }

        // Add AI Racers
        for (ai in engine.aiRacers) {
            val dZ = ai.distance - cameraZ
            if (dZ in 1.0f..130f) {
                renderableItems.add(
                    RenderableEntity(
                        distance = ai.distance,
                        dZ = dZ,
                        isCar = true,
                        racer = ai
                    )
                )
            }
        }

        // Sort descending by distance so furthest objects are drawn first
        renderableItems.sortByDescending { it.distance }

        for (item in renderableItems) {
            val scale = (14f / item.dZ).coerceIn(0.04f, 2.5f)
            val projY = horizonY + (cameraY * scale * screenHeight * 0.32f)
            val roadHalfWidth = (screenWidth * 0.46f) * scale
            val projX = centerX + (curveSum * (1f - (item.dZ / 130f)) * 120f) + (item.lateralPos * roadHalfWidth)

            if (projY in horizonY..(screenHeight + 100f)) {
                if (item.isCar && item.racer != null) {
                    drawCar(
                        drawScope = drawScope,
                        racer = item.racer,
                        screenX = projX,
                        screenY = projY,
                        scale = scale,
                        isPlayer = false
                    )
                } else if (item.obstacle != null) {
                    drawObstacle(
                        drawScope = drawScope,
                        obstacle = item.obstacle,
                        screenX = projX,
                        screenY = projY,
                        scale = scale
                    )
                }
            }
        }

        // 8. Draw Player Car (Always in foreground!)
        val playerScale = 1.35f
        val playerRoadHalfWidth = (screenWidth * 0.46f) * playerScale
        val playerScreenY = screenHeight * 0.82f - (player.jumpHeight * 80f)
        val playerScreenX = centerX + (player.lateralPos * playerRoadHalfWidth * 0.52f)

        drawCar(
            drawScope = drawScope,
            racer = player,
            screenX = playerScreenX,
            screenY = playerScreenY,
            scale = playerScale,
            isPlayer = true
        )

        // 9. Draw Particle FX (Nitro flames, tire smoke, sparks, coins)
        drawParticles(
            drawScope = drawScope,
            engine = engine,
            cameraZ = cameraZ,
            centerX = centerX,
            horizonY = horizonY,
            screenHeight = screenHeight,
            screenWidth = screenWidth
        )

        // 10. Speed Streaks when moving very fast or in Nitro
        val playerKmh = player.speed * 3.6f
        if (playerKmh > 180f || player.nitroTimeRemaining > 0f) {
            drawSpeedStreaks(drawScope, screenWidth, screenHeight, player.nitroTimeRemaining > 0f)
        }
    }

    private fun drawSky(
        drawScope: DrawScope,
        colorTop: Long,
        colorBottom: Long,
        width: Float,
        horizonY: Float
    ) {
        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(colorTop), Color(colorBottom)),
                startY = 0f,
                endY = horizonY
            ),
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY)
        )
    }

    private fun drawHorizonScenery(
        drawScope: DrawScope,
        style: HorizonStyle,
        width: Float,
        horizonY: Float,
        curve: Float,
        distance: Float
    ) {
        val scrollOffset = (distance * 0.08f) % 200f
        when (style) {
            HorizonStyle.CYBER_CITY -> {
                // Skyscraper silhouettes with neon windows
                var x = -scrollOffset
                val buildingWidth = 60f
                var i = 0
                while (x < width + buildingWidth) {
                    val bHeight = 80f + ((i * 37) % 110)
                    drawScope.drawRect(
                        color = Color(0xFF10132B),
                        topLeft = Offset(x, horizonY - bHeight),
                        size = Size(buildingWidth - 8f, bHeight)
                    )
                    // Neon billboard / window dots
                    if (i % 2 == 0) {
                        drawScope.drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 3.5f,
                            center = Offset(x + 20f, horizonY - bHeight + 25f)
                        )
                    }
                    x += buildingWidth
                    i++
                }
            }
            HorizonStyle.VOLCANO_PEAKS -> {
                // Jagged fiery peaks
                val path = Path()
                path.moveTo(0f, horizonY)
                var px = 0f
                var up = true
                while (px < width) {
                    val peakH = if (up) 120f else 40f
                    path.lineTo(px + 80f, horizonY - peakH)
                    px += 80f
                    up = !up
                }
                path.lineTo(width, horizonY)
                path.close()
                drawScope.drawPath(path, color = Color(0xFF3E120A))
            }
            HorizonStyle.TOY_CASTLE -> {
                // Pastel blocks & turrets
                var x = 0f
                while (x < width) {
                    drawScope.drawRect(
                        color = Color(0xFF42A5F5),
                        topLeft = Offset(x + 10f, horizonY - 60f),
                        size = Size(50f, 60f)
                    )
                    drawScope.drawCircle(
                        color = Color(0xFFFFCA28),
                        radius = 16f,
                        center = Offset(x + 35f, horizonY - 65f)
                    )
                    x += 90f
                }
            }
            HorizonStyle.SNOW_MOUNTAINS -> {
                val path = Path()
                path.moveTo(0f, horizonY)
                path.lineTo(width * 0.25f, horizonY - 140f)
                path.lineTo(width * 0.5f, horizonY - 60f)
                path.lineTo(width * 0.75f, horizonY - 150f)
                path.lineTo(width, horizonY)
                path.close()
                drawScope.drawPath(path, color = Color(0xFF1A365D))
            }
            HorizonStyle.DESERT_MESAS -> {
                // Flat-topped mesas
                var x = 0f
                while (x < width) {
                    drawScope.drawRect(
                        color = Color(0xFF795548),
                        topLeft = Offset(x, horizonY - 70f),
                        size = Size(110f, 70f)
                    )
                    x += 160f
                }
            }
            HorizonStyle.CANDY_HILLS -> {
                // Round pastel hills
                var x = 0f
                while (x < width) {
                    drawScope.drawCircle(
                        color = Color(0xFFF06292),
                        radius = 80f,
                        center = Offset(x + 50f, horizonY + 30f)
                    )
                    x += 120f
                }
            }
        }
    }

    private fun drawGroundPlane(
        drawScope: DrawScope,
        colorDark: Long,
        width: Float,
        height: Float,
        horizonY: Float
    ) {
        drawScope.drawRect(
            color = Color(colorDark),
            topLeft = Offset(0f, horizonY),
            size = Size(width, height - horizonY)
        )
    }

    private fun drawRoad(
        drawScope: DrawScope,
        engine: GameEngine,
        screenWidth: Float,
        screenHeight: Float,
        horizonY: Float,
        centerX: Float,
        cameraZ: Float,
        cameraY: Float,
        curveSum: Float
    ) {
        val theme = engine.track.theme
        val segLength = 4.0f // meters per stripe segment
        val maxSegments = 32

        val startSegIndex = (cameraZ / segLength).toInt()

        for (i in maxSegments downTo 1) {
            val segZ = (startSegIndex + i) * segLength
            val dZ = segZ - cameraZ
            if (dZ <= 0.8f) continue

            val scale = (14f / dZ).coerceIn(0.02f, 3.5f)
            val nextScale = (14f / (dZ + segLength)).coerceIn(0.02f, 3.5f)

            val yBottom = horizonY + (cameraY * scale * screenHeight * 0.32f)
            val yTop = horizonY + (cameraY * nextScale * screenHeight * 0.32f)

            if (yBottom < horizonY || yTop > screenHeight + 20f) continue

            val curveProgress = (1f - (dZ / (maxSegments * segLength))).coerceIn(0f, 1f)
            val curveOffset = curveSum * curveProgress * curveProgress * 150f

            val roadHalfWidthBottom = (screenWidth * 0.44f) * scale
            val roadHalfWidthTop = (screenWidth * 0.44f) * nextScale

            val xBottom = centerX + curveOffset
            val xTop = centerX + (curveSum * (curveProgress * 0.9f) * (curveProgress * 0.9f) * 150f)

            val isEven = (startSegIndex + i) % 2 == 0
            val roadColor = if (isEven) Color(theme.roadColorLight) else Color(theme.roadColorDark)
            val curbColor = if (isEven) Color(theme.curbColor1) else Color(theme.curbColor2)

            // Draw Road Surface Polygon
            val roadPath = Path().apply {
                moveTo(xBottom - roadHalfWidthBottom, yBottom)
                lineTo(xBottom + roadHalfWidthBottom, yBottom)
                lineTo(xTop + roadHalfWidthTop, yTop)
                lineTo(xTop - roadHalfWidthTop, yTop)
                close()
            }
            drawScope.drawPath(roadPath, color = roadColor)

            // Draw Curbs (Left and Right)
            val curbWidthBottom = roadHalfWidthBottom * 0.12f
            val curbWidthTop = roadHalfWidthTop * 0.12f

            // Left Curb
            val leftCurb = Path().apply {
                moveTo(xBottom - roadHalfWidthBottom - curbWidthBottom, yBottom)
                lineTo(xBottom - roadHalfWidthBottom, yBottom)
                lineTo(xTop - roadHalfWidthTop, yTop)
                lineTo(xTop - roadHalfWidthTop - curbWidthTop, yTop)
                close()
            }
            drawScope.drawPath(leftCurb, color = curbColor)

            // Right Curb
            val rightCurb = Path().apply {
                moveTo(xBottom + roadHalfWidthBottom, yBottom)
                lineTo(xBottom + roadHalfWidthBottom + curbWidthBottom, yBottom)
                lineTo(xTop + roadHalfWidthTop + curbWidthTop, yTop)
                lineTo(xTop + roadHalfWidthTop, yTop)
                close()
            }
            drawScope.drawPath(rightCurb, color = curbColor)

            // Center Dash Marking
            if (isEven) {
                val dashWidthBottom = roadHalfWidthBottom * 0.04f
                val dashWidthTop = roadHalfWidthTop * 0.04f
                val dashPath = Path().apply {
                    moveTo(xBottom - dashWidthBottom, yBottom)
                    lineTo(xBottom + dashWidthBottom, yBottom)
                    lineTo(xTop + dashWidthTop, yTop)
                    lineTo(xTop - dashWidthTop, yTop)
                    close()
                }
                drawScope.drawPath(dashPath, color = Color(theme.roadMarkingColor))
            }
        }
    }

    private fun drawFinishLineArch(
        drawScope: DrawScope,
        finishDist: Float,
        cameraZ: Float,
        cameraY: Float,
        centerX: Float,
        horizonY: Float,
        screenHeight: Float
    ) {
        val dZ = finishDist - cameraZ
        if (dZ <= 1.0f) return
        val scale = (14f / dZ).coerceIn(0.04f, 3.0f)
        val yBottom = horizonY + (cameraY * scale * screenHeight * 0.32f)
        val archHeight = 120f * scale
        val archWidth = 420f * scale

        // Checkered banner top
        val bannerTop = yBottom - archHeight
        drawScope.drawRect(
            color = Color(0xFFFFD700),
            topLeft = Offset(centerX - archWidth / 2f, bannerTop),
            size = Size(archWidth, 35f * scale)
        )
        // Checkered boxes
        val numBoxes = 14
        val boxW = archWidth / numBoxes
        for (i in 0 until numBoxes) {
            val boxColor = if (i % 2 == 0) Color.White else Color.Black
            drawScope.drawRect(
                color = boxColor,
                topLeft = Offset(centerX - archWidth / 2f + (i * boxW), bannerTop),
                size = Size(boxW, 35f * scale)
            )
        }
        // Arch Pillars
        drawScope.drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(centerX - archWidth / 2f - 15f * scale, bannerTop),
            size = Size(20f * scale, archHeight)
        )
        drawScope.drawRect(
            color = Color(0xFF333333),
            topLeft = Offset(centerX + archWidth / 2f - 5f * scale, bannerTop),
            size = Size(20f * scale, archHeight)
        )
    }

    private fun drawObstacle(
        drawScope: DrawScope,
        obstacle: ActiveObstacle,
        screenX: Float,
        screenY: Float,
        scale: Float
    ) {
        val s = scale.coerceIn(0.15f, 2.8f)

        when (obstacle.kind) {
            ObstacleKind.SPEED_PAD -> {
                // Glowing cyan speed arrows on the asphalt
                val w = 90f * s
                val h = 60f * s
                val path = Path().apply {
                    moveTo(screenX, screenY - h)
                    lineTo(screenX + w / 2f, screenY)
                    lineTo(screenX + w * 0.25f, screenY)
                    lineTo(screenX + w * 0.25f, screenY + h * 0.3f)
                    lineTo(screenX - w * 0.25f, screenY + h * 0.3f)
                    lineTo(screenX - w * 0.25f, screenY)
                    lineTo(screenX - w / 2f, screenY)
                    close()
                }
                drawScope.drawPath(path, color = Color(0xFF00E5FF))
            }
            ObstacleKind.JUMP_RAMP -> {
                // High launch ramp
                val w = 100f * s
                val h = 45f * s
                val rampPath = Path().apply {
                    moveTo(screenX - w / 2f, screenY)
                    lineTo(screenX + w / 2f, screenY)
                    lineTo(screenX + w / 2f, screenY - h)
                    lineTo(screenX - w / 2f, screenY - h)
                    close()
                }
                drawScope.drawPath(rampPath, color = Color(0xFFFFD600))
                drawScope.drawRect(
                    color = Color.Black,
                    topLeft = Offset(screenX - w / 2f, screenY - h),
                    size = Size(w, 8f * s)
                )
            }
            ObstacleKind.COIN_CLUSTER -> {
                val radius = 18f * s
                drawScope.drawCircle(
                    color = Color(0xFFFFD700),
                    radius = radius,
                    center = Offset(screenX, screenY - radius)
                )
                drawScope.drawCircle(
                    color = Color(0xFFFFA000),
                    radius = radius * 0.7f,
                    center = Offset(screenX, screenY - radius)
                )
            }
            ObstacleKind.ROLLING_LAVA_BOULDER -> {
                val radius = 28f * s
                val cy = screenY - radius
                drawScope.drawCircle(
                    color = Color(0xFFD84315),
                    radius = radius,
                    center = Offset(screenX, cy)
                )
                // Magma cracks
                drawScope.drawCircle(
                    color = Color(0xFFFFAB00),
                    radius = radius * 0.55f,
                    center = Offset(screenX, cy)
                )
            }
            ObstacleKind.BOUNCING_RUBBER_BALL -> {
                val bounceH = abs(sin(obstacle.phase * 3f)) * 50f * s
                val radius = 26f * s
                val cy = screenY - radius - bounceH
                drawScope.drawCircle(
                    color = Color(0xFFFF1744),
                    radius = radius,
                    center = Offset(screenX, cy)
                )
                // Stripes
                drawScope.drawCircle(
                    color = Color(0xFFFFEA00),
                    radius = radius * 0.5f,
                    center = Offset(screenX, cy)
                )
            }
            ObstacleKind.GIANT_BOWLING_PINS -> {
                // Bowling pin
                val pinH = 50f * s
                val pinW = 20f * s
                val topY = screenY - pinH
                drawScope.drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(screenX - pinW / 2f, topY),
                    size = Size(pinW, pinH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f * s, 10f * s)
                )
                // Red stripe
                drawScope.drawRect(
                    color = Color(0xFFFF1744),
                    topLeft = Offset(screenX - pinW / 2f, topY + pinH * 0.35f),
                    size = Size(pinW, 6f * s)
                )
            }
            ObstacleKind.DONUT_ROLLER -> {
                val radius = 32f * s
                val cy = screenY - radius
                // Pastry dough
                drawScope.drawCircle(
                    color = Color(0xFFD7CCC8),
                    radius = radius,
                    center = Offset(screenX, cy)
                )
                // Pink frosting
                drawScope.drawCircle(
                    color = Color(0xFFFF4081),
                    radius = radius * 0.85f,
                    center = Offset(screenX, cy)
                )
                // Center hole
                drawScope.drawCircle(
                    color = Color(0xFF3E2723),
                    radius = radius * 0.35f,
                    center = Offset(screenX, cy)
                )
            }
            ObstacleKind.ICE_PATCH -> {
                // Slick ice patch on ground
                drawScope.drawOval(
                    color = Color(0xAA80DEEA),
                    topLeft = Offset(screenX - 45f * s, screenY - 20f * s),
                    size = Size(90f * s, 40f * s)
                )
            }
            else -> {
                // Generic Cyber / Hazard Barrier
                val w = 70f * s
                val h = 40f * s
                drawScope.drawRoundRect(
                    color = Color(0xFFFF9100),
                    topLeft = Offset(screenX - w / 2f, screenY - h),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * s, 6f * s)
                )
                // Caution stripes
                drawScope.drawRect(
                    color = Color.Black,
                    topLeft = Offset(screenX - w / 4f, screenY - h + 10f * s),
                    size = Size(w / 2f, 8f * s)
                )
            }
        }
    }

    private fun drawCar(
        drawScope: DrawScope,
        racer: RacerState,
        screenX: Float,
        screenY: Float,
        scale: Float,
        isPlayer: Boolean
    ) {
        val s = scale.coerceIn(0.12f, 2.5f)
        val carWidth = 92f * s
        val carHeight = 44f * s

        // Apply roll angle (leaning into turns)
        drawScope.rotate(
            degrees = racer.rollAngle + racer.spinAngle,
            pivot = Offset(screenX, screenY - carHeight / 2f)
        ) {
            // 1. Neon Underglow Pool on Road
            if (racer.neonColor != 0L) {
                drawScope.drawOval(
                    color = Color(racer.neonColor).copy(alpha = 0.45f),
                    topLeft = Offset(screenX - carWidth * 0.65f, screenY - 14f * s),
                    size = Size(carWidth * 1.3f, 28f * s)
                )
            }

            // 2. Ground Shadow
            drawScope.drawOval(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(screenX - carWidth * 0.55f, screenY - 8f * s),
                size = Size(carWidth * 1.1f, 20f * s)
            )

            // 3. Racing Tires (Left & Right)
            val tireW = 16f * s
            val tireH = 34f * s
            // Left Tire
            drawScope.drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(screenX - carWidth / 2f - tireW * 0.4f, screenY - tireH + 4f * s),
                size = Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
            )
            // Right Tire
            drawScope.drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(screenX + carWidth / 2f - tireW * 0.6f, screenY - tireH + 4f * s),
                size = Size(tireW, tireH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
            )

            // 4. Main Aerodynamic Body Chassis
            val bodyPath = Path().apply {
                moveTo(screenX - carWidth * 0.42f, screenY)
                lineTo(screenX + carWidth * 0.42f, screenY)
                lineTo(screenX + carWidth * 0.46f, screenY - carHeight * 0.5f)
                lineTo(screenX + carWidth * 0.35f, screenY - carHeight * 0.95f)
                lineTo(screenX - carWidth * 0.35f, screenY - carHeight * 0.95f)
                lineTo(screenX - carWidth * 0.46f, screenY - carHeight * 0.5f)
                close()
            }
            drawScope.drawPath(bodyPath, color = Color(racer.paintColor))

            // 5. Cockpit & Tinted Windshield Glass
            val glassPath = Path().apply {
                moveTo(screenX - carWidth * 0.28f, screenY - carHeight * 0.45f)
                lineTo(screenX + carWidth * 0.28f, screenY - carHeight * 0.45f)
                lineTo(screenX + carWidth * 0.22f, screenY - carHeight * 0.88f)
                lineTo(screenX - carWidth * 0.22f, screenY - carHeight * 0.88f)
                close()
            }
            drawScope.drawPath(glassPath, color = Color(0xFF0F172A))
            // Windshield glass glare
            drawScope.drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(screenX - carWidth * 0.12f, screenY - carHeight * 0.85f),
                end = Offset(screenX - carWidth * 0.20f, screenY - carHeight * 0.5f),
                strokeWidth = 3f * s
            )

            // 6. Rear High-Downforce Spoiler / Wing
            val wingW = carWidth * 0.92f
            val wingH = 7f * s
            val wingY = screenY - carHeight * 1.05f
            drawScope.drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(screenX - wingW / 2f, wingY),
                size = Size(wingW, wingH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * s, 2f * s)
            )
            // Spoiler supports
            drawScope.drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(screenX - carWidth * 0.28f, wingY),
                size = Size(5f * s, carHeight * 0.2f)
            )
            drawScope.drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(screenX + carWidth * 0.28f - 5f * s, wingY),
                size = Size(5f * s, carHeight * 0.2f)
            )

            // 7. Glowing Taillights
            val lightW = 24f * s
            val lightH = 6f * s
            val lightY = screenY - carHeight * 0.38f
            // Left Taillight
            drawScope.drawRoundRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(screenX - carWidth * 0.42f, lightY),
                size = Size(lightW, lightH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * s, 2f * s)
            )
            // Right Taillight
            drawScope.drawRoundRect(
                color = Color(0xFFFF1744),
                topLeft = Offset(screenX + carWidth * 0.42f - lightW, lightY),
                size = Size(lightW, lightH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * s, 2f * s)
            )

            // 8. Dual Exhaust Pipes & Nitro Fire Flames
            val pipeY = screenY - 4f * s
            val pipeSize = 8f * s
            val leftPipeX = screenX - carWidth * 0.22f
            val rightPipeX = screenX + carWidth * 0.22f

            drawScope.drawCircle(color = Color(0xFF475569), radius = pipeSize * 0.5f, center = Offset(leftPipeX, pipeY))
            drawScope.drawCircle(color = Color(0xFF475569), radius = pipeSize * 0.5f, center = Offset(rightPipeX, pipeY))

            if (racer.nitroTimeRemaining > 0f) {
                // Boost flames
                val flameLen = 35f * s + (sin(System.currentTimeMillis() * 0.05f) * 8f * s)
                drawScope.drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0x0000E5FF))),
                    topLeft = Offset(leftPipeX - 8f * s, pipeY),
                    size = Size(16f * s, flameLen)
                )
                drawScope.drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0x0000E5FF))),
                    topLeft = Offset(rightPipeX - 8f * s, pipeY),
                    size = Size(16f * s, flameLen)
                )
            }
        }

        // Draw Name badge over AI cars
        if (!isPlayer && s > 0.35f) {
            val badgeY = screenY - carHeight * 1.35f
            drawScope.drawRoundRect(
                color = Color(0xCC000000),
                topLeft = Offset(screenX - 32f * s, badgeY - 14f * s),
                size = Size(64f * s, 18f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f * s, 4f * s)
            )
        }
    }

    private fun drawParticles(
        drawScope: DrawScope,
        engine: GameEngine,
        cameraZ: Float,
        centerX: Float,
        horizonY: Float,
        screenHeight: Float,
        screenWidth: Float
    ) {
        val player = engine.player
        val roadHalfWidth = screenWidth * 0.44f

        for (p in engine.particles) {
            val dZ = p.z - cameraZ
            if (dZ in 0.5f..80f) {
                val scale = (14f / dZ).coerceIn(0.04f, 2.5f)
                val py = horizonY + ((2.6f - p.y) * scale * screenHeight * 0.32f)
                val px = centerX + (p.x * roadHalfWidth * scale)
                val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)

                drawScope.drawCircle(
                    color = Color(p.color).copy(alpha = alpha),
                    radius = p.size * scale,
                    center = Offset(px, py)
                )
            }
        }
    }

    private fun drawSpeedStreaks(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        isNitro: Boolean
    ) {
        val streakColor = if (isNitro) Color(0x4400E5FF) else Color(0x22FFFFFF)
        val numStreaks = 8
        for (i in 0 until numStreaks) {
            val startX = if (i % 2 == 0) 0f else width
            val endX = if (i % 2 == 0) width * 0.35f else width * 0.65f
            val y = height * (0.2f + (i * 0.09f))
            drawScope.drawLine(
                color = streakColor,
                start = Offset(startX, y),
                end = Offset(endX, y + 25f),
                strokeWidth = 3f
            )
        }
    }
}

private class RenderableEntity(
    val distance: Float,
    val dZ: Float,
    val isCar: Boolean,
    val racer: RacerState? = null,
    val obstacle: ActiveObstacle? = null
) {
    val lateralPos: Float
        get() = racer?.lateralPos ?: obstacle?.currentLateralPos ?: 0f
}
