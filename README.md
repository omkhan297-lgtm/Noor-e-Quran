# Noor-e-Quran — Functional Offline-first Android Project

## Implemented modules
- Quran: bundled 114 surahs / 6,236 verses, Arabic Uthmani reader, ayah search, bookmarks, last-reading position, adjustable font size.
- Prayer Times: on-device calculation using current device coordinates, Hanafi Asr factor, next-prayer display.
- Notifications: prayer alarm scheduling for Fajr, Dhuhr, Asr, Maghrib and Isha; Android notification permission handling.
- Adhan: 50 local voice slots. Each slot can import an audio URI that the user is licensed to use. The selected sound is used by the corresponding prayer notification channel.
- Qibla: bearing to the Kaaba plus rotation-vector compass heading.
- Duas: 40+ entries with Arabic, meaning, source and use.
- Tasbeeh: multiple zikr profiles with separate persistent counters and targets.
- Islamic Calendar: offline Hijri conversion and manual +/- adjustment for local moon-sighting differences.
- Adhkar: morning, evening, after-prayer, sleep, waking, protection, istighfar and salawat sections.
- Bookmarks: multiple Quran ayah bookmarks.
- Theme and settings.

## Important religious-text release checks
The bundled Quran JSON currently contains 114 surahs and 6,236 verses. Before public release, replace/verify it against the latest official Tanzil Uthmani v1.1 text verbatim and keep the required attribution. Tanzil's terms prohibit changing the Quran text.

The app deliberately does not ship third-party adhan recordings without permission. The 50 slots are functional and accept local/licensed audio files.

## Build
Open the project in Android Studio, allow Gradle to sync, then build an APK. This environment does not include the Android SDK/Gradle build toolchain, so the source has not been device-compiled here.
