package com.example.model

data class CarModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val unlockPrice: Int,
    val baseTopSpeed: Float,   // in km/h: e.g. 180 to 260
    val baseHandling: Float,   // 0.5 to 1.0
    val baseArmor: Float,      // 0.5 to 1.0 (resists knockback)
    val baseNitro: Float,      // boost power
    val defaultPaint: Long,
    val defaultNeon: Long,
    val bodyStyle: CarBodyStyle
)

enum class CarBodyStyle {
    SUPER_SPORT,
    MUSCLE,
    FUTURISTIC,
    ARMORED_TRUCK,
    HYPER_FORMULA,
    CYBER_GT
}

object CarCatalog {
    val cars = listOf(
        CarModel(
            id = "apex_gt",
            name = "Apex GT",
            subtitle = "Balanced Street Racer",
            unlockPrice = 0,
            baseTopSpeed = 190f,
            baseHandling = 0.70f,
            baseArmor = 0.65f,
            baseNitro = 0.70f,
            defaultPaint = 0xFFFF1744, // Crimson Red
            defaultNeon = 0xFF00E5FF,  // Neon Cyan
            bodyStyle = CarBodyStyle.SUPER_SPORT
        ),
        CarModel(
            id = "thunder_bolt",
            name = "Thunder Bolt",
            subtitle = "High Torque Muscle",
            unlockPrice = 800,
            baseTopSpeed = 205f,
            baseHandling = 0.60f,
            baseArmor = 0.85f,
            baseNitro = 0.75f,
            defaultPaint = 0xFFFF9100, // Solar Orange
            defaultNeon = 0xFFFFD600,  // Gold Neon
            bodyStyle = CarBodyStyle.MUSCLE
        ),
        CarModel(
            id = "cyber_cruiser",
            name = "Cyber Cruiser",
            subtitle = "Agile Neon Roadster",
            unlockPrice = 1600,
            baseTopSpeed = 215f,
            baseHandling = 0.90f,
            baseArmor = 0.55f,
            baseNitro = 0.80f,
            defaultPaint = 0xFF00E5FF, // Electric Cyan
            defaultNeon = 0xFFD500F9,  // Magenta Neon
            bodyStyle = CarBodyStyle.CYBER_GT
        ),
        CarModel(
            id = "titan_ram",
            name = "Titan Ram",
            subtitle = "Heavyweight Juggernaut",
            unlockPrice = 2400,
            baseTopSpeed = 200f,
            baseHandling = 0.55f,
            baseArmor = 1.00f,
            baseNitro = 0.65f,
            defaultPaint = 0xFF76FF03, // Toxic Lime
            defaultNeon = 0xFF76FF03,  // Green Neon
            bodyStyle = CarBodyStyle.ARMORED_TRUCK
        ),
        CarModel(
            id = "hyper_sonic",
            name = "Hyper Sonic",
            subtitle = "Formula Top-Speed King",
            unlockPrice = 3500,
            baseTopSpeed = 245f,
            baseHandling = 0.85f,
            baseArmor = 0.60f,
            baseNitro = 0.90f,
            defaultPaint = 0xFFE040FB, // Neon Violet
            defaultNeon = 0xFF00E5FF,  // Cyan
            bodyStyle = CarBodyStyle.HYPER_FORMULA
        ),
        CarModel(
            id = "phantom_ex",
            name = "Phantom EX",
            subtitle = "Prototype Legend",
            unlockPrice = 5000,
            baseTopSpeed = 265f,
            baseHandling = 0.95f,
            baseArmor = 0.90f,
            baseNitro = 1.00f,
            defaultPaint = 0xFF2979FF, // Royal Blue
            defaultNeon = 0xFFFF1744,  // Red Laser
            bodyStyle = CarBodyStyle.FUTURISTIC
        )
    )

    val paintPalette: List<Long> = listOf(
        0xFFFF1744L, // Crimson Red
        0xFFFF9100L, // Solar Orange
        0xFFFFEA00L, // Electric Yellow
        0xFF00E676L, // Cyber Green
        0xFF00E5FFL, // Electric Cyan
        0xFF2979FFL, // Cobalt Blue
        0xFFD500F9L, // Vivid Purple
        0xFFF50057L, // Deep Pink
        0xFFFFFFFFL, // Pure White
        0xFF212121L  // Stealth Black
    )

    val neonPalette = listOf(
        0x00000000L, // None
        0xFF00E5FFL, // Cyan
        0xFFD500F9L, // Magenta
        0xFFFFEA00L, // Electric Gold
        0xFF00E676L, // Neon Green
        0xFFFF1744L, // Laser Red
        0xFFFFFFFFL  // Arctic White
    )

    fun getCar(id: String): CarModel {
        return cars.find { it.id == id } ?: cars.first()
    }

    fun getUpgradeCost(currentLevel: Int): Int {
        return when (currentLevel) {
            1 -> 250
            2 -> 500
            3 -> 900
            4 -> 1500
            else -> 0
        }
    }
}
