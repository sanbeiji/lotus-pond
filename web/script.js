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
    selectedModel: 'gemini-flash-lite-latest',
    generatePinyin: false,
    generateZhuyin: false,
    generateTranslation: false,
    showPinyin: true,
    showZhuyin: false,
    studyMode: true,
    showTranslation: true,
    history: [],
    themePreference: 'system',
    fontSizePreference: 'small',
    speechRatePreference: '0.9',
    useGeminiTts: false,
    geminiTtsVoiceStyle: 'standard'
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
    clearFormBtn: document.getElementById('clear-form-btn'),
    
    apiKeyInput: document.getElementById('api-key'),
    persistKeyToggle: document.getElementById('persist-key'),
    clearApiKeyBtn: document.getElementById('clear-api-key'),
    modelSelect: document.getElementById('model-select'),
    themeSelect: document.getElementById('theme-select'),
    ttsEngineRadios: document.querySelectorAll('input[name="tts-engine"]'),
    geminiTtsStyleGroup: document.getElementById('gemini-tts-style-group'),
    geminiTtsStyleRadios: document.querySelectorAll('input[name="gemini-tts-style"]'),
    showPinyinToggle: document.getElementById('show-pinyin'),
    showZhuyinToggle: document.getElementById('show-zhuyin'),
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
    toggleGenerateSettingsBtn: document.getElementById('toggle-generate-settings'),
    generateSettingsPanel: document.getElementById('generate-settings-panel'),
    generatePinyinToggle: document.getElementById('generate-pinyin'),
    generateZhuyinToggle: document.getElementById('generate-zhuyin'),
    generateTranslationToggle: document.getElementById('generate-translation'),
    showTranslationToggle: document.getElementById('show-translation'),
    fontSizeRadios: document.querySelectorAll('input[name="font-size"]'),
    speechRateRadios: document.querySelectorAll('input[name="speech-rate"]'),
    copyBtn: document.getElementById('copy-btn'),
    genrePromptSelect: document.getElementById('genre-prompt-select'),
    
    historyList: document.getElementById('history-list'),
    historyListMobile: document.getElementById('history-list-mobile'),
    clearHistoryBtn: document.getElementById('clear-history'),
    clearHistoryMobileBtn: document.getElementById('clear-history-mobile'),
    showHistoryBtn: document.getElementById('show-history-btn'),
    historyModal: document.getElementById('history-modal'),
    closeHistoryBtn: document.getElementById('close-history'),
    
    aboutBtn: document.getElementById('about-btn'),
    aboutCardBox: document.getElementById('about-card-box'),
    showAboutBtnMobile: document.getElementById('show-about-btn-mobile'),
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

// ─── Audio Caching (IndexedDB) ────────────────────────────────
const AudioCache = {
    DB_NAME: 'LotusPondAudioDB',
    DB_VERSION: 1,
    STORE_NAME: 'audio_cache',
    dbPromise: null,

    init() {
        this.dbPromise = new Promise((resolve, reject) => {
            const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                if (!db.objectStoreNames.contains(this.STORE_NAME)) {
                    db.createObjectStore(this.STORE_NAME, { keyPath: 'id' });
                }
            };
            request.onsuccess = (event) => resolve(event.target.result);
            request.onerror = (event) => reject(event.target.error);
        });
    },

    async get(text, voiceStyle) {
        if (!this.dbPromise) this.init();
        const id = `${voiceStyle}_${text}`;
        const db = await this.dbPromise;
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORE_NAME, 'readonly');
            const store = tx.objectStore(this.STORE_NAME);
            const request = store.get(id);
            request.onsuccess = () => resolve(request.result ? request.result.b64Pcm : null);
            request.onerror = () => reject(request.error);
        });
    },

    async put(text, voiceStyle, b64Pcm) {
        if (!this.dbPromise) this.init();
        const id = `${voiceStyle}_${text}`;
        const db = await this.dbPromise;
        const tx = db.transaction(this.STORE_NAME, 'readwrite');
        const store = tx.objectStore(this.STORE_NAME);
        store.put({ id, b64Pcm, timestamp: Date.now() });
    },

    async remove(text, voiceStyle) {
        if (!this.dbPromise) this.init();
        const id = `${voiceStyle}_${text}`;
        const db = await this.dbPromise;
        const tx = db.transaction(this.STORE_NAME, 'readwrite');
        const store = tx.objectStore(this.STORE_NAME);
        store.delete(id);
    },

    async clearAll() {
        if (!this.dbPromise) this.init();
        const db = await this.dbPromise;
        const tx = db.transaction(this.STORE_NAME, 'readwrite');
        const store = tx.objectStore(this.STORE_NAME);
        store.clear();
    }
};

