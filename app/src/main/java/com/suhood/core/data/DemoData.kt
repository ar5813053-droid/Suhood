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

    // ── Duas (verified everyday adhkar — offline searchable) ────────────────
    val duaItems = listOf(
        DuaItem(
            "1", "Upon waking",
            "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            "Alhamdu lillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur",
            "All praise is for Allah who gave us life after having taken it from us, and unto Him is the resurrection.",
            "Morning",
            listOf("waking", "wake up", "subah", "subah uthe", "uthne", "morning", "أحيانا", "الحمده"),
            "Bukhari"
        ),
        DuaItem(
            "2", "Before sleeping",
            "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            "Bismika Allahumma amutu wa ahya",
            "In Your name, O Allah, I die and I live.",
            "Sleep",
            listOf("sleep", "sone", "sone se pehle", "before sleep", "bed", "night", "nam", "sleeping", "النوم", "سونے"),
            "Bukhari"
        ),
        DuaItem(
            "3", "Before eating",
            "بِسْمِ اللَّهِ",
            "Bismillah",
            "In the name of Allah.",
            "Food",
            listOf("eat", "eating", "khana", "khane se pehle", "food", "before meal", "bismillah", "کھانا"),
            "Abu Dawud"
        ),
        DuaItem(
            "4", "After eating",
            "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
            "Alhamdu lillahil-ladhi at'amana wa saqana wa ja'alana muslimin",
            "All praise is for Allah who fed us and gave us drink and made us Muslims.",
            "Food",
            listOf("after eat", "khana ke baad", "food", "alhamdulillah", "meal", "کھانے کے بعد"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "5", "When wearing clothes",
            "الْحَمْدُ لِلَّهِ الَّذِي كَسَانِي هَذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            "Alhamdu lillahil-ladhi kasani hadha wa razaqanihi min ghayri hawlin minni wa la quwwah",
            "All praise is for Allah who clothed me with this and provided it for me with no power or might from myself.",
            "Clothes",
            listOf("clothes", "kapde", "kapde pehenne", "pehenne", "dress", "wearing", "لبس", "کپڑے", "clothing"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "6", "Entering the home",
            "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا",
            "Bismillahi walajna, wa bismillahi kharajna, wa 'ala Allahi rabbina tawakkalna",
            "In the name of Allah we enter, in the name of Allah we leave, and upon our Lord we place our trust.",
            "Home",
            listOf("home", "ghar", "ghar mein", "enter home", "entering house", "house", "گھر"),
            "Abu Dawud"
        ),
        DuaItem(
            "7", "Leaving the home",
            "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "Bismillah, tawakkaltu 'ala Allah, wa la hawla wa la quwwata illa billah",
            "In the name of Allah, I place my trust in Allah, and there is no might nor power except with Allah.",
            "Home",
            listOf("leave home", "ghar se nikal", "nikalne", "going out", "exit", "travel start", "گھر سے"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "8", "Entering the mosque",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "Allahumma iftah li abwaba rahmatik",
            "O Allah, open the gates of Your mercy for me.",
            "Mosque",
            listOf("mosque", "masjid", "enter mosque", "masjid mein", "مسجد"),
            "Muslim"
        ),
        DuaItem(
            "9", "Leaving the mosque",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            "Allahumma inni as'aluka min fadlik",
            "O Allah, I ask You from Your bounty.",
            "Mosque",
            listOf("leave mosque", "masjid se", "exit mosque", "فضل"),
            "Muslim"
        ),
        DuaItem(
            "10", "When travelling",
            "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ",
            "Subhanalladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila rabbina la-munqalibun",
            "Glory be to Him who has subjected this to us, and we could never have it (by our efforts). And indeed, to our Lord we will return.",
            "Travel",
            listOf("travel", "safar", "journey", "car", "plane", "سفر", "سفر کی دعا"),
            "Quran 43:13-14"
        ),
        DuaItem(
            "11", "Entering the bathroom",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
            "Allahumma inni a'udhu bika minal-khubthi wal-khaba'ith",
            "O Allah, I seek refuge in You from the male and female unclean spirits.",
            "Bathroom",
            listOf("bathroom", "toilet", "wc", "latrine", "guslkhana", "enter toilet", "خلاء"),
            "Bukhari, Muslim"
        ),
        DuaItem(
            "12", "Leaving the bathroom",
            "غُفْرَانَكَ",
            "Ghufranaka",
            "I ask You (Allah) for forgiveness.",
            "Bathroom",
            listOf("leave bathroom", "toilet se", "after toilet", "غفرانك"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "13", "Seeking forgiveness",
            "أَسْتَغْفِرُ اللَّهَ",
            "Astaghfirullah",
            "I seek forgiveness from Allah.",
            "Forgiveness",
            listOf("forgiveness", "astaghfirullah", "tauba", "repent", "maghfirat", "استغفر"),
            "Well-known dhikr"
        ),
        DuaItem(
            "14", "For parents",
            "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            "Rabbir-hamhuma kama rabbayani saghira",
            "My Lord, have mercy upon them as they brought me up when I was small.",
            "Family",
            listOf("parents", "walidain", "mother", "father", "amma", "abba", "والدين", "والدین"),
            "Quran 17:24"
        ),
        DuaItem(
            "15", "In distress",
            "لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            "La ilaha illa anta subhanaka inni kuntu minaz-zalimin",
            "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            "Distress",
            listOf("distress", "anxiety", "hardship", "trouble", "yusuf", "stress", "پریشانی"),
            "Quran 21:87"
        ),
        DuaItem(
            "16", "Protection",
            "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            "A'udhu bi kalimatillahit-tammati min sharri ma khalaq",
            "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            "Protection",
            listOf("protection", "refuge", "evil", "hifazat", "protect", "شر"),
            "Muslim"
        ),

        DuaItem(
            "17", "For travel (mounting)",
            "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَى رَبِّنَا لَمُنقَلِبُونَ",
            "Subhanalladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila rabbina lamunqalibun",
            "Glory to Him who has subjected this to us, and we could never have it by our efforts. And indeed, to our Lord we will return.",
            "Travel",
            listOf("safar", "travel", "journey", "safar ki dua", "musafir", "trip", "سخر", "سفر"),
            "Quran 43:13-14"
        ),
        DuaItem(
            "18", "Leaving home for travel",
            "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "Bismillahi tawakkaltu 'ala Allah, wa la hawla wa la quwwata illa billah",
            "In the name of Allah, I place my trust in Allah, and there is no power nor might except with Allah.",
            "Travel",
            listOf("safar", "travel", "leaving home", "safar ki dua", "tawakkul", "journey"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "19", "When distressed",
            "لَا إِلَهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            "La ilaha illa anta subhanaka inni kuntu minaz-zalimin",
            "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            "Distress",
            listOf("pareshani", "distress", "hardship", "pareshani ki dua", "trouble", "anxiety", "yunus", "ظالمين"),
            "Quran 21:87"
        ),
        DuaItem(
            "20", "For anxiety and sorrow",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ",
            "Allahumma inni a'udhu bika minal-hammi wal-hazan",
            "O Allah, I seek refuge in You from anxiety and sorrow.",
            "Distress",
            listOf("pareshani", "anxiety", "sorrow", "gham", "pareshani ki dua", "worry", "stress", "هم"),
            "Bukhari"
        ),
        DuaItem(
            "21", "Entering the toilet",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
            "Allahumma inni a'udhu bika minal-khubuthi wal-khaba'ith",
            "O Allah, I seek refuge in You from the male and female unclean spirits.",
            "Daily",
            listOf("toilet", "bathroom", "wc", "ghusl room", "enter toilet"),
            "Bukhari, Muslim"
        ),
        DuaItem(
            "22", "Leaving the toilet",
            "غُفْرَانَكَ",
            "Ghufranak",
            "I seek Your forgiveness.",
            "Daily",
            listOf("toilet", "bathroom", "leaving toilet", "ghufranaka"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "23", "Before ablution",
            "بِسْمِ اللَّهِ",
            "Bismillah",
            "In the name of Allah.",
            "Worship",
            listOf("wudu", "ablution", "before wudu", "wuzu"),
            "Abu Dawud, Ibn Majah"
        ),
        DuaItem(
            "24", "After ablution",
            "أَشْهَدُ أَنْ لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ",
            "Ashhadu an la ilaha illallahu wahdahu la sharika lah, wa ashhadu anna Muhammadan 'abduhu wa rasuluh",
            "I bear witness that there is no deity but Allah alone, without partner, and I bear witness that Muhammad is His servant and messenger.",
            "Worship",
            listOf("wudu", "after wudu", "ablution", "wuzu ke baad"),
            "Muslim"
        ),
        DuaItem(
            "25", "Entering the mosque",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "Allahumma iftah li abwaba rahmatik",
            "O Allah, open for me the gates of Your mercy.",
            "Worship",
            listOf("mosque", "masjid", "enter mosque", "masjid me dakhil"),
            "Muslim"
        ),
        DuaItem(
            "26", "Leaving the mosque",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            "Allahumma inni as'aluka min fadlik",
            "O Allah, I ask You from Your bounty.",
            "Worship",
            listOf("mosque", "masjid", "leave mosque", "masjid se bahar"),
            "Muslim"
        ),
        DuaItem(
            "27", "Istikhara (opening)",
            "اللَّهُمَّ إِنِّي أَسْتَخِيرُكَ بِعِلْمِكَ وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ",
            "Allahumma inni astakhiruka bi'ilmika wa astaqdiruka biqudratik",
            "O Allah, I seek Your guidance by Your knowledge and I seek ability by Your power.",
            "Guidance",
            listOf("istikhara", "decision", "guidance", "choice", "mashwara"),
            "Bukhari"
        ),
        DuaItem(
            "28", "For parents",
            "رَبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            "Rabbi irhamhuma kama rabbayani saghira",
            "My Lord, have mercy upon them as they brought me up when I was small.",
            "Family",
            listOf("parents", "walidain", "mother", "father", "maa baap", "dua for parents"),
            "Quran 17:24"
        ),
        DuaItem(
            "29", "Morning — Asbahna",
            "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            "Asbahna wa asbahal-mulku lillah, wal-hamdu lillah, la ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa huwa 'ala kulli shay'in qadir",
            "We have entered the morning and at this time all dominion belongs to Allah. All praise is for Allah. None has the right to be worshipped except Allah, alone, without partner. To Him belongs dominion and praise, and He is over all things competent.",
            "Morning",
            listOf("asbahna", "morning adhkar", "subah", "morning zikr", "صبح"),
            "Abu Dawud"
        ),
        DuaItem(
            "30", "Evening — Amsayna",
            "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
            "Amsayna wa amsal-mulku lillah, wal-hamdu lillah, la ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa huwa 'ala kulli shay'in qadir",
            "We have entered the evening and at this time all dominion belongs to Allah. All praise is for Allah. None has the right to be worshipped except Allah, alone, without partner.",
            "Evening",
            listOf("amsayna", "evening", "shaam", "evening adhkar", "شام"),
            "Abu Dawud"
        ),
        DuaItem(
            "31", "Sayyid al-Istighfar",
            "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَىٰ عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "Allahumma anta Rabbi la ilaha illa anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bi ni'matika 'alayya, wa abu'u bi dhanbi faghfir li fa innahu la yaghfirudh-dhunuba illa anta",
            "O Allah, You are my Lord, none has the right to be worshipped except You. You created me and I am Your servant. I try to keep Your covenant and promise as much as I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for none forgives sins except You.",
            "Morning",
            listOf("istighfar", "sayyid", "forgiveness", "astaghfirullah", "مغفرة", "توبة"),
            "Bukhari"
        ),
        DuaItem(
            "32", "Ayat al-Kursi",
            "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            "Allahu la ilaha illa huwal-hayyul-qayyum. La ta'khudhuhu sinatun wa la nawm. Lahu ma fis-samawati wa ma fil-ard. Man dhal-ladhi yashfa'u 'indahu illa bi idhnih. Ya'lamu ma bayna aydihim wa ma khalfahum. Wa la yuhituna bi shay'im-min 'ilmihi illa bima sha'. Wasi'a kursiyyuhus-samawati wal-ard. Wa la ya'uduhu hifzuhuma. Wa huwal-'aliyyul-'azim",
            "Allah — there is no deity except Him, the Ever-Living, the Sustainer of existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth...",
            "Protection",
            listOf("ayatul kursi", "kursi", "protection", "hifazat", "آية الكرسي", "quran 2:255"),
            "Quran 2:255"
        ),
        DuaItem(
            "33", "Before travel (mounting)",
            "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
            "Subhanal-ladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila rabbina la-munqalibun",
            "Glory be to Him who has subjected this to us, and we could not have done it by ourselves. And indeed, to our Lord we will return.",
            "Travel",
            listOf("travel", "safar", "car", "vehicle", "bus", "plane", "سفر", "گاڑی"),
            "Quran 43:13-14; Muslim"
        ),
        DuaItem(
            "34", "Full travel dua",
            "اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَٰذَا الْبِرَّ وَالتَّقْوَىٰ، وَمِنَ الْعَمَلِ مَا تَرْضَىٰ، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَٰذَا وَاطْوِ عَنَّا بُعْدَهُ، اللَّهُمَّ أَنْتَ الصَّاحِبُ فِي السَّفَرِ، وَالْخَلِيفَةُ فِي الْأَهْلِ",
            "Allahumma inna nas'aluka fi safarina hadhal-birra wat-taqwa, wa minal-'amali ma tarda. Allahumma hawwin 'alayna safarana hadha watwi 'anna bu'dah. Allahumma antas-sahibu fis-safar, wal-khalifatu fil-ahl",
            "O Allah, we ask You on this journey of ours for righteousness and piety, and for deeds that please You. O Allah, make this journey easy for us and fold up its distance. O Allah, You are the companion on the journey and the guardian of the family.",
            "Travel",
            listOf("safar dua", "travel dua", "journey", "safar ki dua"),
            "Muslim"
        ),
        DuaItem(
            "35", "Leaving the home",
            "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "Bismillah, tawakkaltu 'alallah, wa la hawla wa la quwwata illa billah",
            "In the name of Allah, I place my trust in Allah, and there is no might nor power except with Allah.",
            "Home",
            listOf("leaving home", "ghar se nikalna", "exit", "bahar", "توكل"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "36", "Entering the mosque",
            "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "Allahumma iftah li abwaba rahmatik",
            "O Allah, open the gates of Your mercy for me.",
            "Mosque",
            listOf("mosque", "masjid", "enter mosque", "مسجد"),
            "Muslim"
        ),
        DuaItem(
            "37", "Leaving the mosque",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            "Allahumma inni as'aluka min fadlik",
            "O Allah, I ask You from Your bounty.",
            "Mosque",
            listOf("leave mosque", "masjid se nikalna", "mosque exit"),
            "Muslim"
        ),
        DuaItem(
            "38", "Before entering toilet",
            "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
            "Allahumma inni a'udhu bika minal-khubthi wal-khaba'ith",
            "O Allah, I seek refuge in You from the male and female unclean spirits.",
            "Daily",
            listOf("toilet", "bathroom", "wc", "غسل", "ٹوائلٹ"),
            "Bukhari, Muslim"
        ),
        DuaItem(
            "39", "Leaving the toilet",
            "غُفْرَانَكَ",
            "Ghufranaka",
            "I seek Your forgiveness.",
            "Daily",
            listOf("after toilet", "bathroom leave", "غفرانك"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "40", "When it rains",
            "اللَّهُمَّ صَيِّبًا نَافِعًا",
            "Allahumma sayyiban nafi'a",
            "O Allah, make it a beneficial rain.",
            "Weather",
            listOf("rain", "baarish", "rain dua", "مطر"),
            "Bukhari"
        ),
        DuaItem(
            "41", "After the adhan",
            "اللَّهُمَّ رَبَّ هَٰذِهِ الدَّعْوَةِ التَّامَّةِ وَالصَّلَاةِ الْقَائِمَةِ آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
            "Allahumma rabba hadhihid-da'watit-tammah, was-salatil-qa'imah, ati Muhammadanil-wasilata wal-fadilah, wab'athhu maqaman mahmuda alladhi wa'adtah",
            "O Allah, Lord of this perfect call and established prayer, grant Muhammad the intercession and favor, and raise him to the praised station You have promised him.",
            "Prayer",
            listOf("adhan", "azan", "after adhan", "أذان"),
            "Bukhari"
        ),
        DuaItem(
            "42", "Between adhan and iqamah",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ العَفْوَ وَالعَافِيَةَ فِي الدُّنْيَا وَالآخِرَةِ",
            "Allahumma inni as'alukal-'afwa wal-'afiyah fid-dunya wal-akhirah",
            "O Allah, I ask You for pardon and well-being in this world and the Hereafter.",
            "Prayer",
            listOf("iqamah", "between adhan", "afiyah", "عافية"),
            "Ibn Majah"
        ),
        DuaItem(
            "43", "After salah — Istighfar",
            "أَسْتَغْفِرُ اللَّهَ",
            "Astaghfirullah",
            "I seek the forgiveness of Allah. (Said three times after salah)",
            "Prayer",
            listOf("after salah", "namaz ke baad", "istighfar", "استغفر"),
            "Muslim"
        ),
        DuaItem(
            "44", "After salah — Tashahhud dhikr",
            "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            "Allahumma antas-salam wa minkas-salam, tabarakta ya dhal-jalali wal-ikram",
            "O Allah, You are Peace and from You is peace. Blessed are You, O Owner of majesty and honor.",
            "Prayer",
            listOf("after prayer", "salam", "tabarakta", "namaz"),
            "Muslim"
        ),
        DuaItem(
            "45", "When angry",
            "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ",
            "A'udhu billahi minash-shaytanir-rajim",
            "I seek refuge in Allah from Satan, the expelled.",
            "Distress",
            listOf("angry", "gussa", "anger", "غضب", "شیطان"),
            "Bukhari, Muslim"
        ),
        DuaItem(
            "46", "In distress",
            "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ",
            "La ilaha illallahul-'azimul-halim. La ilaha illallahu rabbul-'arshil-'azim. La ilaha illallahu rabbus-samawati wa rabbul-ardi wa rabbul-'arshil-karim",
            "There is no god but Allah, the Magnificent, the Forbearing. There is no god but Allah, Lord of the Magnificent Throne. There is no god but Allah, Lord of the heavens, Lord of the earth, and Lord of the Noble Throne.",
            "Distress",
            listOf("distress", "hardship", "musibat", "anxiety", "tension", "پریشانی"),
            "Bukhari, Muslim"
        ),
        DuaItem(
            "47", "For anxiety and sorrow",
            "اللَّهُمَّ إِنِّي عَبْدُكَ، ابْنُ عَبْدِكَ، ابْنُ أَمَتِكَ، نَاصِيَتِي بِيَدِكَ، مَاضٍ فِيَّ حُكْمُكَ، عَدْلٌ فِيَّ قَضَاؤُكَ، أَسْأَلُكَ بِكُلِّ اسْمٍ هُوَ لَكَ...",
            "Allahumma inni 'abduka, ibnu 'abdika, ibnu amatika, nasiyati bi yadika, madin fiyya hukmuka, 'adlun fiyya qada'uka, as'aluka bi kullismin huwa lak...",
            "O Allah, I am Your servant, son of Your servant, son of Your maidservant. My forelock is in Your hand. Your command concerning me is executed. Your decree concerning me is just. I ask You by every name that is Yours...",
            "Distress",
            listOf("anxiety", "sorrow", "gham", "depression", "حزن", "هم"),
            "Ahmad; graded sahih by Albani"
        ),
        DuaItem(
            "48", "When visiting the sick",
            "لَا بَأْسَ طَهُورٌ إِنْ شَاءَ اللَّهُ",
            "La ba'sa tahurun in sha Allah",
            "No harm, it will be a purification, if Allah wills.",
            "Health",
            listOf("sick", "bimar", "hospital", "illness", "مريض"),
            "Bukhari"
        ),
        DuaItem(
            "49", "For the sick person",
            "أَسْأَلُ اللَّهَ الْعَظِيمَ رَبَّ الْعَرْشِ الْعَظِيمِ أَنْ يَشْفِيَكَ",
            "As'alullahal-'azim rabbal-'arshil-'azim an yashfiyak",
            "I ask Allah the Magnificent, Lord of the Magnificent Throne, to cure you. (Seven times)",
            "Health",
            listOf("shifa", "cure", "healing", "شفاء", "dua for sick"),
            "Abu Dawud, Tirmidhi"
        ),
        DuaItem(
            "50", "Breaking the fast",
            "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
            "Dhahaz-zama'u wabtallatil-'uruqu wa thabatal-ajru in sha Allah",
            "The thirst is gone, the veins are moistened, and the reward is confirmed, if Allah wills.",
            "Food",
            listOf("iftar", "roza", "fasting", "ramadan", "افطار", "صوم"),
            "Abu Dawud"
        ),
        DuaItem(
            "51", "Seeing someone in trial",
            "الْحَمْدُ لِلَّهِ الَّذِي عَافَانِي مِمَّا ابْتَلَاكَ بِهِ وَفَضَّلَنِي عَلَىٰ كَثِيرٍ مِمَّنْ خَلَقَ تَفْضِيلًا",
            "Alhamdu lillahil-ladhi 'afani mimmabtalaka bihi wa faddalani 'ala kathirin mimman khalaqa tafdila",
            "Praise be to Allah who has spared me from what He has tested you with, and has favored me greatly over many of those He has created.",
            "Daily",
            listOf("trial", "seeing affliction", "afani", "عافاني"),
            "Tirmidhi"
        ),
        DuaItem(
            "52", "When looking in the mirror",
            "اللَّهُمَّ كَمَا حَسَّنْتَ خَلْقِي فَحَسِّنْ خُلُقِي",
            "Allahumma kama hassanta khalqi fa hassin khuluqi",
            "O Allah, just as You have made my external features beautiful, make my character beautiful as well.",
            "Daily",
            listOf("mirror", "aina", "beauty", "khuluq", "خلق"),
            "Ahmad"
        ),
        DuaItem(
            "53", "Dua for parents",
            "رَّبِّ ارْحَمْهُمَا كَمَا رَبَّيَانِي صَغِيرًا",
            "Rabbi irhamhuma kama rabbayani saghira",
            "My Lord, have mercy upon them as they brought me up when I was small.",
            "Family",
            listOf("parents", "walidain", "mother", "father", "والدین", "ماں باپ"),
            "Quran 17:24"
        ),
        DuaItem(
            "54", "Dua for knowledge",
            "رَّبِّ زِدْنِي عِلْمًا",
            "Rabbi zidni 'ilma",
            "My Lord, increase me in knowledge.",
            "Knowledge",
            listOf("knowledge", "ilm", "study", "exam", "علم", "پڑھائی"),
            "Quran 20:114"
        ),
        DuaItem(
            "55", "Istikhara (seeking guidance)",
            "اللَّهُمَّ إِنِّي أَسْتَخِيرُكَ بِعِلْمِكَ وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ وَأَسْأَلُكَ مِنْ فَضْلِكَ الْعَظِيمِ...",
            "Allahumma inni astakhiruka bi 'ilmika, wa astaqdiruka bi qudratika, wa as'aluka min fadlikal-'azim...",
            "O Allah, I seek Your guidance through Your knowledge, and I seek ability through Your power, and I ask You from Your immense favor...",
            "Guidance",
            listOf("istikhara", "decision", "guidance", "choice", "استخاره"),
            "Bukhari"
        ),
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
        // Curated Islamic-only catalogue (streamed via Internet Archive / everyayah when online)
        AudioTrack("t1", "Tala'al Badru Alayna", "Traditional Nasheed", "Seerah & Praise", "nasheeds", "Arabic", 240, titleAr = "طلع البدر علينا", creatorAr = "نشيد تقليدي"),
        AudioTrack("t2", "Ya Nabi Salam Alayka", "Maher Zain style nasheed", "Madah", "madah", "Arabic", 265, titleAr = "يا نبي سلام عليك", creatorAr = "منشد"),
        AudioTrack("t3", "Hasbi Rabbi", "Islamic nasheed", "Tawakkul", "nasheeds", "Arabic", 210, titleAr = "حسبي ربي", creatorAr = "منشد"),
        AudioTrack("t4", "Surah Al-Fatihah", "Mishary Rashid Alafasy", "Tilawat", "tilawat", "Arabic", 95, titleAr = "الفاتحة", creatorAr = "مشاري راشد العفاسي"),
        AudioTrack("t5", "Surah Al-Mulk", "Abdul Rahman Al-Sudais", "Tilawat", "tilawat", "Arabic", 480, titleAr = "سورة الملك", creatorAr = "عبد الرحمن السديس"),
        AudioTrack("t6", "Surah Ar-Rahman", "Mahmoud Khalil Al-Husary", "Tilawat", "tilawat", "Arabic", 720, titleAr = "سورة الرحمن", creatorAr = "محمود خليل الحصري"),
        AudioTrack("t7", "Morning Adhkar", "Hisn al-Muslim", "Daily Adhkar", "adhkar", "Arabic", 320, titleAr = "أذكار الصباح", creatorAr = "حصن المسلم"),
        AudioTrack("t8", "Evening Adhkar", "Hisn al-Muslim", "Daily Adhkar", "adhkar", "Arabic", 280, titleAr = "أذكار المساء", creatorAr = "حصن المسلم"),
        AudioTrack("t9", "The Reality of This Dunya", "Islamic bayan", "Reminders", "lectures", "English", 900, titleAr = "حقيقة الدنيا", creatorAr = "محاضرة"),
        AudioTrack("t10", "Importance of Salah", "Islamic bayan", "Reminders", "lectures", "English", 720, titleAr = "أهمية الصلاة", creatorAr = "محاضرة"),
        AudioTrack("t11", "Qasidah Burdah (excerpt)", "Classical nasheed", "Madah", "madah", "Arabic", 300, titleAr = "قصيدة البردة", creatorAr = "منشد"),
        AudioTrack("t12", "Allahul Allah", "Nasheed", "Dhikr", "nasheeds", "Arabic", 200, titleAr = "الله الله", creatorAr = "منشد")
    )

    val sampleAlarms = listOf(
        AlarmConfig(id = "a1", hour = 5, minute = 0, label = "Fajr (Demo)", enabled = true, repeatDays = setOf(1, 2, 3, 4, 5, 6, 7), challengeType = ChallengeType.MATH),
        AlarmConfig(id = "a2", hour = 6, minute = 30, label = "Morning Adhkar", enabled = false, repeatDays = setOf(1, 2, 3, 4, 5), challengeType = ChallengeType.NONE)
    )
}
