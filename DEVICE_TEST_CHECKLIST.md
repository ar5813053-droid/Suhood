# Suhood device test checklist

## Audio (Nasheedify)
- [ ] Play track → sound from sample tone
- [ ] Leave app → playback continues (notification)
- [ ] Lock screen → play/pause/next/prev
- [ ] Start Instagram/YouTube audio → Suhood pauses (audio focus)
- [ ] Unplug headphones → Suhood pauses
- [ ] Favorites persist after force-stop
- [ ] Progress bar moves with real position

## Alarms
- [ ] Create alarm 2 minutes ahead → notification fires with app closed
- [ ] Dismiss / Snooze 5m actions work
- [ ] Toggle off cancels pending alarm
- [ ] Reboot → enabled alarms still scheduled
- [ ] Exact alarm permission on Android 12+

## Focus
- [ ] Start 1-minute session → completes with history saved
- [ ] Pause/resume keeps remaining time after backgrounding
- [ ] Grant Usage Access → restricted app detection shows FocusBlockActivity
- [ ] End session clears block detection

## Prayer
- [ ] Settings method change persists
- [ ] Manual location saves and affects calculation (when Home reads prefs)
- [ ] Countdown ticks every second

## Permissions to enable
1. Notifications
2. Exact alarms (Alarms & reminders)
3. Usage access (optional, for focus app detection)
