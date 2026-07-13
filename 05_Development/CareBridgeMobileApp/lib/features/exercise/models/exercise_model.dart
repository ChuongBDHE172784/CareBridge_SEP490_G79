class ExerciseSession {
  final String id;
  final String exerciseId;
  final String status;
  final DateTime startedAt;
  final bool supportsPostureAnalysis;

  ExerciseSession({
    required this.id,
    required this.exerciseId,
    required this.status,
    required this.startedAt,
    required this.supportsPostureAnalysis,
  });

  factory ExerciseSession.fromJson(Map<String, dynamic> json) {
    return ExerciseSession(
      id: json['exerciseSessionId'] as String,
      exerciseId: json['exerciseId'] as String? ?? '',
      status: json['sessionStatus'] as String? ?? 'IN_PROGRESS',
      startedAt: json['startedAt'] != null
          ? DateTime.parse(json['startedAt'] as String)
          : DateTime.now(),
      supportsPostureAnalysis:
          json['supportsPostureAnalysis'] as bool? ?? false,
    );
  }
}

class SessionResult {
  final String sessionId;
  final String exerciseId;
  final String exerciseTitle;
  final String status;
  final DateTime? startedAt;
  final DateTime? endedAt;
  final int actualDurationSeconds;
  final double completionPercent;
  final double? postureScore;
  final int warningCount;

  SessionResult({
    required this.sessionId,
    required this.exerciseId,
    required this.exerciseTitle,
    required this.status,
    this.startedAt,
    this.endedAt,
    required this.actualDurationSeconds,
    required this.completionPercent,
    this.postureScore,
    required this.warningCount,
  });

  factory SessionResult.fromJson(Map<String, dynamic> json) {
    return SessionResult(
      sessionId: json['exerciseSessionId'] as String,
      exerciseId: json['exerciseId'] as String? ?? '',
      exerciseTitle: json['exerciseTitle'] as String? ?? '',
      status: json['sessionStatus'] as String? ?? 'COMPLETED',
      startedAt: json['startedAt'] != null
          ? DateTime.parse(json['startedAt'] as String)
          : null,
      endedAt: json['endedAt'] != null
          ? DateTime.parse(json['endedAt'] as String)
          : null,
      actualDurationSeconds: (json['actualDurationSeconds'] as int?) ?? 0,
      completionPercent: (json['completionPercent'] as num?)?.toDouble() ?? 0.0,
      postureScore: (json['postureScore'] as num?)?.toDouble(),
      warningCount: (json['warningCount'] as int?) ?? 0,
    );
  }

  int get actualDurationMinutes => (actualDurationSeconds / 60).round();
  int get postureCorrections =>
      postureScore != null ? ((postureScore! / 10).round()) : 0;
}

class ExerciseHistoryItem {
  final String sessionId;
  final String exerciseId;
  final String exerciseTitle;
  final String status;
  final DateTime? startedAt;
  final int actualDurationSeconds;
  final double completionPercent;
  final int warningCount;

  ExerciseHistoryItem({
    required this.sessionId,
    required this.exerciseId,
    required this.exerciseTitle,
    required this.status,
    this.startedAt,
    required this.actualDurationSeconds,
    required this.completionPercent,
    required this.warningCount,
  });

  factory ExerciseHistoryItem.fromJson(Map<String, dynamic> json) {
    return ExerciseHistoryItem(
      sessionId: json['exerciseSessionId'] as String,
      exerciseId: json['exerciseId'] as String? ?? '',
      exerciseTitle: json['exerciseTitle'] as String? ?? '',
      status: json['sessionStatus'] as String? ?? '',
      startedAt: json['startedAt'] != null
          ? DateTime.parse(json['startedAt'] as String)
          : null,
      actualDurationSeconds: (json['actualDurationSeconds'] as int?) ?? 0,
      completionPercent: (json['completionPercent'] as num?)?.toDouble() ?? 0.0,
      warningCount: (json['warningCount'] as int?) ?? 0,
    );
  }

  bool get isCompleted => status == 'COMPLETED';
  int get durationMinutes => (actualDurationSeconds / 60).round();
}
