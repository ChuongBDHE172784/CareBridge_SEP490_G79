import '../../../core/network/api_client.dart';
import '../models/user_checklist_item_model.dart';

typedef ChecklistPostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);

class UserChecklistService {
  UserChecklistService({ChecklistPostRequest? postRequest})
    : _postRequest = postRequest ?? apiPost;

  static final UserChecklistService instance = UserChecklistService();

  final ChecklistPostRequest _postRequest;

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
    ChecklistCategory category = ChecklistCategory.general,
    String? journeyId,
    String? babyId,
    int? itemOrder,
  }) async {
    final data = await apiPost('/api/v1/user-checklist-items', {
      'itemText': itemText,
      'category': category.apiValue,
      if (journeyId != null && journeyId.isNotEmpty) 'journeyId': journeyId,
      if (babyId != null && babyId.isNotEmpty) 'babyId': babyId,
      'itemOrder': ?itemOrder,
    });
    return UserChecklistItem.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<UserChecklistItem> toggleComplete(String itemId) async {
    final data = await apiPatch(
      '/api/v1/user-checklist-items/$itemId/toggle',
      {},
    );
    return UserChecklistItem.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<List<UserChecklistItem>> importFromTemplate({
    required List<String> templateItemIds,
    String? journeyId,
    String? babyId,
  }) async {
    final normalizedJourneyId = journeyId == null || journeyId.isEmpty
        ? null
        : journeyId;
    final normalizedBabyId = babyId == null || babyId.isEmpty ? null : babyId;
    if (normalizedJourneyId != null && normalizedBabyId != null) {
      throw ArgumentError('journeyId and babyId are mutually exclusive');
    }
    if (templateItemIds.isEmpty) return const [];
    final data = await _postRequest('/api/v1/user-checklist-items/import', {
      'journeyId': ?normalizedJourneyId,
      'babyId': ?normalizedBabyId,
      'templateItemIds': templateItemIds,
    });
    return (data['data'] as List? ?? [])
        .cast<Map<String, dynamic>>()
        .map(UserChecklistItem.fromJson)
        .toList(growable: false);
  }
}
