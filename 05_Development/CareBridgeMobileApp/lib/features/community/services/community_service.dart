import '../../../core/network/api_client.dart';
import '../models/community_model.dart';

class CommunityService {
  static final CommunityService instance = CommunityService._();
  CommunityService._();

  Future<List<CommunityTopic>> getTopics({String? keyword}) async {
    final params = <String, String>{};
    if (keyword != null && keyword.isNotEmpty) params['keyword'] = keyword;
    final query = params.isEmpty ? '' : '?${params.entries.map((e) => '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}').join('&')}';
    final json = await apiGet('/api/v1/community/topics$query');
    final list = json['data'] as List? ?? [];
    return list.map((e) => CommunityTopic.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<CommunityFeedItem>> getFeed({String? topicId, int page = 0, int size = 20}) async {
    final params = <String, String>{'page': '$page', 'size': '$size'};
    if (topicId != null) params['topicId'] = topicId;
    final query = params.entries.map((e) => '${e.key}=${e.value}').join('&');
    final json = await apiGet('/api/v1/community/feed?$query');
    final content = json['data'] as List? ?? [];
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
    final content = json['data'] as List? ?? [];
    return content.map((e) => CommunityFeedItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  // UC-199: Get question detail with answers
  Future<QuestionDetail> getQuestionDetail(String questionId) async {
    final json = await apiGet('/api/v1/community/questions/$questionId');
    return QuestionDetail.fromJson(json['data'] as Map<String, dynamic>);
  }

  // UC-58: Toggle bookmark on a question
  Future<BookmarkToggleResult> toggleBookmark(String questionId) async {
    final json = await apiPost('/api/v1/community/questions/$questionId/bookmark', {});
    return BookmarkToggleResult.fromJson(json['data'] as Map<String, dynamic>);
  }

  // Toggle like on a question
  Future<QuestionLikeToggleResult> toggleQuestionLike(String questionId) async {
    final json = await apiPost('/api/v1/community/questions/$questionId/like', {});
    return QuestionLikeToggleResult.fromJson(json['data'] as Map<String, dynamic>);
  }

  // UC-59: Toggle like on an answer
  Future<LikeToggleResult> toggleAnswerLike(String answerId) async {
    final json = await apiPost('/api/v1/community/answers/$answerId/like', {});
    return LikeToggleResult.fromJson(json['data'] as Map<String, dynamic>);
  }

  // UC-55: Edit an existing question
  Future<void> editQuestion(String questionId, {String? title, String? body, bool? isAnonymous, String? urgency}) async {
    final request = <String, dynamic>{};
    if (title != null) request['title'] = title;
    if (body != null) request['body'] = body;
    if (isAnonymous != null) request['isAnonymous'] = isAnonymous;
    if (urgency != null) request['urgency'] = urgency;
    await apiPatch('/api/v1/community/questions/$questionId', request);
  }

  // UC-170: Soft-delete own question
  Future<void> deleteQuestion(String questionId) async {
    await apiDelete('/api/v1/community/questions/$questionId');
  }

  // UC-200: Edit own answer — status resets to PENDING for re-moderation
  Future<void> editAnswer(String questionId, String answerId, {required String body, required bool isPersonalExperience}) async {
    await apiPatch('/api/v1/community/questions/$questionId/answers/$answerId', {
      'body': body,
      'isPersonalExperience': isPersonalExperience,
    });
  }

  // UC-201: Soft-delete own answer
  Future<void> deleteAnswer(String questionId, String answerId) async {
    await apiDelete('/api/v1/community/questions/$questionId/answers/$answerId');
  }

  // UC-171: Toggle follow/unfollow on a community topic
  Future<bool> toggleFollowTopic(String topicId) async {
    final json = await apiPost('/api/v1/community/topics/$topicId/follow', {});
    return json['data']?['followed'] as bool? ?? false;
  }
}
