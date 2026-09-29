package com.example.game

import com.example.model.CarCatalog
import com.example.model.CarModel
import com.example.model.ObstacleKind
import com.example.model.ObstacleSpawn
import com.example.model.TrackCatalog
import com.example.model.TrackData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class RacerState(
    val name: String,
    val isPlayer: Boolean,
    val carId: String,
    val paintColor: Long,
    val neonColor: Long,
    var distance: Float,
    var lateralPos: Float,       // -0.85 to +0.85
    var targetLateralPos: Float, // for smooth steering
    var speed: Float,            // in m/s (1 m/s = 3.6 km/h)
    val maxSpeed: Float,
    val acceleration: Float,
    val handling: Float,
    val armor: Float,
    var nitroTimeRemaining: Float = 0f,
    var stunTimeRemaining: Float = 0f,
    var spinAngle: Float = 0f,   // when hitting ice or obstacle
    var jumpHeight: Float = 0f,
    var jumpVelocity: Float = 0f,
    var rollAngle: Float = 0f,
    var rank: Int = 1,
    var hasFinished: Boolean = false,
    var finishTime: Float = 0f,
    var draftMultiplier: Float = 1.0f
)

data class ActiveObstacle(
    val id: Int,
    val kind: ObstacleKind,
    val baseDistance: Float,
    var currentDistance: Float,
    val baseLateralPos: Float,
    var currentLateralPos: Float,
    val width: Float,
    val isDynamic: Boolean,
    val movementRange: Float,
    val speed: Float,
    var phase: Float,
    var isHit: Boolean = false,
    var hitAnimationTimer: Float = 0f
)

data class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    val maxLife: Float,
    val color: Long,
    val size: Float
)

data class GameEvent(
    val type: EventType,
    val text: String? = null
)

enum class EventType {
    BUMP,
    NITRO,
    COIN_COLLECT,
    OBSTACLE_HIT,
    BOOST_PAD,
    JUMP,
    LAP_COMPLETE,
    COUNTDOWN_TICK,
    COUNTDOWN_GO
}

