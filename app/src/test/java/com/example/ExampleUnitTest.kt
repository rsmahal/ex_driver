package com.example

import com.example.game.EventType
import com.example.game.GameEngine
import com.example.model.CarCatalog
import com.example.model.ObstacleKind
import com.example.model.TrackCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCarCatalogCompleteness() {
        val cars = CarCatalog.cars
        assertEquals(6, cars.size)

        // Starter car is free
        val starter = cars.first()
        assertEquals("apex_gt", starter.id)
        assertEquals(0, starter.unlockPrice)

        // All cars have valid stats
        cars.forEach { car ->
            assertTrue(car.baseTopSpeed >= 180f)
            assertTrue(car.baseHandling in 0.5f..1.0f)
            assertTrue(car.baseArmor in 0.5f..1.0f)
        }

        // Upgrade costs increase
        assertTrue(CarCatalog.getUpgradeCost(1) < CarCatalog.getUpgradeCost(2))
        assertTrue(CarCatalog.getUpgradeCost(2) < CarCatalog.getUpgradeCost(3))
        assertTrue(CarCatalog.getUpgradeCost(3) < CarCatalog.getUpgradeCost(4))
    }

    @Test
    fun testTrackCatalogThemes() {
        val tracks = TrackCatalog.tracks
        assertEquals(6, tracks.size)

        val trackIds = tracks.map { it.id }
        assertTrue(trackIds.contains("track_neon"))
        assertTrue(trackIds.contains("track_volcano"))
        assertTrue(trackIds.contains("track_toyland"))
        assertTrue(trackIds.contains("track_arctic"))
        assertTrue(trackIds.contains("track_desert"))
        assertTrue(trackIds.contains("track_candy"))

        // Each track has obstacles
        tracks.forEach { track ->
            assertTrue(track.lengthMeters >= 1500f)
            assertTrue("Track ${track.name} should have obstacles", track.obstacles.isNotEmpty())
            assertTrue("Track ${track.name} should have curve profile", track.curveProfile.isNotEmpty())
        }
    }

    @Test
    fun testGameEngineInitializationAndCountdown() {
        val track = TrackCatalog.getTrack("track_neon")
        val car = CarCatalog.getCar("apex_gt")

        val engine = GameEngine(
            track = track,
            playerCarModel = car,
            speedLevel = 1,
            handlingLevel = 1,
            armorLevel = 1,
            nitroLevel = 1,
            playerPaintColor = car.defaultPaint,
            playerNeonColor = car.defaultNeon,
            steeringSensitivity = 1.0f
        )

        assertFalse(engine.isRunning)
        assertTrue(engine.countdownTimer > 0f)
        assertEquals(5, engine.aiRacers.size)

        // Simulate 4 seconds to complete countdown
        for (i in 0 until 80) {
            engine.update(0.05f)
        }

        assertTrue(engine.isRunning)
        assertEquals(0f, engine.countdownTimer)
    }

    @Test
    fun testSteeringBounds() {
        val track = TrackCatalog.getTrack("track_neon")
        val car = CarCatalog.getCar("apex_gt")

        val engine = GameEngine(
            track = track,
            playerCarModel = car,
            speedLevel = 1,
            handlingLevel = 1,
            armorLevel = 1,
            nitroLevel = 1,
            playerPaintColor = car.defaultPaint,
            playerNeonColor = car.defaultNeon,
            steeringSensitivity = 1.0f
        )

        // Start engine
        engine.countdownTimer = 0f
        engine.isRunning = true

        // Swipe right extensively
        for (i in 0 until 10) {
            engine.onSwipeSteer(0.3f)
        }
        assertTrue(engine.player.targetLateralPos <= 0.85f)

        // Swipe left extensively
        for (i in 0 until 20) {
            engine.onSwipeSteer(-0.3f)
        }
        assertTrue(engine.player.targetLateralPos >= -0.85f)
    }

    @Test
    fun testAutomaticThrottleAndProgress() {
        val track = TrackCatalog.getTrack("track_neon")
        val car = CarCatalog.getCar("apex_gt")

        val engine = GameEngine(
            track = track,
            playerCarModel = car,
            speedLevel = 1,
            handlingLevel = 1,
            armorLevel = 1,
            nitroLevel = 1,
            playerPaintColor = car.defaultPaint,
            playerNeonColor = car.defaultNeon
        )

        engine.countdownTimer = 0f
        engine.isRunning = true

        val initialDistance = engine.player.distance

        // Simulate 2 seconds of racing
        for (i in 0 until 40) {
            engine.update(0.05f)
        }

        assertTrue("Player should accelerate automatically", engine.player.speed > 0f)
        assertTrue("Player should travel forward automatically", engine.player.distance > initialDistance)
    }

    @Test
    fun testNitroBoostActivation() {
        val track = TrackCatalog.getTrack("track_neon")
        val car = CarCatalog.getCar("apex_gt")

        val engine = GameEngine(
            track = track,
            playerCarModel = car,
            speedLevel = 1,
            handlingLevel = 1,
            armorLevel = 1,
            nitroLevel = 1,
            playerPaintColor = car.defaultPaint,
            playerNeonColor = car.defaultNeon
        )

        engine.countdownTimer = 0f
        engine.isRunning = true

        engine.activateNitro()
        assertTrue(engine.player.nitroTimeRemaining > 0f)

        val events = engine.pollEvents()
        assertTrue(events.any { it.type == EventType.NITRO })
    }
}
