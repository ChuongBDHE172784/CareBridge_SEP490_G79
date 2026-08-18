import 'dart:math' as math;

import '../models/posture_event_model.dart';

/// The small set of exercise families for which the client can show a
/// transient metric.  The model/server response remains the source of
/// posture feedback; this enum is only used to decide which local metrics are
/// applicable to the current screen.
enum ExerciseFeedbackExercise { bicepCurl, squat, lunge, plank, unsupported }

extension ExerciseFeedbackExerciseX on ExerciseFeedbackExercise {
  String get key {
    switch (this) {
      case ExerciseFeedbackExercise.bicepCurl:
        return 'bicep_curl';
      case ExerciseFeedbackExercise.squat:
        return 'squat';
      case ExerciseFeedbackExercise.lunge:
        return 'lunge';
      case ExerciseFeedbackExercise.plank:
        return 'plank';
      case ExerciseFeedbackExercise.unsupported:
        return 'unsupported';
    }
  }
}

/// A frame-level view of client-derived exercise metrics.
///
/// Values are deliberately optional.  A missing/low-visibility landmark does
/// not produce a guessed angle or a repetition.  Counts are cumulative for
/// the current analyzer/session only.
class ExerciseFeedbackMetrics {
  ExerciseFeedbackMetrics({
    required this.exercise,
    required this.leftBicepRepetitions,
    required this.rightBicepRepetitions,
    required this.squatRepetitions,
    required this.lungeRepetitions,
    required Map<String, double> angles,
    required this.leftElbowAngle,
    required this.rightElbowAngle,
    required this.leftKneeAngle,
    required this.rightKneeAngle,
    required this.stage,
    required this.hasVisibleLandmarks,
    required this.feedbackError,
  }) : angles = Map<String, double>.unmodifiable(angles);

  final ExerciseFeedbackExercise exercise;
  final int leftBicepRepetitions;
  final int rightBicepRepetitions;
  final int squatRepetitions;
  final int lungeRepetitions;
  final Map<String, double> angles;
  final double? leftElbowAngle;
  final double? rightElbowAngle;
  final double? leftKneeAngle;
  final double? rightKneeAngle;
  final String? stage;
  final bool hasVisibleLandmarks;
  final bool feedbackError;

  /// The repetition value relevant to this exercise family.
  int get repetitions {
    switch (exercise) {
      case ExerciseFeedbackExercise.bicepCurl:
        return leftBicepRepetitions + rightBicepRepetitions;
      case ExerciseFeedbackExercise.squat:
        return squatRepetitions;
      case ExerciseFeedbackExercise.lunge:
        return lungeRepetitions;
      case ExerciseFeedbackExercise.plank:
      case ExerciseFeedbackExercise.unsupported:
        return 0;
    }
  }

  int get totalRepetitions => repetitions;

  /// Alias useful to result-card callers that call the value a count.
  int get count => repetitions;

  bool get isSupported => exercise != ExerciseFeedbackExercise.unsupported;

  bool get hasApplicableMetrics => angles.isNotEmpty || repetitions > 0;

  /// Returns a JSON-safe, transient summary.  It is not sent to the server.
  Map<String, dynamic> toJson() => <String, dynamic>{
    'exercise': exercise.key,
    'leftBicepRepetitions': leftBicepRepetitions,
    'rightBicepRepetitions': rightBicepRepetitions,
    'squatRepetitions': squatRepetitions,
    'lungeRepetitions': lungeRepetitions,
    'angles': angles,
    if (stage != null) 'stage': stage,
  };
}