class GameEngine(
    val track: TrackData,
    val playerCarModel: CarModel,
    val speedLevel: Int = 1,
    val handlingLevel: Int = 1,
    val armorLevel: Int = 1,
    val nitroLevel: Int = 1,
    val playerPaintColor: Long,
    val playerNeonColor: Long,
    var steeringSensitivity: Float = 1.0f
) {
    var isRunning = false
    var isGameOver = false
    var countdownTimer = 3.99f // 3..2..1..GO!
    var raceTime = 0f
    var coinsEarnedThisRace = 0

    val player: RacerState
    val aiRacers = mutableListOf<RacerState>()
    val activeObstacles = mutableListOf<ActiveObstacle>()
    val particles = mutableListOf<Particle>()
    val eventQueue = mutableListOf<GameEvent>()

    init {
        // Player calculated stats
        val playerTopSpeedKmh = playerCarModel.baseTopSpeed + (speedLevel - 1) * 12f
        val playerTopSpeedMs = playerTopSpeedKmh / 3.6f
        val playerHandling = playerCarModel.baseHandling + (handlingLevel - 1) * 0.08f
        val playerArmor = playerCarModel.baseArmor + (armorLevel - 1) * 0.1f

        player = RacerState(
            name = "Player",
            isPlayer = true,
            carId = playerCarModel.id,
            paintColor = playerPaintColor,
            neonColor = playerNeonColor,
            distance = 0f,
            lateralPos = 0f,
            targetLateralPos = 0f,
            speed = 0f,
            maxSpeed = playerTopSpeedMs,
            acceleration = 9.5f + (speedLevel * 1.2f),
            handling = playerHandling,
            armor = playerArmor
        )

        // Initialize 5 AI Opponents
        val aiConfigs = listOf(
            Triple("Blaze", 0xFFFF3D00L, "thunder_bolt"),
            Triple("Viper", 0xFF00E676L, "cyber_cruiser"),
            Triple("Dash", 0xFFFFD600L, "apex_gt"),
            Triple("Turbo", 0xFFD500F9L, "hyper_sonic"),
            Triple("Roxy", 0xFF00B0FFL, "titan_ram")
        )

        val startingLanes = listOf(-0.55f, 0.55f, -0.3f, 0.3f, 0.0f)
        aiConfigs.forEachIndexed { index, (name, color, carId) ->
            val aiCar = CarCatalog.getCar(carId)
            val aiSpeedMs = (aiCar.baseTopSpeed + (-5f + (index * 2f))) / 3.6f
            aiRacers.add(
                RacerState(
                    name = name,
                    isPlayer = false,
                    carId = carId,
                    paintColor = color,
                    neonColor = 0xFF00E5FFL,
                    distance = -8f * (index + 1), // staggered grid start
                    lateralPos = startingLanes[index % startingLanes.size],
                    targetLateralPos = startingLanes[index % startingLanes.size],
                    speed = 0f,
                    maxSpeed = aiSpeedMs,
                    acceleration = 8.5f + (index * 0.4f),
                    handling = 0.65f,
                    armor = aiCar.baseArmor
                )
            )
        }

        // Initialize obstacles from track data
        track.obstacles.forEachIndexed { index, spawn ->
            activeObstacles.add(
                ActiveObstacle(
                    id = index,
                    kind = spawn.kind,
                    baseDistance = spawn.distance,
                    currentDistance = spawn.distance,
                    baseLateralPos = spawn.lateralPos,
                    currentLateralPos = spawn.lateralPos,
                    width = spawn.width,
                    isDynamic = spawn.isDynamic,
                    movementRange = spawn.movementRange,
                    speed = spawn.speed,
                    phase = spawn.phase
                )
            )
        }
    }

    fun onSwipeSteer(deltaNormalizedX: Float) {
        if (player.stunTimeRemaining > 0.2f) return
        val effectiveDelta = deltaNormalizedX * steeringSensitivity * (1.8f + player.handling)
        player.targetLateralPos = (player.targetLateralPos + effectiveDelta).coerceIn(-0.85f, 0.85f)
    }

    fun activateNitro() {
        if (player.nitroTimeRemaining <= 0f && countdownTimer <= 0f) {
            val nitroDuration = 3.5f + (nitroLevel * 0.5f)
            player.nitroTimeRemaining = nitroDuration
            eventQueue.add(GameEvent(EventType.NITRO))
            spawnNitroParticles(player)
        }
    }

    fun update(deltaTime: Float) {
        val dt = deltaTime.coerceIn(0.001f, 0.05f)

        // Countdown handling
        if (countdownTimer > 0f) {
            val prevSec = countdownTimer.toInt()
            countdownTimer -= dt
            val newSec = countdownTimer.toInt()
            if (countdownTimer <= 0f) {
                countdownTimer = 0f
                eventQueue.add(GameEvent(EventType.COUNTDOWN_GO, "GO!"))
                isRunning = true
            } else if (newSec < prevSec && newSec >= 1) {
                eventQueue.add(GameEvent(EventType.COUNTDOWN_TICK, "$newSec"))
            }
            return
        }

        if (!isRunning) return

        raceTime += dt

        // 1. Update dynamic obstacles (swaying, rolling boulders, spinning fans)
        for (obs in activeObstacles) {
            if (obs.isDynamic) {
                obs.phase += obs.speed * dt
                obs.currentLateralPos = obs.baseLateralPos + (sin(obs.phase) * obs.movementRange)
                    .coerceIn(-0.8f, 0.8f)
            }
            if (obs.isHit) {
                obs.hitAnimationTimer += dt
            }
        }

        // 2. Update all racers (player + AI)
        val allRacers = mutableListOf<RacerState>().apply {
            add(player)
            addAll(aiRacers)
        }

        for (racer in allRacers) {
            if (racer.hasFinished) continue

            // Stun & spin recovery
            if (racer.stunTimeRemaining > 0f) {
                racer.stunTimeRemaining -= dt
                racer.spinAngle += 720f * dt
                if (racer.stunTimeRemaining <= 0f) {
                    racer.spinAngle = 0f
                }
            }

            // Nitro timer
            val isBoosting = racer.nitroTimeRemaining > 0f
            if (isBoosting) {
                racer.nitroTimeRemaining -= dt
                if (racer.isPlayer && Math.random() > 0.4) {
                    spawnNitroParticles(racer)
                }
            }

            // Target speed based on status
            var targetMaxSpeed = racer.maxSpeed
            if (isBoosting) {
                targetMaxSpeed *= 1.35f
            }
            if (racer.stunTimeRemaining > 0f) {
                targetMaxSpeed *= 0.45f
            }

            // Draft (slipstream behind another car)
            racer.draftMultiplier = calculateDraftMultiplier(racer, allRacers)
            targetMaxSpeed *= racer.draftMultiplier

            // Automatic Throttle physics
            if (racer.speed < targetMaxSpeed) {
                val accel = if (isBoosting) racer.acceleration * 1.8f else racer.acceleration
                racer.speed = (racer.speed + accel * dt).coerceAtMost(targetMaxSpeed)
            } else {
                racer.speed = (racer.speed - 12f * dt).coerceAtLeast(targetMaxSpeed)
            }

            // Forward movement along track
            racer.distance += racer.speed * dt

            // Check finish line
            if (racer.distance >= track.lengthMeters) {
                racer.distance = track.lengthMeters
                racer.hasFinished = true
                racer.finishTime = raceTime
                if (racer.isPlayer) {
                    isGameOver = true
                    eventQueue.add(GameEvent(EventType.LAP_COMPLETE, "FINISH!"))
                }
            }

            // Steering physics (interpolation towards target lateral position)
            val steerSpeed = 6.0f + (racer.handling * 8f)
            val lateralDiff = racer.targetLateralPos - racer.lateralPos
            val deltaLat = lateralDiff * (steerSpeed * dt).coerceAtMost(1f)
            racer.lateralPos = (racer.lateralPos + deltaLat).coerceIn(-0.85f, 0.85f)

            // Dynamic chassis roll when steering
            val targetRoll = (-deltaLat / (dt + 0.001f) * 1.5f).coerceIn(-18f, 18f)
            racer.rollAngle += (targetRoll - racer.rollAngle) * 0.2f

            // Ramp jump physics
            if (racer.jumpHeight > 0f || racer.jumpVelocity > 0f) {
                racer.jumpVelocity -= 22f * dt // gravity
                racer.jumpHeight += racer.jumpVelocity * dt
                if (racer.jumpHeight <= 0f) {
                    racer.jumpHeight = 0f
                    racer.jumpVelocity = 0f
                    spawnLandingDust(racer)
                }
            }

            // AI Decision logic (swerving around obstacles and passing rivals)
            if (!racer.isPlayer) {
                updateAiBehavior(racer, activeObstacles, allRacers, dt)
            }
        }

        // 3. Collision Detection: Player against Obstacles
        checkPlayerObstacleCollisions()

        // 4. Collision Detection: Racer vs Racer (Bumping physics)
        checkRacerInteractions(allRacers)

        // 5. Update Ranks (Leaderboard sorting by distance)
        val sortedRacers = allRacers.sortedByDescending { it.distance }
        sortedRacers.forEachIndexed { index, racer ->
            racer.rank = index + 1
        }

        // 6. Update visual particles
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.life -= dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            if (p.life <= 0f) {
                pIter.remove()
            }
        }
    }

    private fun calculateDraftMultiplier(racer: RacerState, allRacers: List<RacerState>): Float {
        // Look for any car directly ahead within 4m to 16m and lateral distance < 0.25f
        for (other in allRacers) {
            if (other === racer) continue
            val distAhead = other.distance - racer.distance
            if (distAhead in 3.5f..16f && abs(other.lateralPos - racer.lateralPos) < 0.28f) {
                return 1.12f // 12% drafting slipstream boost!
            }
        }
        return 1.0f
    }

    private fun updateAiBehavior(
        ai: RacerState,
        obstacles: List<ActiveObstacle>,
        allRacers: List<RacerState>,
        dt: Float
    ) {
        // Look ahead for obstacles
        var urgentEvasion = false
        var evadeTargetLane = ai.targetLateralPos

        for (obs in obstacles) {
            if (obs.isHit) continue
            val distToObs = obs.currentDistance - ai.distance
            if (distToObs in 3f..24f) {
                if (abs(obs.currentLateralPos - ai.lateralPos) < (obs.width + 0.18f)) {
                    urgentEvasion = true
                    // Evade to the clearer side
                    evadeTargetLane = if (obs.currentLateralPos > 0) {
                        (obs.currentLateralPos - obs.width - 0.25f).coerceAtLeast(-0.8f)
                    } else {
                        (obs.currentLateralPos + obs.width + 0.25f).coerceAtMost(0.8f)
                    }
                    break
                }
            }
        }

        if (urgentEvasion) {
            ai.targetLateralPos = evadeTargetLane
        } else {
            // Natural slight weaving / overtaking behavior
            if (Math.random() < 0.02) {
                val lanes = listOf(-0.6f, -0.25f, 0.25f, 0.6f)
                ai.targetLateralPos = lanes.random()
            }
        }
    }

    private fun checkPlayerObstacleCollisions() {
        if (player.hasFinished) return

        for (obs in activeObstacles) {
            if (obs.isHit) continue
            val distDiff = abs(obs.currentDistance - player.distance)
            if (distDiff < 2.5f) {
                val latDiff = abs(obs.currentLateralPos - player.lateralPos)
                if (latDiff < (obs.width / 2f + 0.18f)) {
                    // Collision occurred!
                    handleObstacleCollision(obs)
                }
            }
        }
    }

    private fun handleObstacleCollision(obs: ActiveObstacle) {
        when (obs.kind) {
            ObstacleKind.SPEED_PAD -> {
                player.nitroTimeRemaining = 3.0f
                eventQueue.add(GameEvent(EventType.BOOST_PAD, "BOOST!"))
                spawnNitroParticles(player)
            }
            ObstacleKind.JUMP_RAMP -> {
                player.jumpVelocity = 12f
                player.jumpHeight = 0.2f
                eventQueue.add(GameEvent(EventType.JUMP, "BIG AIR!"))
            }
            ObstacleKind.COIN_CLUSTER -> {
                coinsEarnedThisRace += 30
                obs.isHit = true
                eventQueue.add(GameEvent(EventType.COIN_COLLECT, "+30 COINS"))
            }
            ObstacleKind.ICE_PATCH -> {
                player.stunTimeRemaining = 0.5f
                eventQueue.add(GameEvent(EventType.OBSTACLE_HIT, "ICE DRIFT!"))
                spawnImpactSparks(player.lateralPos, 0xFF80D8FF)
            }
            else -> {
                // Obstacle hit (pins, boulders, barrels, etc.)
                obs.isHit = true
                player.stunTimeRemaining = 0.65f
                player.speed *= 0.5f
                eventQueue.add(GameEvent(EventType.OBSTACLE_HIT, "SMASH!"))
                spawnImpactSparks(player.lateralPos, 0xFFFFD700)
            }
        }
    }

    private fun checkRacerInteractions(allRacers: List<RacerState>) {
        for (i in allRacers.indices) {
            for (j in i + 1 until allRacers.size) {
                val r1 = allRacers[i]
                val r2 = allRacers[j]
                val distDiff = abs(r1.distance - r2.distance)
                if (distDiff < 3.2f) {
                    val latDiff = r1.lateralPos - r2.lateralPos
                    if (abs(latDiff) < 0.26f) {
                        // Lateral bump!
                        val pushSign = if (latDiff >= 0) 1f else -1f
                        val r1Resistance = r1.armor / (r1.armor + r2.armor)
                        val r2Resistance = 1f - r1Resistance

                        r1.lateralPos = (r1.lateralPos + pushSign * 0.12f * (1f - r1Resistance)).coerceIn(-0.85f, 0.85f)
                        r2.lateralPos = (r2.lateralPos - pushSign * 0.12f * (1f - r2Resistance)).coerceIn(-0.85f, 0.85f)
                        r1.targetLateralPos = r1.lateralPos
                        r2.targetLateralPos = r2.lateralPos

                        if (r1.isPlayer || r2.isPlayer) {
                            eventQueue.add(GameEvent(EventType.BUMP))
                            spawnImpactSparks(player.lateralPos, 0xFFFFAB00)
                        }
                    }
                }
            }
        }
    }

    private fun spawnNitroParticles(racer: RacerState) {
        val colors = listOf(0xFF00E5FFL, 0xFF00B0FFL, 0xFFD500F9L)
        for (i in 0 until 4) {
            particles.add(
                Particle(
                    x = racer.lateralPos + (Math.random().toFloat() - 0.5f) * 0.15f,
                    y = racer.jumpHeight + 0.2f,
                    z = racer.distance - 1.5f,
                    vx = (Math.random().toFloat() - 0.5f) * 0.6f,
                    vy = (Math.random().toFloat() * 0.8f),
                    vz = -20f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    color = colors.random(),
                    size = 12f + (Math.random().toFloat() * 10f)
                )
            )
        }
    }

    private fun spawnImpactSparks(lateralPos: Float, color: Long) {
        for (i in 0 until 12) {
            particles.add(
                Particle(
                    x = lateralPos + (Math.random().toFloat() - 0.5f) * 0.2f,
                    y = 0.4f + (Math.random().toFloat() * 0.6f),
                    z = player.distance + 1f,
                    vx = (Math.random().toFloat() - 0.5f) * 3f,
                    vy = 1f + (Math.random().toFloat() * 2f),
                    vz = (Math.random().toFloat() - 0.5f) * 3f,
                    life = 0.4f,
                    maxLife = 0.4f,
                    color = color,
                    size = 8f + (Math.random().toFloat() * 8f)
                )
            )
        }
    }

    private fun spawnLandingDust(racer: RacerState) {
        for (i in 0 until 8) {
            particles.add(
                Particle(
                    x = racer.lateralPos + (Math.random().toFloat() - 0.5f) * 0.4f,
                    y = 0.1f,
                    z = racer.distance - 0.5f,
                    vx = (Math.random().toFloat() - 0.5f) * 2f,
                    vy = 0.5f + (Math.random().toFloat() * 1f),
                    vz = (Math.random().toFloat() - 0.5f) * 1.5f,
                    life = 0.3f,
                    maxLife = 0.3f,
                    color = 0x88CCCCCCL,
                    size = 16f
                )
            )
        }
    }

    fun pollEvents(): List<GameEvent> {
        val list = eventQueue.toList()
        eventQueue.clear()
        return list
    }
}
