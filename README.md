# Suhood

**Less distraction. More Salah, Quran, Dhikr, and mindful focus.**

Suhood is a free, non-commercial native Android app designed to help Muslims reduce distractions and reconnect with worship through prayer reminders, focus tools, alarms, Quran resources, Dhikr, and Islamic audio.

## Features

- **Hybrid Glass UI:** Apple-inspired black, graphite, translucent surfaces, and silver highlights.
- **Home:** Salah dashboard and prayer information.
- **Worship:** Tasbeeh, Adhkar, Dua, and Islamic quiz features.
- **Focus:** Focus sessions, installed-app selection, and accessibility-based focus support where configured.
- **Alarms:** Alarm management and reminder-related settings.
- **Prayer reminders:** Rescheduling support and device boot/timezone handling.
- **Quran:** Surah browsing and search without fabricated Quran verses.
- **Nasheedify:** Islamic audio catalogue and playback interface.
- **Permissions:** Guidance for notifications, alarms, Usage Access, and Accessibility.
- **Tip the developer:** Optional UPI contribution screen with QR support.
- **Settings:** Appearance, notification controls, focus permissions, and app information.

## Technology

- Kotlin
- Jetpack Compose
- Android SDK
- Gradle Kotlin DSL
- Android accessibility and notification APIs
- GitHub Actions for automated APK builds

## Build

Requirements: JDK and Android SDK versions compatible with the project Gradle configuration.

```bash
git clone https://github.com/ar5813053-droid/Suhood.git
cd Suhood
chmod +x gradlew
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at:

`app/build/outputs/apk/debug/app-debug.apk`

## Setup and permissions

Some features require optional Android system permissions. Follow the in-app instructions for notifications, alarms and reminders, Usage Access, and Accessibility.

Accessibility-based focus features depend on Android settings and device behaviour. Prayer calculations and reminder delivery should be verified on the target device before relying on them.

## Privacy and mission

Suhood is intended to remain free and non-commercial, with no advertisements, subscriptions, or in-app purchases. Optional developer tips are voluntary.

## Project status

This repository contains the current production-feature implementation. Successful compilation, automated tests, and real-device verification are separate requirements; features should not be considered fully production-verified until those checks pass.

May Allah accept the intention and make it beneficial.
