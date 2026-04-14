# 🪷 Lotus Pond Reader

**蓮池故事機 (lián chí gù shì jī)** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader is a standalone web application that uses the Google AI Studio Gemini API to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and interlinear Pinyin/Zhuyin pronunciation.

## Features

- **Story generation** — Describe a plot or theme and get a full story in Traditional Mandarin.
- **8 [TOCFL](https://en.wikipedia.org/wiki/Test_of_Chinese_as_a_Foreign_Language)-aligned levels** — From Novice 1 to C6 (Advanced), tailored specifically for Taiwanese linguistic patterns.
- **Pinyin & Zhuyin toggle** — Switch between Pinyin and Zhuyin (Bopomofo) for pronunciation globally.
- **Show pronunciation toggle** — Hide Pinyin/Zhuyin for a reading challenge or show it for assistance.
- **Read aloud (TTS)** — Listen to each sentence with high-quality Mandarin speech synthesis (supports Taiwanese accents).
- **Study mode** — Visually highlight required vocabulary within the generated story.
- **Story history** — Access your last 10 generated stories, saved locally in your browser.
- **Iansui (芫荽) font** — Beautiful handwriting-style font specifically designed for Traditional Chinese legibility.
- **Model selection** — Choose between various stable 'lite' versions of Gemini (2.5, 3.1, etc.).
- **Standalone & portable** — Zero dependencies, no `npm` required. Runs on any web server or directly from your local machine.

## Setup

### Prerequisites

- A **Gemini API key** from [Google AI Studio](https://aistudio.google.com/).

### Running the app

Since this is a static web app, you can run it in several ways:

1.  **Directly:** Open `index.html` in any modern web browser.
2.  **Simple server:** Run a simple local server if needed (e.g., `python3 -m http.server 8000`).
3.  **Static hosting:** Deploy the files (`index.html`, `style.css`, `script.js`, and favicon assets) to GitHub Pages, Netlify, or any static host.

### Configuration

Once the app is open, click the **Settings (⚙️)** icon and enter your Gemini API key. Your key is stored securely in your browser's `localStorage` and is never sent to any server except the Google Gemini API.

## Technology

- **Frontend:** HTML5, CSS3 (Vanilla), Vanilla JavaScript.
- **AI:** Google AI Studio – Gemini 2.5/3.1 Flash Lite.
- **API:** Direct client-side `fetch` calls (Zero-npm).
- **Typography:** Iansui (芫荽) and Klee One (Google Fonts).

## Deployment

The project is structured to be easily hosted in a subdirectory (e.g., `https://sanbeiji.github.io/lotus/`). All asset links are relative to ensure compatibility.

## Development

This project was built to be as portable and lightweight as possible. All logic is contained within `script.js`, and all styling is in `style.css`. No build steps or compilation are required.