/// A copy of the cumulative metrics that can be passed to the result screen.
/// It intentionally contains no server or persistence fields.
class ExerciseFeedbackSnapshot extends ExerciseFeedbackMetrics {
  ExerciseFeedbackSnapshot.fromMetrics(ExerciseFeedbackMetrics metrics)
    : super(
        exercise: metrics.exercise,
        leftBicepRepetitions: metrics.leftBicepRepetitions,
        rightBicepRepetitions: metrics.rightBicepRepetitions,
        squatRepetitions: metrics.squatRepetitions,
        lungeRepetitions: metrics.lungeRepetitions,
        angles: metrics.angles,
        leftElbowAngle: metrics.leftElbowAngle,
        rightElbowAngle: metrics.rightElbowAngle,
        leftKneeAngle: metrics.leftKneeAngle,
        rightKneeAngle: metrics.rightKneeAngle,
        stage: metrics.stage,
        hasVisibleLandmarks: metrics.hasVisibleLandmarks,
        feedbackError: metrics.feedbackError,
      );
}

/// Stateful, pure (no network/UI) feedback derivation for a workout screen.
///
/// A bicep repetition is counted independently for each arm after an
/// observed extended (down) phase followed by a flexed (up) phase.  Squat and
/// lunge repetitions are counted when a down stage follows the required up or
/// init/mid stage.  Stage codes from the model are preferred; joint angles are
/// a conservative local fallback when no stage code is available.
class ExerciseFeedbackAnalyzer {
  ExerciseFeedbackAnalyzer({
    String? positionalExerciseKey,
    String? exerciseKey,
    String? exerciseType,
    String? exerciseId,
    String? exerciseTitle,
    this.visibilityThreshold = 0.5,
    // Match the upstream Exercise-Correction state thresholds: an extended
    // arm is down and a flexed arm is up.  These are presentation counters,
    // not a replacement for the server model's classification.
    this.bicepDownAngle = 120,
    this.bicepUpAngle = 100,
    this.squatDownAngle = 125,
    this.squatUpAngle = 155,
    this.lungeDownAngle = 125,
    this.lungeUpAngle = 155,
  }) : exercise = _resolveExercise(
         positionalExerciseKey ??
             exerciseKey ??
             exerciseType ??
             exerciseId ??
             exerciseTitle,
       ) {
    if (!visibilityThreshold.isFinite ||
        visibilityThreshold < 0 ||
        visibilityThreshold > 1) {
      throw ArgumentError.value(
        visibilityThreshold,
        'visibilityThreshold',
        'must be between 0 and 1',
      );
    }
    _metrics = _emptyMetrics();
  }

  final ExerciseFeedbackExercise exercise;
  final double visibilityThreshold;
  final double bicepDownAngle;
  final double bicepUpAngle;
  final double squatDownAngle;
  final double squatUpAngle;
  final double lungeDownAngle;
  final double lungeUpAngle;

  static const String stageUp = 'up';
  static const String stageDown = 'down';
  static const String stageInit = 'init';
  static const String stageMid = 'mid';
  static const Object _unset = _Unset();

  _ArmPhase _leftArm = _ArmPhase.unknown;
  _ArmPhase _rightArm = _ArmPhase.unknown;
  _SquatPhase _squatPhase = _SquatPhase.unknown;
  _LungePhase _lungePhase = _LungePhase.unknown;
  int _leftBicepCount = 0;
  int _rightBicepCount = 0;
  int _squatCount = 0;
  int _lungeCount = 0;
  bool _feedbackError = false;
  String? _feedbackStage;
  ExerciseFeedbackMetrics _metrics = ExerciseFeedbackMetrics(
    exercise: ExerciseFeedbackExercise.unsupported,
    leftBicepRepetitions: 0,
    rightBicepRepetitions: 0,
    squatRepetitions: 0,
    lungeRepetitions: 0,
    angles: const <String, double>{},
    leftElbowAngle: null,
    rightElbowAngle: null,
    leftKneeAngle: null,
    rightKneeAngle: null,
    stage: null,
    hasVisibleLandmarks: false,
    feedbackError: false,
  );

  ExerciseFeedbackMetrics get metrics => _metrics;

  ExerciseFeedbackSnapshot get snapshot =>
      ExerciseFeedbackSnapshot.fromMetrics(_metrics);

