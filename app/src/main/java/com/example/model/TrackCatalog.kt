package com.example.model

enum class ObstacleKind {
    // Neon
    CYBER_BARRIER,
    GIANT_BOWLING_PINS,
    CYBER_CONE,
    // Volcano
    ROLLING_LAVA_BOULDER,
    MAGMA_GEYSER,
    VOLCANIC_SPIRE,
    // Toyland
    BOUNCING_RUBBER_BALL,
    WINDMILL_BLADES,
    TOPPLING_DOMINO,
    // Arctic
    ROLLING_SNOWBALL,
    ICE_PATCH,
    ICICLE_CLUSTER,
    // Desert
    TUMBLEWEED,
    SWINGING_BALL,
    OIL_BARREL_STACK,
    // Candy
    DONUT_ROLLER,
    SWIRL_LOLLIPOP,
    JELLY_BOUNCER,
    // Universal
    SPEED_PAD,
    JUMP_RAMP,
    COIN_CLUSTER
}

data class ObstacleSpawn(
    val kind: ObstacleKind,
    val distance: Float,      // meters along track
    val lateralPos: Float,    // -0.8f to +0.8f (0 = center)
    val width: Float = 0.35f,
    val isDynamic: Boolean = false,
    val movementRange: Float = 0.5f,
    val speed: Float = 1.0f,
    val phase: Float = 0f
)

data class TrackTheme(
    val skyColorTop: Long,
    val skyColorBottom: Long,
    val horizonElementKind: HorizonStyle,
    val roadColorLight: Long,
    val roadColorDark: Long,
    val roadMarkingColor: Long,
    val curbColor1: Long,
    val curbColor2: Long,
    val guardrailGlow: Long,
    val ambientParticleColor: Long
)

enum class HorizonStyle {
    CYBER_CITY,
    VOLCANO_PEAKS,
    TOY_CASTLE,
    SNOW_MOUNTAINS,
    DESERT_MESAS,
    CANDY_HILLS
}

data class TrackData(
    val id: String,
    val name: String,
    val location: String,
    val difficulty: String,
    val lengthMeters: Float,
    val theme: TrackTheme,
    val unlockTrophies: Int,
    val obstacles: List<ObstacleSpawn>,
    val curveProfile: List<TrackCurveSegment> // gentle curves
)

data class TrackCurveSegment(
    val startDistance: Float,
    val endDistance: Float,
    val curveIntensity: Float // gentle: -0.4f (left) to +0.4f (right)
)

