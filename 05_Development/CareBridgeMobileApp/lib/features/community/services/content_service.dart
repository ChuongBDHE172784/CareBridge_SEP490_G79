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
  static final ContentService instance = ContentService();

  ContentService({Future<dynamic> Function(String path)? getRequest})
    : _getRequest = getRequest ?? apiGet;

  final Future<dynamic> Function(String path) _getRequest;

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
    final query = params.entries
        .map(
          (entry) =>
              '${Uri.encodeComponent(entry.key)}=${Uri.encodeComponent(entry.value)}',
        )
        .join('&');
    final json = await _getRequest('/api/v1/content?$query');
    return PaginatedContent.fromApiResponse(
      Map<String, dynamic>.from(json as Map),
    ).data;
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
      final json = Map<String, dynamic>.from(
        await _getRequest('/api/v1/content?$query') as Map,
      );
      final result = PaginatedContent.fromApiResponse(json);
      final items = result.data;
      final previousItemCount = byId.length;
      for (final item in items) {
        byId[item.id] = item;
      }

      final isLastPage = result.totalPages > 0
          ? page + 1 >= result.totalPages
          : contentPageIsLast(
                  data: json,
                  page: page,
                  requestedPageSize: pageSize,
                  itemCount: items.length,
                ) ||
                byId.length == previousItemCount;
      if (isLastPage) break;
      page++;
    }
    return byId.values.toList(growable: false);
  }

  Future<ContentDetail> getContentDetail(String id) async {
    final json = await _getRequest('/api/v1/content/$id');
    return ContentDetail.fromJson(json['data'] as Map<String, dynamic>);
  }

  Future<LifecycleEnvelope<PaginatedContent>> getLifecycleContent({
    String? type,
    String? topicId,
    int page = 0,
    int size = 20,
  }) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (type != null) params['type'] = type;
    if (topicId != null) params['topicId'] = topicId;
    final query = params.entries
        .map(
          (entry) =>
              '${Uri.encodeComponent(entry.key)}=${Uri.encodeComponent(entry.value)}',
        )
        .join('&');
    final json = Map<String, dynamic>.from(
      await _getRequest('/api/v1/content/lifecycle?$query') as Map,
    );
    return LifecycleEnvelope<PaginatedContent>.fromApiResponse(
      json,
      (payload) =>
          PaginatedContent.fromJson(Map<String, dynamic>.from(payload as Map)),
    );
  }

  Future<LifecycleEnvelope<List<ContentListItem>>> getAllLifecycleContent({
    String? type,
    String? topicId,
    int pageSize = maxPageSize,
    bool Function()? shouldContinue,
  }) async {
    if (pageSize < 1 || pageSize > maxPageSize) {
      throw RangeError.range(pageSize, 1, maxPageSize, 'pageSize');
    }
    final byId = <String, ContentListItem>{};
    String? resolvedStage;
    var page = 0;

    while (true) {
      if (shouldContinue != null && !shouldContinue()) {
        throw StateError('Lifecycle request superseded');
      }
      final envelope = await getLifecycleContent(
        type: type,
        topicId: topicId,
        page: page,
        size: pageSize,
      );
      if (shouldContinue != null && !shouldContinue()) {
        throw StateError('Lifecycle request superseded');
      }

      resolvedStage ??= envelope.stage;
      if (envelope.stage != resolvedStage ||
          envelope.payload.data.any((item) => item.stage != resolvedStage)) {
        throw const FormatException('Lifecycle page stage mismatch');
      }
      if (envelope.payload.page != page) {
        throw const FormatException('Lifecycle page index mismatch');
      }

      final previousItemCount = byId.length;
      for (final item in envelope.payload.data) {
        byId[item.id] = item;
      }
      final totalPages = envelope.payload.totalPages;
      final isLastPage = totalPages > 0
          ? page + 1 >= totalPages
          : envelope.payload.data.length < pageSize ||
                byId.length == previousItemCount;
      if (isLastPage) break;
      page++;
    }

    return LifecycleEnvelope<List<ContentListItem>>(
      stage: resolvedStage,
      payload: byId.values.toList(growable: false),
    );
  }

  Future<LifecycleEnvelope<ContentDetail>> getLifecycleContentDetail(
    String id,
  ) async {
    final json = Map<String, dynamic>.from(
      await _getRequest('/api/v1/content/lifecycle/$id') as Map,
    );
    return LifecycleEnvelope<ContentDetail>.fromApiResponse(
      json,
      (payload) =>
          ContentDetail.fromJson(Map<String, dynamic>.from(payload as Map)),
    );
  }
}