  int get leftBicepRepetitions => _leftBicepCount;
  int get rightBicepRepetitions => _rightBicepCount;
  int get squatRepetitions => _squatCount;
  int get lungeRepetitions => _lungeCount;

  /// Applies the server's existing warning/stage response without changing
  /// the posture-event contract.  The next valid frame uses this stage and
  /// the camera source can use [hasFeedbackError] for its local palette.
  void applyFeedback(PostureFeedback feedback) {
    final severity = feedback.severity.trim().toUpperCase();
    final code = feedback.postureCode.trim().toUpperCase();
    final isGood = code == 'C' ||
        code.endsWith('/C') ||
        code.contains('GOOD_FORM') ||
        code.contains('CORRECT') ||
        code == 'UP' ||
        code == 'DOWN';
    _feedbackError = !isGood &&
        (severity == 'CRITICAL' ||
            (severity == 'WARNING' && !code.contains('MODEL_UNAVAILABLE')) ||
            _isKnownErrorCode(code));
    _feedbackStage = _stageFromCode(code, exercise);
    _metrics = _copyMetrics(
      _metrics,
      feedbackError: _feedbackError,
      stage: _feedbackStage ?? _metrics.stage,
    );
  }



  bool get hasFeedbackError => _feedbackError;

  /// Processes one normalized landmark frame.
  ExerciseFeedbackMetrics analyze(
    Map<String, PostureLandmark> landmarks, {
    PostureFeedback? feedback,
    String? postureCode,
    String? stage,
  }) {
    if (feedback != null) applyFeedback(feedback);
    final explicitStage =
        _normalizeStage(stage) ??
        _stageFromCode(postureCode?.toUpperCase(), exercise) ??
        _feedbackStage;

    if (exercise == ExerciseFeedbackExercise.unsupported ||
        exercise == ExerciseFeedbackExercise.plank) {
      _metrics = _copyMetrics(
        _metrics,
        angles: const <String, double>{},
        leftElbowAngle: null,
        rightElbowAngle: null,
        leftKneeAngle: null,
        rightKneeAngle: null,
        stage: explicitStage,
        hasVisibleLandmarks: false,
      );
      return _metrics;
    }

    final leftElbow = _jointAngle(
      landmarks,
      'left_shoulder',
      'left_elbow',
      'left_wrist',
    );
    final rightElbow = _jointAngle(
      landmarks,
      'right_shoulder',
      'right_elbow',
      'right_wrist',
    );
    final leftKnee = _jointAngle(
      landmarks,
      'left_hip',
      'left_knee',
      'left_ankle',
    );
    final rightKnee = _jointAngle(
      landmarks,
      'right_hip',
      'right_knee',
      'right_ankle',
    );

    final angles = <String, double>{};
    if (exercise == ExerciseFeedbackExercise.bicepCurl) {
      if (leftElbow != null) angles['left_elbow'] = leftElbow;
      if (rightElbow != null) angles['right_elbow'] = rightElbow;
      _updateBicep(leftElbow, isLeft: true);
      _updateBicep(rightElbow, isLeft: false);
    } else if (exercise == ExerciseFeedbackExercise.squat ||
        exercise == ExerciseFeedbackExercise.lunge) {
      if (leftKnee != null) angles['left_knee'] = leftKnee;
      if (rightKnee != null) angles['right_knee'] = rightKnee;
      // The sidecar's squat/lunge models require both knees (and each
      // knee's hip/ankle triplet) to be present.  A partial frame may still
      // display the angle that was drawable, but it must never advance the
      // repetition state machine or let a stale model stage bypass the
      // visibility gate.
      final completeLegPose = leftKnee != null && rightKnee != null;
      final effectiveStage = completeLegPose
          ? explicitStage ?? _inferLegStage(leftKnee, rightKnee, exercise)
          : null;
      if (completeLegPose) {
        if (exercise == ExerciseFeedbackExercise.squat) {
          _updateSquat(effectiveStage);
        } else {
          _updateLunge(effectiveStage);
        }
      }
      _metrics = _copyMetrics(_metrics, stage: effectiveStage);
    }

    final metricsStage =
        exercise == ExerciseFeedbackExercise.squat ||
            exercise == ExerciseFeedbackExercise.lunge
        ? _metrics.stage
        : explicitStage;

    _metrics = _copyMetrics(
      _metrics,
      angles: angles,
      leftElbowAngle: leftElbow,
      rightElbowAngle: rightElbow,
      leftKneeAngle: leftKnee,
      rightKneeAngle: rightKnee,
      stage: metricsStage,
      hasVisibleLandmarks: angles.isNotEmpty,
    );
    return _metrics;
  }

