import 'package:flutter_tts/flutter_tts.dart';

/// Testable boundary for spoken posture feedback.
abstract interface class ExerciseVoiceFeedback {
  Future<void> speak(String message);

  Future<void> stop();

  Future<void> dispose();
}

/// Vietnamese text-to-speech implementation used during an exercise session.
///
/// Platform failures intentionally stay contained here: the session's visual
/// warning is the authoritative fallback and must remain available without TTS.
class SystemExerciseVoiceFeedback implements ExerciseVoiceFeedback {
  SystemExerciseVoiceFeedback({FlutterTts? tts}) : _tts = tts ?? FlutterTts();

  final FlutterTts _tts;
  bool _initialized = false;
  bool _disposed = false;

  Future<void> _initialize() async {
    if (_initialized || _disposed) return;
    try {
      await _tts.setLanguage('vi-VN');
      await _tts.setSpeechRate(0.48);
      await _tts.setVolume(1.0);
      _initialized = true;
    } catch (_) {
      // A later warning may retry initialization if the platform becomes ready.
    }
  }

  @override
  Future<void> speak(String message) async {
    if (_disposed || message.trim().isEmpty) return;
    try {
      await _initialize();
      if (_disposed || !_initialized) return;
      await _tts.stop();
      await _tts.speak(message.trim());
    } catch (_) {
      // Visual posture feedback remains visible when speech is unavailable.
    }
  }

  @override
  Future<void> stop() async {
    if (_disposed) return;
    try {
      await _tts.stop();
    } catch (_) {
      // Stopping speech is best-effort during lifecycle transitions.
    }
  }

  @override
  Future<void> dispose() async {
    if (_disposed) return;
    await stop();
    _disposed = true;
  }
}

/// Applies a small global throttle plus a longer exact-message dedupe window.
/// This prevents a high-frequency posture stream from continuously restarting
/// the same spoken warning.
class ExerciseVoiceFeedbackAnnouncer {
  ExerciseVoiceFeedbackAnnouncer({
    required ExerciseVoiceFeedback voice,
    DateTime Function()? now,
    this.minimumInterval = const Duration(seconds: 2),
    this.repeatInterval = const Duration(seconds: 8),
  }) : _voice = voice,
       _now = now ?? DateTime.now;

  final ExerciseVoiceFeedback _voice;
  final DateTime Function() _now;
  final Duration minimumInterval;
  final Duration repeatInterval;

  DateTime? _lastAnnouncementAt;
  final Map<String, DateTime> _lastMessageAt = <String, DateTime>{};
  bool _disposed = false;

  Future<void> announce(String message) async {
    final normalized = message.trim();
    if (_disposed || normalized.isEmpty) return;

    final now = _now();
    final lastAnnouncementAt = _lastAnnouncementAt;
    if (lastAnnouncementAt != null &&
        now.difference(lastAnnouncementAt) < minimumInterval) {
      return;
    }
    final lastSameMessageAt = _lastMessageAt[normalized];
    if (lastSameMessageAt != null &&
        now.difference(lastSameMessageAt) < repeatInterval) {
      return;
    }

    // Reserve the window before awaiting the platform so concurrent feedback
    // events cannot race through the throttle.
    _lastAnnouncementAt = now;
    _lastMessageAt[normalized] = now;
    try {
      await _voice.speak(normalized);
    } catch (_) {
      // The caller already rendered the visual warning.
    }
  }

  Future<void> stop() async {
    try {
      await _voice.stop();
    } catch (_) {
      // Lifecycle cleanup is best-effort.
    }
  }

  Future<void> dispose() async {
    if (_disposed) return;
    _disposed = true;
    try {
      await _voice.dispose();
    } catch (_) {
      // Platform disposal must not interrupt widget disposal.
    }
  }
}
