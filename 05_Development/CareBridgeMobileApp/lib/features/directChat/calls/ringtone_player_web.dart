import 'dart:async';
import 'dart:js_interop';

// Web Audio, declared through dart:js_interop rather than dart:html.
//
// This file used to reach for html.AudioContext. dart:html no longer exposes it -
// the analyzer reports "Undefined class 'AudioContext'" - so the web build of the
// ringtone could never compile. Only the handful of members the ringtone needs are
// declared here; js_interop ships with the SDK, so nothing new is pulled in.

@JS('AudioContext')
extension type _AudioContext._(JSObject _) implements JSObject {
  external factory _AudioContext();
  external String get state;
  external double get currentTime;
  external _AudioNode get destination;
  external _OscillatorNode createOscillator();
  external _GainNode createGain();
  external JSPromise<JSAny?> resume();
  external JSPromise<JSAny?> close();
}

extension type _AudioNode._(JSObject _) implements JSObject {
  external void connect(_AudioNode destination);
}

extension type _AudioParam._(JSObject _) implements JSObject {
  external void setValueAtTime(double value, double startTime);
  external void exponentialRampToValueAtTime(double value, double endTime);
}

extension type _OscillatorNode._(JSObject _) implements _AudioNode {
  external _AudioParam get frequency;
  external void start(double when);
  external void stop(double when);
}

extension type _GainNode._(JSObject _) implements _AudioNode {
  external _AudioParam get gain;
}

_AudioContext? _audioCtx;
Timer? _timer;
String? _currentMode;

void startRingtoneWeb(String mode) {
  if (_currentMode == mode) return;
  stopRingtoneWeb();
  _currentMode = mode;

  try {
    final ctx = _AudioContext();
    _audioCtx = ctx;
    if (ctx.state == 'suspended') {
      ctx.resume();
    }

    // Two tones over a decaying gain: the ring you hear while a call is dialling
    // out, and the sharper double-beep for one coming in.
    void ring({
      required double toneA,
      required double toneB,
      required double peak,
      required double tail,
      bool pulse = false,
    }) {
      final active = _audioCtx;
      if (active == null || active.state == 'closed') return;
      try {
        final now = active.currentTime;
        final osc1 = active.createOscillator();
        final osc2 = active.createOscillator();
        final gain = active.createGain();

        osc1.frequency.setValueAtTime(toneA, now);
        osc2.frequency.setValueAtTime(toneB, now);

        gain.gain.setValueAtTime(peak, now);
        if (pulse) {
          gain.gain.setValueAtTime(peak, now + 0.4);
          gain.gain.setValueAtTime(0, now + 0.5);
          gain.gain.setValueAtTime(peak, now + 0.6);
        }
        gain.gain.exponentialRampToValueAtTime(0.001, now + tail);

        osc1.connect(gain);
        osc2.connect(gain);
        gain.connect(active.destination);

        osc1.start(now);
        osc2.start(now);
        osc1.stop(now + tail);
        osc2.stop(now + tail);
      } catch (_) {
        // A tone that will not play must never take the call down with it.
      }
    }

    if (mode == 'outgoing') {
      void outgoing() => ring(toneA: 440, toneB: 480, peak: 0.12, tail: 1.8);
      outgoing();
      _timer = Timer.periodic(const Duration(seconds: 3), (_) => outgoing());
    } else if (mode == 'incoming') {
      void incoming() =>
          ring(toneA: 852, toneB: 1209, peak: 0.2, tail: 1.2, pulse: true);
      incoming();
      _timer = Timer.periodic(
        const Duration(milliseconds: 2500),
        (_) => incoming(),
      );
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