  /// Alias for callers that prefer a state-update verb.
  ExerciseFeedbackMetrics update(
    Map<String, PostureLandmark> landmarks, {
    PostureFeedback? feedback,
    String? postureCode,
    String? stage,
  }) => analyze(
    landmarks,
    feedback: feedback,
    postureCode: postureCode,
    stage: stage,
  );

  ExerciseFeedbackMetrics analyzeFrame(
    Map<String, PostureLandmark> landmarks, {
    PostureFeedback? feedback,
    String? postureCode,
    String? stage,
  }) => analyze(
    landmarks,
    feedback: feedback,
    postureCode: postureCode,
    stage: stage,
  );

  void reset() {
    _leftArm = _ArmPhase.unknown;
    _rightArm = _ArmPhase.unknown;
    _squatPhase = _SquatPhase.unknown;
    _lungePhase = _LungePhase.unknown;
    _leftBicepCount = 0;
    _rightBicepCount = 0;
    _squatCount = 0;
    _lungeCount = 0;
    _feedbackError = false;
    _feedbackStage = null;
    _metrics = _emptyMetrics();
  }

  ExerciseFeedbackMetrics _emptyMetrics() => ExerciseFeedbackMetrics(
    exercise: exercise,
    leftBicepRepetitions: 0,
    rightBicepRepetitions: 0,
    squatRepetitions: 0,
    lungeRepetitions: 0,
    angles: const <String, double>{},
    leftElbowAngle: null,
    rightElbowAngle: null,
    leftKneeAngle: null,
    rightKneeAngle: null,
    stage: null,
    hasVisibleLandmarks: false,
    feedbackError: false,
  );

  ExerciseFeedbackMetrics _copyMetrics(
    ExerciseFeedbackMetrics source, {
    Map<String, double>? angles,
    Object? leftElbowAngle = _unset,
    Object? rightElbowAngle = _unset,
    Object? leftKneeAngle = _unset,
    Object? rightKneeAngle = _unset,
    String? stage,
    bool? hasVisibleLandmarks,
    bool? feedbackError,
  }) => ExerciseFeedbackMetrics(
    exercise: exercise,
    leftBicepRepetitions: _leftBicepCount,
    rightBicepRepetitions: _rightBicepCount,
    squatRepetitions: _squatCount,
    lungeRepetitions: _lungeCount,
    angles: angles ?? source.angles,
    leftElbowAngle: identical(leftElbowAngle, _unset)
        ? source.leftElbowAngle
        : leftElbowAngle as double?,
    rightElbowAngle: identical(rightElbowAngle, _unset)
        ? source.rightElbowAngle
        : rightElbowAngle as double?,
    leftKneeAngle: identical(leftKneeAngle, _unset)
        ? source.leftKneeAngle
        : leftKneeAngle as double?,
    rightKneeAngle: identical(rightKneeAngle, _unset)
        ? source.rightKneeAngle
        : rightKneeAngle as double?,
    stage: stage ?? source.stage,
    hasVisibleLandmarks: hasVisibleLandmarks ?? source.hasVisibleLandmarks,
    feedbackError: feedbackError ?? _feedbackError,
  );

