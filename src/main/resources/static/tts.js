const TTS = {
    voice: null,

    init: function () {
        if (!window.speechSynthesis) {
            console.error("Speech Synthesis not supported.");
            return;
        }

        // Wait for voices to load
        if (speechSynthesis.onvoiceschanged !== undefined) {
            speechSynthesis.onvoiceschanged = this.loadVoices.bind(this);
        }
        this.loadVoices(); // Try loading immediately just in case
    },

    loadVoices: function () {
        const voices = speechSynthesis.getVoices();
        if (voices.length === 0) return;

        // Prioritize specific high-quality voices
        const priorities = [
            "Google US English",
            "Microsoft Zira",
            "Samantha",
            "Alex"
        ];

        // 1. Try exact matches from priority list
        for (const name of priorities) {
            const found = voices.find(v => v.name.includes(name));
            if (found) {
                this.voice = found;
                console.log(`TTS: Selected priority voice: ${found.name}`);
                return;
            }
        }

        // 2. Fallback to any English voice
        const englishVoice = voices.find(v => v.lang.startsWith('en'));
        if (englishVoice) {
            this.voice = englishVoice;
            console.log(`TTS: Selected English voice: ${englishVoice.name}`);
            return;
        }

        // 3. Last resort
        this.voice = voices[0];
        console.log(`TTS: Default fallback voice: ${this.voice.name}`);
    },

    speak: function (text) {
        if (!text) return;

        // Cancel previous speech
        window.speechSynthesis.cancel();

        const utt = new SpeechSynthesisUtterance(text);

        // Apply selected voice if available
        if (this.voice) {
            utt.voice = this.voice;
        }

        // Tweaks for better clarity
        utt.rate = 0.9;  // Slightly slower
        utt.pitch = 1.0; // Normal pitch
        utt.volume = 1.0;

        window.speechSynthesis.speak(utt);
    },

    playOnline: function (text) {
        if (!text) return;

        // Cancel browser TTS
        window.speechSynthesis.cancel();

        const audio = new Audio(`/api/tts?text=${encodeURIComponent(text)}`);
        audio.play().catch(e => {
            console.error("Error playing online audio:", e);
            alert("Could not play online audio. Check your connection or credentials.");
        });
    }
};

// Auto-initialize
TTS.init();
