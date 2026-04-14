/**
 * Lotus Pond Reader – script.js
 * Core logic for the standalone Mandarin story generator.
 */

// ─── Constants & Configuration ────────────────────────────────

const MODEL_NAME = 'gemini-3.1-flash-lite-preview';
const API_BASE = 'https://generativelanguage.googleapis.com/v1beta';

const DEFAULT_SETTINGS = {
    apiKey: '',
    pronunciation: 'pinyin',
    studyMode: false,
    history: []
};

// ─── App State ────────────────────────────────────────────────

let state = { ...DEFAULT_SETTINGS };

// ─── DOM Elements ─────────────────────────────────────────────

const elements = {
    storyForm: document.getElementById('story-form'),
    plotInput: document.getElementById('plot'),
    skillLevelSelect: document.getElementById('skillLevel'),
    lengthInput: document.getElementById('lengthInWords'),
    requiredTermsInput: document.getElementById('requiredTerms'),
    generateBtn: document.getElementById('generate-btn'),
    
    apiKeyInput: document.getElementById('api-key'),
    pinyinToggle: document.getElementById('pinyin-toggle'),
    zhuyinToggle: document.getElementById('zhuyin-toggle'),
    studyModeToggle: document.getElementById('study-mode'),
    toggleSettingsBtn: document.getElementById('toggle-settings'),
    settingsContent: document.getElementById('settings-content'),
    
    progressSection: document.getElementById('progress-section'),
    progressFill: document.getElementById('progress-fill'),
    progressText: document.getElementById('progress-text'),
    
    errorSection: document.getElementById('error-section'),
    errorMessage: document.getElementById('error-message'),
    
    resultSection: document.getElementById('result-section'),
    storyContent: document.getElementById('story-content'),
    storyHeading: document.getElementById('story-heading'),
    showPronunciationToggle: document.getElementById('show-pronunciation'),
    copyBtn: document.getElementById('copy-btn'),
    
    historyList: document.getElementById('history-list'),
    historyListMobile: document.getElementById('history-list-mobile'),
    clearHistoryBtn: document.getElementById('clear-history'),
    clearHistoryMobileBtn: document.getElementById('clear-history-mobile'),
    showHistoryBtn: document.getElementById('show-history-btn'),
    historyModal: document.getElementById('history-modal'),
    closeHistoryBtn: document.getElementById('close-history')
};

// ─── Initialization ───────────────────────────────────────────

function init() {
    loadState();
    setupEventListeners();
    renderHistory();
    
    // Auto-expand textarea
    elements.plotInput.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = (this.scrollHeight) + 'px';
    });
}

// ─── State Management ─────────────────────────────────────────

function loadState() {
    const saved = localStorage.getItem('lotusPondState');
    if (saved) {
        try {
            const parsed = JSON.parse(saved);
            state = { ...state, ...parsed };
        } catch (e) {
            console.error('Failed to parse saved state', e);
        }
    }
    
    // Apply state to UI
    elements.apiKeyInput.value = state.apiKey;
    if (state.pronunciation === 'zhuyin') {
        elements.zhuyinToggle.checked = true;
    } else {
        elements.pinyinToggle.checked = true;
    }
    elements.studyModeToggle.checked = state.studyMode;
    
    // If no API key, show settings
    if (!state.apiKey) {
        elements.settingsContent.hidden = false;
    }
}

function saveState() {
    localStorage.setItem('lotusPondState', JSON.stringify(state));
}

// ─── Event Listeners ──────────────────────────────────────────

function setupEventListeners() {
    elements.storyForm.addEventListener('submit', handleGenerate);
    
    elements.toggleSettingsBtn.addEventListener('click', () => {
        elements.settingsContent.hidden = !elements.settingsContent.hidden;
    });
    
    elements.apiKeyInput.addEventListener('change', (e) => {
        state.apiKey = e.target.value.trim();
        saveState();
    });
    
    [elements.pinyinToggle, elements.zhuyinToggle].forEach(el => {
        el.addEventListener('change', (e) => {
            state.pronunciation = e.target.value;
            saveState();
            // Re-render current story if visible
            if (!elements.resultSection.hidden && lastStoryData) {
                renderStory(lastStoryData);
            }
        });
    });
    
    elements.studyModeToggle.addEventListener('change', (e) => {
        state.studyMode = e.target.checked;
        saveState();
        if (!elements.resultSection.hidden && lastStoryData) {
            renderStory(lastStoryData);
        }
    });
    
    elements.showPronunciationToggle.addEventListener('change', updatePronunciationVisibility);
    
    elements.copyBtn.addEventListener('click', copyToClipboard);
    
    // History
    elements.clearHistoryBtn.addEventListener('click', clearHistory);
    elements.clearHistoryMobileBtn.addEventListener('click', clearHistory);
    
    elements.showHistoryBtn.addEventListener('click', () => {
        elements.historyModal.hidden = false;
        renderHistory();
    });
    
    elements.closeHistoryBtn.addEventListener('click', () => {
        elements.historyModal.hidden = true;
    });
    
    window.addEventListener('click', (e) => {
        if (e.target === elements.historyModal) {
            elements.historyModal.hidden = true;
        }
    });
}

