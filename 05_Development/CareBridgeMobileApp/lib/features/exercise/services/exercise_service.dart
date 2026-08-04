import '../../../core/network/api_client.dart';
import '../models/exercise_model.dart';
import '../models/posture_event_model.dart';

class ExerciseService {
  static final ExerciseService instance = ExerciseService._();

  ExerciseService._() : _post = _defaultPost;

  /// Test seam for request-contract tests without changing the shared client.
  ExerciseService.forTesting({
    required Future<dynamic> Function(String path, Map<String, dynamic> body)
    post,
  }) : _post = post;

  final Future<dynamic> Function(String path, Map<String, dynamic> body) _post;

  static Future<dynamic> _defaultPost(String path, Map<String, dynamic> body) =>
      apiPost(path, body);

  Future<List<ExerciseSummary>> getExercises({
    String? trimester,
    String? difficulty,
    int page = 0,
    int size = 20,
  }) async {
    final query = <String>[
      'page=$page',
      'size=$size',
      if (trimester != null && trimester.isNotEmpty) 'trimester=$trimester',
      if (difficulty != null && difficulty.isNotEmpty) 'difficulty=$difficulty',
    ].join('&');
    final res = await apiGet('/api/v1/exercises?$query');
    final raw = res['data'];
    final list = raw is List
        ? raw
        : raw is Map<String, dynamic>
        ? raw['content'] as List<dynamic>? ?? const []
        : const [];
    return list
        .map((e) => ExerciseSummary.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ExerciseDetail> getExerciseDetail(String exerciseId) async {
    final res = await apiGet('/api/v1/exercises/$exerciseId');
    return ExerciseDetail.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<ExerciseSafetyCheck> submitSafetyCheck(
    String exerciseId, {
    String? notes,
  }) async {
    final res = await apiPost('/api/v1/exercises/$exerciseId/safety-check', {
      'q1NoDizziness': true,
      'q2NoContractions': true,
      'q3NoBleeding': true,
      'q4HydratedAndFed': true,
      if (notes != null && notes.isNotEmpty) 'notes': notes,
    });
    return ExerciseSafetyCheck.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<ExerciseSession> startSession(
    String exerciseId,
    String safetyCheckId,
  ) async {
    final res = await apiPost('/api/v1/exercises/$exerciseId/sessions', {
      'safetyCheckId': safetyCheckId,
    });
    return ExerciseSession.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<void> pauseSession(String sessionId) async {
    await apiPatch('/api/v1/exercises/sessions/$sessionId/pause', {});
  }

  Future<void> resumeSession(String sessionId) async {
    await apiPatch('/api/v1/exercises/sessions/$sessionId/resume', {});
  }

  /// Sends one normalized landmark sample to Spring.
  ///
  /// The caller is responsible for sampling camera frames. A realtime source
  /// should use [PostureEventStreamer] so only one request is in flight and
  /// stale frames are dropped while Spring or the sidecar is busy.
  Future<PostureFeedback> analyzePostureEvent({
    required String sessionId,
    required int eventTimeMs,
    required Map<String, PostureLandmark> landmarks,
  }) async {
    if (sessionId.isEmpty) {
      throw ArgumentError.value(sessionId, 'sessionId', 'must not be empty');
    }
    if (eventTimeMs < 0) {
      throw ArgumentError.value(
        eventTimeMs,
        'eventTimeMs',
        'must be non-negative',
      );
    }
    if (landmarks.isEmpty) {
      throw ArgumentError.value(landmarks, 'landmarks', 'must not be empty');
    }

    final response = await _post(
      '/api/v1/exercises/sessions/$sessionId/posture-events',
      <String, dynamic>{
        'eventTimeMs': eventTimeMs,
        'keypointSummaryJson': landmarks.map(
          (name, point) => MapEntry(name, point.toJson()),
        ),
      },
    );
    final data = response is Map<String, dynamic> ? response['data'] : null;
    if (data is! Map<String, dynamic>) {
      throw const FormatException('Posture response data is missing');
    }
    return PostureFeedback.fromJson(data);
  }

  Future<SessionResult> completeSession(String sessionId) async {
    final res = await apiPatch(
      '/api/v1/exercises/sessions/$sessionId/complete',
      {},
    );
    return SessionResult.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<SessionResult> getSessionResult(String sessionId) async {
    final res = await apiGet('/api/v1/exercises/sessions/$sessionId/result');
    return SessionResult.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<List<ExerciseHistoryItem>> getHistory({
    String? trimesterScope,
    int page = 0,
    int size = 20,
  }) async {
    var path = '/api/v1/exercises/sessions/history?page=$page&size=$size';
    if (trimesterScope != null && trimesterScope.isNotEmpty) {
      path += '&trimesterScope=$trimesterScope';
    }
    final res = await apiGet(path);
    final data = res['data'] as Map<String, dynamic>?;
    if (data == null) return [];
    final content = data['content'] as List<dynamic>? ?? [];
    return content
        .map((e) => ExerciseHistoryItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
