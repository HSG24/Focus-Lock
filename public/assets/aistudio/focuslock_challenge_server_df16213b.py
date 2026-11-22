"""
FocusLock Minimalist Challenge Server
=====================================
Monitors Firebase Realtime Database every 2 seconds for an active PIN
with a completely clean terminal output and a minimalist web UI.
"""

import os
import sys
import time
import logging
import threading
import webbrowser
import requests
from flask import Flask, jsonify, render_template_string
import flask.cli

# =====================================================================
# SILENCE FLASK TERMINAL LOGS (Keeps Command Prompt 100% clean)
# =====================================================================
flask.cli.show_server_banner = lambda *args: None
logging.getLogger('werkzeug').setLevel(logging.ERROR)

# =====================================================================
# CONFIGURATION & GLOBAL STATE
# =====================================================================
FIREBASE_PIN_URL = "https://focuslock-b6c0f-default-rtdb.asia-southeast1.firebasedatabase.app/current_lock/pin.json"
POLL_INTERVAL_SECONDS = 2
HOST = "127.0.0.1"
PORT = 5000

state_lock = threading.Lock()
current_pin = None
last_opened_pin = None

app = Flask(__name__)

# =====================================================================
# EMBEDDED MINIMALIST HTML/JS CHALLENGE INTERFACE
# =====================================================================
HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en" class="dark">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FocusLock</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500;600;700&display=swap" rel="stylesheet">
    <style>
        body {
            font-family: 'Inter', sans-serif;
            background-color: #09090b;
            color: #fafafa;
        }
        .mono {
            font-family: 'JetBrains Mono', monospace;
        }
        @keyframes shake {
            0%, 100% { transform: translateX(0); }
            25% { transform: translateX(-6px); }
            75% { transform: translateX(6px); }
        }
        .shake {
            animation: shake 0.25s ease-in-out;
        }
    </style>
