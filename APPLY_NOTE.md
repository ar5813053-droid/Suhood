# Suhood production features (path B prep)

## Included on top of existing main

1. **Removed Phase 1/2 demo messaging** from Home, Focus, Alarms, Onboarding, Settings, Nasheedify, Quran copy
2. **FocusAccessibilityService** (AppBlock-style) + manifest + strings
3. **Settings → Permissions** guide (notifications, exact alarms, usage, accessibility)
4. **Settings → Tip the developer** — UPI `syedfurqan95@fam`, QR, purpose story
5. **Settings → Notifications** — open system settings + **Reschedule prayer reminders**
6. **Settings → Focus** — Usage Access + Accessibility buttons
7. **SalahReminderScheduler.rescheduleAll** + boot/timezone hook
8. **ZXing** for offline UPI QR

## Termux apply (recommended)

If your clone is already Suhood:

```bash
cd ~
mv Suhood Suhood-backup-$(date +%s)
# unzip this archive so the folder is Suhood-main content
unzip suhood-production-features.zip
# rename extracted folder to Suhood if needed
cd Suhood   # or suhood-production-features
git remote -v   # ensure origin is your repo
git add -A
git commit -m "feat: production feature set — permissions, tip UPI, focus a11y, prayer reschedule"
git push origin main
```

Or copy individual paths from the zip into your existing clone carefully.

## After install on phone

1. Notifications  
2. Alarms & reminders  
3. Accessibility → Suhood Focus  
4. Usage Access (optional)  
5. Settings → Notifications → Reschedule prayer reminders  
6. Settings → Tip the developer (optional)

## Still for full Play Store later

- Privacy policy **URL** (host separately)
- Signed release AAB + Play Console
- Device QA checklist
