/**
 * Lotus Pond Reader – script.js
 * Core logic for the standalone Mandarin story generator.
 */

// ─── Constants & Configuration ────────────────────────────────

const API_BASE = 'https://generativelanguage.googleapis.com/v1beta';

const LOADING_QUOTES = [
    "正在喚醒神龍...\nZhèngzài huànxǐng shénlóng...\nWaking up the dragon...",
    "正在磨練書法筆...\nZhèngzài móliàn shūfǎ bǐ...\nSharpening the calligraphy brushes...",
    "正在泡高山烏龍茶...\nZhèngzài pào gāoshān wūlóngchá...\nBrewing some high-mountain oolong...",
    "正在練習「臺灣」的筆畫...\nZhèngzài liànxí \"Táiwān\" de bǐhuà...\nPracticing the brush strokes for writing 臺灣...",
    "正在查閱農曆...\nZhèngzài cháyuè nónglì...\nConsulting the lunar calendar...",
    "正在種植蓮子...\nZhèngzài zhòngzhí liánzǐ...\nPlanting the lotus seeds...",
    "正在拉伸墨條...\nZhèngzài lāshēn mòtiáo...\nStretching the ink sticks...",
    "正在向灶神尋求靈感...\nZhèngzài xiàng Zàoshén xúnqiú línggǎn...\nAsking the kitchen god for inspiration...",
    "正在調古箏...\nZhèngzài tiáo gǔzhēng...\nTuning the guzheng...",
    "正在變魔術...\nZhèngzài biàn móshù...\nWhipping up the magic...",
    "你需要更多水晶塔...\nNǐ xūyào gèng duō shuǐjīng tǎ...\nYou require more pylons...",
    "攪拌，不要搖晃...\nJiǎobàn, bùyào yáohuàng...\nShaking, not stirring...",
    "正在做墨西哥捲餅...\nZhèngzài zuò mòxīgē juǎnbǐng...\nMaking the chimichangas..."
];

const DEFAULT_SETTINGS = {
    apiKey: '',
    persistKey: true,
    selectedModel: 'gemini-2.5-flash-lite',
    pronunciation: 'pinyin',
    studyMode: true,
    showTranslation: true,
    history: [],
    themePreference: 'system',
    fontSizePreference: 'small'
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
    persistKeyToggle: document.getElementById('persist-key'),
    clearApiKeyBtn: document.getElementById('clear-api-key'),
    modelSelect: document.getElementById('model-select'),
    themeSelect: document.getElementById('theme-select'),
    pinyinToggle: document.getElementById('pinyin-toggle'),
    zhuyinToggle: document.getElementById('zhuyin-toggle'),
    studyModeToggle: document.getElementById('study-mode'),
    toggleSettingsBtn: document.getElementById('toggle-settings'),
    settingsContent: document.getElementById('settings-content'),
    
    progressSection: document.getElementById('progress-section'),
    progressText: document.getElementById('progress-text'),
    
    errorSection: document.getElementById('error-section'),
    errorMessage: document.getElementById('error-message'),
    
    resultSection: document.getElementById('result-section'),
    storyContent: document.getElementById('story-content'),
    storyHeading: document.getElementById('story-heading'),
    toggleStorySettingsBtn: document.getElementById('toggle-story-settings'),
    storySettingsPanel: document.getElementById('story-settings-panel'),
    showPronunciationToggle: document.getElementById('show-pronunciation'),
    showTranslationToggle: document.getElementById('show-translation'),
    fontSizeRadios: document.querySelectorAll('input[name="font-size"]'),
    copyBtn: document.getElementById('copy-btn'),
    
    historyList: document.getElementById('history-list'),
    historyListMobile: document.getElementById('history-list-mobile'),
    clearHistoryBtn: document.getElementById('clear-history'),
    clearHistoryMobileBtn: document.getElementById('clear-history-mobile'),
    showHistoryBtn: document.getElementById('show-history-btn'),
    historyModal: document.getElementById('history-modal'),
    closeHistoryBtn: document.getElementById('close-history'),
    
    aboutBtn: document.getElementById('about-btn'),
    headerAboutBtn: document.getElementById('header-about-btn'),
    aboutModal: document.getElementById('about-modal'),
    closeAboutBtn: document.getElementById('close-about')
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
    
    // Vocab validation
    const vocabWarning = document.getElementById('vocab-warning');
    if (elements.requiredTermsInput) {
        elements.requiredTermsInput.addEventListener('input', function() {
            const val = this.value.trim();
            if (val === '') {
                this.classList.remove('invalid');
                if (vocabWarning) vocabWarning.hidden = true;
                return;
            }
            
            // Allowed: CJK characters, English and Chinese commas, spaces
            const validRegex = /^[\u4e00-\u9fa5,，\s]*$/;
            if (!validRegex.test(val)) {
                this.classList.add('invalid');
                if (vocabWarning) vocabWarning.hidden = false;
            } else {
                this.classList.remove('invalid');
                if (vocabWarning) vocabWarning.hidden = true;
            }
        });
    }
}

