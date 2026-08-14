import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/services/exercise_voice_feedback.dart';

class _FakeVoiceFeedback implements ExerciseVoiceFeedback {
  final List<String> spoken = <String>[];
  int stopCalls = 0;
  int disposeCalls = 0;
  bool failSpeak = false;

  @override
  Future<void> speak(String message) async {
    if (failSpeak) throw StateError('TTS unavailable');
    spoken.add(message);
  }

  @override
  Future<void> stop() async {
    stopCalls++;
  }

  @override
  Future<void> dispose() async {
    disposeCalls++;
  }
}

void main() {
  test(
    'dedupes repeated warnings and throttles different rapid warnings',
    () async {
      final voice = _FakeVoiceFeedback();
      var now = DateTime(2026, 8, 14, 9);
      final announcer = ExerciseVoiceFeedbackAnnouncer(
        voice: voice,
        now: () => now,
      );

      await announcer.announce('  Giữ lưng thẳng  ');
      now = now.add(const Duration(seconds: 3));
      await announcer.announce('Giữ lưng thẳng');
      await announcer.announce('Hạ vai xuống');
      now = now.add(const Duration(seconds: 2));
      await announcer.announce('Hạ vai xuống');
      now = now.add(const Duration(seconds: 4));
      await announcer.announce('Giữ lưng thẳng');

      expect(voice.spoken, <String>[
        'Giữ lưng thẳng',
        'Hạ vai xuống',
        'Giữ lưng thẳng',
      ]);
    },
  );

  test(
    'contains TTS failures so visual feedback can remain authoritative',
    () async {
      final voice = _FakeVoiceFeedback()..failSpeak = true;
      final announcer = ExerciseVoiceFeedbackAnnouncer(voice: voice);

      await expectLater(announcer.announce('Sửa tư thế'), completes);
    },
  );

  test('stops and disposes the injected voice boundary', () async {
    final voice = _FakeVoiceFeedback();
    final announcer = ExerciseVoiceFeedbackAnnouncer(voice: voice);

    await announcer.stop();
    await announcer.dispose();
    await announcer.dispose();
    await announcer.announce('Không được đọc sau dispose');

    expect(voice.stopCalls, 1);
    expect(voice.disposeCalls, 1);
    expect(voice.spoken, isEmpty);
  });
}