  void _updateBicep(double? angle, {required bool isLeft}) {
    if (angle == null) return;
    var phase = isLeft ? _leftArm : _rightArm;
    if (angle >= bicepDownAngle) {
      phase = _ArmPhase.down;
    } else if (angle <= bicepUpAngle) {
      if (phase == _ArmPhase.down) {
        if (isLeft) {
          _leftBicepCount++;
        } else {
          _rightBicepCount++;
        }
      }
      phase = _ArmPhase.up;
    }
    if (isLeft) {
      _leftArm = phase;
    } else {
      _rightArm = phase;
    }
  }

  void _updateSquat(String? stage) {
    if (stage == null) return;
    if (stage == stageUp) {
      _squatPhase = _SquatPhase.up;
    } else if (stage == stageDown) {
      if (_squatPhase == _SquatPhase.up) _squatCount++;
      _squatPhase = _SquatPhase.down;
    }
  }

  void _updateLunge(String? stage) {
    if (stage == null) return;
    switch (stage) {
      case stageInit:
        _lungePhase = _LungePhase.init;
      case stageMid:
        if (_lungePhase == _LungePhase.unknown) {
          _lungePhase = _LungePhase.mid;
        } else if (_lungePhase == _LungePhase.init) {
          _lungePhase = _LungePhase.mid;
        }
      case stageDown:
        if (_lungePhase == _LungePhase.init || _lungePhase == _LungePhase.mid) {
          _lungeCount++;
        }
        _lungePhase = _LungePhase.down;
      case stageUp:
        // An explicit up response starts the next lunge cycle.
        _lungePhase = _LungePhase.init;
    }
  }

  String? _inferLegStage(
    double? leftKnee,
    double? rightKnee,
    ExerciseFeedbackExercise family,
  ) {
    if (leftKnee == null || rightKnee == null) return null;
    final angle = (leftKnee + rightKnee) / 2;
    if (family == ExerciseFeedbackExercise.squat) {
      if (angle <= squatDownAngle) return stageDown;
      if (angle >= squatUpAngle) return stageUp;
      return null;
    }
    if (angle <= lungeDownAngle) return stageDown;
    if (angle >= lungeUpAngle) return stageUp;
    return null;
  }

  double? _jointAngle(
    Map<String, PostureLandmark> landmarks,
    String first,
    String joint,
    String last,
  ) {
    final a = landmarks[first];
    final b = landmarks[joint];
    final c = landmarks[last];
    if (!_drawable(a) || !_drawable(b) || !_drawable(c)) return null;
    return calculateJointAngle(a!, b!, c!);
  }

  bool _drawable(PostureLandmark? point) =>
      point != null &&
      point.visibility.isFinite &&
      point.visibility >= visibilityThreshold &&
      point.x.isFinite &&
      point.y.isFinite;

  /// Geometry helper exposed for deterministic unit tests and other local
  /// presentation code.  It never throws for a degenerate/missing pose.
  static double? calculateJointAngle(
    PostureLandmark? a,
    PostureLandmark? b,
    PostureLandmark? c,
  ) {
    if (a == null || b == null || c == null) return null;
    final abx = a.x - b.x;
    final aby = a.y - b.y;
    final cbx = c.x - b.x;
    final cby = c.y - b.y;
    final magnitude =
        math.sqrt(abx * abx + aby * aby) * math.sqrt(cbx * cbx + cby * cby);
    if (!magnitude.isFinite || magnitude <= 0) return null;
    final cosine = ((abx * cbx + aby * cby) / magnitude).clamp(-1.0, 1.0);
    final angle = math.acos(cosine) * 180 / math.pi;
    return angle.isFinite ? angle : null;
  }

