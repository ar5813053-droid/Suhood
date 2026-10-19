package com.suhood.core.data

import com.suhood.core.model.*

/**
 * Bundled offline catalog used only when MediaStore has no audio.
 * All religious content is either verified sample or clearly labelled illustrative.
 */
object DemoData {

    // ── Prayer times (DEMO — not location-based) ────────────────────────────
    fun demoPrayerSchedule(): DemoPrayerSchedule {
        val times = listOf(
            PrayerTime("Fajr", "05:12", isPassed = true),
            PrayerTime("Dhuhr", "12:18", isPassed = true),
            PrayerTime("Asr", "15:42", isNext = true),
            PrayerTime("Maghrib", "18:05"),
            PrayerTime("Isha", "19:30")
        )
        return DemoPrayerSchedule(
            dateLabel = "Sunday, 18 Oct 2026",
            hijriLabel = "Demo Hijri · 26 Rabiʻ II 1448 (illustrative)",
            nextPrayer = times.first { it.isNext },
            countdownSeconds = 3720, // ~1h 2m illustrative
            times = times
        )
    }

    // ── Distracting apps (demo) ─────────────────────────────────────────────
    val distractingApps = listOf(
        DistractingApp("1", "Social Feed", "com.demo.social", true),
        DistractingApp("2", "Short Videos", "com.demo.shorts", true),
        DistractingApp("3", "Games Hub", "com.demo.games", false),
        DistractingApp("4", "Chat App", "com.demo.chat", true),
        DistractingApp("5", "News Scroll", "com.demo.news", false),
        DistractingApp("6", "Streaming", "com.demo.stream", false)
    )

    // ── Surah list (metadata only — no fabricated verses) ───────────────────
    val surahList = listOf(
        SurahInfo(1, "الفاتحة", "Al-Fatihah", "The Opening", 7, "Meccan"),
        SurahInfo(2, "البقرة", "Al-Baqarah", "The Cow", 286, "Medinan"),
        SurahInfo(3, "آل عمران", "Ali 'Imran", "Family of Imran", 200, "Medinan"),
        SurahInfo(4, "النساء", "An-Nisa", "The Women", 176, "Medinan"),
        SurahInfo(5, "المائدة", "Al-Ma'idah", "The Table", 120, "Medinan"),
        SurahInfo(6, "الأنعام", "Al-An'am", "The Cattle", 165, "Meccan"),
        SurahInfo(7, "الأعراف", "Al-A'raf", "The Heights", 206, "Meccan"),
        SurahInfo(18, "الكهف", "Al-Kahf", "The Cave", 110, "Meccan"),
        SurahInfo(36, "يس", "Ya-Sin", "Ya Sin", 83, "Meccan"),
        SurahInfo(55, "الرحمن", "Ar-Rahman", "The Beneficent", 78, "Medinan"),
        SurahInfo(67, "الملك", "Al-Mulk", "The Sovereignty", 30, "Meccan"),
        SurahInfo(112, "الإخلاص", "Al-Ikhlas", "The Sincerity", 4, "Meccan"),
        SurahInfo(113, "الفلق", "Al-Falaq", "The Daybreak", 5, "Meccan"),
        SurahInfo(114, "الناس", "An-Nas", "Mankind", 6, "Meccan")
    )

    // ── Dhikr ───────────────────────────────────────────────────────────────
    val dhikrItems = listOf(
        DhikrItem("1", "سُبْحَانَ اللَّهِ", "SubhanAllah", "Glory be to Allah", 33),
        DhikrItem("2", "الْحَمْدُ لِلَّهِ", "Alhamdulillah", "All praise is due to Allah", 33),
        DhikrItem("3", "اللَّهُ أَكْبَرُ", "Allahu Akbar", "Allah is the Greatest", 34),
        DhikrItem("4", "لَا إِلَٰهَ إِلَّا اللَّهُ", "La ilaha illallah", "There is no god but Allah", 100),
        DhikrItem("5", "أَسْتَغْفِرُ اللَّهَ", "Astaghfirullah", "I seek forgiveness from Allah", 100),
        DhikrItem("6", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "SubhanAllahi wa bihamdihi", "Glory and praise be to Allah", 100)
    )

    // ── Duas (sample, respectful) ───────────────────────────────────────────
    val duaItems = listOf(
        DuaItem(
            "1", "Upon waking",
            "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur",
            "All praise is for Allah who gave us life after having taken it from us, and unto Him is the resurrection.",
            "Morning"
        ),
        DuaItem(
            "2", "Before eating",
            "بِسْمِ اللَّهِ",
            "Bismillah",
            "In the name of Allah.",
            "Daily"
        ),
        DuaItem(
            "3", "After eating",
            "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
            "Alhamdu lillahil-ladhi at'amana wa saqana wa ja'alana muslimin",
            "All praise is for Allah who fed us and gave us drink and made us Muslims.",
            "Daily"
        ),
        DuaItem(
            "4", "Entering the mosque",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "Allahumma iftah li abwaba rahmatik",
            "O Allah, open the gates of Your mercy for me.",
            "Mosque"
        )
    )

