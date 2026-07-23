import '../../../core/network/api_client.dart';
import '../models/user_checklist_item_model.dart';

class UserChecklistService {
  UserChecklistService._();
  static final UserChecklistService instance = UserChecklistService._();

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
    required String journeyId,
  }) async {
    if (templateItemIds.isEmpty) return const [];
    final data = await apiPost('/api/v1/user-checklist-items/import', {
      'journeyId': journeyId,
      'templateItemIds': templateItemIds,
    });
    return (data['data'] as List? ?? [])
        .cast<Map<String, dynamic>>()
        .map(UserChecklistItem.fromJson)
        .toList(growable: false);
  }
}
