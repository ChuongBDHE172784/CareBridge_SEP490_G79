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

  Future<List<CommunityFeedItem>> searchQuestions({
    String? keyword,
    String? stage,
    String? topicId,
    bool? hasExpertAnswer,
    int page = 0,
    int size = 20,
  }) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (keyword != null && keyword.isNotEmpty) params['keyword'] = keyword;
    if (stage != null) params['stage'] = stage;
    if (topicId != null) params['topicId'] = topicId;
    if (hasExpertAnswer != null) params['hasExpertAnswer'] = '$hasExpertAnswer';
    final query = params.entries.map((e) => '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}').join('&');
    final json = await apiGet('/api/v1/community/questions?$query');
    final content = json['data']?['content'] as List? ?? [];
    return content.map((e) => CommunityFeedItem.fromJson(e as Map<String, dynamic>)).toList();
  }
}
