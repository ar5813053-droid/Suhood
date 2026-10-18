package com.suhood.core.model

import java.util.UUID

// ── Prayer (demo) ───────────────────────────────────────────────────────────
data class PrayerTime(
    val name: String,
    val time: String, // HH:mm display
    val isNext: Boolean = false,
    val isPassed: Boolean = false
)

data class DemoPrayerSchedule(
    val dateLabel: String,
    val hijriLabel: String,
    val nextPrayer: PrayerTime,
    val countdownSeconds: Int,
    val times: List<PrayerTime>
)

// ── Focus ───────────────────────────────────────────────────────────────────
data class DistractingApp(
    val id: String,
    val name: String,
    val packageName: String,
    val isSelected: Boolean = false
)

enum class FocusSessionState { IDLE, RUNNING, PAUSED, COMPLETED, CANCELLED }

data class FocusSession(
    val id: String = UUID.randomUUID().toString(),
    val durationMinutes: Int,
    val remainingSeconds: Int,
    val state: FocusSessionState = FocusSessionState.IDLE,
    val selectedAppIds: List<String> = emptyList(),
    val startedAt: Long? = null
)

data class FocusHistoryEntry(
    val id: String,
    val durationMinutes: Int,
    val completedAt: Long,
    val wasCompleted: Boolean
)

// ── Alarms ──────────────────────────────────────────────────────────────────
data class AlarmConfig(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val enabled: Boolean = true,
    val repeatDays: Set<Int> = emptySet(), // 1=Mon … 7=Sun
    val soundId: String = "default",
    val challengeType: ChallengeType = ChallengeType.NONE,
    val preFajrMinutes: Int = 0
)

enum class ChallengeType { NONE, QUIZ, DHIKR }

// ── Quran (placeholder-aware) ───────────────────────────────────────────────
data class SurahInfo(
    val id: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val nameEnglish: String,
    val ayahCount: Int,
    val revelationType: String
)

data class QuranBookmark(
    val id: String = UUID.randomUUID().toString(),
    val surahId: Int,
    val ayahNumber: Int,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ── Worship / Tasbeeh ───────────────────────────────────────────────────────
data class TasbeehSession(
    val id: String = UUID.randomUUID().toString(),
    val dhikrText: String,
    val target: Int,
    val count: Int = 0,
    val completedAt: Long? = null
)

data class DhikrItem(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val defaultTarget: Int = 33
)

data class DuaItem(
    val id: String,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val category: String
)

// ── Quiz ────────────────────────────────────────────────────────────────────
data class QuizQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val difficulty: QuizDifficulty
)

enum class QuizDifficulty { EASY, MEDIUM, HARD }

data class QuizResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val answers: List<Boolean>
)

// ── Nasheedify Audio ────────────────────────────────────────────────────────
data class AudioTrack(
    val id: String,
    val title: String,
    val creator: String,
    val collection: String,
    val categoryId: String,
    val language: String,
    val durationSeconds: Int,
    val artworkKey: String = "default",
    val isDemo: Boolean = true
)

data class AudioCategory(
    val id: String,
    val name: String,
    val description: String,
    val trackCount: Int
)

data class AudioCreator(
    val id: String,
    val name: String,
    val bio: String = ""
)

data class AudioCollection(
    val id: String,
    val title: String,
    val creatorId: String,
    val trackIds: List<String>,
    val year: Int = 2024
)

data class UserPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trackIds: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val queue: List<AudioTrack> = emptyList(),
    val queueIndex: Int = 0,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val isDemoPlayback: Boolean = true
)

enum class RepeatMode { OFF, ONE, ALL }

// ── Settings / Preferences ──────────────────────────────────────────────────
data class UserPreferences(
    val themeMode: String = "DARK",
    val language: String = "en",
    val onboardingCompleted: Boolean = false,
    val reducedMotion: Boolean = false
)
