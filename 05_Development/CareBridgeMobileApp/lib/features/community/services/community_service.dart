import '../../../core/network/api_client.dart';
import '../models/community_model.dart';

class CommunityService {
  static final CommunityService instance = CommunityService._();
  CommunityService._();

  Future<List<CommunityTopic>> getTopics() async {
    final json = await apiGet('/api/v1/community/topics');
    final list = json['data'] as List? ?? [];
    return list.map((e) => CommunityTopic.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<CommunityFeedItem>> getFeed({String? topicId, int page = 0, int size = 20}) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (topicId != null) params['topicId'] = topicId;
    final query = params.entries.map((e) => '${e.key}=${e.value}').join('&');
    final json = await apiGet('/api/v1/community/feed?$query');
    final content = json['data']?['content'] as List? ?? [];
    return content.map((e) => CommunityFeedItem.fromJson(e as Map<String, dynamic>)).toList();
  }
}