object TrackCatalog {
    val tracks: List<TrackData> = listOf(
        // TRACK 1: NEON METROPOLIS
        TrackData(
            id = "track_neon",
            name = "Neon Metropolis",
            location = "Cyber District",
            difficulty = "Easy",
            lengthMeters = 1600f,
            unlockTrophies = 0,
            theme = TrackTheme(
                skyColorTop = 0xFF0D0B24,
                skyColorBottom = 0xFF2A1654,
                horizonElementKind = HorizonStyle.CYBER_CITY,
                roadColorLight = 0xFF1C1D2A,
                roadColorDark = 0xFF141522,
                roadMarkingColor = 0xFF00E5FF,
                curbColor1 = 0xFFFF007F,
                curbColor2 = 0xFF00E5FF,
                guardrailGlow = 0xFFD500F9,
                ambientParticleColor = 0xFF00E5FF
            ),
            curveProfile = listOf(
                TrackCurveSegment(0f, 250f, 0f),
                TrackCurveSegment(250f, 500f, 0.25f),  // gentle right curve
                TrackCurveSegment(500f, 750f, 0f),
                TrackCurveSegment(750f, 1050f, -0.22f), // gentle left curve
                TrackCurveSegment(1050f, 1300f, 0.18f), // gentle right curve
                TrackCurveSegment(1300f, 1600f, 0f)    // straight to finish
            ),
            obstacles = generateTrackObstacles(
                length = 1600f,
                kinds = listOf(
                    ObstacleKind.CYBER_BARRIER,
                    ObstacleKind.GIANT_BOWLING_PINS,
                    ObstacleKind.CYBER_CONE,
                    ObstacleKind.SPEED_PAD,
                    ObstacleKind.JUMP_RAMP
                )
            )
        ),

        // TRACK 2: VOLCANO VALLEY
        TrackData(
            id = "track_volcano",
            name = "Volcano Valley",
            location = "Magma Rift",
            difficulty = "Medium",
            lengthMeters = 1800f,
            unlockTrophies = 3,
            theme = TrackTheme(
                skyColorTop = 0xFF210802,
                skyColorBottom = 0xFF6B1A04,
                horizonElementKind = HorizonStyle.VOLCANO_PEAKS,
                roadColorLight = 0xFF241C1A,
                roadColorDark = 0xFF191312,
                roadMarkingColor = 0xFFFF6D00,
                curbColor1 = 0xFFFF3D00,
                curbColor2 = 0xFFFFD600,
                guardrailGlow = 0xFFFF6D00,
                ambientParticleColor = 0xFFFF9100
            ),
            curveProfile = listOf(
                TrackCurveSegment(0f, 300f, 0f),
                TrackCurveSegment(300f, 600f, -0.25f),
                TrackCurveSegment(600f, 900f, 0.20f),
                TrackCurveSegment(900f, 1300f, -0.28f),
                TrackCurveSegment(1300f, 1550f, 0.22f),
                TrackCurveSegment(1550f, 1800f, 0f)
            ),
            obstacles = generateTrackObstacles(
                length = 1800f,
                kinds = listOf(
                    ObstacleKind.ROLLING_LAVA_BOULDER,
                    ObstacleKind.MAGMA_GEYSER,
                    ObstacleKind.VOLCANIC_SPIRE,
                    ObstacleKind.SPEED_PAD,
                    ObstacleKind.JUMP_RAMP
                )
            )
        ),

        // TRACK 3: TOYLAND RUSH
        TrackData(
            id = "track_toyland",
            name = "Toyland Rush",
            location = "Playroom Boulevard",
            difficulty = "Easy",
            lengthMeters = 1600f,
            unlockTrophies = 6,
            theme = TrackTheme(
                skyColorTop = 0xFF1E88E5,
                skyColorBottom = 0xFF81D4FA,
                horizonElementKind = HorizonStyle.TOY_CASTLE,
                roadColorLight = 0xFFECEFF1,
                roadColorDark = 0xFFCFD8DC,
                roadMarkingColor = 0xFFFFEA00,
                curbColor1 = 0xFFFF1744,
                curbColor2 = 0xFF2979FF,
                guardrailGlow = 0xFFFFEA00,
                ambientParticleColor = 0xFFFF4081
            ),
            curveProfile = listOf(
                TrackCurveSegment(0f, 300f, 0f),
                TrackCurveSegment(300f, 650f, 0.22f),
                TrackCurveSegment(650f, 950f, -0.20f),
                TrackCurveSegment(950f, 1250f, 0.25f),
                TrackCurveSegment(1250f, 1600f, 0f)
            ),
            obstacles = generateTrackObstacles(
                length = 1600f,
                kinds = listOf(
                    ObstacleKind.BOUNCING_RUBBER_BALL,
                    ObstacleKind.WINDMILL_BLADES,
                    ObstacleKind.TOPPLING_DOMINO,
                    ObstacleKind.SPEED_PAD,
                    ObstacleKind.JUMP_RAMP
                )
            )
        ),

        // TRACK 4: FROZEN ARCTIC
        TrackData(
            id = "track_arctic",
            name = "Frozen Arctic",
            location = "Glacier Pass",
            difficulty = "Medium",
            lengthMeters = 1900f,
            unlockTrophies = 10,
            theme = TrackTheme(
                skyColorTop = 0xFF021B2B,
                skyColorBottom = 0xFF0B4668,
                horizonElementKind = HorizonStyle.SNOW_MOUNTAINS,
                roadColorLight = 0xFFB2EBF2,
                roadColorDark = 0xFF80DEEA,
                roadMarkingColor = 0xFFFFFFFF,
                curbColor1 = 0xFF00E5FF,
                curbColor2 = 0xFF0091EA,
                guardrailGlow = 0xFF80D8FF,
                ambientParticleColor = 0xFFE0F7FA
            ),
            curveProfile = listOf(
                TrackCurveSegment(0f, 350f, 0f),
                TrackCurveSegment(350f, 750f, -0.24f),
                TrackCurveSegment(750f, 1100f, 0.26f),
                TrackCurveSegment(1100f, 1500f, -0.22f),
                TrackCurveSegment(1500f, 1900f, 0f)
            ),
            obstacles = generateTrackObstacles(
                length = 1900f,
                kinds = listOf(
                    ObstacleKind.ROLLING_SNOWBALL,
                    ObstacleKind.ICE_PATCH,
                    ObstacleKind.ICICLE_CLUSTER,
                    ObstacleKind.SPEED_PAD,
                    ObstacleKind.JUMP_RAMP
                )
            )
        ),

        // TRACK 5: DESERT CANYON
        TrackData(
            id = "track_desert",
            name = "Desert Canyon",
            location = "Sun Mesa Speedway",
            difficulty = "Hard",
            lengthMeters = 2000f,
            unlockTrophies = 15,
            theme = TrackTheme(
                skyColorTop = 0xFFE65100,
                skyColorBottom = 0xFFFFCC80,
                horizonElementKind = HorizonStyle.DESERT_MESAS,
                roadColorLight = 0xFF4E342E,
                roadColorDark = 0xFF3E2723,
                roadMarkingColor = 0xFFFFD54F,
                curbColor1 = 0xFFFF6F00,
                curbColor2 = 0xFFFFD54F,
                guardrailGlow = 0xFFFFAB00,
                ambientParticleColor = 0xFFFFE082
            ),
            curveProfile = listOf(
                TrackCurveSegment(0f, 300f, 0f),
                TrackCurveSegment(300f, 700f, 0.28f),
                TrackCurveSegment(700f, 1100f, -0.28f),
                TrackCurveSegment(1100f, 1550f, 0.25f),
                TrackCurveSegment(1550f, 2000f, 0f)
            ),
            obstacles = generateTrackObstacles(
                length = 2000f,
                kinds = listOf(
                    ObstacleKind.TUMBLEWEED,
                    ObstacleKind.SWINGING_BALL,
                    ObstacleKind.OIL_BARREL_STACK,
                    ObstacleKind.SPEED_PAD,
                    ObstacleKind.JUMP_RAMP
                )
            )
        ),

        // TRACK 6: CANDY COAST
        TrackData(
            id = "track_candy",
            name = "Candy Coast",
            location = "Sugar Sweet Highway",
            difficulty = "Expert",
            lengthMeters = 2100f,
            unlockTrophies = 20,
            theme = TrackTheme(
                skyColorTop = 0xFF4A148C,
                skyColorBottom = 0xFFF48FB1,
                horizonElementKind = HorizonStyle.CANDY_HILLS,
                roadColorLight = 0xFFFCE4EC,
                roadColorDark = 0xFFF8BBD0,
                roadMarkingColor = 0xFFFFFFFF,
                curbColor1 = 0xFFFF4081,
                curbColor2 = 0xFF7C4DFF,
                guardrailGlow = 0xFFFF80AB,
                ambientParticleColor = 0xFFFF80AB
            ),
            curveProfile = listOf(
                TrackCurveSegment(0f, 350f, 0f),
                TrackCurveSegment(350f, 750f, -0.26f),
                TrackCurveSegment(750f, 1200f, 0.30f),
                TrackCurveSegment(1200f, 1650f, -0.24f),
                TrackCurveSegment(1650f, 2100f, 0f)
            ),
            obstacles = generateTrackObstacles(
                length = 2100f,
                kinds = listOf(
                    ObstacleKind.DONUT_ROLLER,
                    ObstacleKind.SWIRL_LOLLIPOP,
                    ObstacleKind.JELLY_BOUNCER,
                    ObstacleKind.SPEED_PAD,
                    ObstacleKind.JUMP_RAMP
                )
            )
        )
    )

