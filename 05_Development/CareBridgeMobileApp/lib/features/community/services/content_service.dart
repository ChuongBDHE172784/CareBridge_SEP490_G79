import '../../../core/network/api_client.dart';
import '../models/content_model.dart';

List<ContentListItem> contentItemsFromApiData(dynamic data) {
  final rawItems = data is List
      ? data
      : (data as Map<String, dynamic>?)?['content'] as List? ?? const [];
  return rawItems
      .map((item) => ContentListItem.fromJson(item as Map<String, dynamic>))
      .toList(growable: false);
}

bool contentPageIsLast({
  required dynamic data,
  required int page,
  required int requestedPageSize,
  required int itemCount,
}) {
  if (itemCount == 0) return true;
  if (data is Map<String, dynamic>) {
    if (data['last'] == true) return true;
    final totalPages = data['totalPages'] as int?;
    if (totalPages != null) return page + 1 >= totalPages;
  }
  return itemCount < requestedPageSize;
}

class ContentService {
  static const maxPageSize = 50;
  static final ContentService instance = ContentService._();
  ContentService._();

  Future<List<ContentListItem>> getContent({
    String? type,
    String? stage,
    String? topicId,
    int page = 0,
    int size = 10,
  }) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (type != null) params['type'] = type;
    if (stage != null) params['stage'] = stage;
    if (topicId != null) params['topicId'] = topicId;
    final query = params.entries.map((e) => '${e.key}=${e.value}').join('&');
    final json = await apiGet('/api/v1/content?$query');
    return contentItemsFromApiData(json['data']);
  }

  Future<List<ContentListItem>> getAllContent({
    String? type,
    String? stage,
    String? topicId,
  }) async {
    const pageSize = maxPageSize;
    final byId = <String, ContentListItem>{};
    var page = 0;

    while (true) {
      final params = <String, String>{'page': '$page', 'size': '$pageSize'};
      if (type != null) params['type'] = type;
      if (stage != null) params['stage'] = stage;
      if (topicId != null) params['topicId'] = topicId;
      final query = params.entries
          .map(
            (entry) =>
                '${Uri.encodeComponent(entry.key)}=${Uri.encodeComponent(entry.value)}',
          )
          .join('&');
      final json = await apiGet('/api/v1/content?$query');
      final data = json['data'];
      final items = contentItemsFromApiData(data);
      for (final item in items) {
        byId[item.id] = item;
      }

      if (contentPageIsLast(
        data: data,
        page: page,
        requestedPageSize: pageSize,
        itemCount: items.length,
      )) {
        break;
      }
      page++;
    }

    return byId.values.toList(growable: false);
  }

  Future<ContentDetail> getContentDetail(String id) async {
    final json = await apiGet('/api/v1/content/$id');
    return ContentDetail.fromJson(json['data'] as Map<String, dynamic>);
  }

  Future<List<ContentListItem>> searchContent(
    String keyword, {
    String? type,
    String? stage,
    int page = 0,
  }) async {
    final params = <String, String>{
      'keyword': keyword,
      'page': '$page',
      'size': '10',
    };
    if (type != null) params['type'] = type;
    if (stage != null) params['stage'] = stage;
    final query = params.entries
        .map(
          (e) =>
              '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}',
        )
        .join('&');
    final json = await apiGet('/api/v1/content/search?$query');
    return contentItemsFromApiData(json['data']);
  }

  Future<List<ChecklistTemplate>> getChecklists({String? stage}) async {
    final q = stage != null ? '?stage=$stage' : '';
    final json = await apiGet('/api/v1/content/checklists$q');
    final list = json['data'] as List? ?? [];
    return list
        .map((e) => ChecklistTemplate.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
