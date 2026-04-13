'use strict';

const form = document.getElementById('story-form');
const generateBtn = document.getElementById('generate-btn');
const btnText = generateBtn.querySelector('.btn-text');
const btnLoading = generateBtn.querySelector('.btn-loading');
const resultSection = document.getElementById('result-section');
const errorSection = document.getElementById('error-section');
const errorMessage = document.getElementById('error-message');
const storyContent = document.getElementById('story-content');
const copyBtn = document.getElementById('copy-btn');

/** Toggle the loading/disabled state of the generate button. */
function setLoading(isLoading) {
  generateBtn.disabled = isLoading;
  btnText.hidden = isLoading;
  btnLoading.hidden = !isLoading;
}

/**
 * Render the story from the API response.
 * mandarinLines[i] is paired with pinyinLines[i].
 * The first pair is treated as the story title and styled accordingly.
 */
function displayStory(data) {
  const mandarinLines = (data['mandarin-result'] || '')
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean);

  const pinyinLines = (data['pinyin-result'] || '')
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean);

  storyContent.innerHTML = '';

  const maxLines = Math.max(mandarinLines.length, pinyinLines.length);

  for (let i = 0; i < maxLines; i++) {
    const block = document.createElement('div');
    block.className =
      i === 0 ? 'sentence-block sentence-block--title' : 'sentence-block';

    if (mandarinLines[i]) {
      const mandarinEl = document.createElement('div');
      mandarinEl.className = 'mandarin';
      mandarinEl.textContent = mandarinLines[i];
      block.appendChild(mandarinEl);
    }

    if (pinyinLines[i]) {
      const pinyinEl = document.createElement('div');
      pinyinEl.className = 'pinyin';
      pinyinEl.textContent = pinyinLines[i];
      block.appendChild(pinyinEl);
    }

    storyContent.appendChild(block);
  }
}

/** Handle form submission. */
form.addEventListener('submit', async (e) => {
  e.preventDefault();

  const plot = document.getElementById('plot').value.trim();
  const skillLevel = parseInt(document.getElementById('skillLevel').value, 10);
  const requiredTerms = document.getElementById('requiredTerms').value.trim();
  const lengthInWords = parseInt(
    document.getElementById('lengthInWords').value,
    10
  );

  // Basic client-side validation
  if (!plot) {
    document.getElementById('plot').focus();
    return;
  }

  setLoading(true);
  resultSection.hidden = true;
  errorSection.hidden = true;

  try {
    const response = await fetch('/api/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ plot, skillLevel, requiredTerms, lengthInWords }),
    });

    const data = await response.json();

    if (!response.ok) {
      throw new Error(data.error || 'Failed to generate story.');
    }

    displayStory(data);
    resultSection.hidden = false;
    resultSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (err) {
    errorMessage.textContent = `Error: ${err.message}`;
    errorSection.hidden = false;
    errorSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } finally {
    setLoading(false);
  }
});

/** Copy the full story (interleaved Mandarin + Pinyin) to the clipboard. */
copyBtn.addEventListener('click', () => {
  const blocks = storyContent.querySelectorAll('.sentence-block');
  const lines = [];

  blocks.forEach((block) => {
    const mandarin = block.querySelector('.mandarin');
    const pinyin = block.querySelector('.pinyin');
    if (mandarin) lines.push(mandarin.textContent);
    if (pinyin) lines.push(pinyin.textContent);
    lines.push('');          // blank line between sentence pairs
  });

  const text = lines.join('\n').replace(/\n{3,}/g, '\n\n').trim();

  navigator.clipboard
    .writeText(text)
    .then(() => {
      copyBtn.textContent = '✓ Copied!';
      setTimeout(() => {
        copyBtn.textContent = 'Copy Story';
      }, 2000);
    })
    .catch(() => {
      copyBtn.textContent = 'Copy failed';
      setTimeout(() => {
        copyBtn.textContent = 'Copy Story';
      }, 2000);
    });
});