// ─── State Management ─────────────────────────────────────────

function loadState() {
    // 1. Try persistent storage
    let saved = localStorage.getItem('lotusPondState');
    
    // 2. If not found or if session-only was preferred, try session storage
    if (!saved) {
        saved = sessionStorage.getItem('lotusPondState');
    }

    if (saved) {
        try {
            const parsed = JSON.parse(saved);
            state = { ...state, ...parsed };
            
            // Clean up other storage if preference changed
            if (!state.persistKey) {
                localStorage.removeItem('lotusPondState');
            } else {
                sessionStorage.removeItem('lotusPondState');
            }
        } catch (e) {
            console.error('Failed to parse saved state', e);
        }
    }
    
    // Apply state to UI
    if (elements.apiKeyInput) elements.apiKeyInput.value = state.apiKey;
    if (elements.persistKeyToggle) elements.persistKeyToggle.checked = state.persistKey;
    if (elements.modelSelect) {
        elements.modelSelect.value = state.selectedModel || 'gemini-2.5-flash-lite';
    }
    if (elements.themeSelect) {
        elements.themeSelect.value = state.themePreference || 'system';
    }
    applyTheme();
    if (state.pronunciation === 'zhuyin') {
        if (elements.zhuyinToggle) elements.zhuyinToggle.checked = true;
    } else {
        if (elements.pinyinToggle) elements.pinyinToggle.checked = true;
    }
    if (elements.studyModeToggle) elements.studyModeToggle.checked = state.studyMode;
    if (elements.showTranslationToggle) elements.showTranslationToggle.checked = state.showTranslation;
    
    if (elements.fontSizeRadios) {
        elements.fontSizeRadios.forEach(radio => {
            if (radio.value === (state.fontSizePreference || 'small')) {
                radio.checked = true;
            }
        });
    }
    applyFontSize();
    
    updateModelFooter();
    
    // If no API key, show settings
    if (!state.apiKey && elements.settingsContent) {
        elements.settingsContent.hidden = false;
    }
}

