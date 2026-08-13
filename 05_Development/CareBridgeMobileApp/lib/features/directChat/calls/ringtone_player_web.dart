import 'dart:async';
import 'package:web/web.dart' as web;

web.AudioContext? _audioCtx;
Timer? _timer;
String? _currentMode;

void startRingtoneWeb(String mode) {
  if (_currentMode == mode) return;
  stopRingtoneWeb();
  _currentMode = mode;

  try {
    _audioCtx = web.AudioContext();
    if (_audioCtx?.state == 'suspended') {
      _audioCtx?.resume();
    }

    void playOutgoing() {
      final ctx = _audioCtx;
      if (ctx == null || ctx.state == 'closed') return;
      try {
        final now = ctx.currentTime;
        final osc1 = ctx.createOscillator();
        final osc2 = ctx.createOscillator();
        final gain = ctx.createGain();

        osc1.frequency.setValueAtTime(440, now);
        osc2.frequency.setValueAtTime(480, now);

        gain.gain.setValueAtTime(0.12, now);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 1.8);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(ctx.destination);

        osc1.start(now);
        osc2.start(now);
        osc1.stop(now + 1.8);
        osc2.stop(now + 1.8);
      } catch (_) {}
    }

    void playIncoming() {
      final ctx = _audioCtx;
      if (ctx == null || ctx.state == 'closed') return;
      try {
        final now = ctx.currentTime;
        final osc1 = ctx.createOscillator();
        final osc2 = ctx.createOscillator();
        final gain = ctx.createGain();

        osc1.frequency.setValueAtTime(852, now);
        osc2.frequency.setValueAtTime(1209, now);

        gain.gain.setValueAtTime(0.2, now);
        gain.gain.setValueAtTime(0.2, now + 0.4);
        gain.gain.setValueAtTime(0, now + 0.5);
        gain.gain.setValueAtTime(0.2, now + 0.6);
        gain.gain.exponentialRampToValueAtTime(0.001, now + 1.2);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(ctx.destination);

        osc1.start(now);
        osc2.start(now);
        osc1.stop(now + 1.2);
        osc2.stop(now + 1.2);
      } catch (_) {}
    }

    if (mode == 'outgoing') {
      playOutgoing();
      _timer = Timer.periodic(const Duration(seconds: 3), (_) => playOutgoing());
    } else if (mode == 'incoming') {
      playIncoming();
      _timer = Timer.periodic(const Duration(milliseconds: 2500), (_) => playIncoming());
    }
  } catch (_) {}
}

void stopRingtoneWeb() {
  _currentMode = null;
  _timer?.cancel();
  _timer = null;
  try {
    _audioCtx?.close();
  } catch (_) {}
  _audioCtx = null;
}
