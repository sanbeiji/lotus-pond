# 🪷 Lotus Pond Reader

**蓮花池讀者** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader is a standalone web application that uses the Google AI Studio Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation.

## Features

- **Story Generation** — Describe a plot/theme and get a full story in Traditional Mandarin.
- **8 Difficulty Levels** — From Novice 1 to C6 (Advanced), tailored for Taiwanese patterns.
- **Pinyin & Zhuyin Toggle** — Switch between Pinyin and Zhuyin (Bopomofo) for pronunciation.
- **Read Aloud (TTS)** — Listen to each sentence with high-quality Mandarin speech synthesis.
- **Study Mode** — Visually highlight required vocabulary within the generated story.
- **Story History** — Access your last 10 generated stories, saved locally in your browser.
- **Standalone & Portable** — Zero dependencies, no `npm` required. Runs on any web server or directly from your local machine.

## Setup

### Prerequisites

- A **Gemini API Key** from [Google AI Studio](https://aistudio.google.com/).

### Running the App

Since this is a static web app, you can run it in several ways:

1.  **Directly:** Open `index.html` in any modern web browser.
2.  **Simple Server:** Run a simple local server if needed (e.g., `python3 -m http.server 8000`).
3.  **Static Hosting:** Deploy the files (`index.html`, `style.css`, `script.js`) to GitHub Pages, Netlify, or any static host.

### Configuration

Once the app is open, click the **Settings (⚙️)** icon and enter your Gemini API Key. Your key is stored securely in your browser's `localStorage` and is never sent to any server except the Google Gemini API.

## Technology

- **Frontend:** HTML5, CSS3 (Vanilla), Vanilla JavaScript.
- **AI:** Google AI Studio – Gemini 1.5 Flash.
- **API:** Direct client-side `fetch` calls (Zero-npm).

## Development

This project was built to be as portable and lightweight as possible. All logic is contained within `script.js`, and all styling is in `style.css`. No build steps or compilation are required.
