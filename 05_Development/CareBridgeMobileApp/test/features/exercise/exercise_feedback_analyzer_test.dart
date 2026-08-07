import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/models/posture_event_model.dart';
import 'package:untitled/features/exercise/services/exercise_feedback_analyzer.dart';

PostureLandmark _point(double x, double y, [double visibility = 1]) =>
    PostureLandmark(x: x, y: y, z: 0, visibility: visibility);

Map<String, PostureLandmark> _bicepFrame({
  required bool leftUp,
  required bool rightUp,
  double visibility = 1,
}) {
  final frame = <String, PostureLandmark>{
    'left_shoulder': _point(0, 0, visibility),
    'left_elbow': _point(1, 0, visibility),
    'left_wrist': leftUp
        ? _point(0.5, 0.866, visibility)
        : _point(2, 0, visibility),
    'right_shoulder': _point(0, 0.5, visibility),
    'right_elbow': _point(1, 0.5, visibility),
    'right_wrist': rightUp
        ? _point(0.5, 1.366, visibility)
        : _point(2, 0.5, visibility),
  };
  return frame;
}

Map<String, PostureLandmark> _legFrame({required bool down}) =>
    <String, PostureLandmark>{
      'left_hip': _point(0, 0),
      'left_knee': _point(1, 0),
      'left_ankle': down ? _point(1, 1) : _point(2, 0),
      'right_hip': _point(0, 0.5),
      'right_knee': _point(1, 0.5),
      'right_ankle': down ? _point(1, 1.5) : _point(2, 0.5),
    };

void main() {
  test('counts each bicep arm once after down-to-up transition', () {
    final analyzer = ExerciseFeedbackAnalyzer(exerciseKey: 'bicep_curl');

    analyzer.analyze(_bicepFrame(leftUp: false, rightUp: false));
    final firstUp = analyzer.analyze(_bicepFrame(leftUp: true, rightUp: false));
    expect(firstUp.leftBicepRepetitions, 1);
    expect(firstUp.rightBicepRepetitions, 0);
    expect(firstUp.leftElbowAngle, closeTo(60, 0.2));
    expect(firstUp.rightElbowAngle, closeTo(180, 0.2));

    final heldUp = analyzer.analyze(_bicepFrame(leftUp: true, rightUp: false));
    expect(heldUp.leftBicepRepetitions, 1);
  });

  test('counts squat and lunge down stages once', () {
    final squat = ExerciseFeedbackAnalyzer(exerciseKey: 'squat');
    squat.analyze(_legFrame(down: false));
    final down = squat.analyze(_legFrame(down: true));
    expect(down.squatRepetitions, 1);
    expect(squat.analyze(_legFrame(down: true)).squatRepetitions, 1);
    expect(down.leftKneeAngle, closeTo(90, 0.2));

    final lunge = ExerciseFeedbackAnalyzer(exerciseKey: 'lunge');
    lunge.analyze(_legFrame(down: false), stage: 'init');
    lunge.analyze(_legFrame(down: false), stage: 'mid');
    final lungeDown = lunge.analyze(_legFrame(down: true), stage: 'down');
    expect(lungeDown.lungeRepetitions, 1);
  });

  test('does not advance a leg counter from a partial pose or stale stage', () {
    final analyzer = ExerciseFeedbackAnalyzer(exerciseKey: 'squat');
    analyzer.analyze(_legFrame(down: false), postureCode: 'up');

    final partial = _legFrame(down: true)..remove('right_ankle');
    final metrics = analyzer.analyze(partial, postureCode: 'down');

    expect(metrics.squatRepetitions, 0);
    expect(metrics.angles, contains('left_knee'));
    expect(metrics.angles, isNot(contains('right_knee')));
  });

  test(
    'low visibility fails closed and unsupported exercises expose no metrics',
    () {
      final analyzer = ExerciseFeedbackAnalyzer(exerciseKey: 'bicep_curl');
      final lowVisibility = analyzer.analyze(
        _bicepFrame(leftUp: false, rightUp: false, visibility: 0.49),
      );
      expect(lowVisibility.angles, isEmpty);
      expect(lowVisibility.repetitions, 0);

      final plank = ExerciseFeedbackAnalyzer(
        exerciseKey: 'plank',
      ).analyze(_legFrame(down: false));
      expect(plank.angles, isEmpty);
      expect(plank.repetitions, 0);

      final unsupported = ExerciseFeedbackAnalyzer(
        exerciseKey: 'walking',
      ).analyze(_legFrame(down: true));
      expect(unsupported.isSupported, isFalse);
      expect(unsupported.angles, isEmpty);
    },
  );

  test('warning state is transient presentation state only', () {
    final analyzer = ExerciseFeedbackAnalyzer(exerciseKey: 'squat');
    analyzer.applyFeedback(
      const PostureFeedback(
        postureCode: 'SQUAT_KNEES_TOO_TIGHT',
        confidenceScore: 0.9,
        severity: 'WARNING',
      ),
    );
    expect(analyzer.hasFeedbackError, isTrue);
    analyzer.applyFeedback(
      const PostureFeedback(
        postureCode: 'GOOD_FORM',
        confidenceScore: 0.9,
        severity: 'INFO',
      ),
    );
    expect(analyzer.hasFeedbackError, isFalse);
  });
}
