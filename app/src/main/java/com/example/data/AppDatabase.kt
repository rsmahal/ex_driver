package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val coins: Int = 1500,
    val trophies: Int = 0,
    val selectedCarId: String = "apex_gt",
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val steeringSensitivity: Float = 1.0f
)

@Entity(tableName = "car_status")
data class CarStatus(
    @PrimaryKey val carId: String,
    val isUnlocked: Boolean,
    val speedLevel: Int = 1,
    val handlingLevel: Int = 1,
    val armorLevel: Int = 1,
    val nitroLevel: Int = 1,
    val paintColor: Long = 0xFFFF1744, // Default Red
    val neonColor: Long = 0xFF00E5FF  // Default Cyan Neon
)

@Entity(tableName = "track_record")
data class TrackRecord(
    @PrimaryKey val trackId: String,
    val isUnlocked: Boolean = false,
    val bestTimeSeconds: Float = 0f,
    val bestPosition: Int = 0,
    val stars: Int = 0
)

@Dao
interface GameDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getPlayerProfileSync(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfile)

    @Query("SELECT * FROM car_status")
    fun getAllCars(): Flow<List<CarStatus>>

    @Query("SELECT * FROM car_status WHERE carId = :carId")
    suspend fun getCarStatus(carId: String): CarStatus?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCar(car: CarStatus)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCars(cars: List<CarStatus>)

    @Query("SELECT * FROM track_record")
    fun getAllTrackRecords(): Flow<List<TrackRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTrackRecord(record: TrackRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTrackRecords(records: List<TrackRecord>)
}

@Database(entities = [PlayerProfile::class, CarStatus::class, TrackRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ex_driver_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
