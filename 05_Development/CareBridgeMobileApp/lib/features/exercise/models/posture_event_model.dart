/// A single normalized MediaPipe Pose landmark sent to the posture endpoint.
///
/// The Exercise-Correction sidecar accepts named landmarks only. The
/// `fromPoseJson` factory also accepts the legacy `score` field used by the
/// existing browser demo and maps it to the canonical `visibility` field.
class PostureLandmark {
  final double x;
  final double y;
  final double z;
  final double visibility;

  const PostureLandmark({
    required this.x,
    required this.y,
    required this.z,
    required this.visibility,
  });

  factory PostureLandmark.fromPoseJson(Map<String, dynamic> json) {
    final visibility = json['visibility'] ?? json['score'];
    final landmark = PostureLandmark(
      x: _finiteNumber(json['x'], field: 'x'),
      y: _finiteNumber(json['y'], field: 'y'),
      z: _finiteNumber(json['z'] ?? 0.0, field: 'z'),
      visibility: _finiteNumber(visibility, field: 'visibility'),
    );
    if (landmark.visibility < 0.0 || landmark.visibility > 1.0) {
      throw const FormatException(
        'Landmark visibility must be between 0 and 1',
      );
    }
    return landmark;
  }

  Map<String, double> toJson() => <String, double>{
    'x': x,
    'y': y,
    'z': z,
    'visibility': visibility,
  };

  static double _finiteNumber(Object? value, {required String field}) {
    final number = value is num ? value.toDouble() : double.nan;
    if (!number.isFinite) {
      throw FormatException('Landmark $field must be finite');
    }
    return number;
  }
}

class PostureFeedback {
  final String postureCode;
  final double confidenceScore;
  final String severity;
  final String? feedbackText;

  const PostureFeedback({
    required this.postureCode,
    required this.confidenceScore,
    required this.severity,
    this.feedbackText,
  });

  factory PostureFeedback.fromJson(Map<String, dynamic> json) {
    final confidence = json['confidenceScore'];
    if (confidence is! num || !confidence.toDouble().isFinite) {
      throw const FormatException(
        'Posture response confidenceScore is invalid',
      );
    }
    return PostureFeedback(
      postureCode: json['postureCode'] as String? ?? 'UNKNOWN',
      confidenceScore: confidence.toDouble(),
      severity: json['severity'] as String? ?? 'INFO',
      feedbackText: json['feedbackText'] as String?,
    );
  }
}
