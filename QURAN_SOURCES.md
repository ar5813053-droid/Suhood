# Quran & audio sources (verified)

## Text & structure
- **API:** https://api.alquran.cloud/v1/
- **Arabic edition:** `quran-uthmani`
- **Meta:** 114 surahs, 6236 ayahs (confirmed live `GET /meta`)

## Urdu translation
- **Edition:** `ur.jalandhry` — Fateh Muhammad Jalandhry
- Loaded via `GET /surah/{n}/editions/quran-uthmani,ur.jalandhry`

## Audio (verse-by-verse)
- **CDN:** `https://cdn.islamic.network/quran/audio/128/{edition}/{globalAyahNumber}.mp3`
- **Qaris (API audio editions):** ar.alafasy, ar.abdurrahmaansudais, ar.mahermuaiqly, ar.abdulsamad, ar.minshawi, ar.husary, ar.shaatree
- Full-surah files: `https://cdn.islamic.network/quran/audio-surah/128/{edition}/{surah}.mp3`

## Licensing / attribution
AlQuran.Cloud / Islamic Network public API. Attribute the translation and reciters. Do not claim ownership of Quran text or recordings.

## Nasheedify online
Commercial nasheed streaming is not connected (no DRM scrape). Local MediaStore + tilawat via this Quran audio path.
