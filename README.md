<p align="center">
  <img src="screenshots/app-icon.png" alt="Suhood" width="120" />
</p>

<h1 align="center">Suhood</h1>

<p align="center">
  <strong>Less distraction. More Salah, Quran, Dhikr, and mindful focus.</strong>
</p>

<p align="center">
  <a href="https://github.com/ar5813053-droid/Suhood/actions/workflows/android-apk.yml"><img src="https://github.com/ar5813053-droid/Suhood/actions/workflows/android-apk.yml/badge.svg" alt="Build APK" /></a>
  <img src="https://img.shields.io/badge/version-1.2.1-blue" alt="Version 1.2.1" />
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-green" alt="Min SDK 26" />
  <img src="https://img.shields.io/badge/license-MIT-lightgrey" alt="MIT" />
</p>

<p align="center">
  <img src="screenshots/app-logo-full.jpg" alt="Suhood logo" width="280" />
</p>

---

## What is Suhood?

**Suhood** (Arabic sense of *conscious presence*) is a free, non-commercial **native Android** app for Muslims who want fewer distractions and a clearer path back to worship. It is built in **Kotlin + Jetpack Compose** with a hybrid liquid-glass UI (dark graphite, translucent surfaces, silver accents).

No ads. No subscriptions. No in-app purchases. Optional voluntary tips to the developer only.

> **Current release:** **Phase 1.2.1** (`versionCode 3`) — production-feature implementation with automated GitHub Actions APK builds. Always verify prayer times and alarms on your own device before relying on them.

---

## Phase 1.2.1 — what’s in this release

| Area | Highlights |
|------|------------|
| **Home** | Live Next-Salah bar, location-based times, progress cards, quest entry |
| **Salah** | Mark On time / Jama‘ah / Late / Qaza; **custom time wheel** (alarm-style); times drive home bar, notifications, and Focus block |
| **Focus** | Wall-clock sessions, real installed-app picker, Usage Access + Accessibility app limits |
| **Alarms** | Room-backed alarms, full-screen ring, snooze/dismiss, custom sound, wheel picker + tick sound, “Alarm in Xh Ym” countdown |
| **Phone** | Dialer-style UI, contacts, recents, themes, **emoji / avatar / gallery photo** persistence, blocked numbers with confirm dialog |
| **Quran** | Surah browse & search — no fabricated verses |
| **Nasheedify** | Media3 playback, local library, Islamic content filter, playlists / favorites |
| **Worship** | Tasbeeh, Adhkar, Dua, quiz |
| **Qibla · Mosque · Books · Quest · Muhasabah · Journey · Widgets** | Supporting modules for direction, nearby mosques, Islamic books, daily quests, self-accountability, and home-screen widgets |

---

## Features (detailed)

### 🕌 Salah & prayer times
- Offline calculated times (MWL, ISNA, Egypt, Umm al-Qura)
- Device location (with permission) or fallback coordinates
- **Custom Salah times:** tap the HH:mm on the Salah screen → iOS-style hour/minute wheel → **Done**
- Custom times apply **app-wide**: Home Next-Salah bar, reminder alarms, and Focus auto-block
- Status cycle: On time · Jama‘ah · Late · Qaza · Not prayed
- History & summary (week / month / year)
- Optional near-mosque auto-mark (location-assisted, never claims a prayer was proven)
- Prayer reminder notifications at effective times
- At Salah time, if Focus blocked-apps are selected → **Focus session starts** (real app limits when Accessibility + Usage Access are on)

### 🎯 Focus
- Timed focus sessions based on wall-clock end time (accurate after backgrounding)
- Pick distracting apps from the device’s installed list
- Usage Access detection + Focus block activity
- Accessibility service for soft app limits (not force-kill — Android policy)

### ⏰ Alarms
- Create / edit with hour · minute · AM/PM wheels and key-click tick sound
- Live **“Alarm in X hours Y minutes”** banner
- Single label field (tap to edit in dialog)
- Full-screen `AlarmRingActivity`, vibration, custom SAF sound URI
- Boot / timezone reschedule

### 📞 Suhood Phone
- Contacts list, detail, dial pad, recents
- **Persistent contact identity:** emoji, built-in avatar pack, or gallery photo URI (survives edits and number changes via migrate)
- Call themes (mosques, flags, geometry, glass styles)
- **Blocked numbers:** full contacts list, `+` / manual entry, “Are you sure you want to block?” confirmation
- Call screening / in-call integration where the OS allows

### 📖 Quran
- Surah list, search, reader flows
- Sources documented in [`QURAN_SOURCES.md`](QURAN_SOURCES.md) — **no fabricated ayah text**

### 🎵 Nasheedify
- Media3 / MediaSession playback
- Local MediaStore library (`READ_MEDIA_AUDIO`)
- Online Islamic search providers where configured
- Content classifier (allow / block / uncertain) with “No Haram Music” gate
- Discover, categories, artists, albums, playlists, favorites, queue, full player

### 🤲 Worship & accountability
- Tasbeeh counter with goal
- Adhkar & Dua collections
- Islamic quiz
- Muhasabah (self-review)
- Journey & Quest modules with home card entry

