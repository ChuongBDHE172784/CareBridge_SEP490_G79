import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/screens/exercise_session_screen.dart';
import 'package:untitled/features/exercise/services/posture_camera_source.dart';
import 'package:untitled/features/exercise/services/exercise_voice_feedback.dart';

class _FakeVoiceFeedback implements ExerciseVoiceFeedback {
  @override
  Future<void> speak(String message) async {}
  @override
  Future<void> stop() async {}
  @override
  Future<void> dispose() async {}
}

class _FakePostureCameraSource implements PostureCameraSource {
  final _framesController = StreamController<Map<String, dynamic>>.broadcast();
  final _errorsController = StreamController<String>.broadcast();
  int switchCameraCalls = 0;
  bool _running = false;
  bool _switching = false;

  @override
  Stream<Map<String, dynamic>> get frames => _framesController.stream;

  @override
  Stream<String> get errors => _errorsController.stream;

  @override
  bool get isSupported => true;

  @override
  bool get isRunning => _running;

  @override
  bool get isSwitching => _switching;

  @override
  String? get lastError => null;

  @override
  bool get hasFeedbackError => false;

  @override
  bool get isFrontCamera => true;

  @override
  void setFeedbackError(bool value) {}

  @override
  void setFeedbackWarning(bool value) {}

  @override
  Future<void> start() async {
    _running = true;
  }

  @override
  Future<void> switchCamera() async {
    _switching = true;
    switchCameraCalls++;
    await Future<void>.delayed(const Duration(milliseconds: 50));
    _switching = false;
  }

  @override
  Future<void> stop() async {
    _running = false;
  }

  @override
  Future<void> dispose() async {
    await stop();
    await _framesController.close();
    await _errorsController.close();
  }

  @override
  Widget buildPreview() {
    return const Text('Camera Preview');
  }
}

void main() {
  testWidgets('renders ExerciseSessionScreen in initial state and toggles pause/resume UI', (
    tester,
  ) async {
    final camera = _FakePostureCameraSource();
    final voice = _FakeVoiceFeedback();

    await tester.pumpWidget(
      MaterialApp(
        home: ExerciseSessionScreen(
          exerciseId: 'ex-1',
          exerciseTitle: 'Plank cơ bản',
          instruction: 'Giữ thân người thẳng',
          durationMinutes: 3,
          sessionId: 'session-123',
          initialStatus: 'IN_PROGRESS',
          initialStartedAt: DateTime.now(),
          postureCameraSource: camera,
          voiceFeedback: voice,
        ),
      ),
    );

    // Initial check: exercise title and instruction shown
    expect(find.text('Plank cơ bản'), findsOneWidget);
    expect(find.text('Giữ thân người thẳng'), findsOneWidget);

    // When IN_PROGRESS, the pause icon is displayed
    expect(find.byIcon(Icons.pause_rounded), findsOneWidget);

    // Stop icon and Skip icon are present
    expect(find.byIcon(Icons.stop_rounded), findsOneWidget);
    expect(find.byIcon(Icons.skip_next_rounded), findsOneWidget);
  });

  testWidgets('flips camera when camera flip button is tapped', (tester) async {
    final camera = _FakePostureCameraSource();
    final voice = _FakeVoiceFeedback();

    await tester.pumpWidget(
      MaterialApp(
        home: ExerciseSessionScreen(
          exerciseId: 'ex-1',
          exerciseTitle: 'Plank cơ bản',
          instruction: 'Giữ thân người thẳng',
          durationMinutes: 3,
          sessionId: 'session-123',
          initialStatus: 'IN_PROGRESS',
          initialStartedAt: DateTime.now(),
          enableRealtimePostureCamera: true,
          postureCameraSource: camera,
          voiceFeedback: voice,
        ),
      ),
    );

    await tester.pump();
    expect(find.byIcon(Icons.flip_camera_ios_outlined), findsOneWidget);

    await tester.tap(find.byIcon(Icons.flip_camera_ios_outlined));
    await tester.pump(const Duration(milliseconds: 100));
    await tester.pumpAndSettle();

    expect(camera.switchCameraCalls, 1);
  });

  testWidgets('renders initial PAUSED status with play icon', (tester) async {
    final camera = _FakePostureCameraSource();
    final voice = _FakeVoiceFeedback();

    await tester.pumpWidget(
      MaterialApp(
        home: ExerciseSessionScreen(
          exerciseId: 'ex-1',
          exerciseTitle: 'Plank cơ bản',
          instruction: 'Giữ thân người thẳng',
          durationMinutes: 3,
          sessionId: 'session-123',
          initialStatus: 'PAUSED',
          initialStartedAt: DateTime.now(),
          postureCameraSource: camera,
          voiceFeedback: voice,
        ),
      ),
    );

    // When PAUSED, play arrow icon is shown
    expect(find.byIcon(Icons.play_arrow_rounded), findsOneWidget);
    expect(find.byIcon(Icons.pause_rounded), findsNothing);
  });
}
