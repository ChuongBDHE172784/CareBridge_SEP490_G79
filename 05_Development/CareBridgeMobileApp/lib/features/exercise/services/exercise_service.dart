import '../../../core/network/api_client.dart';
import '../models/exercise_model.dart';

class ExerciseService {
  static final ExerciseService instance = ExerciseService._();
  ExerciseService._();

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
