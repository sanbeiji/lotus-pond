# Lotus Pond

This is the root repository for the Lotus Pond projects, containing the web application and native apps.

## About Lotus Pond Reader

**蓮池故事機 (liánchí gùshìjī)** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader uses the Google AI Studio Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation. It features multiple TOCFL-aligned levels, text-to-speech, and study modes to assist language learners.

## The Inspiration

The name of this app is inspired by **Lotus Pond (蓮池潭)**, an artificial lake located in Zuoying District, Kaohsiung City, Taiwan. Famous for its beautiful lotus plants and the numerous temples that line its shores—including the iconic Dragon and Tiger Pagodas, the Spring and Autumn Pavilions, and the Kaohsiung Confucian Temple—Lotus Pond is a vibrant symbol of traditional Taiwanese culture, history, and spirituality. Much like the serene yet culturally rich atmosphere of the pond, this app aims to provide language learners with an immersive and authentic experience on their journey to master Taiwanese Mandarin.

## Changelog

### 2026-04-27
- **Web App Enhancements**:
  - Added a font size preference (Normal, Larger, Largest) for improved readability.
  - Implemented a floating, toggleable Story Settings panel (`⚙️📖`) with smooth animations to declutter the UI.
  - Synced Text-To-Speech (TTS) speaker icon visibility with the pronunciation toggle.
  - Renamed the top-level settings to "Global settings" to differentiate from story-specific options.
- **Android App Modernization**: 
  - Implemented Material 3 Expressive UI with full edge-to-edge layouts, including dynamic light/dark status bar text contrast.
  - Added support for Android 12+ Dynamic Color with brand color fallbacks.
  - Implemented responsive navigation (`NavigationRail` for tablets/foldables, `NavigationBar` for phones) with primary color theming.
  - Added an interactive API Key prompt dialog that appears on startup if a key is missing.
  - Added a global font size preference (Small, Medium, Large) dynamically scaling Mandarin, Pinyin/Zhuyin, and English text.
  - Segregated typography using `Iansui` font strictly for Mandarin/Pinyin content and Roboto for UI elements.
  - Upgraded Settings screen with modern `SegmentedButton` controls and external help links.
  - Implemented Text-To-Speech (TTS) for reading Mandarin sentences aloud, matching the web app's `🔊` icon.
  - Refined gesture navigation to intercept back presses and gracefully exit or return Home.
  - Resolved edge-to-edge scrolling bugs in the Story view and improved layout spacing.
  - Converted the web app favicon into a unified Android Adaptive Icon.
- **Android App Initial Prototype**: 
  - Created native Android application using Jetpack Compose and Material 3.
  - Migrated core story generation logic and Gemini API integration (Ktor).
  - Added Room Database for offline story history caching.
  - Added Jetpack DataStore for securely persisting user preferences.
  - Implemented Home, History, and Settings screens with native navigation.
- **Project Restructure**:
  - Migrated the `lotus-pond-reader-web` project into a monorepo structure.
  - Preserved full Git history of the web project under the `web/` directory.

### 2026-04-16
- **Web App Refinements**: Added refinements to the prompt and story generation output to optimize token usage.

### 2026-04-15
- **Initial Web Prototype**: Created the initial functional web application.
