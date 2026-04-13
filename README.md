# 🪷 Lotus Pond Reader

**蓮花池讀者** — AI-powered Mandarin story generator for language learners.

Lotus Pond Reader uses Google Vertex AI (Gemini Flash) to generate short stories in Taiwanese Mandarin, complete with traditional Chinese characters and Pinyin pronunciation under each sentence.

## Features

- **Story generation** — describe a plot/theme and get a full story in Traditional Mandarin
- **Difficulty levels 1–8** — aligned with the TOCFL exam (levels 1–6) plus expert and literary tiers
- **Required vocabulary** — optionally specify Mandarin words the story must include
- **Word count control** — set the desired story length
- **Pinyin display** — every sentence is followed by its Pinyin pronunciation
- **Copy to clipboard** — copy the full interleaved story with one click

## Technology

- **Backend:** Node.js + Express
- **AI:** Vertex AI – Gemini Flash (`gemini-2.0-flash-001`)
- **Frontend:** HTML, CSS, Vanilla JavaScript

## Setup

### Prerequisites

- Node.js 20+
- A Google Cloud project with the Vertex AI API enabled
- Application Default Credentials configured (`gcloud auth application-default login`)

### Install & run

```bash
# 1. Install dependencies
npm install

# 2. Set required environment variable
export GOOGLE_CLOUD_PROJECT=your-gcp-project-id

# 3. Start the server
npm start
```

The app will be available at <http://localhost:3000>.

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `GOOGLE_CLOUD_PROJECT` | **Yes** | – | Your GCP project ID |
| `LOCATION` | No | `us-central1` | Vertex AI region |
| `GEMINI_MODEL` | No | `gemini-2.0-flash-001` | Gemini model name |
| `PORT` | No | `3000` | Server port |

See `.env.example` for a template.

## Deploying to Google Cloud Run

```bash
gcloud run deploy lotus-pond-reader \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GOOGLE_CLOUD_PROJECT=$(gcloud config get-value project)
```

Cloud Run automatically provides the credentials needed for Vertex AI.