// ─── API Integration ──────────────────────────────────────────

let lastStoryData = null;

async function handleGenerate(e) {
    e.preventDefault();
    
    if (!state.apiKey) {
        showError('Please enter your Gemini API Key in the settings.');
        elements.settingsContent.hidden = false;
        elements.apiKeyInput.focus();
        return;
    }
    
    const plot = elements.plotInput.value.trim();
    const skillLevel = elements.skillLevelSelect.value;
    const length = elements.lengthInput.value;
    const requiredTerms = elements.requiredTermsInput.value.trim();
    
    if (!plot) {
        showError('Please provide a plot or theme for the story.');
        return;
    }
    
    hideError();
    showLoading(true);
    
    try {
        const prompt = buildPrompt(plot, skillLevel, length, requiredTerms);
        const result = await callGemini(prompt);
        const storyData = parseResponse(result);
        
        lastStoryData = storyData;
        renderStory(storyData);
        addToHistory(storyData);
        
        elements.resultSection.hidden = false;
        elements.resultSection.scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
        console.error(err);
        showError(err.message || 'An unexpected error occurred during generation.');
    } finally {
        showLoading(false);
    }
}

function buildPrompt(plot, skillLevel, length, requiredTerms) {
    return `You are teaching Mandarin to an English speaker. Generate a story in Mandarin to be used for the purposes of learning to read, write, and speak Mandarin. 

CRITICAL REQUIREMENTS:
1. TRADITIONAL CHARACTERS: Use traditional Mandarin characters only.
2. TAIWANESE STYLE: Use language, grammar, slang, and idioms common to Taiwan (e.g., use 影片 instead of 視頻, 捷運 instead of 地鐵).
3. SKILL LEVEL: Adhere strictly to the ${skillLevel} level.
4. WORD COUNT: Aim for approximately ${length} Mandarin characters.
5. VOCABULARY: Include these terms if provided: "${requiredTerms}".
6. STRUCTURE: Break the story into logical sentences. Each sentence must be its own object in the response.

OUTPUT FORMAT:
You must return a valid JSON object with NO OTHER TEXT.
The JSON must follow this exact structure:
{
  "title": "Story Title in Traditional Mandarin",
  "sentences": [
    {
      "mandarin": "Mandarin sentence here",
      "pinyin": "Pinyin pronunciation here (with tone marks)",
      "zhuyin": "Zhuyin/Bopomofo pronunciation here"
    },
    ...
  ]
}

Plot for the story: ${plot}`;
}

async function callGemini(prompt) {
    updateProgress(10, 'Sending request to Gemini...');
    
    const url = `${API_BASE}/models/${MODEL_NAME}:generateContent?key=${state.apiKey}`;
    
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: {
                temperature: 0.7,
                topK: 40,
                topP: 0.95,
                maxOutputTokens: 2048,
                responseMimeType: "application/json"
            }
        })
    });
    
    updateProgress(50, 'Receiving story from AI...');
    
    if (!response.ok) {
        const err = await response.json();
        throw new Error(err.error?.message || 'API request failed');
    }
    
    const data = await response.json();
    updateProgress(90, 'Finalizing content...');
    
    return data.candidates[0].content.parts[0].text;
}

function parseResponse(text) {
    try {
        // Handle potential markdown code blocks in response
        const cleanText = text.replace(/```json/g, '').replace(/```/g, '').trim();
        return JSON.parse(cleanText);
    } catch (e) {
        console.error('JSON Parse Error:', e, text);
        throw new Error('Failed to parse AI response. The AI might have returned invalid JSON. Please try again.');
    }
}

// ─── Rendering & UI ───────────────────────────────────────────

