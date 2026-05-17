# 🪷 Lotus Pond Reader

**蓮池故事機 (liánchí gùshìjī)** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader is an AI-powered tool that uses the Google Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation.

## 📥 Access & Download

### 🌐 Web Version
The latest web version is always available and up-to-date at:
**[https://sanbeiji.github.io/lotus/](https://sanbeiji.github.io/lotus/)**

### 📱 Android App
Install the standalone Android application for a dedicated mobile experience:
* **[Download Latest APK](https://github.com/sanbeiji/lotus-pond/releases/latest/download/app-debug.apk)** (Direct Download)
* **[View All Releases](https://github.com/sanbeiji/lotus-pond/releases/latest)**

> **Note:** To install the APK, you may need to enable "Install from Unknown Sources" in your Android system settings.

---

## Features

- **Story generation** — Describe a plot or theme and get a full story in Traditional Mandarin.
- **8 [TOCFL](https://en.wikipedia.org/wiki/Test_of_Chinese_as_a_Foreign_Language)-aligned levels** — From Novice 1 to C6 (Advanced), tailored specifically for Taiwanese linguistic patterns.
- **Pinyin & Zhuyin toggle** — Switch between Pinyin and Zhuyin (Bopomofo) for pronunciation globally.
- **Show pronunciation toggle** — Hide Pinyin/Zhuyin for a reading challenge or show it for assistance.
- **Read aloud (TTS)** — Listen to each sentence with browser-native Mandarin speech synthesis.
- **Study mode** — Visually highlight required vocabulary within the generated story.
- **Story history** — Access your last 20 generated stories, saved locally in your browser or app.
- **[Iansui (芫荽) font](https://fonts.google.com/specimen/Iansui)** — Beautiful handwriting-style font specifically designed for Traditional Chinese legibility.
- **Model selection** — Choose between various stable 'lite' versions of Gemini (2.5, 3.1, etc.).
- **Standalone & portable** — Zero dependencies, no `npm` required. 

## Setup

### Prerequisites

- A **Gemini API key** from [Google AI Studio](https://aistudio.google.com/).

### Configuration

Once the app is open (Web or Android), click the **Settings (⚙️)** icon and enter your Gemini API key. Your key is stored securely in your device's local storage and is never sent to any server except the Google Gemini API.

## Technology

- **Web Frontend:** HTML5, CSS3 (Vanilla), Vanilla JavaScript.
- **Android App:** Kotlin, Jetpack Compose (WebView-based wrapper).
- **AI:** Google AI Studio – Gemini 2.5/3.1 Flash Lite.
- **API:** Direct client-side `fetch` calls (Zero-npm).
- **Typography:** Iansui (芫荽) and Klee One (Google Fonts).

## Deployment & Development

This project uses **GitHub Actions** for automated CI/CD:
- **Web:** Automatically deployed to `sanbeiji.github.io/lotus/` upon tagging a new release.
- **Android:** The APK is automatically compiled and attached to the GitHub Release assets.

All web logic is contained within `/web/script.js` and styling is in `/web/style.css`. The Android wrapper is located in the `/android` directory.