AudioCache.init();

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
            if (parsed.showPinyin === undefined) {
                parsed.showPinyin = true;
                parsed.showZhuyin = false;
                delete parsed.pronunciation;
            }
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
    
    // Normalize cached model selection
    if (state.selectedModel !== 'gemini-flash-latest' && state.selectedModel !== 'gemini-flash-lite-latest') {
        state.selectedModel = 'gemini-flash-lite-latest';
        saveState();
    }

    // Apply state to UI
    if (elements.apiKeyInput) elements.apiKeyInput.value = state.apiKey;
    if (elements.persistKeyToggle) elements.persistKeyToggle.checked = state.persistKey;
    if (elements.modelSelect) {
        elements.modelSelect.value = state.selectedModel || 'gemini-flash-lite-latest';
    }
    if (elements.themeSelect) {
        elements.themeSelect.value = state.themePreference || 'system';
    }
    if (elements.ttsEngineRadios) {
        elements.ttsEngineRadios.forEach(radio => {
            if (radio.value === 'gemini') {
                radio.checked = state.useGeminiTts;
            } else if (radio.value === 'browser') {
                radio.checked = !state.useGeminiTts;
            }
        });
    }
    updateTtsUi();
    if (elements.geminiTtsStyleRadios) {
        elements.geminiTtsStyleRadios.forEach(radio => {
            if (radio.value === (state.geminiTtsVoiceStyle || 'standard')) {
                radio.checked = true;
            }
        });
    }
    applyTheme();
    if (elements.generatePinyinToggle) elements.generatePinyinToggle.checked = state.generatePinyin;
    if (elements.generateZhuyinToggle) elements.generateZhuyinToggle.checked = state.generateZhuyin;
    if (elements.generateTranslationToggle) elements.generateTranslationToggle.checked = state.generateTranslation;
    if (elements.showPinyinToggle) elements.showPinyinToggle.checked = state.showPinyin;
    if (elements.showZhuyinToggle) elements.showZhuyinToggle.checked = state.showZhuyin;
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
    
    if (elements.speechRateRadios) {
        elements.speechRateRadios.forEach(radio => {
            if (radio.value === (state.speechRatePreference || '0.9')) {
                radio.checked = true;
            }
        });
    }
    
    updateModelFooter();
    
    // If no API key, show settings
    if (!state.apiKey && elements.settingsContent) {
        elements.settingsContent.hidden = false;
    }
}

function updateTtsUi() {
    const subtextEl = document.getElementById('tts-engine-subtext');
    if (subtextEl) {
        subtextEl.textContent = state.useGeminiTts 
            ? "Uses Gemini AI (Experimental). High-quality voices and regional accents; requires internet and has minor initial latency/token costs."
            : "Uses native text-to-speech. Fast, free, and works offline.";
    }
    if (elements.geminiTtsStyleGroup) {
        elements.geminiTtsStyleGroup.hidden = !state.useGeminiTts;
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
    if (modelDisplay) modelDisplay.textContent = state.selectedModel || 'gemini-flash-lite-latest';
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
    elements.clearFormBtn?.addEventListener('click', () => {
        elements.storyForm.reset();
        if (elements.plotInput) elements.plotInput.style.height = 'auto';
        if (elements.requiredTermsInput) {
            elements.requiredTermsInput.classList.remove('invalid');
            const vocabWarning = document.getElementById('vocab-warning');
            if (vocabWarning) vocabWarning.hidden = true;
        }
    });
    elements.genrePromptSelect?.addEventListener('change', async (e) => {
        const selectedGenre = e.target.value;
        if (!selectedGenre) return;

        const currentPlot = elements.plotInput.value.trim();
        if (currentPlot.length > 0) {
            const confirmed = confirm("This will replace your current plot text. Do you want to proceed?");
            if (!confirmed) {
                // Reset dropdown back to default placeholder
                elements.genrePromptSelect.value = "";
                return;
            }
        }

        // UI Lock State
        elements.plotInput.disabled = true;
        elements.genrePromptSelect.disabled = true;
        elements.plotInput.value = "";
        elements.plotInput.placeholder = `Generating ${selectedGenre} prompt...`;
        hideError();

        try {
            const generatedPrompt = await generateGenrePrompt(selectedGenre);
            elements.plotInput.value = generatedPrompt;
            elements.plotInput.style.height = 'auto';
            elements.plotInput.style.height = elements.plotInput.scrollHeight + 'px';
        } catch (err) {
            showError(err.message || "An unexpected error occurred.");
            elements.plotInput.placeholder = "e.g. A college student looking for a job…";
        } finally {
            elements.plotInput.disabled = false;
            elements.genrePromptSelect.disabled = false;
            // Reset dropdown cleanly
            elements.genrePromptSelect.value = "";
        }
    });

    document.getElementById('close-error-btn')?.addEventListener('click', hideError);
    
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

    if (elements.toggleGenerateSettingsBtn && elements.generateSettingsPanel) {
        elements.toggleGenerateSettingsBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            const isShowing = elements.generateSettingsPanel.classList.toggle('show');
            elements.toggleGenerateSettingsBtn.dataset.tooltip = isShowing ? "Close generate preferences" : "Edit generate preferences";
        });

        document.addEventListener('click', (e) => {
            if (elements.generateSettingsPanel.classList.contains('show') &&
                !elements.generateSettingsPanel.contains(e.target) &&
                e.target !== elements.toggleGenerateSettingsBtn) {
                elements.generateSettingsPanel.classList.remove('show');
                elements.toggleGenerateSettingsBtn.dataset.tooltip = "Edit generate preferences";
            }
        });
        elements.generateSettingsPanel.addEventListener('click', (e) => {
            e.stopPropagation();
        });
    }

    elements.generatePinyinToggle?.addEventListener('change', (e) => {
        state.generatePinyin = e.target.checked;
        saveState();
    });

    elements.generateZhuyinToggle?.addEventListener('change', (e) => {
        state.generateZhuyin = e.target.checked;
        saveState();
    });

    elements.generateTranslationToggle?.addEventListener('change', (e) => {
        state.generateTranslation = e.target.checked;
        saveState();
    });
    
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

    if (elements.ttsEngineRadios) {
        elements.ttsEngineRadios.forEach(radio => {
            radio.addEventListener('change', (e) => {
                if (e.target.checked) {
                    state.useGeminiTts = (e.target.value === 'gemini');
                    saveState();
                    updateTtsUi();
                }
            });
        });
    }

    if (elements.geminiTtsStyleRadios) {
        elements.geminiTtsStyleRadios.forEach(radio => {
            radio.addEventListener('change', (e) => {
                if (e.target.checked) {
                    state.geminiTtsVoiceStyle = e.target.value;
                    saveState();
                }
            });
        });
    }

    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
        if (state.themePreference === 'system') {
            applyTheme();
        }
    });
    
    elements.showPinyinToggle?.addEventListener('change', async (e) => {
        state.showPinyin = e.target.checked;
        saveState();
        updatePinyinVisibility();
        if (state.showPinyin && lastStoryData) {
            await checkAndFetchMissing('pinyin');
        }
    });

    elements.showZhuyinToggle?.addEventListener('change', async (e) => {
        state.showZhuyin = e.target.checked;
        saveState();
        updateZhuyinVisibility();
        if (state.showZhuyin && lastStoryData) {
            await checkAndFetchMissing('zhuyin');
        }
    });
    
    elements.studyModeToggle?.addEventListener('change', (e) => {
        state.studyMode = e.target.checked;
        saveState();
        if (!elements.resultSection.hidden && lastStoryData) {
            renderStory(lastStoryData);
        }
    });

    elements.showTranslationToggle?.addEventListener('change', async (e) => {
        state.showTranslation = e.target.checked;
        saveState();
        updateTranslationVisibility();
        if (state.showTranslation && lastStoryData) {
            await checkAndFetchMissing('english');
        }
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
    
    if (elements.speechRateRadios) {
        elements.speechRateRadios.forEach(radio => {
            radio.addEventListener('change', (e) => {
                if (e.target.checked) {
                    state.speechRatePreference = e.target.value;
                    saveState();
                }
            });
        });
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

    elements.aboutCardBox?.addEventListener('click', () => {
        elements.aboutModal.hidden = false;
    });

    elements.aboutCardBox?.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            elements.aboutModal.hidden = false;
        }
    });

    elements.showAboutBtnMobile?.addEventListener('click', () => {
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
        storyData.requiredTerms = requiredTerms;
        
        lastStoryData = storyData;
        renderStory(storyData);
        addToHistory(storyData, skillLevel);
        
        elements.resultSection.hidden = false;
        elements.resultSection.scrollIntoView({ behavior: 'smooth' });

        state.showPinyin = state.generatePinyin;
        state.showZhuyin = state.generateZhuyin;
        state.showTranslation = state.generateTranslation;
        if (elements.showPinyinToggle) elements.showPinyinToggle.checked = state.showPinyin;
        if (elements.showZhuyinToggle) elements.showZhuyinToggle.checked = state.showZhuyin;
        if (elements.showTranslationToggle) elements.showTranslationToggle.checked = state.showTranslation;
        saveState();
        updatePinyinVisibility();
        updateZhuyinVisibility();
        updateTranslationVisibility();

        if (state.generatePinyin) checkAndFetchMissing('pinyin');
        if (state.generateZhuyin) checkAndFetchMissing('zhuyin');
        if (state.generateTranslation) checkAndFetchMissing('english');
    } catch (err) {
        console.error(err);
        showError(err.message || 'An unexpected error occurred during generation.');
    } finally {
        showLoading(false);
    }
}

