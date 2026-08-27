class RingtonePlayer {
  private audioCtx: AudioContext | null = null;
  private intervalId: number | null = null;
  private isPlaying = false;
  private currentMode: 'outgoing' | 'incoming' | null = null;

  start(mode: 'outgoing' | 'incoming') {
    if (this.isPlaying && this.currentMode === mode) return;
    this.stop();

    this.isPlaying = true;
    this.currentMode = mode;

    try {
      const AudioContextClass =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      if (!AudioContextClass) return;
      this.audioCtx = new AudioContextClass();

      if (this.audioCtx.state === 'suspended') {
        void this.audioCtx.resume();
      }

      if (mode === 'outgoing') {
        const playTone = () => {
          if (!this.audioCtx || this.audioCtx.state === 'closed') return;
          try {
            const now = this.audioCtx.currentTime;
            const osc1 = this.audioCtx.createOscillator();
            const osc2 = this.audioCtx.createOscillator();
            const gain = this.audioCtx.createGain();

            osc1.frequency.setValueAtTime(440, now);
            osc2.frequency.setValueAtTime(480, now);

            gain.gain.setValueAtTime(0.12, now);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 1.8);

            osc1.connect(gain);
            osc2.connect(gain);
            gain.connect(this.audioCtx.destination);

            osc1.start(now);
            osc2.start(now);
            osc1.stop(now + 1.8);
            osc2.stop(now + 1.8);
          } catch {
            // ignore playback error
          }
        };

        playTone();
        this.intervalId = window.setInterval(playTone, 3000);
      } else {
        const playTone = () => {
          if (!this.audioCtx || this.audioCtx.state === 'closed') return;
          try {
            const now = this.audioCtx.currentTime;
            const osc1 = this.audioCtx.createOscillator();
            const osc2 = this.audioCtx.createOscillator();
            const gain = this.audioCtx.createGain();

            osc1.frequency.setValueAtTime(852, now);
            osc2.frequency.setValueAtTime(1209, now);

            gain.gain.setValueAtTime(0.2, now);
            gain.gain.setValueAtTime(0.2, now + 0.4);
            gain.gain.setValueAtTime(0, now + 0.5);
            gain.gain.setValueAtTime(0.2, now + 0.6);
            gain.gain.exponentialRampToValueAtTime(0.001, now + 1.2);

            osc1.connect(gain);
            osc2.connect(gain);
            gain.connect(this.audioCtx.destination);

            osc1.start(now);
            osc2.start(now);
            osc1.stop(now + 1.2);
            osc2.stop(now + 1.2);
          } catch {
            // ignore playback error
          }
        };

        playTone();
        this.intervalId = window.setInterval(playTone, 2500);
      }
    } catch {
      // ignore audio context initialization error
    }
  }

  stop() {
    this.isPlaying = false;
    this.currentMode = null;
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    if (this.audioCtx) {
      try {
        void this.audioCtx.close();
      } catch {
        // ignore audio context close error
      }
      this.audioCtx = null;
    }
  }
}

export const ringtonePlayer = new RingtonePlayer();
