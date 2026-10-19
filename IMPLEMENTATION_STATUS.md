# Suhood real-function upgrade — status

## Implemented in source

| Feature | Status |
|---------|--------|
| Alarms (Room + AlarmManager + snooze/dismiss) | Implemented — device test required |
| Full-screen AlarmRingActivity + vibration + separate MediaPlayer | Implemented — device test required |
| Custom alarm sound URI (SAF) | Implemented — device test required |
| Boot / timezone reschedule | Implemented |
| Focus wall-clock session | Implemented |
| Real installed app picker | Implemented — device test required |
| Usage Access detection + FocusBlockActivity | Implemented — not force-kill |
| Media3 Nasheedify + MediaSession | Implemented |
| MediaStore local library | Implemented — needs READ_MEDIA_AUDIO |
| Islamic content filter (allow/block/uncertain) | Implemented + unit tests |
| Pre-playback classification gate | Implemented |
| Blocked-query dialog (“No Haram Music”) | Implemented |
| Salah reminder scheduler | Implemented — needs prefs UI wiring on Home |
| Privacy / FAQ | Implemented |
| Suhood logo launcher | Implemented |

## Blocked by platform / policy

- YouTube Music / Spotify streaming like Meld (no scrape/DRM bypass)
- Force-stopping third-party apps
- Alarms after **Force Stop** of the app (Android limitation)

## Permissions user must enable

1. Notifications
2. Alarms & reminders (exact)
3. Music and audio
4. Usage access (Focus)
5. Full-screen intents (alarm, Android 14+ may restrict)

## Build

Not executed in this environment. Push to GitHub Actions for APK verification.