</head>
<body class="min-h-screen flex items-center justify-center p-6 selection:bg-zinc-800 selection:text-white">

    <main class="w-full max-w-md">

        <!-- STATE 1: STANDBY -->
        <div id="waiting-view" class="text-center py-16 px-8 rounded-3xl bg-zinc-900/40 border border-zinc-800/80 backdrop-blur-sm">
            <div class="w-3 h-3 rounded-full bg-zinc-600 mx-auto mb-6 animate-pulse"></div>
            <h1 class="text-lg font-medium text-zinc-200 tracking-tight mb-1">Standby</h1>
            <p class="text-sm text-zinc-500">Waiting for active session</p>
        </div>

        <!-- STATE 2: MINIMALIST CHALLENGE -->
        <div id="challenge-view" class="hidden">
            <div id="quiz-card" class="bg-zinc-900/50 border border-zinc-800/90 rounded-3xl p-8 sm:p-10 shadow-2xl backdrop-blur-sm transition-all">
                
                <!-- Top Progress -->
                <div class="flex items-center justify-between text-xs text-zinc-500 mb-6">
                    <span id="question-category" class="uppercase tracking-widest font-medium text-zinc-400">Challenge</span>
                    <span id="progress-text" class="mono">1 / 6</span>
                </div>

                <!-- Minimal Progress Bar -->
                <div class="w-full h-1 bg-zinc-800 rounded-full overflow-hidden mb-10">
                    <div id="progress-bar" class="h-full bg-zinc-100 transition-all duration-300" style="width: 15%"></div>
                </div>

                <!-- Question Display -->
                <div class="text-center mb-10">
                    <p id="question-hint" class="text-xs text-zinc-500 mb-3">Solve</p>
                    <div id="question-prompt" class="text-2xl sm:text-3xl font-semibold tracking-tight text-white mono py-2 select-none">
                        ...
                    </div>
                </div>

                <!-- Clean Input -->
                <form id="answer-form" class="space-y-4">
                    <input 
                        type="text" 
                        id="answer-input" 
                        autocomplete="off" 
                        autocorrect="off" 
                        autocapitalize="off" 
                        spellcheck="false"
                        placeholder="Answer" 
                        class="w-full px-4 py-3.5 bg-zinc-950/80 border border-zinc-800 focus:border-zinc-400 focus:outline-none rounded-2xl text-lg text-center font-medium text-white mono placeholder-zinc-700 transition-colors"
                    >
                    <p id="feedback-msg" class="text-center text-xs text-rose-400 min-h-[1rem]"></p>

                    <button 
                        type="submit" 
                        class="w-full py-3.5 rounded-2xl bg-zinc-100 hover:bg-white active:bg-zinc-200 text-zinc-950 font-semibold text-sm transition-all cursor-pointer"
                    >
                        Continue
                    </button>
                </form>
            </div>
        </div>

        <!-- STATE 3: PIN REVEALED -->
        <div id="reveal-view" class="hidden text-center bg-zinc-900/40 border border-zinc-800/80 rounded-3xl p-10 shadow-2xl backdrop-blur-sm">
            <p class="text-xs uppercase tracking-widest text-zinc-500 mb-6">Access Code</p>

            <div class="py-8 px-4 bg-zinc-950/80 border border-zinc-800/80 rounded-2xl">
                <div id="pin-display" class="text-5xl sm:text-6xl font-bold tracking-[0.2em] text-white mono select-all">
                    ------
                </div>
            </div>
        </div>

    </main>

    <script>
        let questions = [];
        let currentQuestionIndex = 0;
        let isUnlocked = false;
        let activePinFromServer = null;

        // =================================================================
        // PERSISTENT QUESTION MEMORY (Never repeat a question ever again)
        // =================================================================
        const STORAGE_KEY = 'focuslock_asked_questions_v1';
        let askedSignatures = new Set();
        try {
            const saved = localStorage.getItem(STORAGE_KEY);
            if (saved) {
                askedSignatures = new Set(JSON.parse(saved));
            }
        } catch (e) {
            askedSignatures = new Set();
        }

        function markQuestionAsked(sig) {
            askedSignatures.add(sig);
            try {
                localStorage.setItem(STORAGE_KEY, JSON.stringify([...askedSignatures]));
            } catch (e) {}
        }

        const waitingView = document.getElementById('waiting-view');
        const challengeView = document.getElementById('challenge-view');
        const revealView = document.getElementById('reveal-view');

        const quizCard = document.getElementById('quiz-card');
        const questionCategory = document.getElementById('question-category');
        const progressText = document.getElementById('progress-text');
        const progressBar = document.getElementById('progress-bar');
        const questionHint = document.getElementById('question-hint');
        const questionPrompt = document.getElementById('question-prompt');
        const answerForm = document.getElementById('answer-form');
        const answerInput = document.getElementById('answer-input');
        const feedbackMsg = document.getElementById('feedback-msg');

        const pinDisplay = document.getElementById('pin-display');

        function randInt(min, max) {
            return Math.floor(Math.random() * (max - min + 1)) + min;
        }

        function shuffleArray(arr) {
            const copy = [...arr];
            for (let i = copy.length - 1; i > 0; i--) {
                const j = Math.floor(Math.random() * (i + 1));
                [copy[i], copy[j]] = [copy[j], copy[i]];
            }
            return copy;
        }

        // =================================================================
        // 1. PROCEDURAL ODD ONE OUT (Thousands of unique combinations)
        // =================================================================
        const categoryPools = {
            animals: ["DOG", "CAT", "LION", "BEAR", "WOLF", "DEER", "FROG", "DUCK", "GOAT", "CRAB", "HAWK", "SWAN", "SEAL", "PUMA", "CROW", "MULE", "TOAD", "LYNX", "ORCA", "HARE"],
            fruits: ["APPLE", "MANGO", "GRAPE", "LEMON", "PEACH", "MELON", "BERRY", "PLUM", "GUAVA", "KIWI", "PEAR", "LIME", "FIG", "DATE", "PAPAYA", "LYCHEE", "CHERRY", "OLIVE"],
            colors: ["RED", "BLUE", "GREEN", "BLACK", "WHITE", "PINK", "GOLD", "CYAN", "GREY", "BROWN", "NAVY", "TEAL", "CORAL", "IVORY", "AMBER", "BEIGE"],
            vehicles: ["CAR", "BUS", "TRAIN", "TRUCK", "PLANE", "BOAT", "SHIP", "BIKE", "TAXI", "JEEP", "VAN", "TRAM", "YACHT", "METRO", "FERRY", "SLEIGH"],
            furniture: ["CHAIR", "TABLE", "DESK", "SOFA", "COUCH", "BED", "SHELF", "STOOL", "BENCH", "FUTON", "CRADLE", "CLOSET"],
            planets: ["MARS", "VENUS", "EARTH", "PLUTO", "SATURN", "URANUS", "NEPTUNE", "MERCURY", "JUPITER"],
            shapes: ["CIRCLE", "SQUARE", "CUBE", "CONE", "OVAL", "LINE", "STAR", "RING", "SPHERE", "PRISM", "SPIRAL", "HEXAGON"],
            weather: ["RAIN", "SNOW", "WIND", "STORM", "CLOUD", "FROST", "HAIL", "MIST", "FOG", "HUMID", "BREEZE", "THUNDER"]
        };

        function generateOddOneOutQuestion() {
            const keys = Object.keys(categoryPools);
            const mainKey = keys[randInt(0, keys.length - 1)];
            let oddKey = keys[randInt(0, keys.length - 1)];
            while (oddKey === mainKey) {
                oddKey = keys[randInt(0, keys.length - 1)];
            }

            const mainWords = shuffleArray(categoryPools[mainKey]).slice(0, 3);
            const oddWord = categoryPools[oddKey][randInt(0, categoryPools[oddKey].length - 1)];
            const finalWords = shuffleArray([...mainWords, oddWord]);
            const sortedComboSig = [...mainWords].sort().join(",") + "|" + oddWord;

            return {
                category: "Odd One Out",
                hint: "Type the word that doesn't belong",
                prompt: finalWords.join(" • "),
                answers: [oddWord],
                signature: `ODD:${sortedComboSig}`
            };
        }

        // =================================================================
        // 2. INSTANT MATH / NUMBER REFLEX (Infinite procedural generator)
        // =================================================================
        function generateMathReflexQuestion() {
            const mode = randInt(1, 4);

            if (mode === 1) {
                const findEven = Math.random() < 0.5;
                const targetPool = [];
                const distractorPool = [];
                while (distractorPool.length < 3) {
                    const n = randInt(11, 199);
                    if (findEven && n % 2 !== 0 && !distractorPool.includes(n)) distractorPool.push(n);
                    if (!findEven && n % 2 === 0 && !distractorPool.includes(n)) distractorPool.push(n);
                }
                while (targetPool.length < 1) {
                    const n = randInt(11, 199);
                    if (findEven && n % 2 === 0) targetPool.push(n);
                    if (!findEven && n % 2 !== 0) targetPool.push(n);
                }
                const nums = shuffleArray([...distractorPool, targetPool[0]]);
                return {
                    category: "Number Reflex",
                    hint: findEven ? "Type the ONLY EVEN number" : "Type the ONLY ODD number",
                    prompt: nums.join(" • "),
                    answers: [String(targetPool[0])],
                    signature: `REFLEX_EO:${findEven ? 'E' : 'O'}:${[...nums].sort().join(',')}`
                };
            }

            if (mode === 2) {
                const findMax = Math.random() < 0.5;
                const nums = [];
                while (nums.length < 4) {
                    const n = randInt(10, 250);
                    if (!nums.includes(n)) nums.push(n);
                }
                const ans = findMax ? Math.max(...nums) : Math.min(...nums);
                return {
                    category: "Number Reflex",
                    hint: findMax ? "Type the HIGHEST number" : "Type the LOWEST number",
                    prompt: nums.join(" • "),
                    answers: [String(ans)],
                    signature: `REFLEX_MM:${findMax ? 'MAX' : 'MIN'}:${[...nums].sort().join(',')}`
                };
            }

            if (mode === 3) {
                const x = randInt(2, 45);
                const b = randInt(2, 45);
                const sum = x + b;
                return {
                    category: "Quick X",
                    hint: "Find x",
                    prompt: `x + ${b} = ${sum}`,
                    answers: [String(x)],
                    signature: `QUICK_X:${x}+${b}=${sum}`
                };
            }

            const steps = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25];
            const step = steps[randInt(0, steps.length - 1)];
            const start = randInt(1, 30) * step;
            const seq = [start, start + step, start + step * 2, start + step * 3];
            const hideIdx = randInt(1, 3);
            const ans = seq[hideIdx];
            seq[hideIdx] = "?";

            return {
                category: "Number Flow",
                hint: "Type the missing number",
                prompt: seq.join("  →  "),
                answers: [String(ans)],
                signature: `FLOW:${start}:${step}:${hideIdx}`
            };
        }

        // =================================================================
        // 3. VISUAL EMOJI ALGEBRA & SOLO HUNTER (100% Clear & Infinite)
        // =================================================================
        function generateVisualMathGame() {
            const emojis = ["⚡", "🔥", "💎", "🚀", "⭐", "🍎", "🎲", "🎯", "🪐", "🍀", "🌙", "👑", "🔮", "🎵", "🧊"];
            const e1 = emojis[randInt(0, emojis.length - 1)];
            let e2 = emojis[randInt(0, emojis.length - 1)];
            while (e2 === e1) {
                e2 = emojis[randInt(0, emojis.length - 1)];
            }

            const mode = randInt(1, 3);

            if (mode === 1) {
                const val = randInt(2, 25);
                const count = randInt(2, 3);
                const extra = randInt(1, 30);
                const emojisStr = Array(count).fill(e1).join(" + ");
                const total = (val * count) + extra;

                return {
                    category: "Emoji Algebra",
                    hint: `If ${e1} = ${val}, find the total`,
                    prompt: `${emojisStr} + ${extra} = ?`,
                    answers: [String(total)],
                    signature: `EMOJI_ALG:${e1}:${val}:${count}:${extra}`
                };
            }

            if (mode === 2) {
                const addMode = Math.random() < 0.7;
                const v1 = addMode ? randInt(2, 45) : randInt(2, 12);
                const v2 = addMode ? randInt(2, 45) : randInt(2, 12);
                const total = addMode ? (v1 + v2) : (v1 * v2);
                const symbol = addMode ? "+" : "×";

                return {
                    category: "Symbol Swap",
                    hint: `${e1} = ${v1}   and   ${e2} = ${v2}`,
                    prompt: `${e1}  ${symbol}  ${e2}  =  ?`,
                    answers: [String(total)],
                    signature: `SYM_SWAP:${e1}:${v1}:${symbol}:${e2}:${v2}`
                };
            }

            const letters = ["A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"];
            const chosenLetters = shuffleArray(letters).slice(0, 3);
            const soloLetter = chosenLetters[0];
            const dup1 = chosenLetters[1];
            const dup2 = chosenLetters[2];
            const finalSeq = shuffleArray([soloLetter, dup1, dup1, dup2, dup2]);

            return {
                category: "Solo Hunter",
                hint: "Type the letter that appears ONLY ONCE",
                prompt: finalSeq.join("  •  "),
                answers: [soloLetter],
                signature: `SOLO:${finalSeq.join("")}`
            };
        }

        // =================================================================
        // 4. EASY UNSCRAMBLE (Large Pool + Fallback to Infinite Procedural)
        // =================================================================
        const easyFourLetterWords = [
            "MATH", "PLUS", "ZERO", "CUBE", "LINE", "AREA", "AXIS", "DATA", "CODE", "GRID",
            "TIME", "WORK", "GOAL", "CALM", "MIND", "BOOK", "DESK", "PLAN", "TASK", "NOTE",
            "STAR", "MOON", "WIND", "RAIN", "FIRE", "TREE", "SNOW", "LAKE", "ROCK", "WAVE",
            "GOLD", "BLUE", "GRAY", "PINK", "FAST", "SLOW", "HIGH", "DEEP", "COOL", "WARM",
            "GAME", "PLAY", "JUMP", "WALK", "IDEA", "FACT", "TRUE", "EASY", "OPEN", "SAFE",
            "ATOM", "BEAM", "BOLT", "BOND", "BYTE", "CELL", "CHIP", "COIL", "CORE", "DISK",
            "ECHO", "EDGE", "FLUX", "FONT", "FORM", "GAIN", "GATE", "GEAR", "GLOW", "GRAM",
            "HASH", "HEAT", "ICON", "ITEM", "JOIN", "KILO", "LENS", "LINK", "LIST", "LOAD",
            "LOCK", "LOOP", "MASS", "MEAN", "MESH", "MODE", "NANO", "NODE", "NORM", "NULL",
            "PATH", "PEAK", "PING", "PLOT", "POLE", "PORT", "QUAD", "RATE", "RING", "ROOT",
            "RULE", "SCAN", "SEED", "SIGN", "SINE", "SIZE", "SLOT", "SORT", "SPAN", "SPIN",
            "STEP", "SYNC", "TERM", "TEST", "TEXT", "TIDE", "TONE", "TOOL", "TYPE", "UNIT",
            "USER", "VAST", "VIEW", "VOID", "VOLT", "WATT", "WIRE", "WORD", "ZONE", "ARCH",
            "BARK", "BIRD", "BOAT", "CAFE", "CAMP", "CARD", "CART", "CAVE", "CITY", "CLAY",
            "CLUB", "COAT", "COIN", "CORN", "CROP", "CROW", "DAWN", "DEER", "DOCK", "DOOR",
            "DOVE", "DRUM", "DUCK", "DUST", "EARN", "EAST", "EXIT", "FACE", "FARM", "FERN",
            "FILM", "FISH", "FLAG", "FLAT", "FLOW", "FOLK", "FOOD", "FOOT", "FORK", "FORT",
            "FROG", "FUEL", "GIFT", "GIRL", "GOAT", "GOLF", "HAND", "HARP", "HAWK", "HEAD",
            "HERB", "HILL", "HOME", "HOPE", "HORN", "HOST", "HOUR", "HUNT", "IRON", "JADE",
            "JAZZ", "JEEP", "JOKE", "KEEP", "KING", "KITE", "KNOT", "LAMP", "LAND", "LANE",
            "LEAF", "LIFE", "LIFT", "LIME", "LION", "LOAF", "LOVE", "LUCK", "MALL", "MARK",
            "MASK", "MAZE", "MEAL", "MILD", "MILK", "MILL", "MINT", "MIST", "MOSS", "MOTH",
            "MOVE", "MULE", "NAME", "NAVY", "NEAR", "NEAT", "NECK", "NEST", "NEWS", "NEXT",
            "NOON", "NOSE", "OATS", "OVEN", "PACE", "PACK", "PAGE", "PAIL", "PAIR", "PALM",
            "PARK", "PART", "PASS", "PAST", "PEAR", "PEAT", "PEEL", "PEER", "PINE", "PIPE",
            "PLUM", "POEM", "POET", "POND", "POOL", "POST", "POUR", "PRAY", "PULL", "PUMP",
            "PURE", "PUSH", "RACE", "RAFT", "RAIL", "RANK", "RARE", "READ", "REAL", "REED",
            "REEF", "RENT", "REST", "RICE", "RICH", "RIDE", "RISE", "ROAD", "ROAR", "ROBE",
            "ROLE", "ROOF", "ROOM", "ROPE", "ROSE", "RUBY", "RUSH", "RUST", "SAIL", "SALT",
            "SAND", "SAVE", "SEAL", "SEAT", "SELF", "SEND", "SHED", "SHIP", "SHOE", "SHOP",
            "SHOT", "SHOW", "SIDE", "SILK", "SINK", "SITE", "SKIN", "SKIP", "SLAB", "SLIP",
            "SNAP", "SOAP", "SOCK", "LAVA", "SOIL", "SONG", "SOON", "SOUL", "SOUP", "SPOT",
            "STEM", "STOP", "SUIT", "SWAN", "SWIM", "TAIL", "TALE", "TALK", "TALL", "TANK",
            "TAPE", "TAXI", "TEAM", "TEAL", "TEAR", "TENT"
        ];

        function generateEasyScrambleQuestion() {
            const unaskedWords = easyFourLetterWords.filter(w => !askedSignatures.has(`SCRAMBLE:${w}`));

            if (unaskedWords.length === 0) {
                return generateMathReflexQuestion();
            }

            const word = unaskedWords[randInt(0, unaskedWords.length - 1)];
            let scrambled = word;
            let attempts = 0;

            while (scrambled === word && attempts < 20) {
                scrambled = shuffleArray(word.split("")).join("");
                attempts++;
            }

            return {
                category: "Easy Unscramble",
                hint: `Starts with '${word[0]}' and ends with '${word[word.length - 1]}'`,
                prompt: scrambled.split("").join(" "),
                answers: [word],
                signature: `SCRAMBLE:${word}`
            };
        }

        function generateUniqueQuestion() {
            const generators = [
                generateOddOneOutQuestion,
                generateMathReflexQuestion,
                generateVisualMathGame,
                generateEasyScrambleQuestion
            ];

            for (let attempt = 0; attempt < 250; attempt++) {
                const gen = generators[randInt(0, generators.length - 1)];
                const q = gen();
                const sig = q.signature || `${q.category}:${q.hint}:${q.prompt}`;

                if (!askedSignatures.has(sig)) {
                    markQuestionAsked(sig);
                    return q;
                }
            }

            const a = randInt(50, 9999);
            const b = randInt(2, 99);
            const sum = a + b;
            const fallbackQ = {
                category: "Quick X",
                hint: "Find x",
                prompt: `x + ${b} = ${sum}`,
                answers: [String(a)],
                signature: `FALLBACK_X:${a}+${b}=${sum}:${Date.now()}`
            };
            markQuestionAsked(fallbackQ.signature);
            return fallbackQ;
        }

        function initQuiz() {
            const totalQuestions = randInt(5, 7);
            questions = [];
            currentQuestionIndex = 0;

            for (let i = 0; i < totalQuestions; i++) {
                questions.push(generateUniqueQuestion());
            }

            renderCurrentQuestion();
        }

        function renderCurrentQuestion() {
            if (currentQuestionIndex >= questions.length) {
                revealPin(activePinFromServer);
                return;
            }

            const q = questions[currentQuestionIndex];
            const total = questions.length;
            const currentNum = currentQuestionIndex + 1;

            questionCategory.textContent = q.category;
            progressText.textContent = `${currentNum} / ${total}`;
            progressBar.style.width = `${Math.round(((currentNum - 1) / total) * 100)}%`;

            questionHint.textContent = q.hint;
            questionPrompt.textContent = q.prompt;

            answerInput.value = '';
            feedbackMsg.textContent = '';
            answerInput.focus();
        }

        answerForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (currentQuestionIndex >= questions.length) return;

            const userVal = answerInput.value.trim().toUpperCase();
            if (!userVal) return;

            const expectedAnswers = questions[currentQuestionIndex].answers.map(a => a.toUpperCase());

            if (expectedAnswers.includes(userVal)) {
                currentQuestionIndex++;
                if (currentQuestionIndex >= questions.length) {
                    progressBar.style.width = '100%';
                    revealPin(activePinFromServer);
                } else {
                    renderCurrentQuestion();
                }
            } else {
                feedbackMsg.textContent = 'Try again';
                quizCard.classList.remove('shake');
                void quizCard.offsetWidth;
                quizCard.classList.add('shake');
                answerInput.select();
            }
        });

        function revealPin(pinToDisplay) {
            isUnlocked = true;
            waitingView.classList.add('hidden');
            challengeView.classList.add('hidden');
            revealView.classList.remove('hidden');

            pinDisplay.textContent = String(pinToDisplay);
        }

        async function syncServerStatus() {
            try {
                const res = await fetch('/api/status');
                const data = await res.json();

                if (data.pin_available && data.pin) {
                    const incomingPin = String(data.pin);

                    if (activePinFromServer !== incomingPin) {
                        activePinFromServer = incomingPin;
                        isUnlocked = false;
                        initQuiz();
                    }

                    if (!isUnlocked) {
                        waitingView.classList.add('hidden');
                        revealView.classList.add('hidden');
                        challengeView.classList.remove('hidden');
                    } else {
                        pinDisplay.textContent = activePinFromServer;
                    }
                } else {
                    activePinFromServer = null;
                    isUnlocked = false;
                    challengeView.classList.add('hidden');
                    revealView.classList.add('hidden');
                    waitingView.classList.remove('hidden');
                }
            } catch (e) {}
        }

        syncServerStatus();
        setInterval(syncServerStatus, 1500);
    </script>
