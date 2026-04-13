'use strict';

const express = require('express');
const path = require('path');
const { GoogleGenAI } = require('@google/genai');

const app = express();
const PORT = process.env.PORT || 3000;
const PROJECT_ID = process.env.GOOGLE_CLOUD_PROJECT;
const LOCATION = process.env.LOCATION || 'us-central1';
const MODEL = process.env.GEMINI_MODEL || 'gemini-2.0-flash-001';

if (!PROJECT_ID) {
  console.error(
    'ERROR: The GOOGLE_CLOUD_PROJECT environment variable is required.\n' +
      'Set it to your Google Cloud project ID before starting the server.'
  );
  process.exit(1);
}

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

/**
 * Build the Gemini prompt from user inputs.
 */
function buildPrompt(plot, skillLevel, requiredTerms, lengthInWords) {
  const termsSection =
    requiredTerms && requiredTerms.trim() ? requiredTerms.trim() : 'None';

  return (
    `You are teaching Mandarin to an English speaker. Generate a story in Mandarin to be used ` +
    `for the purposes of learning to read, write, and speak Mandarin. Output should use ` +
    `traditional Mandarin characters.\n\n` +
    `Use a style of language, including grammar, slang, stylistic variances, character variations, ` +
    `cultural topics, and idioms that are common to Taiwan. Be creative with the details, adding ` +
    `dialogue or situations as appropriate.\n\n` +
    `Use repetition of key terms, especially required vocabulary, when it makes sense to do so. ` +
    `Write one sentence per line of text. After each sentence, on the next line, include the ` +
    `Pinyin pronunciation.\n\n` +
    `Mandarin skill levels are ranked 1 through 8. For this setting, responses should try to ` +
    `adhere to what each level means:\n\n` +
    `1 = equivalent to TOCFL level 1\n` +
    `2 = equivalent to TOCFL level 2\n` +
    `3 = equivalent to TOCFL level 3\n` +
    `4 = equivalent to TOCFL level 4\n` +
    `5 = equivalent to TOCFL level 5\n` +
    `6 = equivalent to TOCFL level 6\n` +
    `7 = very advanced, beyond TOCFL levels\n` +
    `8 = arcane or poetic forms of language\n\n` +
    `Crucial: You must return the result as a JSON object.\n` +
    `Place the Mandarin text in the \`mandarin-result\` field.\n` +
    `Place the Pinyin transcription in the \`pinyin-result\` field.\n` +
    `Do not include any prose, greetings, or explanations outside of the JSON structure.\n\n` +
    `Plot for the story:\n${plot}\n\n` +
    `Mandarin skill level (1-8):\n${skillLevel}\n\n` +
    `Optional Required vocabulary - use these Mandarin words in the story:\n${termsSection}\n\n` +
    `Length of story - number of words:\n${lengthInWords}`
  );
}

/**
 * Strip markdown code fences that some models prepend/append around JSON output.
 */
function stripCodeFences(text) {
  return text
    .replace(/^```(?:json)?\s*/i, '')
    .replace(/\s*```$/, '')
    .trim();
}

app.post('/api/generate', async (req, res) => {
  try {
    const { plot, skillLevel, requiredTerms, lengthInWords } = req.body;

    if (!plot || skillLevel == null || !lengthInWords) {
      return res.status(400).json({
        error: 'Missing required fields: plot, skillLevel, lengthInWords',
      });
    }

    const prompt = buildPrompt(plot, skillLevel, requiredTerms, lengthInWords);

    const ai = new GoogleGenAI({ vertexai: true, project: PROJECT_ID, location: LOCATION });

    const response = await ai.models.generateContent({
      model: MODEL,
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        temperature: 0.7,
        maxOutputTokens: 8192,
      },
    });

    const responseText = response.text;

    let parsed;
    try {
      parsed = JSON.parse(stripCodeFences(responseText));
    } catch (parseErr) {
      console.error('Failed to parse AI response as JSON:', responseText);
      return res.status(500).json({
        error: 'Failed to parse story response from AI.',
        raw: responseText,
      });
    }

    res.json(parsed);
  } catch (err) {
    console.error('Error generating story:', err);
    res.status(500).json({ error: err.message || 'Failed to generate story.' });
  }
});

app.listen(PORT, () => {
  console.log(`Lotus Pond Reader server listening on port ${PORT}`);
});
