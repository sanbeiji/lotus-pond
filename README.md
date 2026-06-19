# 🪷 Lotus Pond Reader

**蓮池故事機 (liánchí gùshìjī)** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader is an AI-powered tool that uses the Google Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation.

## 🪷 Inspiration

The name is inspired by the **Lotus Pond (蓮池潭)** in Kaohsiung, Taiwan—a place famous for its temples, pagodas, and vibrant traditional culture. This app aims to provide a similarly immersive experience for mastering the beauty of Taiwanese Mandarin.

## 🚀 Try it out

### 🔑 Mandatory API key
Lotus Pond Reader operates entirely client-side and **requires your own Gemini API key** to generate stories. 
* 🔗 **[Get your free Gemini API key from Google AI Studio](https://aistudio.google.com/app/apikey)**. 

The key is completely free for individual developer use within standard rate limits. Once open (Web or Android), click the **Settings (⚙️)** icon to securely save your key locally on your device.

### 🌐 Web version
The latest web version is always available and up-to-date at:
**[https://sanbeiji.com/lotus/](https://sanbeiji.com/lotus/)**

### 📱 Android app
Install the standalone Android application for a dedicated mobile experience:
* **[Download latest APK](https://github.com/sanbeiji/lotus-pond/releases/latest/download/app-debug.apk)** (direct download)
* **[View latest release](https://github.com/sanbeiji/lotus-pond/releases/latest)**

> **Note:** To install the APK, you may need to enable "Install from Unknown Sources" in your Android system settings.

---

## 📱 Features

- **Story generation** — Describe a plot or theme and get a full story in Traditional Mandarin.
- **8 [TOCFL](https://en.wikipedia.org/wiki/Test_of_Chinese_as_a_Foreign_Language)-aligned levels** — From Novice 1 to C6 (Advanced), tailored specifically for Taiwanese linguistic patterns.
- **Offline dictionary lookup (Android)** — Tap any character, word, or phrase in the story to view its pronunciation (with converted Pinyin tone marks) and English definition.
- **Pleco integration (Android)** — Tap words in the lookup popup to seamlessly open and search them in the external Pleco dictionary app.
- **Pinyin & Zhuyin toggle** — Switch between Pinyin and Zhuyin (Bopomofo) for pronunciation globally.
- **Show pronunciation toggle** — Hide Pinyin/Zhuyin for a reading challenge or show it for assistance.
- **Advanced read aloud (TTS)** — Select voice gender and choose natural accent styles (including **Southern + Minnan** or **Beijing** accents) for text-to-speech.
- **Study mode** — Visually highlight required vocabulary within the generated story.
- **Refreshed "Inspire Me"** — Use dropdown categories (including the new **Music** category) with randomized scenarios.
- **Story history** — Access your last 20 generated stories, saved locally in your browser or app.
- **Wear OS companion app** — Real-time story synchronization to your watch with support for scrolling content using the physical watch crown (rotary dial).
- **[Iansui (芫荽) font](https://fonts.google.com/specimen/Iansui)** — Beautiful handwriting-style font specifically designed for Traditional Chinese legibility.
- **Model selection** — Choose version of Gemini for text generation.
- **Standalone & portable** — Zero dependencies, no `npm` required.

---

## 🛠️ Building and running locally

### 🖥️ Web development
1. Open `web/index.html` directly in any modern browser, or launch a simple local development server (e.g., `python3 -m http.server 8081`).
2. All logic is self-contained in `web/script.js` with zero build steps or package managers required.

### 🤖 Android app development
1. Build the project using [Android Studio](https://developer.android.com/studio) or the [Android CLI](https://developer.android.com/tools/agents).
    - Requires **Android Studio Ladybug** or newer.
    - **Min SDK**: 31 (Android 12)
    - **Target SDK**: 36
2. On first launch, the app will prompt you for your API Key.
3. Your key is stored securely on your device using **EncryptedSharedPreferences**.

### ⌚ Wear OS companion app
To build, install, and test the companion Wear OS app on a physical watch (e.g., Pixel Watch) locally, follow these steps:
1. **Enable Developer Options on the Watch**:
   - Go to **Settings** > **System** > **About** > **Versions** on the watch.
   - Tap **Build number** 7 times until you see the developer options toast.
   - Go back and open the new **Developer Options** section.
2. **Turn on Wireless Debugging**:
   - Connect both your computer and watch to the **same Wi-Fi network**.
   - Enable **ADB Debugging** and **Wireless Debugging** in Developer Options.
   - Tap **Wireless Debugging** to open details and view the IP/port.
3. **Pair and Connect from Computer**:
   - Tap **Pair new device** on the watch to get a pairing port and 6-digit code, then run:
     ```bash
     adb pair <WATCH_IP>:<PAIRING_PORT>
     ```
   - Connect using the main IP and port:
     ```bash
     adb connect <WATCH_IP>:<PORT>
     ```
   - Check connection via `adb devices`.
4. **Deploy and Run**:
   - Install the debug Wear OS APK:
     ```bash
     JAVA_HOME=/Library/Java/JavaVirtualMachines/microsoft-17.jdk/Contents/Home ./gradlew :wear:installDebug
     ```
   - Spin the physical crown (rotary dial) on both the story list screen and reading screen for smooth content scrolling.

---

## 🛠️ Architecture and tech stack

### Android app
- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3 Expressive)
- **Networking**: Ktor Client
- **Database**: Room (for story history)
- **Storage**: 
    - **Jetpack DataStore**: For general user preferences.
    - **EncryptedSharedPreferences**: For sensitive data (API Key).
- **Architecture**: MVVM with Repository pattern.

### Web app
- **Language**: Vanilla JavaScript / HTML5 / CSS3
- **Styling**: Modern CSS with custom properties and animations.
- **Storage**: `localStorage` for both settings and history.
- **AI**: Google AI Studio – Gemini 2.5/3.1 Flash Lite.
- **API**: Direct client-side `fetch` calls (Zero-npm).
- **Typography**: Iansui (芫荽) and Klee One (Google Fonts).

---

## 🚀 Deployment and CI/CD

This project uses **GitHub Actions** for automated CI/CD:
- **Web:** Automatically deployed to `sanbeiji.github.io/lotus/` upon tagging a new release.
- **Android:** The APK is automatically compiled and attached to the GitHub Release assets.

All web logic is contained within `/web/script.js` and styling is in `/web/style.css`. The Android app is located in the `/android` directory.

---

## 🔒 Security and privacy

- **API Keys**: Your API key is **never** sent to any server except directly to Google's Gemini API endpoints.
- **Local Storage**: 
    - On **Android**, the key is encrypted at rest using the Android Keystore.
    - On **Web**, the key is stored in `localStorage`. Users should be aware that `localStorage` is accessible to scripts on the same origin (XSS risk).
- **No Tracking**: This app does not include any analytics or tracking scripts.

---

## 💬 Feedback and issues

Have feedback, discovered a bug, or want to suggest a feature? I welcome your input! Please open an issue using the **[GitHub Issues](https://github.com/sanbeiji/lotus-pond/issues)** tracker.

---

## 📜 License and credits

- **License**: This project is licensed under the [GNU AGPLv3](LICENSE).
- **Font**: Uses the [Iansui (芫荽)](CREDITS.md) font (SIL Open Font License 1.1).
- **Author**: [Joseph R. Lewis](https://sanbeiji.com) ([@sanbeiji](https://github.com/sanbeiji/))

---

## ⚠️ Disclaimer for developers

If you fork this repository:
1. **Do not hard-code API keys.** Always use a secure method for secret storage.
2. **License Compliance**: As this is an AGPL-licensed project, any modifications you make and host publicly must also be open-sourced under the same license.