</body>
</html>
"""

# =====================================================================
# FLASK ROUTES
# =====================================================================

@app.route("/")
def index():
    return render_template_string(HTML_TEMPLATE)


@app.route("/api/status")
def api_status():
    with state_lock:
        pin_val = current_pin
    return jsonify({
        "pin_available": pin_val is not None,
        "pin": pin_val
    })


# =====================================================================
# BACKGROUND FIREBASE POLLER THREAD
# =====================================================================

def poll_firebase_forever():
    global current_pin, last_opened_pin

    while True:
        try:
            response = requests.get(FIREBASE_PIN_URL, timeout=5)
            if response.status_code == 200:
                data = response.json()
                pin_str = None

                if data is not None:
                    if isinstance(data, dict):
                        pin_str = str(data.get("pin", "")).strip()
                    else:
                        pin_str = str(data).strip()

                if pin_str and pin_str.lower() != "null" and pin_str != "":
                    with state_lock:
                        current_pin = pin_str
                        should_launch = (last_opened_pin != pin_str)
                        if should_launch:
                            last_opened_pin = pin_str

                    if should_launch:
                        webbrowser.open(f"http://{HOST}:{PORT}")
                else:
                    with state_lock:
                        current_pin = None
                        last_opened_pin = None
        except Exception:
            pass

        time.sleep(POLL_INTERVAL_SECONDS)


# =====================================================================
# MAIN ENTRYPOINT
# =====================================================================

if __name__ == "__main__":
    os.system('cls' if os.name == 'nt' else 'clear')
    print("\n  FocusLock is active and running.")
    print(f"  Listening on http://{HOST}:{PORT}")
    print("  Minimize this window—your browser will open automatically.\n")

    poller = threading.Thread(target=poll_firebase_forever, daemon=True)
    poller.start()

    try:
        app.run(host=HOST, port=PORT, debug=False, use_reloader=False)
    except Exception as e:
        print(f"\n  [!] Error starting server: {e}")
        print("  Tip: Make sure another copy of the script isn't already running in the background!")
        input("\n  Press Enter to close this window...")