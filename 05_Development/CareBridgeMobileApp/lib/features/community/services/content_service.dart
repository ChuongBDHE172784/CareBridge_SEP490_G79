import '../../../core/network/api_client.dart';
import '../models/content_model.dart';

class ContentService {
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
    final query = params.entries.map((e) => '${e.key}=${e.value}').join('&');
    final json = await _getRequest('/api/v1/content?$query');
    return PaginatedContent.fromApiResponse(
      Map<String, dynamic>.from(json as Map),
    ).data;
  }

  Future<ContentDetail> getContentDetail(String id) async {
    final json = await _getRequest('/api/v1/content/$id');
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
    final json = await _getRequest('/api/v1/content/search?$query');
    return PaginatedContent.fromApiResponse(
      Map<String, dynamic>.from(json as Map),
    ).data;
  }

  Future<List<ChecklistTemplate>> getChecklists({String? stage}) async {
    final q = stage != null ? '?stage=$stage' : '';
    final json = await _getRequest('/api/v1/content/checklists$q');
    final list = json['data'] as List? ?? [];
    return list
        .map((e) => ChecklistTemplate.fromJson(e as Map<String, dynamic>))
        .toList();
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

  Future<LifecycleEnvelope<List<ChecklistTemplate>>>
  getLifecycleChecklists() async {
    final json = Map<String, dynamic>.from(
      await _getRequest('/api/v1/content/lifecycle/checklists') as Map,
    );
    return LifecycleEnvelope<List<ChecklistTemplate>>.fromApiResponse(
      json,
      (payload) => (payload as List)
          .map(
            (item) => ChecklistTemplate.fromJson(
              Map<String, dynamic>.from(item as Map),
            ),
          )
          .toList(growable: false),
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