function applyTheme() {
    let isDark = false;
    if (state.themePreference === 'dark') {
        isDark = true;
    } else if (state.themePreference === 'system') {
        isDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    
    if (isDark) {
        document.documentElement.setAttribute('data-theme', 'dark');
    } else {
        document.documentElement.removeAttribute('data-theme');
    }
}

function applyFontSize() {
    elements.storyContent.className = 'story-content';
    if (state.fontSizePreference === 'medium') {
        elements.storyContent.classList.add('font-size-medium');
    } else if (state.fontSizePreference === 'large') {
        elements.storyContent.classList.add('font-size-large');
    }
}

function updateModelFooter() {
    const modelDisplay = document.getElementById('model-version');
    if (modelDisplay) modelDisplay.textContent = state.selectedModel || 'gemini-2.5-flash-lite';
}

function saveState() {
    const data = JSON.stringify(state);
    if (state.persistKey) {
        localStorage.setItem('lotusPondState', data);
        sessionStorage.removeItem('lotusPondState');
    } else {
        sessionStorage.setItem('lotusPondState', data);
        localStorage.removeItem('lotusPondState');
    }
}

// ─── Event Listeners ──────────────────────────────────────────

function setupEventListeners() {
    elements.storyForm.addEventListener('submit', handleGenerate);
    
    elements.toggleSettingsBtn.addEventListener('click', () => {
        const isHidden = !elements.settingsContent.hidden;
        elements.settingsContent.hidden = isHidden;
        elements.toggleSettingsBtn.dataset.tooltip = isHidden ? "Edit global settings" : "Close global settings";
    });

    if (elements.toggleStorySettingsBtn && elements.storySettingsPanel) {
        elements.toggleStorySettingsBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isShowing = elements.storySettingsPanel.classList.toggle('show');
            elements.toggleStorySettingsBtn.dataset.tooltip = isShowing ? "Close reading settings" : "Edit reading settings";
        });

        document.addEventListener('click', (e) => {
            if (elements.storySettingsPanel.classList.contains('show') &&
                !elements.storySettingsPanel.contains(e.target) &&
                e.target !== elements.toggleStorySettingsBtn) {
                elements.storySettingsPanel.classList.remove('show');
                elements.toggleStorySettingsBtn.dataset.tooltip = "Edit reading settings";
            }
        });
        // Prevent clicks inside the panel from closing it
        elements.storySettingsPanel.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    }
    
    elements.apiKeyInput.addEventListener('change', (e) => {
        state.apiKey = e.target.value.trim();
        saveState();
    });

    elements.persistKeyToggle?.addEventListener('change', (e) => {
        state.persistKey = e.target.checked;
        saveState();
    });

    elements.clearApiKeyBtn?.addEventListener('click', () => {
        if (confirm('Delete your API Key from this browser?')) {
            state.apiKey = '';
            elements.apiKeyInput.value = '';
            saveState();
        }
    });
    
    elements.modelSelect.addEventListener('change', (e) => {
        state.selectedModel = e.target.value;
        saveState();
        updateModelFooter();
    });
    
    elements.themeSelect?.addEventListener('change', (e) => {
        state.themePreference = e.target.value;
        saveState();
        applyTheme();
    });

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (state.themePreference === 'system') {
            applyTheme();
        }
    });
    
    [elements.pinyinToggle, elements.zhuyinToggle].forEach(el => {
        el?.addEventListener('change', (e) => {
            state.pronunciation = e.target.value;
            saveState();
        });
    });
    
    elements.studyModeToggle?.addEventListener('change', (e) => {
        state.studyMode = e.target.checked;
        saveState();
        if (!elements.resultSection.hidden && lastStoryData) {
            renderStory(lastStoryData);
        }
    });

    elements.showTranslationToggle?.addEventListener('change', (e) => {
        state.showTranslation = e.target.checked;
        saveState();
        updateTranslationVisibility();
    });
    
    if (elements.fontSizeRadios) {
        elements.fontSizeRadios.forEach(radio => {
            radio.addEventListener('change', (e) => {
                if (e.target.checked) {
                    state.fontSizePreference = e.target.value;
                    saveState();
                    applyFontSize();
                }
            });
        });
    }

    if (elements.showPronunciationToggle) {
        elements.showPronunciationToggle.addEventListener('change', updatePronunciationVisibility);
    }

    if (elements.copyBtn) {
        elements.copyBtn.addEventListener('click', copyToClipboard);
    }
    
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

    elements.aboutBtn.addEventListener('click', () => {
        elements.aboutModal.hidden = false;
    });

    elements.headerAboutBtn?.addEventListener('click', () => {
        elements.aboutModal.hidden = false;
    });
    
    elements.closeAboutBtn.addEventListener('click', () => {
        elements.aboutModal.hidden = true;
    });
    
    window.addEventListener('click', (e) => {
        if (e.target === elements.historyModal) {
            elements.historyModal.hidden = true;
        }
        if (e.target === elements.aboutModal) {
            elements.aboutModal.hidden = true;
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
    let length = parseInt(elements.lengthInput.value);
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
    const levelGuide = `
SKILL LEVEL DEFINITIONS (TOCFL BANDS):
- Novice 1/2: Extremely simple S-V-O sentences. Use only the most basic daily vocabulary (numbers, greetings, colors, family). Avoid all complex grammar.
- A1 (Entry): Basic social interactions. Simple daily topics (shopping, weather). Clear, short sentences.
- A2 (Foundation): Common life situations. Basic connectors (because, but). Simple descriptions of past/future events.
- B3 (Intermediate): Fluent daily communication. Use of more varied conjunctions and descriptive adverbs. Discussion of work/travel.
- B4 (Upper Intermediate): Can discuss abstract topics. Uses passive voice and complex relative clauses. Varied vocabulary.
- C5 (Fluent): Professional and academic topics. High-level idioms and nuanced cultural expressions.
- C6 (Advanced): Academic, technical, and literary proficiency. Use of sophisticated Chengyu (idioms), classical structures, and nuanced stylistic variances.
`;

    // Realistic sentence targets: ~20-25 characters per sentence
    const sentenceTarget = Math.max(3, Math.ceil(length / 22));
    
    const novellaInstruction = length > 1000 ? `
The requested story is a NOVELLA (at least ${length} characters). 
You MUST structure it as a 5-chapter story with distinct scenes for each chapter. 
Expand on the world-building, sensory details, internal character thoughts, and extensive dialogue. 
DO NOT SUMMARIZE. Write as if you are a professional author.
` : "";

    const lengthPriority = length > 1000 ? "ABSOLUTE HIGHEST priority" : "important target";
    const lengthAdjective = length > 1000 ? "AT LEAST" : "approximately";

    const pronLabel = state.pronunciation === 'zhuyin' ? 'zhuyin' : 'pinyin';
    const pronInstruction = state.pronunciation === 'zhuyin' ? 'Zhuyin/Bopomofo pronunciation here' : 'Pinyin pronunciation here (Taiwanese style, e.g. \'hàn\')';

    return `You are teaching Mandarin to an English speaker. Generate a story in Mandarin to be used for the purposes of learning to read, write, and speak Mandarin. 

${levelGuide}

CRITICAL LINGUISTIC REQUIREMENTS:
1. TRADITIONAL CHARACTERS: Use traditional Mandarin characters only.
2. TAIWANESE STYLE: Use grammar, slang, and idioms common to Taiwan (e.g., use 影片 instead of 視頻, 捷運 instead of 地鐵, 腳踏車 instead of 自行車).
3. TAIWANESE PRONUNCIATION: The ${state.pronunciation.toUpperCase()} MUST reflect local Taiwanese pronunciation. 
   - CRUCIAL: '和' must be pronounced 'hàn' (not 'hé').
   - Use other Taiwanese variations where applicable (e.g., 垃圾 as 'lèsè').
4. CULTURAL & GEOGRAPHICAL BREADTH: Explore the full diversity of Taiwan. Do not over-rely on Taipei or common tropes. 
   - GEOGRAPHY: Vary the settings across different cities (e.g., Taichung, Tainan, Hualien, Keelung), counties (e.g., Yilan, Pingtung, Nantou), and landscapes (high mountain tea farms, coastal fishing villages, bustling night markets, quiet rural towns).
   - CULTURE: Incorporate a wide range of Taiwanese life, such as temple festivals, traditional arts (like glove puppetry), tea ceremonies, hiking culture, family dynamics, local snacks (小吃), and historical landmarks.
   - SOCIAL NORMS: Reflect authentic Taiwanese social etiquette and daily interactions.
5. SKILL LEVEL: Adhere strictly to the ${skillLevel} level requirements defined above.
6. VOCABULARY INTEGRATION: If specific vocabulary terms are provided ("${requiredTerms}"), you MUST include EVERY term at least TWICE in the story. Ensure they are used naturally but frequently enough for the reader to practice them. Integrate them into both narrative and dialogue where appropriate.
7. STRUCTURE: Break the story into logical sentences. Each sentence must be its own object in the response.

OUTPUT FORMAT:
You must return a valid JSON object with NO OTHER TEXT before or after the JSON. DO NOT include markdown code blocks.
The JSON must follow this exact structure:
{
  "title": "Story Title in Traditional Mandarin",
  "sentences": [
    {
      "mandarin": "Mandarin sentence here",
      "${pronLabel}": "${pronInstruction}",
      "english": "Natural English translation here"
    },
    ...
  ]
}

CRITICAL LENGTH REQUIREMENT:
${novellaInstruction}
The user has requested a story of ${lengthAdjective} ${length} Mandarin characters.
To achieve this, you MUST:
- Generate approximately ${sentenceTarget} sentences.
- Do not summarize. 
- This length requirement is an ${lengthPriority}.

Plot for the story: ${plot}`;
}

async function callGemini(prompt) {
    const model = state.selectedModel || 'gemini-2.5-flash-lite';
    const url = `${API_BASE}/models/${model}:generateContent?key=${state.apiKey}`;
    
    const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: {
                temperature: 0.7,
                topK: 40,
                topP: 0.95,
                maxOutputTokens: 8192,
                responseMimeType: "application/json"
            }
        })
    });
    
    if (!response.ok) {
        const err = await response.json();
        throw new Error(err.error?.message || 'API request failed');
    }
    
    const data = await response.json();
    return data.candidates[0].content.parts[0].text;
}

