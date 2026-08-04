import '../../../core/network/api_client.dart';
import '../models/checklist_history_model.dart';

typedef ChecklistHistoryGetRequest =
    Future<dynamic> Function(String path, {Map<String, dynamic>? queryParams});

class ChecklistHistoryService {
  ChecklistHistoryService({ChecklistHistoryGetRequest? getRequest})
    : _getRequest =
          getRequest ??
          ((path, {queryParams}) => apiGet(path, queryParams: queryParams));

  static final ChecklistHistoryService instance = ChecklistHistoryService();

  final ChecklistHistoryGetRequest _getRequest;

  Future<ChecklistHistoryPage> loadHistory({
    int page = 0,
    int size = 20,
    ChecklistHistoryTargetSubject? targetSubject,
    String? careGroupId,
  }) async {
    final path = careGroupId == null
        ? '/api/v1/checklists/history'
        : '/api/v1/care-groups/$careGroupId/checklists/history';
    final envelope = await _getRequest(
      path,
      queryParams: {
        'page': page,
        'size': size,
        if (targetSubject != null) 'targetSubject': targetSubject.apiValue,
      },
    );
    final Map<String, dynamic> raw = envelope is Map
        ? Map<String, dynamic>.from(envelope)
        : <String, dynamic>{};
    final Object? payload = raw['data'];
    final Map<String, dynamic> data = payload is Map
        ? Map<String, dynamic>.from(payload)
        : raw;
    return ChecklistHistoryPage.fromJson(data);
  }
}