### 🧭 More
- Qibla direction
- Mosque finder / detail
- Islamic books (Internet Archive–backed search, library, reader)
- Home-screen widgets (Next Salah, prayer tracker, Quran companion, dua, Islamic date, habits, and more)
- Settings: appearance, notifications, focus permissions, tip the developer (UPI / QR)

---

## Screenshots

App icon and brand mark (device UI captures can be dropped into `screenshots/`):

| Brand | |
|-------|--|
| ![Icon](screenshots/app-icon.png) | ![Logo](screenshots/app-logo-full.jpg) |

**Suggested captures to add later** (same folder, then link here):

| File | Screen |
|------|--------|
| `01-home-salah-bar.png` | Home — live Next Salah bar |
| `02-salah-custom-time.png` | Salah — time wheel dialog |
| `03-focus-session.png` | Focus — running session + app list |
| `04-alarm-create.png` | Create alarm — wheels + countdown |
| `05-phone-contacts.png` | Phone — contacts with avatars |
| `06-quran-surah.png` | Quran — surah list |
| `07-nasheedify-player.png` | Nasheedify — player |
| `08-widgets.png` | Home-screen widgets |

Interactive feature map (HTML): [`suhood-feature-expansion-preview.html`](suhood-feature-expansion-preview.html)

---

## Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | Feature packages + core (`prayer`, `audio`, `database`, `datastore`, `navigation`) |
| Local data | Room, DataStore Preferences, SharedPreferences (Phone / Focus) |
| Async | Kotlin Coroutines + Flow |
| Media | Media3 (ExoPlayer) + MediaSession |
| Images | Coil (contact photos, assets) |
| Build | Gradle Kotlin DSL, AGP, JDK 17 |
| CI | GitHub Actions → debug APK artifact |

**SDK:** `minSdk 26` · `targetSdk 35` · `applicationId com.suhood.app`

---

## Download / build

### Pre-built APK (CI)

1. Open [Actions → Android APK](https://github.com/ar5813053-droid/Suhood/actions/workflows/android-apk.yml)
2. Open the latest **successful** run on `main`
3. Download the APK artifact

### Build from source

```bash
git clone https://github.com/ar5813053-droid/Suhood.git
cd Suhood
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug
```

Debug APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Release (when signing is configured):

```bash
./gradlew assembleRelease
```

---

## Permissions (enable in system settings when prompted)

| Permission | Why |
|------------|-----|
| Notifications | Prayer, alarm, focus, Quran reminders |
| Alarms & reminders (exact) | Reliable Salah + alarm triggers |
| Location | Accurate prayer times / mosque helpers |
| Music & audio | Local library in Nasheedify |
| Usage access | Focus — detect foreground apps |
| Accessibility | Focus — soft block of selected apps |
| Phone / call-related | Suhood Phone (default dialer role where used) |
| Full-screen intent | Alarm ring on Android 14+ |

See also [`DEVICE_TEST_CHECKLIST.md`](DEVICE_TEST_CHECKLIST.md).

---

## Project layout (high level)

```text
app/src/main/java/com/suhood/
├── app/                 # Application, MainActivity
├── core/
│   ├── prayer/          # Calculator, EffectivePrayerTimes, reminders, location
│   ├── audio/           # Playback, MediaStore, online nasheed providers
│   ├── database/        # Room
│   ├── datastore/       # PreferencesRepository
│   ├── designsystem/    # Glass components, theme
│   ├── navigation/      # Routes + NavGraph
│   └── notifications/   # Alarm / boot receivers
└── feature/
    ├── home/ salah/ focus/ alarms/ phone/
    ├── quran/ nasheedify/ worship/ qibla/
    ├── mosque/ books/ quest/ muhasabah/ journey/
    ├── more/ settings/ onboarding/ widgets/
```

---

## Docs in this repo

| File | Content |
|------|---------|
| [`FUNCTIONALITY.md`](FUNCTIONALITY.md) | Real vs demo behaviour audit |
| [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md) | Feature status matrix |
| [`DEVICE_TEST_CHECKLIST.md`](DEVICE_TEST_CHECKLIST.md) | Device QA checklist |
| [`QURAN_SOURCES.md`](QURAN_SOURCES.md) | Quran text source notes |
| [`APPLY_NOTE.md`](APPLY_NOTE.md) | Patch / apply notes |

---

## Privacy & mission

- Free and intended to stay **non-commercial**
- No ads, no paid unlocks, no tracking SDKs for monetization
- Optional tip (UPI / QR) is voluntary
- Prayer times are **calculated** (or user-custom) — confirm with your local mosque when precision matters
- Focus cannot force-stop other apps; limits depend on Android Accessibility / Usage Access behaviour

---

## Contributing

1. Fork and branch from `main`
2. Keep changes focused; prefer boring, maintainable Kotlin
3. Run unit tests before PR: `./gradlew testDebugUnitTest`
4. Device-test Salah, alarms, and Focus if you touch those modules

---

## License

[MIT](LICENSE) © 2026 [ar5813053-droid](https://github.com/ar5813053-droid)

---

<p align="center"><em>May Allah accept the intention and make it beneficial.</em></p>
