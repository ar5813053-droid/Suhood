# Suhood

**Help Muslims spend less time distracted and more time reconnecting with Allah.**

Suhood is a free, non-commercial native Android application focused on reducing social-media and gaming distractions, returning to Salah, remembering Allah through Dhikr, reading the Quran, and accessing Islamic audio (Nasheedify).

This repository contains **Phase 1 — Complete Frontend**: every major screen, Hybrid Glass UI, navigation, demo interactions, local preference persistence, and a GitHub Actions debug APK workflow.

## Features (Phase 1)

- **Hybrid Glass UI** — Apple-inspired liquid-glass aesthetics on a monochrome black / graphite / silver palette (Compose)
- **Floating top navigation** capsule (Home · Worship · Focus · Audio)
- **Onboarding** with theme and language selection (persisted)
- **Home dashboard** with demo Salah times and countdown (clearly labelled as demo)
- **Focus sessions** — simulated countdown, app selection, history placeholders
- **Alarms** — local CRUD UI, simulated ringing, Quiz and Dhikr dismissal challenges
- **Quran** — surah list/search UI; **no fabricated verses** (placeholder reader ready for verified content)
- **Worship** — functional Tasbeeh counter, Adhkar, Dua library, Islamic quiz with scoring, daily checklist
- **Nasheedify** — full audio UI: categories, search, favorites, queue, mini-player, full player (demo playback state)
- **Settings** — appearance (Dark / Light / System), language, privacy, about; Phase 2 notes for real integrations
- **Local persistence** via DataStore for theme and onboarding
- **Offline demo data** — no network required

## Technology

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 foundation |
| Navigation | Navigation Compose |
| Architecture | MVVM-style with repositories |
| Preferences | DataStore |
| Build | Gradle Kotlin DSL + version catalog |
| Min SDK | 26 |
| Target / Compile SDK | 35 |

## Project structure

```
app/src/main/java/com/suhood/
├── app/                 # Application, MainActivity
├── core/
│   ├── designsystem/    # Theme tokens, glass components, floating nav
│   ├── navigation/      # Routes, NavGraph
│   ├── model/           # Domain models
│   ├── data/            # Demo data
│   └── datastore/       # PreferencesRepository
└── feature/
    ├── onboarding/
    ├── home/
    ├── focus/
    ├── alarms/
    ├── quran/
    ├── worship/
    ├── nasheedify/
    └── settings/
```

## Local setup

### Requirements

- Android Studio Ladybug (or newer) / JDK 17
- Android SDK 35
- Optional: physical device or emulator API 26+

### Build & run

```bash
# Clone
git clone <your-repo-url> suhood
cd suhood

# Assemble debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Unit tests
./gradlew test
```

Open the project in Android Studio and run the `app` configuration.

### First launch

Onboarding appears once. Theme preference is saved immediately. Skip any optional intro step.

## GitHub Actions — debug APK

Workflow: `.github/workflows/android-apk.yml`

**Triggers:** push / PR to `main` or `master`, and manual `workflow_dispatch`.

**Steps:** checkout → JDK 17 → Android SDK → unit tests → `assembleDebug` → upload artifact `suhood-debug-apk`.

### Download the APK

1. Open the repository on GitHub → **Actions**
2. Select the latest successful **Build Suhood Debug APK** run
3. Download the **suhood-debug-apk** artifact
4. Unzip and install `app-debug.apk` on a device (enable “Install from unknown sources” if needed)

Artifacts expire according to GitHub retention (default ~14 days in this workflow).

**Do not treat the debug APK as a production release.** It is unsigned for store distribution.

## Phase 1 limitations (honest)

| Feature | Phase 1 status |
|---------|----------------|
| Prayer times | Demo values only — not location-based |
| Alarms | UI + simulated ring; no OS AlarmManager |
| App blocking | Demo list; no Usage Access / Accessibility |
| Quran text | Interface only — no fabricated verses |
| Audio | Demo metadata + playback *state*; no real Media3 stream |
| Notifications | Not scheduled |
| Monetization | None (by design) |

## Phase 2 (planned)

- Real Salah calculation and local notifications
- Fajr alarms with OS scheduling
- Quiz / Dhikr alarm dismissal
- Device-level focus blocking
- Verified Quran content + recitation
- Media3 foreground playback service
- Authorized Nasheedify catalog sources

## Design

**Hybrid Glass** — deep black backgrounds, graphite translucent surfaces, thin silver borders, restrained highlights. No green-and-gold stereotype theme. Dark theme is the default.

## License & intent

Suhood is a free, non-commercial project. No advertisements, subscriptions, or in-app purchases.

May Allah accept the intention and make it beneficial.