function parseResponse(text) {
    try {
        // 1. Try a standard clean parse first
        let start = text.indexOf('{');
        let end = text.lastIndexOf('}');
        
        if (start !== -1 && end !== -1 && end > start) {
            const cleanJSON = text.substring(start, end + 1);
            try {
                return JSON.parse(cleanJSON);
            } catch (e) {
                // If standard parse fails, fall through to recovery
            }
        }

        // 2. Recovery Mode: The response was likely truncated due to token limits or contains invalid characters
        console.warn("Response appears truncated or invalid. Attempting partial recovery...");
        console.log("Raw text for recovery:", text);
        
        // Find all complete sentence objects using a more robust regex that handles potential internal quotes/noise
        // This looks for: { "mandarin": "...", "[pinyin/zhuyin]": "...", "english": "..." }
        const pronField = state.pronunciation === 'zhuyin' ? 'zhuyin' : 'pinyin';
        
        // Improved regex: matches the keys and values while allowing for escaped characters or different whitespace
        const sentenceRegex = new RegExp(`\\{\\s*"mandarin"\\s*:\\s*"(.*?)"\\s*,\\s*"${pronField}"\\s*:\\s*"(.*?)"\\s*,\\s*"english"\\s*:\\s*"(.*?)"\\s*\\}`, 'gs');
        
        const sentences = [];
        let match;
        while ((match = sentenceRegex.exec(text)) !== null) {
            const sentence = {
                mandarin: match[1],
                english: match[3]
            };
            sentence[pronField] = match[2];
            sentences.push(sentence);
        }

        if (sentences.length > 0) {
            // Extract title if possible
            const titleMatch = /"title"\s*:\s*"([^"]*)"/.exec(text);
            const title = titleMatch ? titleMatch[1] : "Recovered Story (Incomplete)";
            
            showError("The story was so long it was cut off by the AI's limit, but we recovered " + sentences.length + " sentences for you.");
            
            return {
                title: title + " [Truncated]",
                sentences: sentences
            };
        }

        throw new Error('Could not find any valid story sentences in the response.');
    } catch (e) {
        console.error('JSON Parse/Recovery Error:', e, text);
        throw new Error('Failed to parse AI response. The story may be too long for the AI to finish. Try a shorter length.');
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
        playBtn.hidden = !elements.showPronunciationToggle.checked;
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
        pron.textContent = s[state.pronunciation]; // Use the selected pronunciation directly
        pron.hidden = !elements.showPronunciationToggle.checked;

        const english = document.createElement('div');
        english.className = 'english-translation';
        english.textContent = s.english;
        english.hidden = !elements.showTranslationToggle.checked;

        
        block.appendChild(playBtn);
        block.appendChild(mandarin);
        block.appendChild(pron);
        block.appendChild(english);
        elements.storyContent.appendChild(block);
    });
}

