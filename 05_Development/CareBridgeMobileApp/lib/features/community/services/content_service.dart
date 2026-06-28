import '../../../core/network/api_client.dart';
import '../models/content_model.dart';

class ContentService {
  static final ContentService instance = ContentService._();
  ContentService._();

  Future<List<ContentListItem>> getContent({String? type, String? stage, String? topicId, int page = 0, int size = 10}) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (type != null) params['type'] = type;
    if (stage != null) params['stage'] = stage;
    if (topicId != null) params['topicId'] = topicId;
    final query = params.entries.map((e) => '${e.key}=${e.value}').join('&');
    final json = await apiGet('/api/v1/content?$query');
    final content = json['data']?['content'] as List? ?? [];
    return content.map((e) => ContentListItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<ContentDetail> getContentDetail(String id) async {
    final json = await apiGet('/api/v1/content/$id');
    return ContentDetail.fromJson(json['data'] as Map<String, dynamic>);
  }

  Future<List<ContentListItem>> searchContent(String keyword, {String? type, String? stage, int page = 0}) async {
    final params = <String, String>{'keyword': keyword, 'page': '$page', 'size': '10'};
    if (type != null) params['type'] = type;
    if (stage != null) params['stage'] = stage;
    final query = params.entries.map((e) => '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}').join('&');
    final json = await apiGet('/api/v1/content/search?$query');
    final content = json['data']?['content'] as List? ?? [];
    return content.map((e) => ContentListItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<ChecklistTemplate>> getChecklists({String? stage}) async {
    final q = stage != null ? '?stage=$stage' : '';
    final json = await apiGet('/api/v1/content/checklists$q');
    final list = json['data'] as List? ?? [];
    return list.map((e) => ChecklistTemplate.fromJson(e as Map<String, dynamic>)).toList();
  }
}
