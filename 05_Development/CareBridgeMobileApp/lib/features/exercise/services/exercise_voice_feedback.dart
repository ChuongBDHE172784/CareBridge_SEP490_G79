import 'dart:io' show Platform;
import 'package:flutter/foundation.dart' show debugPrint, kIsWeb;
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
  bool _voiceConfigured = false;

  Future<void> _initialize() async {
    if (_initialized || _disposed) return;
    try {
      if (!kIsWeb && Platform.isAndroid) {
        final dynamic engines = await _tts.getEngines;
        if (engines is List && engines.contains('com.google.android.tts')) {
          await _tts.setEngine('com.google.android.tts');
        }
      }
      await _tts.setLanguage('vi-VN');
      await _configureVietnameseVoice();

      // On Web Speech API, 1.0 is normal rate (0.5 is 50% slow-motion/sluggish).
      // On Android/iOS flutter_tts, 0.5 corresponds to standard 1.0x speed.
      final speechRate = kIsWeb ? 0.95 : 0.5;
      await _tts.setSpeechRate(speechRate);
      await _tts.setVolume(1.0);
      await _tts.setPitch(1.0);
      _initialized = true;
    } catch (e) {
      debugPrint('TTS initialization warning: $e');
      _initialized = true;
    }
  }

  Future<void> _configureVietnameseVoice() async {
    try {
      final dynamic voices = await _tts.getVoices;
      if (voices is! List || voices.isEmpty) return;

      Map<String, String>? bestVoice;
      for (final voice in voices) {
        if (voice is Map) {
          final name = voice['name']?.toString() ?? '';
          final locale = voice['locale']?.toString() ?? '';
          final nameLower = name.toLowerCase();
          final localeLower = locale.toLowerCase();

          final isVietnamese = localeLower.startsWith('vi') ||
              localeLower.contains('vn') ||
              localeLower.contains('vietnam') ||
              nameLower.contains('vietnam') ||
              nameLower.contains('tiếng việt') ||
              nameLower.contains('vi-vn') ||
              nameLower.contains('vi_vn');

          if (isVietnamese) {
            final voiceMap = <String, String>{
              'name': name,
              'locale': locale.isNotEmpty ? locale : 'vi-VN',
            };

            // Prefer high quality Natural / Online voices (e.g. HoaiMy, NamMinh, Google)
            if (nameLower.contains('natural') ||
                nameLower.contains('online') ||
                nameLower.contains('hoaimy') ||
                nameLower.contains('namminh') ||
                nameLower.contains('google')) {
              bestVoice = voiceMap;
              break;
            }
            bestVoice ??= voiceMap;
          }
        }
      }

      if (bestVoice != null) {
        await _tts.setVoice(bestVoice);
        _voiceConfigured = true;
      }
    } catch (e) {
      debugPrint('TTS voice selection warning: $e');
    }
  }

  /// Clean punctuation marks (like em-dashes or slashes) that cause TTS engines
  /// to pause awkwardly or pronounce symbol names aloud.
  static String sanitizeForSpeech(String text) {
    return text
        .replaceAll(RegExp(r'\s*[—–]\s*'), ', ')
        .replaceAll(RegExp(r'\s+-\s+'), ', ')
        .replaceAll(RegExp(r'\s*/\s*'), ' hoặc ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  @override
  Future<void> speak(String message) async {
    if (_disposed || message.trim().isEmpty) return;
    try {
      await _initialize();
      if (!_voiceConfigured) {
        // Retry voice lookup in case Web Speech voices loaded asynchronously
        await _configureVietnameseVoice();
      }
      if (_disposed) return;
      await _tts.stop();
      final textToSpeak = sanitizeForSpeech(message);
      await _tts.speak(textToSpeak);
    } catch (e) {
      debugPrint('TTS speak warning: $e');
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

/// Bộ điều phối phát giọng nói hướng dẫn (TTS Announcer):
/// Áp dụng cơ chế điều tiết (Throttle 2s) và chống lặp cùng câu nói (Dedupe window 8s).
/// Mục đích UX & Y tế: Tránh việc máy liên tục nói đè hoặc lặp đi lặp lại một câu cảnh báo
/// khi mẹ bầu đang trong quá trình điều chỉnh tư thế.
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

  /// Đọc câu hướng dẫn nhắc nhở tư thế qua loa/tai nghe
  Future<void> announce(String message) async {
    final normalized = message.trim();
    if (_disposed || normalized.isEmpty) return;

    final now = _now();
    final lastAnnouncementAt = _lastAnnouncementAt;
    // Kiểm tra khoảng cách tối thiểu giữa 2 lần phát âm thanh (tránh nói chen ngang)
    if (lastAnnouncementAt != null &&
        now.difference(lastAnnouncementAt) < minimumInterval) {
      return;
    }
    // Kiểm tra thời gian lặp lại của cùng 1 nội dung cảnh báo (tối thiểu 8s)
    final lastSameMessageAt = _lastMessageAt[normalized];
    if (lastSameMessageAt != null &&
        now.difference(lastSameMessageAt) < repeatInterval) {
      return;
    }

    _lastAnnouncementAt = now;
    _lastMessageAt[normalized] = now;
    try {
      await _voice.speak(normalized);
    } catch (_) {
      // Nếu thiết bị không phát được TTS thì giao diện hình ảnh vẫn hiển thị bình thường
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
