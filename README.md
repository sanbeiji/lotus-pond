# Lotus Pond Reader (蓮池故事機)

**Lotus Pond Reader** is an AI-powered Mandarin story generator designed for Mandarin Chinese language learners, with a focus on Taiwanese Mandarin pronunciation, vocabulary, and style.

It uses the Google Gemini API to create personalized reading material tailored to your proficiency level, complete with interlinear Pinyin/Zhuyin pronunciation and Text-To-Speech (TTS) support.

## 🪷 Inspiration

The name is inspired by the **Lotus Pond (蓮池潭)** in Kaohsiung, Taiwan—a place famous for its temples, pagodas, and vibrant traditional culture. This app aims to provide a similarly immersive experience for mastering the beauty of Taiwanese Mandarin.

## 📱 Features

- **Custom Story Generation**: Provide a plot or theme, and Gemini creates a unique story.
- **TOCFL-Aligned Levels**: Choose from levels 1 to 8 (aligned with Taiwan's TOCFL standards).
- **Interlinear Pronunciation**: Toggle between **Pinyin** or **Zhuyin (Bopomofo)**.
- **Study Mode**: Highlight specific vocabulary within the generated story.
- **Text-To-Speech**: Listen to sentences read aloud using high-quality Taiwanese Mandarin voices.
- **Offline History**: Save your generated stories to read again later.
- **Cross-Platform**: Available as a modern Web App and a native Android App.

---

## 🚀 Getting Started

To use Lotus Pond Reader, you need a **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/app/apikey). The key is free for individual use (within certain limits).

### 🖥️ Web Version
1. Open `web/index.html` in any modern browser, or check out the [hosted version](https://sanbeiji.com/lotus).
2. Click the **Settings (⚙️)** icon.
3. Enter your Gemini API Key.
4. Your key is stored locally in your browser's `localStorage`.

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

---

## 🔒 Security & Privacy

- **API Keys**: Your API key is **never** sent to any server except directly to Google's Gemini API endpoints.
- **Local Storage**: 
    - On **Android**, the key is encrypted at rest using the Android Keystore.
    - On **Web**, the key is stored in `localStorage`. Users should be aware that `localStorage` is accessible to scripts on the same origin (XSS risk).
- **No Tracking**: This app does not include any analytics or tracking scripts.


## 📜 License & Credits

- **License**: This project is licensed under the [GNU AGPLv3](LICENSE).
- **Font**: Uses the [Iansui (芫荽)](CREDITS.md) font (SIL Open Font License 1.1).
- **Author**: Joseph R. Lewis

---

## ⚠️ Disclaimer for Developers

If you fork this repository:
1. **Do not hard-code API keys.** Always use a secure method for secret storage.
2. **License Compliance**: As this is an AGPL-licensed project, any modifications you make and host publicly must also be open-sourced under the same license.