function renderStory(storyData) {
    elements.storyHeading.textContent = storyData.title;
    elements.storyContent.innerHTML = '';
    
    const requiredTerms = elements.requiredTermsInput.value.split(/[ ,，]+/).filter(t => t.length > 0);
    
    storyData.sentences.forEach((s, index) => {
        const block = document.createElement('div');
        block.className = 'sentence-block';
        
        const playBtn = document.createElement('button');
        playBtn.className = 'sentence-play-btn';
        playBtn.innerHTML = '🔊';
        playBtn.title = 'Read aloud';
        playBtn.onclick = () => speak(s.mandarin);
        
        const mandarin = document.createElement('div');
        mandarin.className = 'mandarin';
        
        let html = s.mandarin;
        if (state.studyMode && requiredTerms.length > 0) {
            requiredTerms.forEach(term => {
                const regex = new RegExp(term, 'g');
                html = html.replace(regex, `<span class="vocab-highlight">${term}</span>`);
            });
        }
        mandarin.innerHTML = html;
        
        const pron = document.createElement('div');
        pron.className = 'pronunciation';
        pron.textContent = state.pronunciation === 'zhuyin' ? s.zhuyin : s.pinyin;
        pron.hidden = !elements.showPronunciationToggle.checked;
        
        block.appendChild(playBtn);
        block.appendChild(mandarin);
        block.appendChild(pron);
        elements.storyContent.appendChild(block);
    });
}

function updatePronunciationVisibility() {
    const show = elements.showPronunciationToggle.checked;
    const prons = elements.storyContent.querySelectorAll('.pronunciation');
    prons.forEach(p => p.hidden = !show);
}

function showLoading(show) {
    elements.generateBtn.disabled = show;
    elements.progressSection.hidden = !show;
    if (show) {
        updateProgress(0, 'Waking up the dragon...');
        elements.resultSection.hidden = true;
        elements.errorSection.hidden = true;
    }
}

function updateProgress(percent, text) {
    elements.progressFill.style.width = `${percent}%`;
    if (text) elements.progressText.textContent = text;
}

function showError(msg) {
    elements.errorMessage.textContent = msg;
    elements.errorSection.hidden = false;
    elements.errorSection.scrollIntoView({ behavior: 'smooth' });
}

function hideError() {
    elements.errorSection.hidden = true;
}

// ─── TTS (Read Aloud) ─────────────────────────────────────────

function speak(text) {
    // Cancel any ongoing speech
    window.speechSynthesis.cancel();
    
    const utterance = new SpeechSynthesisUtterance(text);
    
    // Try to find a Chinese voice (preferably Traditional/Taiwanese)
    const voices = window.speechSynthesis.getVoices();
    const zhVoice = voices.find(v => v.lang === 'zh-TW') || 
                   voices.find(v => v.lang.startsWith('zh')) ||
                   voices[0];
    
    if (zhVoice) {
        utterance.voice = zhVoice;
        utterance.lang = zhVoice.lang;
    }
    
    utterance.rate = 0.9; // Slightly slower for learners
    window.speechSynthesis.speak(utterance);
}

// ─── History Management ───────────────────────────────────────

function addToHistory(storyData) {
    const historyItem = {
        id: Date.now(),
        title: storyData.title,
        data: storyData,
        date: new Date().toLocaleString()
    };
    
    state.history.unshift(historyItem);
    if (state.history.length > 10) {
        state.history.pop();
    }
    
    saveState();
    renderHistory();
}

function renderHistory() {
    const containers = [elements.historyList, elements.historyListMobile];
    
    containers.forEach(container => {
        if (!container) return;
        container.innerHTML = '';
        
        if (state.history.length === 0) {
            container.innerHTML = '<p class="empty-msg">No stories yet.</p>';
            return;
        }
        
        state.history.forEach(item => {
            const el = document.createElement('div');
            el.className = 'history-item';
            el.innerHTML = `
                <div class="history-title">${item.title}</div>
                <div class="history-date">${item.date}</div>
            `;
            el.onclick = () => {
                lastStoryData = item.data;
                renderStory(item.data);
                elements.resultSection.hidden = false;
                elements.resultSection.scrollIntoView({ behavior: 'smooth' });
                elements.historyModal.hidden = true;
            };
            container.appendChild(el);
        });
    });
}

function clearHistory() {
    if (confirm('Are you sure you want to clear your story history?')) {
        state.history = [];
        saveState();
        renderHistory();
    }
}

// ─── Utils ────────────────────────────────────────────────────

function copyToClipboard() {
    if (!lastStoryData) return;
    
    let text = `${lastStoryData.title}

`;
    lastStoryData.sentences.forEach(s => {
        text += `${s.mandarin}
`;
        text += `${state.pronunciation === 'zhuyin' ? s.zhuyin : s.pinyin}

`;
    });
    
    navigator.clipboard.writeText(text).then(() => {
        const originalText = elements.copyBtn.textContent;
        elements.copyBtn.textContent = '✅ Copied!';
        setTimeout(() => {
            elements.copyBtn.textContent = originalText;
        }, 2000);
    }).catch(err => {
        console.error('Copy failed', err);
        alert('Failed to copy to clipboard.');
    });
}

// Run init
init();
// Load voices once they are ready
window.speechSynthesis.onvoiceschanged = () => {
    // Just warms up the voice list
    window.speechSynthesis.getVoices();
};