async function generateGenrePrompt(genre) {
    const taiwanSettings = [
        "a night market in Kaohsiung",
        "a quiet tea house in Jiufen",
        "a traditional bakery in Taichung",
        "a street in Taipei on a rainy day",
        "an old temple in Tainan",
        "a sunny beach in Kenting",
        "a slow train ride along the east coast",
        "a busy boba tea shop",
        "a sky lantern festival in Pingxi",
        "a hot spring in Beitou",
        "a breakfast shop in Taipei",
        "a historic street in Lukang",
        "a seaside path in Tamsui",
        "a pottery workshop in Yingge",
        "a seafood market in Keelung",
        "a mango ice shop in Taipei",
        "a green onion farm in Yilan",
        "a path in Yangmingshan",
        "the Taiwan High-Speed Rail",
        "a cozy bookstore in Taipei",
        "a hotel in Sun Moon Lake",
        "a tea plantation in Maokong"
    ];
    let prompt;
    if (genre.toLowerCase() === "music") {
        const instruments = [
            "Violin", "Violin", "Violin", "Violin",
            "Viola", "Viola", "Viola", "Viola",
            "Cello", "Cello", "Cello", "Cello",
            "Double bass", "Double bass", "Double bass", "Double bass",
            "Piano", "Piano", "Piano", "Piano",
            "Guitar", "Clarinet", "Flute", "Oboe", "Bassoon",
            "Trombone", "Trumpet", "French horn", "Tuba", "Tympani", "Percussion"
        ];
        const selectedInstrument = instruments[Math.floor(Math.random() * instruments.length)];

        const musicalActivities = [
            "preparing for a big concert in a symphony hall",
            "practicing a difficult piece on the <instrument> until late at night",
            "teaching a young student how to play the <instrument>",
            "playing in a professional symphony orchestra rehearsal",
            "rehearsing chamber music with a string quartet",
            "auditioning for a prestigious orchestra",
            "discussing musical interpretation with other musicians",
            "Musicians hanging out after the concert"
        ];
        const activityTemplate = musicalActivities[Math.floor(Math.random() * musicalActivities.length)];
        const activity = activityTemplate.replace("<instrument>", selectedInstrument.toLowerCase());
        prompt = `Return a JSON object with a "premise" key containing a short story idea (1 to 2 sentences) in simple English about a professional classical musician who plays the ${selectedInstrument}. Set the story around or connect it to: ${activity}. Ensure the character is not always nervous; they can be happy, inspired, tired, or excited. Use very basic words so it is easy to read. Do not use complex language.`;
    } else {
        const randomSetting = taiwanSettings[Math.floor(Math.random() * taiwanSettings.length)];
        prompt = `Return a JSON object with a "premise" key containing a short story idea (1 to 2 sentences) in simple English for the "${genre}" genre. Set the story in or connect it to: ${randomSetting}. Use very basic words so it is easy to read. Do not use complex language.`;
    }
    
    try {
        // Force gemini-flash-lite-latest as mandated by requirements
        const result = await callGemini(prompt, 'gemini-flash-lite-latest');
        let cleaned = result.trim();
        
        cleaned = cleaned.replace(/^```(json)?\s*|\s*```$/gi, '').trim();
        
        // 1. Try to parse as JSON if it looks like valid JSON
        if (cleaned.startsWith('{') && cleaned.endsWith('}')) {
            try {
                const parsed = JSON.parse(cleaned);
                const keys = Object.keys(parsed);
                if (keys.length > 0) {
                    const value = parsed[keys[0]];
                    if (typeof value === 'string') {
                        cleaned = value;
                    }
                }
            } catch (e) {
                // Not valid JSON (e.g. raw text in braces), strip the braces
                cleaned = cleaned.substring(1, cleaned.length - 1).trim();
            }
        }
        
        // Remove "premise:", "prompt:", etc prefix
        cleaned = cleaned.replace(/^(?:"?premise"?|"?prompt"?|"?story"?)\s*:\s*/i, '').trim();
        
        // Remove outer quotes and return cleaned text
        return cleaned.replace(/^["'“”‘’]+|["'“”‘’]+$/g, '').trim();
    } catch (err) {
        console.error("Genre prompt generation failed:", err);
        throw new Error("Failed to generate prompt for " + genre + ". " + err.message);
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

    return `You are teaching Mandarin to an English speaker. Generate a story in Mandarin to be used for the purposes of learning to read, write, and speak Mandarin. 

${levelGuide}

CRITICAL LINGUISTIC REQUIREMENTS:
1. TRADITIONAL CHARACTERS: Use traditional Mandarin characters only.
2. TAIWANESE STYLE: Use grammar, slang, and idioms common to Taiwan (e.g., use 影片 instead of 視頻, 捷運 instead of 地鐵, 腳踏車 instead of 自行車).
3. CULTURAL & GEOGRAPHICAL BREADTH: Explore the full diversity of Taiwan. Do not over-rely on Taipei or common tropes. 
   - GEOGRAPHY: Vary the settings across different cities (e.g., Taichung, Tainan, Hualien, Keelung), counties (e.g., Yilan, Pingtung, Nantou), and landscapes (high mountain tea farms, coastal fishing villages, bustling night markets, quiet rural towns).
   - CULTURE: Incorporate a wide range of Taiwanese life, such as temple festivals, traditional arts (like glove puppetry), tea ceremonies, hiking culture, family dynamics, local snacks (小吃), and historical landmarks.
   - SOCIAL NORMS: Reflect authentic Taiwanese social etiquette and daily interactions.
4. SKILL LEVEL: Adhere strictly to the ${skillLevel} level requirements defined above.
5. VOCABULARY INTEGRATION: If specific vocabulary terms are provided ("${requiredTerms}"), you MUST include EVERY term at least TWICE in the story. Ensure they are used naturally but frequently enough for the reader to practice them. Integrate them into both narrative and dialogue where appropriate.
6. STRUCTURE: Break the story into logical sentences. Each sentence must be its own object in the response.

OUTPUT FORMAT:
You must return a valid JSON object with NO OTHER TEXT before or after the JSON. DO NOT include markdown code blocks.
The JSON must follow this exact structure:
{
  "title": "Story Title in Traditional Mandarin",
  "sentences": [
    {
      "mandarin": "Mandarin sentence here"
    }
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

async function callGemini(prompt, forceModel = null) {
    const model = forceModel || state.selectedModel || 'gemini-flash-lite-latest';
    const url = `${API_BASE}/models/${model}:generateContent?key=${state.apiKey}`;
    
    const timeoutMs = model === 'gemini-flash-latest' ? 180000 : 60000;
    const controller = new AbortController();
    const timerId = setTimeout(() => controller.abort(), timeoutMs);
    
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            signal: controller.signal,
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
        
        clearTimeout(timerId);
        if (!response.ok) {
            const err = await response.json();
            throw new Error(err.error?.message || 'API request failed');
        }
        
        const data = await response.json();
        if (!data.candidates || data.candidates.length === 0) {
            throw new Error('API returned no candidates. Please verify content safety and API limits.');
        }
        const candidate = data.candidates[0];
        if (!candidate.content || !candidate.content.parts || candidate.content.parts.length === 0) {
            throw new Error(`API returned empty content. Finish reason: ${candidate.finishReason || 'Unknown'}`);
        }
        return candidate.content.parts[0].text;
    } catch (err) {
        let msg = err.message || 'Network request failed';
        if (err.name === 'AbortError') {
            msg = `Request timed out after ${timeoutMs / 1000} seconds. Please verify connection speed and server load.`;
        }
        if (state.apiKey) {
            msg = msg.split(state.apiKey).join('[REDACTED]');
        }
        throw new Error(msg);
    }
}

function parseResponse(text) {
    try {
        let start = text.indexOf('{');
        let end = text.lastIndexOf('}');
        
        if (start !== -1 && end !== -1 && end > start) {
            const cleanJSON = text.substring(start, end + 1);
            try {
                return JSON.parse(cleanJSON);
            } catch (e) {
            }
        }

        console.warn("Response appears truncated or invalid. Attempting partial recovery...");
        const sentenceRegex = /\{\s*"mandarin"\s*:\s*"(.*?)"/gs;
        
        const sentences = [];
        let match;
        while ((match = sentenceRegex.exec(text)) !== null) {
            sentences.push({ mandarin: match[1] });
        }

        if (sentences.length > 0) {
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
    
    const requiredTermsStr = storyData.requiredTerms || '';
    const requiredTerms = requiredTermsStr.split(/[ ,，]+/).filter(t => t.length > 0);
    
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
        
        const pinyin = document.createElement('div');
        if (s.pinyin) {
            pinyin.className = 'pinyin-text';
            pinyin.textContent = s.pinyin;
        } else if (state.showPinyin) {
            pinyin.className = 'inline-loading loading-pinyin';
            pinyin.textContent = 'Loading...';
        } else {
            pinyin.className = 'pinyin-text';
        }
        pinyin.hidden = !state.showPinyin;

        const zhuyin = document.createElement('div');
        if (s.zhuyin) {
            zhuyin.className = 'zhuyin-text';
            zhuyin.textContent = s.zhuyin;
        } else if (state.showZhuyin) {
            zhuyin.className = 'inline-loading loading-zhuyin';
            zhuyin.textContent = 'Loading...';
        } else {
            zhuyin.className = 'zhuyin-text';
        }
        zhuyin.hidden = !state.showZhuyin;

        const english = document.createElement('div');
        if (s.english) {
            english.className = 'english-translation';
            english.textContent = s.english;
        } else if (state.showTranslation) {
            english.className = 'inline-loading loading-english';
            english.textContent = 'Loading...';
        } else {
            english.className = 'english-translation';
        }
        english.hidden = !state.showTranslation;

        block.appendChild(playBtn);
        block.appendChild(mandarin);
        block.appendChild(pinyin);
        block.appendChild(zhuyin);
        block.appendChild(english);
        elements.storyContent.appendChild(block);
    });
}

function updatePinyinVisibility() {
    const show = elements.showPinyinToggle ? elements.showPinyinToggle.checked : state.showPinyin;
    const items = elements.storyContent.querySelectorAll('.pinyin-text, .loading-pinyin');
    items.forEach(p => p.hidden = !show);
}

function updateZhuyinVisibility() {
    const show = elements.showZhuyinToggle ? elements.showZhuyinToggle.checked : state.showZhuyin;
    const items = elements.storyContent.querySelectorAll('.zhuyin-text, .loading-zhuyin');
    items.forEach(p => p.hidden = !show);
}

function updateTranslationVisibility() {
    const show = elements.showTranslationToggle ? elements.showTranslationToggle.checked : state.showTranslation;
    const items = elements.storyContent.querySelectorAll('.english-translation, .loading-english');
    items.forEach(t => t.hidden = !show);
}

let toastTimeout = null;

function showToast(msg, actionLabel = null, actionCallback = null, duration = 3000) {
    const toastEl = document.getElementById('toast');
    const toastTextEl = document.getElementById('toast-text');
    const toastActionBtn = document.getElementById('toast-action-btn');
    if (!toastEl || !toastTextEl) return;

    toastTextEl.textContent = msg;

    if (actionLabel && actionCallback && toastActionBtn) {
        toastActionBtn.textContent = actionLabel;
        toastActionBtn.hidden = false;
        toastActionBtn.onclick = (e) => {
            e.stopPropagation();
            actionCallback();
            // Dismiss toast instantly
            toastEl.classList.remove('show');
            if (toastTimeout) clearTimeout(toastTimeout);
            setTimeout(() => toastEl.hidden = true, 300);
        };
    } else if (toastActionBtn) {
        toastActionBtn.hidden = true;
        toastActionBtn.onclick = null;
    }

    toastEl.hidden = false;
    toastEl.classList.add('show');

    if (toastTimeout) clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
        toastEl.classList.remove('show');
        setTimeout(() => toastEl.hidden = true, 300);
    }, duration);
}

async function checkAndFetchMissing(type) {
    if (!lastStoryData || !lastStoryData.sentences || lastStoryData.sentences.length === 0) return;
    
    const isMissing = lastStoryData.sentences.some(s => !s[type]);
    if (!isMissing) return;

    const toggleEl = type === 'pinyin' ? elements.showPinyinToggle : 
                     type === 'zhuyin' ? elements.showZhuyinToggle : 
                     elements.showTranslationToggle;

    if (toggleEl) toggleEl.disabled = true;

    renderStory(lastStoryData);
    const typeLabel = type === 'pinyin' ? 'Pinyin' : type === 'zhuyin' ? 'Zhuyin' : 'English translation';
    showToast(`${typeLabel} loading, please stand by...`);

    try {
        const sentencesArr = lastStoryData.sentences.map(s => s.mandarin);
        let prompt = '';
        if (type === 'pinyin') {
            prompt = `Generate Pinyin pronunciation (Taiwanese style, e.g. '和' as 'hàn') for the following Traditional Mandarin sentences. You MUST adhere to these strict Pinyin guidelines: 1. Capitalization: Capitalize first letter of each sentence, proper nouns (Běijīng, Zhōngguó), and personal names (e.g. Wáng Xiǎoyún). 2. Word Grouping: Group multi-syllable words continuously (fánguǎn, not fán guǎn), separate distinct words with spaces (Wǒ qù fánguǎn), keep particles (de, le, ma) as separate words, and use apostrophes before a, e, or o for ambiguous boundaries (píng'ān). Return ONLY a valid JSON object with a "result" key containing an array of strings corresponding exactly 1-to-1 with the input sentences: ${JSON.stringify(sentencesArr)}`;
        } else if (type === 'zhuyin') {
            prompt = `Generate Zhuyin/Bopomofo pronunciation for the following Traditional Mandarin sentences. Return ONLY a valid JSON object with a "result" key containing an array of strings corresponding exactly 1-to-1 with the input sentences: ${JSON.stringify(sentencesArr)}`;
        } else {
            prompt = `Generate natural English translations for the following Traditional Mandarin sentences. Return ONLY a valid JSON object with a "result" key containing an array of strings corresponding exactly 1-to-1 with the input sentences: ${JSON.stringify(sentencesArr)}`;
        }

        const rawResult = await callGemini(prompt, 'gemini-flash-lite-latest');
        let cleaned = rawResult.replace(/```json/g, '').replace(/```/g, '').trim();
        let start = cleaned.indexOf('{');
        let end = cleaned.lastIndexOf('}');
        let parsed;
        try {
            if (start === -1 || end === -1) throw new Error("Missing JSON curly braces");
            parsed = JSON.parse(cleaned.substring(start, end + 1));
        } catch (e) {
            console.warn("JSON parse failed for dynamic content, attempting array extraction...", e);
            let arrStart = cleaned.indexOf('[');
            let arrEnd = cleaned.lastIndexOf(']');
            if (arrStart !== -1 && arrEnd !== -1 && arrEnd > arrStart) {
                let arrStr = cleaned.substring(arrStart, arrEnd + 1);
                arrStr = arrStr.replace(/,\s*\]/g, ']');
                parsed = { result: JSON.parse(arrStr) };
            } else {
                throw e;
            }
        }
        const resArray = parsed.result;
        if (!Array.isArray(resArray) || resArray.length !== lastStoryData.sentences.length) {
            throw new Error("Length mismatch in dynamic content fetch");
        }

        lastStoryData.sentences.forEach((s, idx) => {
            s[type] = resArray[idx];
        });

        const histObj = state.history.find(h => h.data && h.data.title === lastStoryData.title);
        if (histObj) histObj.data = lastStoryData;
        saveState();

        if (lastStoryData) renderStory(lastStoryData);
        showToast(`${typeLabel} loaded successfully!`);
    } catch (err) {
        console.error("Dynamic fetch failed:", err);
        showError(`Failed to retrieve ${type}. ${err.message}`);
        
        if (type === 'pinyin') state.showPinyin = false;
        if (type === 'zhuyin') state.showZhuyin = false;
        if (type === 'english') state.showTranslation = false;
        if (toggleEl) toggleEl.checked = false;
        saveState();
        if (lastStoryData) renderStory(lastStoryData);
    } finally {
        if (toggleEl) toggleEl.disabled = false;
    }
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
    if (state.useGeminiTts) {
        speakWithGemini(text);
        return;
    }

    // Cancel any ongoing speech
    window.speechSynthesis.cancel();
    if (window.currentGeminiAudio) {
        window.currentGeminiAudio.pause();
    }
    
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
    
    utterance.rate = parseFloat(state.speechRatePreference || '0.9');
    window.speechSynthesis.speak(utterance);
}

/**
 * Wraps raw 16-bit Signed Little-Endian Mono PCM data in a WAV container header.
 * @param {string} b64Pcm Base64 encoded string of raw PCM bytes
 * @param {number} sampleRate Default sampling rate for Gemini (24000 Hz)
 * @returns {string} Fully playable Blob URL
 */
function pcmToWavBlobUrl(b64Pcm, sampleRate = 24000) {
  try {
    const rawBinary = atob(b64Pcm);
    const dataLen = rawBinary.length;
    
    const buffer = new ArrayBuffer(44 + dataLen);
    const view = new DataView(buffer);
    
    view.setUint32(0, 0x52494646, false); 
    view.setUint32(4, 36 + dataLen, true);
    view.setUint32(8, 0x57415645, false);
    view.setUint32(12, 0x666d7420, false);
    view.setUint32(16, 16, true);
    view.setUint16(20, 1, true);
    view.setUint16(22, 1, true);
    view.setUint32(24, sampleRate, true);
    view.setUint32(28, sampleRate * 1 * 2, true);
    view.setUint16(32, 2, true);
    view.setUint16(34, 16, true);
    view.setUint32(36, 0x64617461, false);
    view.setUint32(40, dataLen, true);
    
    const u8Buffer = new Uint8Array(buffer, 44);
    for (let i = 0; i < dataLen; i++) {
      u8Buffer[i] = rawBinary.charCodeAt(i);
    }
    
    const wavBlob = new Blob([buffer], { type: "audio/wav" });
    return URL.createObjectURL(wavBlob);
  } catch (error) {
    console.error("PCM-to-WAV conversion error:", error);
    return null;
  }
}

async function speakWithGemini(text) {
    if (!state.apiKey) {
        showError('Please enter your Gemini API Key in the settings for TTS.');
        return;
    }

    const voicePrompt = {
        'standard': '[Voice Style: Speak in a natural, standard, clear reading style.]\nRead in standard, clear Taiwanese Mandarin (都會風格台北/台灣腔) as heard in public announcements (like the Taipei MRT) or urban professional settings.\n- Speak in a natural, clean, moderately fast, modern Taiwanese tempo.\n- Retroflex sounds (zh, ch, sh) are relaxed and naturally simplified, avoiding any dry retroflex friction or thick northern Beijing acoustics. No "er" (no 兒化音).\n- Render neutral tones (輕聲) in accordance with general urban Taiwanese Mandarin usage (typically pronounced as lighter full tones rather than clipped neutral vowels).\n- Deliver with a clean, melodic, polite, and professional Taiwanese tone.',
        'southern': '[Voice Style: Speak gently, softly, and reassuringly, at a relaxed pace with extreme warmth.]\nRead in a warm, relaxed, authentic Southern Taiwanese Mandarin (台灣國語) regional accent (popular in Tainan, Kaohsiung, and Pingtung).\n- Speak with a friendly, local Taiwanese cadence and relaxed mouth positioning.\n- Strictly avoid Beijing-style speech: absolutely no curl-tongue "er" (no 兒化音) and do not retroflex sounds like zh, ch, sh (pronounce them shifted toward z, c, s, e.g. 知道 sounds like zīdào, 是 sounds like sì).\n- Do not suppress tones into neutral short tones (輕聲), pronounce grammatically light words with their full traditional Taiwanese Mandarin tones (e.g. 舒服 is shūfú, 先生 is xiānshēng).\n- Keep any natural sentence-final particles from Taiwan (like \'啦\', \'齁\', \'喔\', \'欸\') represented with authentic, comfortable, musical southern cadence.',
        'heavy_southern': '[Voice Style: Speak in a natural, calm, and effortless everyday conversational style, like a friendly adult family member or colleague explaining something. Maintain a normal, steady, and comfortable speaking pace, strictly avoiding any dramatic, slow, or exaggerated theatrical delivery.]\nRead in an authentic Southern Taiwanese Mandarin colloquial style, naturally blending in Taiwanese (Minnan/Hokkien) vocabulary and substrate.\n- Deliver the speech with a warm, down-to-earth, and polite regional tone.\n- When the text contains mixed Taiwanese (Minnan) vocabulary, phrases, or sentence-final particles, pronounce them naturally and integrate them smoothly into the flow of speech.\n- Naturally relax retroflex sounds (zh, ch, sh tend toward z, c, s, but in a subtle, unforced way).\n- Avoid Beijing-style neutral/light tones; pronounce characters with comfortable, full traditional tones.\n- Keep the cadence relaxed, warm, and conversational.'
    };
    
    const voiceStyle = state.geminiTtsVoiceStyle || 'standard';
    const styleInstruction = voicePrompt[voiceStyle];
    const fullText = `${styleInstruction}\nPlease recite the following text exactly as requested: "${text}"`;

    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-tts-preview:generateContent?key=${state.apiKey}`;
    
    // Stop native speech synthesis if any
    window.speechSynthesis.cancel();
    if (window.currentGeminiAudio) {
        window.currentGeminiAudio.pause();
    }
    
    try {
        let b64Pcm = await AudioCache.get(text, voiceStyle);
        
        if (!b64Pcm) {
            const response = await fetch(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    contents: [{ parts: [{ text: fullText }] }],
                    generationConfig: {
                        responseModalities: ["AUDIO"],
                        speechConfig: {
                            voiceConfig: {
                                prebuiltVoiceConfig: {
                                    voiceName: "Kore"
                                }
                            }
                        }
                    }
                })
            });
            
            if (!response.ok) {
                const err = await response.json();
                throw new Error(err.error?.message || 'API request failed');
            }
            
            const data = await response.json();
            const candidate = data.candidates && data.candidates[0];
            const part = candidate?.content?.parts?.find(p => p.inlineData && p.inlineData.mimeType.startsWith('audio/'));
            
            if (!part) {
                throw new Error('No audio returned from Gemini API.');
            }
            
            b64Pcm = part.inlineData.data;
            await AudioCache.put(text, voiceStyle, b64Pcm);
        }
        
        const playUrl = pcmToWavBlobUrl(b64Pcm, 24000);
        if (!playUrl) throw new Error("Audio conversion failed");
        
        const audioPlayer = new Audio();
        audioPlayer.src = playUrl;
        audioPlayer.playbackRate = parseFloat(state.speechRatePreference || '0.9');
        audioPlayer.play();
        window.currentGeminiAudio = audioPlayer;
        
    } catch (err) {
        console.error("Gemini TTS Error:", err);
        showError("Gemini TTS failed: " + err.message);
    }
}

// ─── History Management ───────────────────────────────────────

let pendingDeleteStoryId = null;
let pendingDeleteTimeoutId = null;
let pendingDeleteStoryObj = null;

function deleteHistoryItem(id) {
    // 1. Commit any previous pending deletions immediately
    if (pendingDeleteStoryId !== null) {
        commitPendingDelete();
    }

    // 2. Stash optimistic delete details
    pendingDeleteStoryId = id;
    pendingDeleteStoryObj = state.history.find(h => h.id === id);

    // 3. Re-render UI (optimistically hides the item from screen)
    renderHistory();

    // 4. Launch functional Toast Action
    showToast("Story deleted from history", "Undo", undoDelete, 4000);

    // 5. Start auto-commit timer
    pendingDeleteTimeoutId = setTimeout(() => {
        commitPendingDelete();
    }, 4000);
}

function undoDelete() {
    if (pendingDeleteTimeoutId) {
        clearTimeout(pendingDeleteTimeoutId);
    }
    pendingDeleteStoryId = null;
    pendingDeleteStoryObj = null;
    pendingDeleteTimeoutId = null;
    renderHistory();
    showToast("Story restored!");
}

function commitPendingDelete() {
    if (pendingDeleteStoryId === null) return;
    
    if (pendingDeleteTimeoutId) {
        clearTimeout(pendingDeleteTimeoutId);
        pendingDeleteTimeoutId = null;
    }

    const storyToDelete = state.history.find(h => h.id === pendingDeleteStoryId);
    if (storyToDelete && storyToDelete.data && storyToDelete.data.sentences) {
        storyToDelete.data.sentences.forEach(s => {
            AudioCache.remove(s.mandarin, 'standard');
            AudioCache.remove(s.mandarin, 'southern');
            AudioCache.remove(s.mandarin, 'heavy_southern');
        });
    }

    state.history = state.history.filter(h => h.id !== pendingDeleteStoryId);
    pendingDeleteStoryId = null;
    pendingDeleteStoryObj = null;
    saveState();
    renderHistory();
}

function addToHistory(storyData, skillLevel) {
    // Ensure pending deletions are committed before appending new story
    if (pendingDeleteStoryId !== null) {
        commitPendingDelete();
    }

    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const hh = String(now.getHours()).padStart(2, '0');
    const min = String(now.getMinutes()).padStart(2, '0');
    const formattedDate = `${yyyy}-${mm}-${dd}  ${hh}:${min}`;

    const historyItem = {
        id: Date.now(),
        title: storyData.title,
        data: storyData,
        date: formattedDate,
        level: skillLevel
    };
    
    state.history.unshift(historyItem);
    if (state.history.length > 20) {
        const popped = state.history.pop();
        if (popped && popped.data && popped.data.sentences) {
            popped.data.sentences.forEach(s => {
                AudioCache.remove(s.mandarin, 'standard');
                AudioCache.remove(s.mandarin, 'southern');
                AudioCache.remove(s.mandarin, 'heavy_southern');
            });
        }
    }
    
    saveState();
    renderHistory();
}

function renderHistory() {
    const containers = [elements.historyList, elements.historyListMobile];
    
    containers.forEach(container => {
        if (!container) return;
        container.innerHTML = '';
        
        // Filter out any optimistically deleted item
        const activeHistory = state.history.filter(item => item.id !== pendingDeleteStoryId);
        
        if (activeHistory.length === 0) {
            container.innerHTML = '<p class="empty-msg">No stories yet.</p>';
            return;
        }
        
        activeHistory.forEach(item => {
            const el = document.createElement('div');
            el.className = 'history-item';
            let levelHtml = item.level ? `<div class="history-level">${item.level}</div>` : '';
            el.innerHTML = `
                <div class="history-title">${item.title}</div>
                <div class="history-meta">
                    <div class="history-date">${item.date}</div>
                    ${levelHtml}
                </div>
            `;
            
            // Create permanently visible Delete button
            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'btn-delete-history';
            deleteBtn.innerHTML = '✕';
            deleteBtn.title = 'Delete story';
            deleteBtn.onclick = (e) => {
                e.stopPropagation();
                deleteHistoryItem(item.id);
            };
            el.appendChild(deleteBtn);

            el.onclick = () => {
                lastStoryData = item.data;

                if (lastStoryData && lastStoryData.sentences && lastStoryData.sentences.length > 0) {
                    const hasPinyin = lastStoryData.sentences.some(s => !!s.pinyin);
                    if (!hasPinyin) {
                        state.showPinyin = false;
                        if (elements.showPinyinToggle) elements.showPinyinToggle.checked = false;
                    }
                    const hasZhuyin = lastStoryData.sentences.some(s => !!s.zhuyin);
                    if (!hasZhuyin) {
                        state.showZhuyin = false;
                        if (elements.showZhuyinToggle) elements.showZhuyinToggle.checked = false;
                    }
                    const hasEnglish = lastStoryData.sentences.some(s => !!s.english);
                    if (!hasEnglish) {
                        state.showTranslation = false;
                        if (elements.showTranslationToggle) elements.showTranslationToggle.checked = false;
                    }
                    saveState();
                }

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
        // Commit / cancel any pending delete to keep state perfectly clean
        if (pendingDeleteTimeoutId) {
            clearTimeout(pendingDeleteTimeoutId);
            pendingDeleteTimeoutId = null;
        }
        pendingDeleteStoryId = null;
        pendingDeleteStoryObj = null;

        state.history = [];
        AudioCache.clearAll();
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
        if (state.showPinyin && s.pinyin) {
            text += `${s.pinyin}\n`;
        }
        if (state.showZhuyin && s.zhuyin) {
            text += `${s.zhuyin}\n`;
        }
        if (state.showTranslation && s.english) {
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
