package com.suhood.core.database

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_tracks")
data class FavoriteTrackEntity(
    @PrimaryKey val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_history")
data class FocusHistoryEntity(
    @PrimaryKey val id: String,
    val durationMinutes: Int,
    val completedAt: Long,
    val wasCompleted: Boolean
)

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val repeatDaysCsv: String = "",
    val soundId: String = "default"
)

@Entity(tableName = "recent_tracks")
data class RecentTrackEntity(
    @PrimaryKey val trackId: String,
    val playedAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "quran_bookmarks")
data class QuranBookmarkEntity(
    @PrimaryKey val id: String,
    val surah: Int,
    val ayah: Int,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quran_last_read")
data class QuranLastReadEntity(
    @PrimaryKey val id: Int = 1,
    val surah: Int,
    val ayah: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface SuhoodDao {
    @Query("SELECT trackId FROM favorite_tracks ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE trackId = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE trackId = :id)")
    suspend fun isFavorite(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusHistory(entry: FocusHistoryEntity)

    @Query("SELECT * FROM focus_history ORDER BY completedAt DESC LIMIT 50")
    fun observeFocusHistory(): Flow<List<FocusHistoryEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAlarms(): Flow<List<AlarmEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlarm(alarm: AlarmEntity)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarm(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(entity: RecentTrackEntity)

    @Query("SELECT trackId FROM recent_tracks ORDER BY playedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(b: QuranBookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)

    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<QuranBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setLastRead(e: QuranLastReadEntity)

    @Query("SELECT * FROM quran_last_read WHERE id = 1 LIMIT 1")
    suspend fun getLastRead(): QuranLastReadEntity?
}

@Database(
    entities = [
        FavoriteTrackEntity::class,
        FocusHistoryEntity::class,
        AlarmEntity::class,
        RecentTrackEntity::class,
        QuranBookmarkEntity::class,
        QuranLastReadEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SuhoodDatabase : RoomDatabase() {
    abstract fun dao(): SuhoodDao

    companion object {
        @Volatile private var instance: SuhoodDatabase? = null

        fun get(context: Context): SuhoodDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SuhoodDatabase::class.java,
                    "suhood.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}
