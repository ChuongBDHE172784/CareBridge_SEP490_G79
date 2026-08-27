import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/models/posture_event_model.dart';

void main() {
  test('maps legacy score to canonical visibility JSON', () {
    final landmark = PostureLandmark.fromPoseJson({
      'x': 0.2,
      'y': 0.3,
      'z': -0.1,
      'score': 0.92,
    });

    expect(landmark.visibility, 0.92);
    expect(landmark.toJson(), {
      'x': 0.2,
      'y': 0.3,
      'z': -0.1,
      'visibility': 0.92,
    });
  });

  test('rejects non-finite and out-of-range landmarks', () {
    expect(
      () => PostureLandmark.fromPoseJson({
        'x': double.nan,
        'y': 0.3,
        'z': 0.0,
        'visibility': 0.9,
      }),
      throwsFormatException,
    );
    expect(
      () => PostureLandmark.fromPoseJson({
        'x': 0.2,
        'y': 0.3,
        'z': 0.0,
        'visibility': 1.1,
      }),
      throwsFormatException,
    );
  });

  test('parses normalized posture feedback', () {
    final feedback = PostureFeedback.fromJson({
      'postureCode': 'GOOD_FORM',
      'confidenceScore': 0.91,
      'severity': 'INFO',
      'feedbackText': 'Keep this form.',
    });

    expect(feedback.postureCode, 'GOOD_FORM');
    expect(feedback.confidenceScore, 0.91);
    expect(feedback.severity, 'INFO');
    expect(feedback.feedbackText, 'Keep this form.');
  });
}
