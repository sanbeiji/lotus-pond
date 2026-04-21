# Lotus Pond

This is the root repository for the Lotus Pond projects, containing the web application and native apps.

## About Lotus Pond Reader

**蓮池故事機 (liánchí gùshìjī)** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader uses the Google AI Studio Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation. It features multiple TOCFL-aligned levels, text-to-speech, and study modes to assist language learners.

## Changelog

### Unreleased
- **Android App Initial Prototype**: 
  - Created native Android application using Jetpack Compose and Material 3.
  - Migrated core story generation logic and Gemini API integration (Ktor).
  - Added Room Database for offline story history caching.
  - Added Jetpack DataStore for securely persisting user preferences.
  - Implemented Home, History, and Settings screens with native navigation.
- **Project Restructure**:
  - Migrated the `lotus-pond-reader-web` project into a monorepo structure.
  - Preserved full Git history of the web project under the `web/` directory.