function updatePronunciationVisibility() {
    const show = elements.showPronunciationToggle.checked;
    const prons = elements.storyContent.querySelectorAll('.pronunciation');
    prons.forEach(p => p.hidden = !show);
    const playBtns = elements.storyContent.querySelectorAll('.sentence-play-btn');
    playBtns.forEach(btn => btn.hidden = !show);
}

function updateTranslationVisibility() {
    const show = elements.showTranslationToggle.checked;
    const translations = elements.storyContent.querySelectorAll('.english-translation');
    translations.forEach(t => t.hidden = !show);
}

let loadingInterval = null;

function showLoading(show) {
    elements.generateBtn.disabled = show;
    elements.progressSection.hidden = !show;
    
    if (show) {
        elements.resultSection.hidden = true;
        elements.errorSection.hidden = true;
        
        let quoteIndex = 0;
        elements.progressText.innerHTML = LOADING_QUOTES[quoteIndex].replace(/\n/g, '<br>');
        
        loadingInterval = setInterval(() => {
            quoteIndex = (quoteIndex + 1) % LOADING_QUOTES.length;
            elements.progressText.innerHTML = LOADING_QUOTES[quoteIndex].replace(/\n/g, '<br>');
        }, 3000);
    } else {
        if (loadingInterval) {
            clearInterval(loadingInterval);
            loadingInterval = null;
        }
    }
}

// updateProgress is no longer needed but kept as an empty function to avoid breaking other calls if any
function updateProgress(percent, text) {
    // No-op
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

    // Prioritize high-quality Chrome/Google voices over OS defaults
    const zhVoice = voices.find(v => v.name === 'Google 國語（臺灣）') ||
                   voices.find(v => v.name === 'Google 國語（臺灣）') || // Sometimes listed twice
                   voices.find(v => v.name.includes('Taiwan')) ||
                   voices.find(v => v.lang === 'zh-TW') ||
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
    
    let text = `${lastStoryData.title}\n\n`;
    lastStoryData.sentences.forEach(s => {
        text += `${s.mandarin}\n`;
        if (elements.showPronunciationToggle && elements.showPronunciationToggle.checked) {
            text += `${s[state.pronunciation]}\n`; // Use the dynamically accessed pronunciation
        }
        if (elements.showTranslationToggle && elements.showTranslationToggle.checked) {
            text += `${s.english}\n`;
        }
        text += `\n`;
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
