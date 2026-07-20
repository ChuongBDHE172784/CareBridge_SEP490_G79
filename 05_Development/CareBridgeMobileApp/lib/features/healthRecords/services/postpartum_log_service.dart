import '../../../core/network/api_client.dart';
import '../models/postpartum_log_model.dart';

class PostpartumLogService {
  Future<PostpartumLogPage> list(
    String journeyId, {
    int page = 0,
    int size = 20,
  }) async {
    final envelope =
        await apiGet(
              '/api/v1/postpartum-logs',
              queryParams: {'journeyId': journeyId, 'page': page, 'size': size},
            )
            as Map<String, dynamic>;
    final items = (envelope['data'] as List<dynamic>? ?? const [])
        .map((item) => PostpartumLog.fromJson(item as Map<String, dynamic>))
        .toList();
    return PostpartumLogPage(
      items: items,
      page: (envelope['page'] as num?)?.toInt() ?? page,
      totalPages: (envelope['totalPages'] as num?)?.toInt() ?? 0,
      totalElements:
          (envelope['totalElements'] as num?)?.toInt() ?? items.length,
    );
  }

  Future<PostpartumLog> detail(String logId) async {
    final envelope =
        await apiGet('/api/v1/postpartum-logs/$logId') as Map<String, dynamic>;
    return PostpartumLog.fromJson(envelope['data'] as Map<String, dynamic>);
  }

  Future<PostpartumLog> create(
    String journeyId,
    PostpartumLogDraft draft,
  ) async {
    final envelope =
        await apiPost(
              '/api/v1/journeys/$journeyId/postpartum-logs',
              draft.toJson(),
            )
            as Map<String, dynamic>;
    return PostpartumLog.fromJson(envelope['data'] as Map<String, dynamic>);
  }

  Future<PostpartumLog> update(String logId, PostpartumLogDraft draft) async {
    final envelope =
        await apiPatch(
              '/api/v1/postpartum-logs/$logId',
              draft.toJson(
                includeSubmissionId: false,
                includeEmptyOptionals: true,
              ),
            )
            as Map<String, dynamic>;
    return PostpartumLog.fromJson(envelope['data'] as Map<String, dynamic>);
  }

  Future<void> delete(String logId) =>
      apiDelete('/api/v1/postpartum-logs/$logId');
}
