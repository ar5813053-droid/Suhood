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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    val soundUri: String = "",
    val challengeType: String = "NONE",
    val challengeDifficulty: Int = 1,
    val isFajrAuto: Boolean = false
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

/** Per-prayer daily log. Status: JAMA AH, ON_TIME, LATE, NOT_PRAYED */
@Entity(tableName = "salah_logs")
data class SalahLogEntity(
    @PrimaryKey val id: String,
    val dateIso: String, // yyyy-MM-dd
    val prayerName: String, // Fajr, Dhuhr, Asr, Maghrib, Isha
    val status: String,
    val updatedAt: Long = System.currentTimeMillis()
)


@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val category: String = "custom", // salah, quran, hifz, dhikr, custom
    val targetCount: Int = 1,
    val frequency: String = "daily", // daily, weekly
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey val id: String, // habitId|dateIso
    val habitId: String,
    val dateIso: String,
    val count: Int = 1,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "muhasabah_entries")
data class MuhasabahEntryEntity(
    @PrimaryKey val dateIso: String, // yyyy-MM-dd
    val reflection: String = "",
    val gratitude: String = "",
    val lessons: String = "",
    val intention: String = "",
    val prompt: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasbeeh_state")
data class TasbeehStateEntity(
    @PrimaryKey val id: Int = 1,
    val count: Int = 0,
    val goal: Int = 33,
    val dhikrIndex: Int = 0,
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

    @Query("SELECT * FROM alarms")
    suspend fun getAllAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarm(id: String): AlarmEntity?

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

    // Salah
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSalahLog(log: SalahLogEntity)

    @Query("SELECT * FROM salah_logs WHERE dateIso = :dateIso")
    fun observeSalahForDate(dateIso: String): Flow<List<SalahLogEntity>>

    @Query("SELECT * FROM salah_logs WHERE dateIso = :dateIso")
    suspend fun getSalahForDate(dateIso: String): List<SalahLogEntity>

    @Query("SELECT * FROM salah_logs WHERE dateIso BETWEEN :fromIso AND :toIso")
    suspend fun getSalahBetween(fromIso: String, toIso: String): List<SalahLogEntity>

    @Query("SELECT * FROM salah_logs ORDER BY dateIso DESC")
    fun observeAllSalahLogs(): Flow<List<SalahLogEntity>>

    // Tasbeeh
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasbeeh(state: TasbeehStateEntity)

    @Query("SELECT * FROM tasbeeh_state WHERE id = 1 LIMIT 1")
    fun observeTasbeeh(): Flow<TasbeehStateEntity?>

    @Query("SELECT * FROM tasbeeh_state WHERE id = 1 LIMIT 1")
    suspend fun getTasbeeh(): TasbeehStateEntity?

    // Habits
    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabit(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: String)

    @Query("SELECT * FROM habit_completions WHERE dateIso = :dateIso")
    fun observeHabitCompletionsForDate(dateIso: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY dateIso DESC")
    fun observeHabitCompletions(habitId: String): Flow<List<HabitCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitCompletion(c: HabitCompletionEntity)

    @Query("SELECT * FROM habit_completions WHERE dateIso >= :fromIso AND dateIso <= :toIso")
    suspend fun habitCompletionsInRange(fromIso: String, toIso: String): List<HabitCompletionEntity>

    // Muhasabah
    @Query("SELECT * FROM muhasabah_entries ORDER BY dateIso DESC")
    fun observeMuhasabah(): Flow<List<MuhasabahEntryEntity>>

    @Query("SELECT * FROM muhasabah_entries WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getMuhasabah(dateIso: String): MuhasabahEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMuhasabah(entry: MuhasabahEntryEntity)

    @Query("DELETE FROM muhasabah_entries WHERE dateIso = :dateIso")
    suspend fun deleteMuhasabah(dateIso: String)

    @Query("SELECT * FROM muhasabah_entries WHERE reflection LIKE '%' || :q || '%' OR gratitude LIKE '%' || :q || '%' OR lessons LIKE '%' || :q || '%' OR intention LIKE '%' || :q || '%' ORDER BY dateIso DESC")
    suspend fun searchMuhasabah(q: String): List<MuhasabahEntryEntity>

}



val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS habits (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT 'custom',
                targetCount INTEGER NOT NULL DEFAULT 1,
                frequency TEXT NOT NULL DEFAULT 'daily',
                reminderHour INTEGER,
                reminderMinute INTEGER,
                enabled INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS habit_completions (
                id TEXT NOT NULL PRIMARY KEY,
                habitId TEXT NOT NULL,
                dateIso TEXT NOT NULL,
                count INTEGER NOT NULL DEFAULT 1,
                completedAt INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS muhasabah_entries (
                dateIso TEXT NOT NULL PRIMARY KEY,
                reflection TEXT NOT NULL DEFAULT '',
                gratitude TEXT NOT NULL DEFAULT '',
                lessons TEXT NOT NULL DEFAULT '',
                intention TEXT NOT NULL DEFAULT '',
                prompt TEXT NOT NULL DEFAULT '',
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS salah_logs (
                id TEXT NOT NULL PRIMARY KEY,
                dateIso TEXT NOT NULL,
                prayerName TEXT NOT NULL,
                status TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS tasbeeh_state (
                id INTEGER NOT NULL PRIMARY KEY,
                count INTEGER NOT NULL,
                goal INTEGER NOT NULL,
                dhikrIndex INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )"""
        )
    }
}

@Database(
    entities = [
        FavoriteTrackEntity::class,
        FocusHistoryEntity::class,
        AlarmEntity::class,
        RecentTrackEntity::class,
        QuranBookmarkEntity::class,
        QuranLastReadEntity::class,
        SalahLogEntity::class,
        TasbeehStateEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        MuhasabahEntryEntity::class
    ],
    version = 5,
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
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
