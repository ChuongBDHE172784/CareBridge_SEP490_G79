import '../../../core/network/api_client.dart';
import '../models/conversation_call.dart';
import '../models/direct_conversation.dart';
import '../models/timeline_item.dart';
import '../models/timeline_page.dart';

class DirectChatService {
  static final DirectChatService instance = DirectChatService._();
  DirectChatService._();

  Future<DirectConversation> findOrCreateConversation(String expertProfileId) async {
    final response = await apiPost('/api/v1/direct-conversations/expert/$expertProfileId', {});
    return DirectConversation.fromJson(response['data'] as Map<String, dynamic>);
  }

  Future<List<DirectConversationSummary>> listMyConversations() async {
    final response = await apiGet('/api/v1/direct-conversations');
    final rows = response['data'] as List? ?? [];
    return rows
        .cast<Map<String, dynamic>>()
        .map(DirectConversationSummary.fromJson)
        .toList();
  }

  Future<DirectConversation> getConversation(String conversationId) async {
    final response = await apiGet('/api/v1/direct-conversations/$conversationId');
    return DirectConversation.fromJson(response['data'] as Map<String, dynamic>);
  }

  /// Idempotent under retry with the same [clientMessageId] (BR-DCC-005) — the response
  /// shape is identical whether the server created a new row (201) or returned an existing
  /// one (200); [ApiClient] doesn't currently surface the raw status code, and the caller
  /// doesn't need it: reconciling the optimistic entry works the same either way.
  Future<TimelineItem> sendMessage(
    String conversationId, {
    required String clientMessageId,
    required String messageBody,
  }) async {
    final response = await apiPost(
      '/api/v1/direct-conversations/$conversationId/messages',
      {'clientMessageId': clientMessageId, 'messageBody': messageBody},
    );
    return TimelineItem.fromJson(response['data'] as Map<String, dynamic>);
  }

  Future<TimelinePage> getTimeline(
    String conversationId, {
    String? after,
    String? before,
    int limit = 30,
  }) async {
    assert(after == null || before == null, 'after and before are mutually exclusive');
    final response = await apiGet(
      '/api/v1/direct-conversations/$conversationId/timeline',
      queryParams: {
        if (after != null) 'after': after,
        if (before != null) 'before': before,
        'limit': limit,
      },
    );
    return TimelinePage.fromJson(response['data'] as Map<String, dynamic>);
  }

  Future<ConversationCall> initiateCall(String conversationId, {required String callType}) async {
    final response = await apiPost(
      '/api/v1/direct-conversations/$conversationId/calls',
      {'callType': callType},
    );
    return ConversationCall.fromJson(response['data'] as Map<String, dynamic>);
  }

  Future<ConversationCall> markRinging(String conversationId, String callId) =>
      _patchCall(conversationId, callId, 'ringing');

  Future<ConversationCall> answerCall(String conversationId, String callId) =>
      _patchCall(conversationId, callId, 'answer');

  Future<ConversationCall> declineCall(String conversationId, String callId) =>
      _patchCall(conversationId, callId, 'decline');

  Future<ConversationCall> endCall(String conversationId, String callId) =>
      _patchCall(conversationId, callId, 'end');

  Future<ConversationCall> _patchCall(String conversationId, String callId, String action) async {
    final response = await apiPatch(
      '/api/v1/direct-conversations/$conversationId/calls/$callId/$action',
      {},
    );
    return ConversationCall.fromJson(response['data'] as Map<String, dynamic>);
  }
}