  static ExerciseFeedbackExercise _resolveExercise(String? value) {
    final normalized = (value ?? '').toLowerCase().replaceAll('-', '_');
    if (normalized.contains('bicep') ||
        normalized.contains('curl') ||
        normalized.contains('bap_tay') ||
        normalized.endsWith('000000000004')) {
      return ExerciseFeedbackExercise.bicepCurl;
    }
    if (normalized.contains('squat') || normalized.endsWith('000000000006')) {
      return ExerciseFeedbackExercise.squat;
    }
    if (normalized.contains('lunge') ||
        normalized.contains('chung') ||
        normalized.endsWith('000000000007')) {
      return ExerciseFeedbackExercise.lunge;
    }
    if (normalized.contains('plank') || normalized.endsWith('000000000005')) {
      return ExerciseFeedbackExercise.plank;
    }
    return ExerciseFeedbackExercise.unsupported;
  }

  static String? _normalizeStage(String? value) {
    final normalized = value?.trim().toLowerCase();
    if (normalized == null || normalized.isEmpty) return null;
    if (normalized == stageUp || normalized == 'standing') return stageUp;
    if (normalized == stageDown || normalized == 'lowered') return stageDown;
    if (normalized == stageInit || normalized == 'initial') return stageInit;
    if (normalized == stageMid || normalized == 'middle') return stageMid;
    return null;
  }

  static String? _stageFromCode(String? code, ExerciseFeedbackExercise family) {
    if (code == null || code.isEmpty) return null;
    final normalized = code.toUpperCase();
    if (family == ExerciseFeedbackExercise.squat &&
        (normalized == stageDown.toUpperCase() ||
            normalized == stageUp.toUpperCase())) {
      return normalized == stageDown.toUpperCase() ? stageDown : stageUp;
    }
    if (family == ExerciseFeedbackExercise.squat &&
        normalized.contains('SQUAT')) {
      if (normalized.contains('DOWN')) return stageDown;
      if (normalized.contains('UP')) return stageUp;
    }
    if (family == ExerciseFeedbackExercise.lunge) {
      // The current sidecar returns the upstream stage labels directly
      // (I=init, M=mid, D=down), while future adapters may prefix them.
      if (normalized == 'D') return stageDown;
      if (normalized == 'M') return stageMid;
      if (normalized == 'I') return stageInit;
    }
    if (family == ExerciseFeedbackExercise.lunge &&
        normalized.contains('LUNGE')) {
      if (normalized.contains('DOWN')) return stageDown;
      if (normalized.contains('MID')) return stageMid;
      if (normalized.contains('INIT') || normalized.contains('UP')) {
        return stageInit;
      }
    }
    return null;
  }

  static bool _isKnownErrorCode(String code) {
    if (code.isEmpty ||
        code == 'C' ||
        code.endsWith('/C') ||
        code.contains('GOOD_FORM') ||
        code.contains('CORRECT') ||
        code == 'UP' ||
        code == 'DOWN' ||
        code == 'INIT' ||
        code == 'MID') {
      return false;
    }
    if (code.contains('RULE_FALLBACK_GOOD_FORM') ||
        code == 'MODEL_UNAVAILABLE' ||
        code == 'MODEL_LOW_CONFIDENCE') {
      return false;
    }
    return code.contains('WARNING') ||
        code.contains('CRITICAL') ||
        code.contains('ERROR') ||
        code.contains('BAD_') ||
        code.contains('TOO_') ||
        code.contains('OVER_') ||
        code.contains('LEAN') ||
        code.contains('NARROW') ||
        code.contains('WIDE');
  }


}

/// Backwards-compatible names for callers that describe the result as state
/// or a snapshot rather than metrics.
typedef ExerciseFeedbackState = ExerciseFeedbackMetrics;
typedef ExerciseFeedbackResult = ExerciseFeedbackMetrics;

enum _ArmPhase { unknown, down, up }

enum _SquatPhase { unknown, up, down }

enum _LungePhase { unknown, init, mid, down }

class _Unset {
  const _Unset();
}

/// Top-level geometry helper for tests that do not need an analyzer instance.
double? calculateJointAngle(
  PostureLandmark? a,
  PostureLandmark? b,
  PostureLandmark? c,
) => ExerciseFeedbackAnalyzer.calculateJointAngle(a, b, c);