    // ── Quiz (carefully checked demo set) ───────────────────────────────────
    val quizQuestions = listOf(
        QuizQuestion(
            "q1",
            "How many pillars of Islam are there?",
            listOf("3", "4", "5", "6"),
            2,
            "The five pillars are: Shahada, Salah, Zakat, Sawm, and Hajj.",
            QuizDifficulty.EASY
        ),
        QuizQuestion(
            "q2",
            "What is the first month of the Islamic calendar?",
            listOf("Ramadan", "Muharram", "Shawwal", "Dhul Hijjah"),
            1,
            "Muharram is the first month of the Hijri calendar.",
            QuizDifficulty.EASY
        ),
        QuizQuestion(
            "q3",
            "Which prayer is performed at dawn?",
            listOf("Dhuhr", "Asr", "Fajr", "Isha"),
            2,
            "Fajr is the dawn prayer.",
            QuizDifficulty.EASY
        ),
        QuizQuestion(
            "q4",
            "What does 'SubhanAllah' mean?",
            listOf("All praise is due to Allah", "Glory be to Allah", "Allah is the Greatest", "There is no god but Allah"),
            1,
            "SubhanAllah means 'Glory be to Allah'.",
            QuizDifficulty.MEDIUM
        ),
        QuizQuestion(
            "q5",
            "How many verses (ayat) are in Surah Al-Fatihah?",
            listOf("5", "6", "7", "8"),
            2,
            "Surah Al-Fatihah consists of 7 verses.",
            QuizDifficulty.MEDIUM
        ),
        QuizQuestion(
            "q6",
            "What is the night journey of the Prophet ﷺ commonly called?",
            listOf("Hijra", "Isra and Mi'raj", "Battle of Badr", "Treaty of Hudaybiyyah"),
            1,
            "Isra and Mi'raj refers to the night journey and ascension.",
            QuizDifficulty.HARD
        )
    )

    // ── Audio catalog (demo metadata only) ──────────────────────────────────
    val audioCategories = listOf(
        AudioCategory("nasheeds", "Nasheeds", "Vocal Islamic songs and anthems", 8),
        AudioCategory("madah", "Madah", "Devotional poetry and praise", 4),
        AudioCategory("tilawat", "Quran Tilawat", "Recitation of the Quran", 6),
        AudioCategory("adhkar", "Adhkar", "Remembrance and morning/evening adhkar", 5),
        AudioCategory("duas", "Duas", "Supplications", 4),
        AudioCategory("lectures", "Lectures", "Islamic reminders and talks", 3),
        AudioCategory("reminders", "Reminders", "Short reminders", 4)
    )

        val audioTracks = listOf(
        AudioTrack("t1", "Morning Light", "Demo Ensemble", "Dawn Collection", "nasheeds", "English", 210, titleAr = "نور الصباح", creatorAr = "فرقة تجريبية"),
        AudioTrack("t2", "Path of Peace", "Demo Ensemble", "Dawn Collection", "nasheeds", "English", 185, titleAr = "طريق السلام", creatorAr = "فرقة تجريبية"),
        AudioTrack("t3", "Ya Rahman", "Demo Vocalists", "Devotion", "madah", "Arabic", 240, titleAr = "يا رحمن", creatorAr = "منشدون"),
        AudioTrack("t4", "Al-Fatihah (Demo)", "Demo Qari", "Short Surahs", "tilawat", "Arabic", 95, titleAr = "الفاتحة", creatorAr = "قارئ تجريبي"),
        AudioTrack("t5", "Morning Adhkar", "Demo Reciter", "Daily Adhkar", "adhkar", "Arabic", 320, titleAr = "أذكار الصباح", creatorAr = "مُذكّر"),
        AudioTrack("t6", "Evening Adhkar", "Demo Reciter", "Daily Adhkar", "adhkar", "Arabic", 280, titleAr = "أذكار المساء", creatorAr = "مُذكّر"),
        AudioTrack("t7", "Dua for Guidance", "Demo Reciter", "Selected Duas", "duas", "Arabic", 60, titleAr = "دعاء الهداية", creatorAr = "مُذكّر"),
        AudioTrack("t8", "Remembering the Hereafter", "Demo Speaker", "Short Talks", "lectures", "English", 420, titleAr = "تذكرة الآخرة", creatorAr = "محاضر"),
        AudioTrack("t9", "Gratitude Reminder", "Demo Speaker", "Short Talks", "reminders", "English", 90, titleAr = "تذكير الشكر", creatorAr = "محاضر"),
        AudioTrack("t10", "Patience in Hardship", "Demo Speaker", "Short Talks", "reminders", "English", 110, titleAr = "الصبر في البلاء", creatorAr = "محاضر"),
        AudioTrack("t11", "Heart at Rest", "Demo Ensemble", "Calm", "nasheeds", "English", 195, titleAr = "قلب مطمئن", creatorAr = "فرقة تجريبية"),
        AudioTrack("t12", "Tawakkul", "Demo Vocalists", "Devotion", "madah", "Arabic", 220, titleAr = "توكل", creatorAr = "منشدون")
    )

    val sampleAlarms = listOf(
        AlarmConfig(id = "a1", hour = 5, minute = 0, label = "Fajr (Demo)", enabled = true, repeatDays = setOf(1, 2, 3, 4, 5, 6, 7), challengeType = ChallengeType.QUIZ),
        AlarmConfig(id = "a2", hour = 6, minute = 30, label = "Morning Adhkar", enabled = false, repeatDays = setOf(1, 2, 3, 4, 5), challengeType = ChallengeType.DHIKR)
    )
}
