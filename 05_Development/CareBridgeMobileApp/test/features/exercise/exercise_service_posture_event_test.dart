import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/models/posture_event_model.dart';
import 'package:untitled/features/exercise/services/exercise_service.dart';

void main() {
  test(
    'serializes normalized landmarks for the posture-event contract',
    () async {
      String? capturedPath;
      Map<String, dynamic>? capturedBody;

      final service = ExerciseService.forTesting(
        post: (path, body) async {
          capturedPath = path;
          capturedBody = body;
          return <String, dynamic>{
            'data': <String, dynamic>{
              'postureCode': 'GOOD_FORM',
              'confidenceScore': 0.95,
              'severity': 'INFO',
              'feedbackText': 'Tư thế chuẩn',
            },
          };
        },
      );

      final feedback = await service.analyzePostureEvent(
        sessionId: 'session-1',
        eventTimeMs: 1234,
        landmarks: const <String, PostureLandmark>{
          'left_shoulder': PostureLandmark(
            x: 0.25,
            y: 0.35,
            z: -0.1,
            visibility: 0.98,
          ),
        },
      );

      expect(
        capturedPath,
        '/api/v1/exercises/sessions/session-1/posture-events',
      );
      expect(capturedBody?['eventTimeMs'], 1234);
      expect(capturedBody?['keypointSummaryJson'], <String, dynamic>{
        'left_shoulder': <String, double>{
          'x': 0.25,
          'y': 0.35,
          'z': -0.1,
          'visibility': 0.98,
        },
      });
      expect(feedback.postureCode, 'GOOD_FORM');
      expect(feedback.confidenceScore, 0.95);
    },
  );
}