    fun getTrack(id: String): TrackData {
        return tracks.find { it.id == id } ?: tracks.first()
    }

    private fun generateTrackObstacles(
        length: Float,
        kinds: List<ObstacleKind>
    ): List<ObstacleSpawn> {
        val list = mutableListOf<ObstacleSpawn>()
        var dist = 140f // start after safe start zone

        while (dist < length - 120f) {
            val kind = kinds.random()
            val lateral = listOf(-0.6f, -0.3f, 0.0f, 0.3f, 0.6f).random()
            val isDynamic = (kind == ObstacleKind.ROLLING_LAVA_BOULDER ||
                    kind == ObstacleKind.ROLLING_SNOWBALL ||
                    kind == ObstacleKind.DONUT_ROLLER ||
                    kind == ObstacleKind.SWINGING_BALL ||
                    kind == ObstacleKind.CYBER_BARRIER ||
                    kind == ObstacleKind.BOUNCING_RUBBER_BALL)

            list.add(
                ObstacleSpawn(
                    kind = kind,
                    distance = dist,
                    lateralPos = lateral,
                    width = if (kind == ObstacleKind.SPEED_PAD || kind == ObstacleKind.JUMP_RAMP) 0.45f else 0.32f,
                    isDynamic = isDynamic,
                    movementRange = if (isDynamic) 0.55f else 0f,
                    speed = if (isDynamic) 1.5f + (Math.random().toFloat() * 1.5f) else 0f,
                    phase = (Math.random() * Math.PI * 2).toFloat()
                )
            )

            // Also sprinkle occasional coin clusters between obstacles
            if (Math.random() > 0.4) {
                list.add(
                    ObstacleSpawn(
                        kind = ObstacleKind.COIN_CLUSTER,
                        distance = dist + 40f,
                        lateralPos = listOf(-0.5f, 0.0f, 0.5f).random(),
                        width = 0.25f,
                        isDynamic = false
                    )
                )
            }

            dist += (70f + (Math.random().toFloat() * 50f))
        }

        return list.sortedBy { it.distance }
    }
}
