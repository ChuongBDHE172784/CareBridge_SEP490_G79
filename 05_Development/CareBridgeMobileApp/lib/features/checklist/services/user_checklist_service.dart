import '../../../core/network/api_client.dart';
import '../models/user_checklist_item_model.dart';
import 'checklist_assignment_refresh_bus.dart';

typedef ChecklistPostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef ChecklistPostV2Request =
    Future<dynamic> Function(
      String path,
      Map<String, dynamic> body,
      Map<String, String> headers,
    );
typedef ChecklistDeleteRequest = Future<dynamic> Function(String path);

class ChecklistTemplateAssignmentResult {
  const ChecklistTemplateAssignmentResult({
    required this.createdTasks,
    required this.existingTasks,
    required this.deniedRecipients,
    required this.conflicts,
    required this.failures,
  });

  final int createdTasks;
  final int existingTasks;
  final int deniedRecipients;
  final int conflicts;
  final int failures;

  bool get hasAssignedTasks => createdTasks + existingTasks > 0;

  factory ChecklistTemplateAssignmentResult.fromJson(
    Map<String, dynamic> json,
  ) => ChecklistTemplateAssignmentResult(
    createdTasks: (json['createdTasks'] as num?)?.toInt() ?? 0,
    existingTasks: (json['existingTasks'] as num?)?.toInt() ?? 0,
    deniedRecipients: (json['deniedRecipients'] as num?)?.toInt() ?? 0,
    conflicts: (json['conflicts'] as num?)?.toInt() ?? 0,
    failures: (json['failures'] as num?)?.toInt() ?? 0,
  );
}

class UserChecklistService {
  UserChecklistService({
    ChecklistPostRequest? postRequest,
    ChecklistPostV2Request? postV2Request,
    ChecklistDeleteRequest? deleteRequest,
  }) : _postRequest = postRequest ?? apiPost,
       _postV2Request =
           postV2Request ??
           (postRequest == null
               ? ((path, body, headers) =>
                     apiPost(path, body, extraHeaders: headers))
               : ((path, body, headers) => postRequest(path, body))),
       _deleteRequest = deleteRequest ?? ((path) => apiDelete(path));

  static final UserChecklistService instance = UserChecklistService();

  final ChecklistPostRequest _postRequest;
  final ChecklistPostV2Request _postV2Request;
  final ChecklistDeleteRequest _deleteRequest;

  Future<void> deleteItem(String itemId) async {
    await _deleteRequest('/api/v1/user-checklist-items/$itemId');
    ChecklistAssignmentRefreshBus.notify();
  }

  Future<List<UserChecklistItem>> listItems({
    String? journeyId,
    String? babyId,
  }) async {
    final query = <String, dynamic>{
      if (journeyId != null && journeyId.isNotEmpty) 'journeyId': journeyId,
      if (babyId != null && babyId.isNotEmpty) 'babyId': babyId,
    };
    final data = await apiGet(
      '/api/v1/user-checklist-items',
      queryParams: query.isEmpty ? null : query,
    );
    final items =
        (data['data'] as List? ?? [])
            .cast<Map<String, dynamic>>()
            .map(UserChecklistItem.fromJson)
            .toList()
          ..sort((a, b) {
            final categoryCompare = a.category.apiValue.compareTo(
              b.category.apiValue,
            );
            if (categoryCompare != 0) return categoryCompare;
            return a.itemOrder.compareTo(b.itemOrder);
          });
    return items;
  }

  Future<UserChecklistItem> addItem({
    required String itemText,
    required String targetSubject,
    required String clientTaskId,
    ChecklistCategory category = ChecklistCategory.general,
    String? journeyId,
    String? babyId,
    String? careGroupId,
    int? itemOrder,
  }) async {
    final data = await _postRequest('/api/v1/user-checklist-items', {
      'itemText': itemText,
      'targetSubject': targetSubject,
      'clientTaskId': clientTaskId,
      'category': category.apiValue,
      if (journeyId != null && journeyId.isNotEmpty) 'journeyId': journeyId,
      if (babyId != null && babyId.isNotEmpty) 'babyId': babyId,
      if (careGroupId != null && careGroupId.isNotEmpty)
        'careGroupId': careGroupId,
      'itemOrder': ?itemOrder,
    });
    final item = UserChecklistItem.fromJson(
      data['data'] as Map<String, dynamic>,
    );
    ChecklistAssignmentRefreshBus.notify();
    return item;
  }

  /// Creates a targetless user-created task using the explicit V2 wire
  /// contract. The existing [addItem] method remains the V1 compatibility
  /// adapter for callers that still provide a target subject.
  Future<UserChecklistItem> addItemV2({
    required String itemText,
    required String clientTaskId,
    ChecklistCategory category = ChecklistCategory.general,
    String? journeyId,
    String? babyId,
    String? careGroupId,
    int? itemOrder,
  }) async {
    final data = await _postV2Request(
      '/api/v1/user-checklist-items',
      {
        'itemText': itemText,
        'clientTaskId': clientTaskId,
        'category': category.apiValue,
        if (journeyId != null && journeyId.isNotEmpty) 'journeyId': journeyId,
        if (babyId != null && babyId.isNotEmpty) 'babyId': babyId,
        if (careGroupId != null && careGroupId.isNotEmpty)
          'careGroupId': careGroupId,
        'itemOrder': ?itemOrder,
      },
      const {'X-Checklist-Contract-Version': '2'},
    );
    final item = UserChecklistItem.fromJson(
      data['data'] as Map<String, dynamic>,
    );
    ChecklistAssignmentRefreshBus.notify();
    return item;
  }

  Future<ChecklistTemplateAssignmentResult> addTemplate({
    required String templateId,
    String? journeyId,
    String? babyId,
  }) async {
    final payload =
        await _postRequest('/api/v1/user-checklist-items/from-template', {
          'templateId': templateId,
          if (journeyId != null && journeyId.isNotEmpty) 'journeyId': journeyId,
          if (babyId != null && babyId.isNotEmpty) 'babyId': babyId,
        });
    final envelope = Map<String, dynamic>.from(payload as Map);
    final raw = envelope['data'] is Map ? envelope['data'] as Map : envelope;
    final result = ChecklistTemplateAssignmentResult.fromJson(
      Map<String, dynamic>.from(raw),
    );
    if (!result.hasAssignedTasks ||
        result.deniedRecipients > 0 ||
        result.conflicts > 0 ||
        result.failures > 0) {
      throw const FormatException(
        'Checklist assignment did not create or find usable tasks',
      );
    }
    ChecklistAssignmentRefreshBus.notify();
    return result;
  }
}
