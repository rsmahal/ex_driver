package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynthesizer
import com.example.data.AppDatabase
import com.example.data.CarStatus
import com.example.data.PlayerProfile
import com.example.data.TrackRecord
import com.example.game.EventType
import com.example.game.GameEngine
import com.example.model.CarCatalog
import com.example.model.TrackCatalog
import com.example.util.HapticHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    TRACK_SELECT,
    GARAGE,
    RACE,
    PODIUM,
    SETTINGS
}

data class RaceResult(
    val rank: Int,
    val timeSeconds: Float,
    val coinsEarned: Int,
    val trophiesEarned: Int,
    val stars: Int,
    val isNewRecord: Boolean,
    val trackName: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.gameDao()

    val soundSynthesizer = SoundSynthesizer()
    val hapticHelper = HapticHelper(application)

    private val _screen = MutableStateFlow(AppScreen.HOME)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _profile = MutableStateFlow(PlayerProfile())
    val profile: StateFlow<PlayerProfile> = _profile.asStateFlow()

    private val _cars = MutableStateFlow<List<CarStatus>>(emptyList())
    val cars: StateFlow<List<CarStatus>> = _cars.asStateFlow()

    private val _trackRecords = MutableStateFlow<List<TrackRecord>>(emptyList())
    val trackRecords: StateFlow<List<TrackRecord>> = _trackRecords.asStateFlow()

    private val _selectedTrackId = MutableStateFlow("track_neon")
    val selectedTrackId: StateFlow<String> = _selectedTrackId.asStateFlow()

    private val _currentGameEngine = MutableStateFlow<GameEngine?>(null)
    val currentGameEngine: StateFlow<GameEngine?> = _currentGameEngine.asStateFlow()

    private val _lastRaceResult = MutableStateFlow<RaceResult?>(null)
    val lastRaceResult: StateFlow<RaceResult?> = _lastRaceResult.asStateFlow()

    init {
        initializeDatabase()
    }

    private fun initializeDatabase() {
        viewModelScope.launch {
            // Collect profile
            launch {
                dao.getPlayerProfile().collect { p ->
                    if (p != null) {
                        _profile.value = p
                        soundSynthesizer.setMuted(!p.soundEnabled)
                    } else {
                        // First time launch setup
                        val defaultProf = PlayerProfile(
                            coins = 1200,
                            trophies = 0,
                            selectedCarId = "apex_gt",
                            soundEnabled = true,
                            hapticEnabled = true,
                            steeringSensitivity = 1.0f
                        )
                        dao.insertOrUpdateProfile(defaultProf)
                    }
                }
            }

            // Collect cars
            launch {
                dao.getAllCars().collect { carList ->
                    if (carList.isEmpty()) {
                        val initialCars = CarCatalog.cars.map { model ->
                            CarStatus(
                                carId = model.id,
                                isUnlocked = (model.unlockPrice == 0),
                                speedLevel = 1,
                                handlingLevel = 1,
                                armorLevel = 1,
                                nitroLevel = 1,
                                paintColor = model.defaultPaint,
                                neonColor = model.defaultNeon
                            )
                        }
                        dao.insertAllCars(initialCars)
                    } else {
                        _cars.value = carList
                    }
                }
            }

            // Collect tracks
            launch {
                dao.getAllTrackRecords().collect { records ->
                    if (records.isEmpty()) {
                        val initialTracks = TrackCatalog.tracks.mapIndexed { idx, track ->
                            TrackRecord(
                                trackId = track.id,
                                isUnlocked = (idx == 0)
                            )
                        }
                        dao.insertAllTrackRecords(initialTracks)
                    } else {
                        _trackRecords.value = records
                    }
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _screen.value = screen
    }

    fun selectTrack(trackId: String) {
        _selectedTrackId.value = trackId
    }

    fun startRace() {
        val currentCarId = _profile.value.selectedCarId
        val carStatus = _cars.value.find { it.carId == currentCarId } ?: _cars.value.firstOrNull()
        val carModel = CarCatalog.getCar(currentCarId)
        val trackData = TrackCatalog.getTrack(_selectedTrackId.value)

        val engine = GameEngine(
            track = trackData,
            playerCarModel = carModel,
            speedLevel = carStatus?.speedLevel ?: 1,
            handlingLevel = carStatus?.handlingLevel ?: 1,
            armorLevel = carStatus?.armorLevel ?: 1,
            nitroLevel = carStatus?.nitroLevel ?: 1,
            playerPaintColor = carStatus?.paintColor ?: carModel.defaultPaint,
            playerNeonColor = carStatus?.neonColor ?: carModel.defaultNeon,
            steeringSensitivity = _profile.value.steeringSensitivity
        )

        _currentGameEngine.value = engine
        _screen.value = AppScreen.RACE
    }

    fun onSteerInput(deltaNormalizedX: Float) {
        _currentGameEngine.value?.onSwipeSteer(deltaNormalizedX)
    }

    fun triggerNitro() {
        _currentGameEngine.value?.activateNitro()
    }

    fun updateGameLoop(deltaTime: Float) {
        val engine = _currentGameEngine.value ?: return
        engine.update(deltaTime)

        // Process audio/haptic events
        val events = engine.pollEvents()
        for (event in events) {
            when (event.type) {
                EventType.COUNTDOWN_TICK -> {
                    soundSynthesizer.playCountdownBeep(isFinalGo = false)
                    if (_profile.value.hapticEnabled) hapticHelper.vibrateShort()
                }
                EventType.COUNTDOWN_GO -> {
                    soundSynthesizer.playCountdownBeep(isFinalGo = true)
                    if (_profile.value.hapticEnabled) hapticHelper.vibrateHeavy()
                }
                EventType.BUMP -> {
                    soundSynthesizer.playBumpSound()
                    if (_profile.value.hapticEnabled) hapticHelper.vibrateShort()
                }
                EventType.COIN_COLLECT -> {
                    soundSynthesizer.playCoinSound()
                }
                EventType.NITRO, EventType.BOOST_PAD -> {
                    soundSynthesizer.playNitroSound()
                    if (_profile.value.hapticEnabled) hapticHelper.vibrateShort()
                }
                EventType.OBSTACLE_HIT -> {
                    soundSynthesizer.playBumpSound()
                    if (_profile.value.hapticEnabled) hapticHelper.vibrateHeavy()
                }
                EventType.LAP_COMPLETE -> {
                    soundSynthesizer.playVictoryFanfare()
                    if (_profile.value.hapticEnabled) hapticHelper.vibrateSuccess()
                    onRaceFinished(engine)
                }
                else -> {}
            }
        }
    }

    private fun onRaceFinished(engine: GameEngine) {
        val rank = engine.player.rank
        val timeSec = engine.raceTime
        val baseCoins = when (rank) {
            1 -> 600
            2 -> 400
            3 -> 250
            else -> 120
        }
        val totalCoinsEarned = baseCoins + engine.coinsEarnedThisRace
        val trophiesEarned = when (rank) {
            1 -> 3
            2 -> 2
            3 -> 1
            else -> 0
        }
        val stars = when (rank) {
            1 -> 3
            2 -> 2
            3 -> 1
            else -> 0
        }

        viewModelScope.launch {
            val prof = _profile.value
            val newProfile = prof.copy(
                coins = prof.coins + totalCoinsEarned,
                trophies = prof.trophies + trophiesEarned
            )
            dao.insertOrUpdateProfile(newProfile)

            // Update Track Record
            val existing = _trackRecords.value.find { it.trackId == engine.track.id }
            val isNewRecord = existing == null || existing.bestTimeSeconds == 0f || timeSec < existing.bestTimeSeconds
            val bestTime = if (isNewRecord) timeSec else (existing?.bestTimeSeconds ?: timeSec)
            val bestStars = maxOf(existing?.stars ?: 0, stars)

            dao.insertOrUpdateTrackRecord(
                TrackRecord(
                    trackId = engine.track.id,
                    isUnlocked = true,
                    bestTimeSeconds = bestTime,
                    bestPosition = minOf(existing?.bestPosition ?: 99, rank),
                    stars = bestStars
                )
            )

            // Check next track unlocks
            val tracks = TrackCatalog.tracks
            val currentIndex = tracks.indexOfFirst { it.id == engine.track.id }
            if (currentIndex >= 0 && currentIndex < tracks.size - 1) {
                val nextTrack = tracks[currentIndex + 1]
                if (newProfile.trophies >= nextTrack.unlockTrophies) {
                    dao.insertOrUpdateTrackRecord(
                        TrackRecord(
                            trackId = nextTrack.id,
                            isUnlocked = true
                        )
                    )
                }
            }

            _lastRaceResult.value = RaceResult(
                rank = rank,
                timeSeconds = timeSec,
                coinsEarned = totalCoinsEarned,
                trophiesEarned = trophiesEarned,
                stars = stars,
                isNewRecord = isNewRecord,
                trackName = engine.track.name
            )
            _screen.value = AppScreen.PODIUM
        }
    }

    fun purchaseCar(carId: String) {
        val carModel = CarCatalog.getCar(carId)
        val profile = _profile.value
        if (profile.coins >= carModel.unlockPrice) {
            viewModelScope.launch {
                dao.insertOrUpdateProfile(profile.copy(coins = profile.coins - carModel.unlockPrice))
                dao.insertOrUpdateCar(
                    CarStatus(
                        carId = carId,
                        isUnlocked = true,
                        speedLevel = 1,
                        handlingLevel = 1,
                        armorLevel = 1,
                        nitroLevel = 1,
                        paintColor = carModel.defaultPaint,
                        neonColor = carModel.defaultNeon
                    )
                )
                soundSynthesizer.playCoinSound()
                if (profile.hapticEnabled) hapticHelper.vibrateSuccess()
            }
        }
    }

    fun selectCar(carId: String) {
        viewModelScope.launch {
            dao.insertOrUpdateProfile(_profile.value.copy(selectedCarId = carId))
        }
    }

    fun upgradeStat(carId: String, statName: String) {
        val carStatus = _cars.value.find { it.carId == carId } ?: return
        val currentLvl = when (statName) {
            "speed" -> carStatus.speedLevel
            "handling" -> carStatus.handlingLevel
            "armor" -> carStatus.armorLevel
            "nitro" -> carStatus.nitroLevel
            else -> 1
        }
        if (currentLvl >= 5) return

        val cost = CarCatalog.getUpgradeCost(currentLvl)
        val prof = _profile.value
        if (prof.coins >= cost) {
            viewModelScope.launch {
                dao.insertOrUpdateProfile(prof.copy(coins = prof.coins - cost))
                val updatedCar = when (statName) {
                    "speed" -> carStatus.copy(speedLevel = currentLvl + 1)
                    "handling" -> carStatus.copy(handlingLevel = currentLvl + 1)
                    "armor" -> carStatus.copy(armorLevel = currentLvl + 1)
                    "nitro" -> carStatus.copy(nitroLevel = currentLvl + 1)
                    else -> carStatus
                }
                dao.insertOrUpdateCar(updatedCar)
                soundSynthesizer.playCoinSound()
                if (prof.hapticEnabled) hapticHelper.vibrateShort()
            }
        }
    }

    fun setPaintColor(carId: String, color: Long) {
        val car = _cars.value.find { it.carId == carId } ?: return
        viewModelScope.launch {
            dao.insertOrUpdateCar(car.copy(paintColor = color))
        }
    }

    fun setNeonColor(carId: String, color: Long) {
        val car = _cars.value.find { it.carId == carId } ?: return
        viewModelScope.launch {
            dao.insertOrUpdateCar(car.copy(neonColor = color))
        }
    }

    fun toggleSound() {
        val newSound = !_profile.value.soundEnabled
        viewModelScope.launch {
            dao.insertOrUpdateProfile(_profile.value.copy(soundEnabled = newSound))
            soundSynthesizer.setMuted(!newSound)
        }
    }

    fun toggleHaptics() {
        val newHaptic = !_profile.value.hapticEnabled
        viewModelScope.launch {
            dao.insertOrUpdateProfile(_profile.value.copy(hapticEnabled = newHaptic))
        }
    }

    fun setSensitivity(sensitivity: Float) {
        viewModelScope.launch {
            dao.insertOrUpdateProfile(_profile.value.copy(steeringSensitivity = sensitivity))
        }
    }
}
