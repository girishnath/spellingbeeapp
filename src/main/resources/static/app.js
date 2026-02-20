const state = {
    studentId: localStorage.getItem('studentId'),
    quizMode: localStorage.getItem('quizMode') || 'NORMAL',
    questions: [],
    currentIdx: 0,
    answers: [],
    timer: (localStorage.getItem('quizMode') === 'QUICK' ? 5 : 50) * 60,
    timerInterval: null
};

if (!state.studentId) window.location.href = 'index.html';

const els = {
    timeDisplay: document.getElementById('timeDisplay'),
    currentQ: document.getElementById('currentQ'),
    totalQ: document.getElementById('totalQ'),
    progressFill: document.getElementById('progressFill'),
    qInstruction: document.getElementById('qInstruction'),
    audioBtn: document.getElementById('audioBtn'),
    defAccordion: document.getElementById('defAccordion'),
    defHeader: document.getElementById('defHeader'),
    defContent: document.getElementById('defContent'),
    defText: document.getElementById('defText'),
    optionsGrid: document.getElementById('optionsGrid'),
    nextBtn: document.getElementById('nextBtn')
};

// Initial Fetch
async function init() {
    try {
        const res = await fetch(`/api/quiz/${state.studentId}?mode=${state.quizMode}`);
        state.questions = await res.json();
        els.totalQ.textContent = state.questions.length;

        // Hide loading, show quiz
        document.getElementById('loadingOverlay').style.display = 'none';
        document.getElementById('quizContainer').style.display = 'block';

        startTimer();
        renderQuestion();
    } catch (e) {
        alert("Failed to load quiz.");
    }
}

function startTimer() {
    els.timeDisplay.textContent = formatTime(state.timer);
    state.timerInterval = setInterval(() => {
        state.timer--;
        els.timeDisplay.textContent = formatTime(state.timer);
        if (state.timer <= 0) submitQuiz();
    }, 1000);
}

function formatTime(sec) {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
}

function renderQuestion() {
    const q = state.questions[state.currentIdx];
    els.currentQ.textContent = state.currentIdx + 1;
    els.progressFill.style.width = `${((state.currentIdx) / state.questions.length) * 100}%`;

    // Reset UI
    els.defContent.classList.remove('open');
    els.defText.textContent = "Loading...";
    els.nextBtn.disabled = true;
    els.nextBtn.style.opacity = "0.5";
    els.optionsGrid.innerHTML = '';

    // Audio Handler
    els.audioBtn.onclick = () => TTS.speak(q.word);
    document.getElementById('altAudioBtn').onclick = () => TTS.playOnline(q.word);

    // Type Specific Logic
    if (q.type === 'SPELLING') {
        els.qInstruction.textContent = "Listen and select the correct spelling:";
        els.defAccordion.style.display = 'block';
        els.defHeader.onclick = () => toggleDef(q.word);
    } else {
        els.qInstruction.textContent = q.questionText; // "What is the definition of..."
        els.defAccordion.style.display = 'none'; // Hide hint for vocab
    }

    // Options
    q.options.forEach(opt => {
        const btn = document.createElement('button');
        btn.className = 'option-btn';
        btn.textContent = opt;
        btn.onclick = () => selectOption(btn, opt);
        els.optionsGrid.appendChild(btn);
    });

    // Auto-play audio once
    setTimeout(() => TTS.speak(q.word), 500);
}



async function toggleDef(word) {
    const isOpen = els.defContent.classList.contains('open');
    if (isOpen) {
        els.defContent.classList.remove('open');
    } else {
        els.defContent.classList.add('open');
        if (els.defText.textContent === "Loading...") {
            try {
                const res = await fetch(`/api/definition/${word}`);
                if (res.ok) {
                    const def = await res.text();
                    els.defText.textContent = def || "No definition found.";
                } else {
                    els.defText.textContent = "Definition unavailable.";
                }
            } catch {
                els.defText.textContent = "Error loading definition.";
            }
        }
    }
}

function selectOption(btn, text) {
    // Clear previous selection
    document.querySelectorAll('.option-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');

    // Save temporary answer
    state.currentAnswer = {
        questionId: state.questions[state.currentIdx].id,
        selectedOption: text
    };

    els.nextBtn.disabled = false;
    els.nextBtn.style.opacity = "1";
}

els.nextBtn.onclick = () => {
    if (!state.currentAnswer) return;

    state.answers.push(state.currentAnswer);
    state.currentAnswer = null;

    if (state.currentIdx < state.questions.length - 1) {
        state.currentIdx++;
        renderQuestion();
    } else {
        submitQuiz();
    }
};

async function submitQuiz() {
    clearInterval(state.timerInterval);
    const payload = {
        studentId: state.studentId,
        answers: state.answers,
        totalQuestions: state.questions.length
    };

    try {
        const res = await fetch('/api/submit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const result = await res.json();
        localStorage.setItem('lastResult', JSON.stringify(result));
        window.location.href = 'result.html';
    } catch (e) {
        alert("Error submitting quiz.");
    }
}

// Exit quiz early
function exitQuiz() {
    if (confirm('Are you sure you want to exit? Your progress will not be saved.')) {
        clearInterval(state.timerInterval);
        window.location.href = 'dashboard.html';
    }
}

init();
