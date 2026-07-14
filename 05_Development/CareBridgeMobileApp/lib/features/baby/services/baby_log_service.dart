import '../../../core/network/api_client.dart';
import '../models/baby_daily_log_model.dart';
import '../models/milestone_model.dart';

class BabyLogService {
  Future<BabyDailyLog> getDailyLogDetail(String babyId, String logId) async {
    final data = await apiGet('/api/v1/babies/$babyId/daily-logs/$logId');
    return BabyDailyLog.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-35: Update a baby daily log
  Future<BabyDailyLog> updateDailyLog(
    String babyId,
    String logId,
    UpdateBabyDailyLogRequest request,
  ) async {
    final data = await apiPut(
      '/api/v1/babies/$babyId/daily-logs/$logId',
      request.toJson(),
    );
    return BabyDailyLog.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-35: Delete a baby daily log
  Future<void> deleteDailyLog(String babyId, String logId) async {
    await apiDelete('/api/v1/babies/$babyId/daily-logs/$logId');
  }

  // UC-36: Get daily log summary for a baby
  Future<BabyLogSummaryResponse> getLogSummary(
    String babyId, {
    String period = '24h',
  }) async {
    final data = await apiGet(
      '/api/v1/babies/$babyId/daily-logs/summary?period=$period',
    );
    return BabyLogSummaryResponse.fromJson(
      data['data'] as Map<String, dynamic>,
    );
  }

  // UC-37: Record a development milestone
  Future<Milestone> addMilestone(
    String babyId,
    AddMilestoneRequest request,
  ) async {
    final data = await apiPost(
      '/api/v1/babies/$babyId/milestones',
      request.toJson(),
    );
    return Milestone.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<Milestone> updateMilestone(
    String babyId,
    String milestoneId, {
    DateTime? achievedDate,
    String? note,
    String? status,
  }) async {
    final body = <String, dynamic>{};
    if (achievedDate != null) {
      final d = achievedDate;
      body['achievedDate'] =
          '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    }
    if (note != null) body['note'] = note;
    if (status != null) body['status'] = status;

    final data = await apiPatch(
      '/api/v1/babies/$babyId/milestones/$milestoneId',
      body,
    );
    return Milestone.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<void> deleteMilestone(String babyId, String milestoneId) async {
    await apiDelete('/api/v1/babies/$babyId/milestones/$milestoneId');
  }
}
