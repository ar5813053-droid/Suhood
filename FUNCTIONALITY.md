# Suhood — Real Functionality Status

## Audit (verified from source)

| Area | Before | After (this phase) |
|------|--------|-------------------|
| Prayer times | Hardcoded demo schedule | Offline **calculated** times (MWL default, Makkah coords) |
| Countdown | Static demo | Live 1s ticker |
| Audio | UI-only fake progress | **Media3 ExoPlayer** plays local sample tones |
| Favorites | In-memory list | Room-ready DAO (wire remaining UI next) |
| Focus history | Empty placeholder | Room insert on session complete |
| Alarms | UI only | AlarmManager scheduler + notification receiver |
| Preferences | Theme / onboarding | + lat/lng / method / location label |
| Notifications | None | Channels: prayer, focus, alarm, audio |

## Prayer times

- Algorithm: standard solar-position formulation
- Methods: MWL, ISNA, Egypt, Umm al-Qura
- Default location: Makkah (21.4225, 39.8262) until user sets location in Settings
- **These are calculated, not mosque-verified schedules**

## Audio (Nasheedify)

- `SampleToneGenerator` writes short PCM WAV tones to cache (royalty-free, not commercial nasheeds)
- `PlaybackController` uses Media3 ExoPlayer for real play/pause/next/previous
- **Not** a streaming catalog; no YouTube/DRM scraping
- Future: implement `AudioCatalogProvider` with licensed API + credentials server-side

## Permissions (AndroidManifest)

- POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED
- FOREGROUND_SERVICE / MEDIA_PLAYBACK, WAKE_LOCK, VIBRATE

## Remaining / next phases

1. Wire Settings location picker + prayer method UI to PreferencesRepository
2. MediaSessionService for lock-screen controls
3. Connect Nasheedify UI fully to PlaybackController singleton
4. Room-backed favorites UI + recent tracks
5. Focus app-blocking (Usage Access) with explicit consent
6. Quran text from verified offline mushaf source (licensing required)
7. Exact alarm permission rationale on Android 12+

## Build

GitHub Actions must still compile after these changes. Media3 deps added to version catalog.
