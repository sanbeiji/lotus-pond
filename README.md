# 🪷 Lotus Pond Reader

**蓮池故事機 (liánchí gùshìjī)** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader is an AI-powered tool that uses the Google Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation.

## 🪷 Inspiration

The name is inspired by the **Lotus Pond (蓮池潭)** in Kaohsiung, Taiwan—a place famous for its temples, pagodas, and vibrant traditional culture. This app aims to provide a similarly immersive experience for mastering the beauty of Taiwanese Mandarin.

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

## 📱 Features

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

---

## 🚀 Setup & Getting Started

### Prerequisites
- A **Gemini API key** from [Google AI Studio](https://aistudio.google.com/). The key is free for individual use (within certain limits).

### 🖥️ Web Version
1. Open `web/index.html` in any modern browser, or visit the [hosted version](https://sanbeiji.github.io/lotus/).
2. Click the **Settings (⚙️)** icon and enter your Gemini API Key.
3. Your key is stored locally in your browser's `localStorage`.

### 🤖 Android Version
1. Build the project using [Android Studio](https://developer.android.com/studio) or the [Android CLI](https://developer.android.com/tools/agents).
    - Requires **Android Studio Ladybug** or newer.
    - **Min SDK**: 31 (Android 12)
    - **Target SDK**: 36
2. On first launch, the app will prompt you for your API Key.
3. Your key is stored securely on your device using **EncryptedSharedPreferences**.

---

## 🛠️ Architecture & Tech Stack

### Android App
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3 Expressive)
- **Networking**: Ktor Client
- **Database**: Room (for story history)
- **Storage**: 
    - **Jetpack DataStore**: For general user preferences.
    - **EncryptedSharedPreferences**: For sensitive data (API Key).
- **Architecture**: MVVM with Repository pattern.

### Web App
- **Language**: Vanilla JavaScript / HTML5 / CSS3
- **Styling**: Modern CSS with custom properties and animations.
- **Storage**: `localStorage` for both settings and history.
- **AI**: Google AI Studio – Gemini 2.5/3.1 Flash Lite.
- **API**: Direct client-side `fetch` calls (Zero-npm).
- **Typography**: Iansui (芫荽) and Klee One (Google Fonts).

---

## 🚀 Deployment & CI/CD

This project uses **GitHub Actions** for automated CI/CD:
- **Web:** Automatically deployed to `sanbeiji.github.io/lotus/` upon tagging a new release.
- **Android:** The APK is automatically compiled and attached to the GitHub Release assets.

All web logic is contained within `/web/script.js` and styling is in `/web/style.css`. The Android app is located in the `/android` directory.

---

## 🔒 Security & Privacy

- **API Keys**: Your API key is **never** sent to any server except directly to Google's Gemini API endpoints.
- **Local Storage**: 
    - On **Android**, the key is encrypted at rest using the Android Keystore.
    - On **Web**, the key is stored in `localStorage`. Users should be aware that `localStorage` is accessible to scripts on the same origin (XSS risk).
- **No Tracking**: This app does not include any analytics or tracking scripts.

---

## 📜 License & Credits

- **License**: This project is licensed under the [GNU AGPLv3](LICENSE).
- **Font**: Uses the [Iansui (芫荽)](CREDITS.md) font (SIL Open Font License 1.1).
- **Author**: [Joseph R. Lewis](https://sanbeiji.com) ([@sanbeiji](https://github.com/sanbeiji/))

---

## ⚠️ Disclaimer for Developers

If you fork this repository:
1. **Do not hard-code API keys.** Always use a secure method for secret storage.
2. **License Compliance**: As this is an AGPL-licensed project, any modifications you make and host publicly must also be open-sourced under the same license.