import 'dart:async';

import 'package:web/web.dart' as web;

// Migrated from `dart:html` + `dart:web_audio`: `AudioContext` was removed from
// `dart:html`, and `dart:web_audio` no longer exists in the current Dart SDK.
// `package:web` + `dart:js_interop` is the supported replacement. Tone frequencies,
// envelopes and repeat intervals are unchanged from the previous implementation.

web.AudioContext? _audioCtx;
Timer? _timer;
String? _currentMode;

void startRingtoneWeb(String mode) {
  if (_currentMode == mode) return;
  stopRingtoneWeb();
  _currentMode = mode;

  try {
    final ctx = web.AudioContext();
    _audioCtx = ctx;
    if (ctx.state == 'suspended') {
      ctx.resume();
    }

    if (mode == 'outgoing') {
      _playOutgoing();
      _timer = Timer.periodic(const Duration(seconds: 3), (_) => _playOutgoing());
    } else if (mode == 'incoming') {
      _playIncoming();
      _timer = Timer.periodic(
        const Duration(milliseconds: 2500),
        (_) => _playIncoming(),
      );
    }
  } catch (_) {}
}

/// Ring-back tone: 440 Hz + 480 Hz, decaying over 1.8 s.
void _playOutgoing() {
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

/// Incoming ring: 852 Hz + 1209 Hz, pulsed twice over 1.2 s.
void _playIncoming() {
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

void stopRingtoneWeb() {
  _currentMode = null;
  _timer?.cancel();
  _timer = null;
  try {
    _audioCtx?.close();
  } catch (_) {}
  _audioCtx = null;
}
