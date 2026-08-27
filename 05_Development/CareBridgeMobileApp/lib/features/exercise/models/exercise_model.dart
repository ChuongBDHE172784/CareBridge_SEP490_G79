class ExerciseSummary {
  final String id;
  final String title;
  final String description;
  final String trimesterScope;
  final String difficultyLevel;
  final int durationMinutes;
  final String? mediaUrl;
  final String? safetyWarning;
  final bool supportsPostureAnalysis;

  const ExerciseSummary({
    required this.id,
    required this.title,
    required this.description,
    required this.trimesterScope,
    required this.difficultyLevel,
    required this.durationMinutes,
    this.mediaUrl,
    this.safetyWarning,
    required this.supportsPostureAnalysis,
  });

  factory ExerciseSummary.fromJson(Map<String, dynamic> json) {
    return ExerciseSummary(
      id: json['exerciseId'] as String,
      title: json['title'] as String? ?? '',
      description: json['description'] as String? ?? '',
      trimesterScope: json['trimesterScope'] as String? ?? '',
      difficultyLevel: json['difficultyLevel'] as String? ?? '',
      durationMinutes: (json['durationMinutes'] as num?)?.toInt() ?? 0,
      mediaUrl: json['mediaUrl'] as String?,
      safetyWarning: json['safetyWarning'] as String?,
      supportsPostureAnalysis:
          json['supportsPostureAnalysis'] as bool? ?? false,
    );
  }
}

class ExerciseDetail {
  final String id;
  final String title;
  final String description;
  final String trimesterScope;
  final String difficultyLevel;
  final int durationMinutes;
  final String instructionContent;
  final String? mediaUrl;
  final String? safetyWarning;
  final bool supportsPostureAnalysis;

  const ExerciseDetail({
    required this.id,
    required this.title,
    required this.description,
    required this.trimesterScope,
    required this.difficultyLevel,
    required this.durationMinutes,
    required this.instructionContent,
    this.mediaUrl,
    this.safetyWarning,
    required this.supportsPostureAnalysis,
  });

  factory ExerciseDetail.fromJson(Map<String, dynamic> json) {
    return ExerciseDetail(
      id: json['exerciseId'] as String,
      title: json['title'] as String? ?? '',
      description: json['description'] as String? ?? '',
      trimesterScope: json['trimesterScope'] as String? ?? '',
      difficultyLevel: json['difficultyLevel'] as String? ?? '',
      durationMinutes: (json['durationMinutes'] as num?)?.toInt() ?? 0,
      instructionContent: json['instructionContent'] as String? ?? '',
      mediaUrl: json['mediaUrl'] as String?,
      safetyWarning: json['safetyWarning'] as String?,
      supportsPostureAnalysis:
          json['supportsPostureAnalysis'] as bool? ?? false,
    );
  }

  /// Local demo media takes precedence for the two supported exercises.
  /// Plank and Squat intentionally have no detail media yet.
  String? get detailMediaUrl => ExerciseMediaAssets.forExercise(title);

  List<String> get instructionSteps {
    final source = instructionContent.trim().isNotEmpty
        ? instructionContent.trim()
        : description.trim();
    if (source.isEmpty) return const [];

    final lines = source
        .split(RegExp(r'(?:\r?\n)+|(?<=[.!?])\s+'))
        .map(
          (step) => step.replaceFirst(RegExp(r'^\s*\d+[.)-]?\s*'), '').trim(),
        )
        .where((step) => step.isNotEmpty)
        .toList(growable: false);
    return lines.isEmpty ? [source] : lines;
  }
}

abstract final class ExerciseMediaAssets {
  static const bicepCurl = 'assets/exercises/bicep-curl.mp4';
  static const lunge = 'assets/exercises/lunge.mp4';

  static String? forExercise(String title) {
    final normalized = title
        .toLowerCase()
        .replaceAll(RegExp(r'[^a-z0-9\s]'), ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();

    if (normalized.contains('bicep curl')) return bicepCurl;
    if (normalized.contains('lunge')) return lunge;
    return null;
  }
}

class ExerciseSafetyCheck {
  final String id;
  final String exerciseId;
  final String resultStatus;
  final bool redFlagDetected;
  final String? blockedReason;

  const ExerciseSafetyCheck({
    required this.id,
    required this.exerciseId,
    required this.resultStatus,
    required this.redFlagDetected,
    this.blockedReason,
  });

  factory ExerciseSafetyCheck.fromJson(Map<String, dynamic> json) {
    return ExerciseSafetyCheck(
      id: json['safetyCheckId'] as String,
      exerciseId: json['exerciseId'] as String? ?? '',
      resultStatus: json['resultStatus'] as String? ?? '',
      redFlagDetected: json['redFlagDetected'] as bool? ?? false,
      blockedReason: json['blockedReason'] as String?,
    );
  }

  bool get isCleared =>
      resultStatus.toUpperCase() == 'CLEARED' && !redFlagDetected;
}

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
