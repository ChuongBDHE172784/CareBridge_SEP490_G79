import '../../../core/network/api_client.dart';
import '../models/conversation_call.dart';
import '../models/direct_conversation.dart';
import '../models/expert_directory_item.dart';
import '../models/timeline_item.dart';
import '../models/timeline_page.dart';

class DirectChatService {
  // Mutable (not `instance`-final) so widget tests can swap in a fake subclass; the class
  // itself is stateless (a thin HTTP wrapper), so a non-private constructor is safe to expose.
  static DirectChatService instance = DirectChatService();

  Future<Map<String, dynamic>> getExpertProfile(String expertProfileId) async {
    final response = await apiGet('/api/v1/expert/profiles/$expertProfileId');
    return response['data'] as Map<String, dynamic>;
  }

  Future<DirectConversation> findOrCreateConversation(
    String expertProfileId,
  ) async {
    final response = await apiPost(
      '/api/v1/direct-conversations/expert/$expertProfileId',
      {},
    );
    return DirectConversation.fromJson(
      response['data'] as Map<String, dynamic>,
    );
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
    final response = await apiGet(
      '/api/v1/direct-conversations/$conversationId',
    );
    return DirectConversation.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }

  /// Idempotent under retry with the same [clientMessageId] (BR-DCC-005) — the response
  /// shape is identical whether the server created a new row (201) or returned an existing
  /// one (200); [ApiClient] doesn't currently surface the raw status code, and the caller
  /// doesn't need it: reconciling the optimistic entry works the same either way.
  Future<TimelineItem> sendMessage(
    String conversationId, {
    required String clientMessageId,
    String? messageBody,
    String messageType = 'TEXT',
    String? attachmentId,
  }) async {
    final response =
        await apiPost('/api/v1/direct-conversations/$conversationId/messages', {
          'clientMessageId': clientMessageId,
          if (messageBody != null) 'messageBody': messageBody,
          'messageType': messageType,
          if (attachmentId != null) 'attachmentId': attachmentId,
        });
    return TimelineItem.fromJson(response['data'] as Map<String, dynamic>);
  }

  Future<TimelinePage> getTimeline(
    String conversationId, {
    String? after,
    String? before,
    int limit = 30,
  }) async {
    assert(
      after == null || before == null,
      'after and before are mutually exclusive',
    );
    final response = await apiGet(
      '/api/v1/direct-conversations/$conversationId/timeline',
      queryParams: {'after': ?after, 'before': ?before, 'limit': limit},
    );
    return TimelinePage.fromJson(response['data'] as Map<String, dynamic>);
  }

  /// ADR-MEDI-003 §9.4 — [lastSeenMessageId] must be the id of the newest MESSAGE item the
  /// client has actually rendered, never a server-side "latest" guess. Idempotent, safe to
  /// call repeatedly (server-side cursor only ever advances, never regresses).
  Future<void> markRead(String conversationId, String lastSeenMessageId) async {
    await apiPatch('/api/v1/direct-conversations/$conversationId/read', {
      'lastSeenMessageId': lastSeenMessageId,
    });
  }

  Future<void> recallMessage(
    String conversationId,
    String messageId,
  ) => apiPatch(
    '/api/v1/direct-conversations/$conversationId/messages/$messageId/recall',
    {},
  );

  Future<UnreadSummary> getUnreadSummary() async {
    final response = await apiGet(
      '/api/v1/direct-conversations/unread-summary',
    );
    return UnreadSummary.fromJson(response['data'] as Map<String, dynamic>);
  }

  /// ADR-MEDI-001 — directory search/pagination now actually reach the backend query.
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) async {
    final response = await apiGet(
      '/api/v1/expert/directory',
      queryParams: {
        if (q != null && q.isNotEmpty) 'q': q,
        if (specialty != null && specialty.isNotEmpty) 'specialty': specialty,
        'page': page,
        'size': size,
      },
    );
    return ExpertDirectoryPage.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }

  Future<ConversationCall> initiateCall(
    String conversationId, {
    required String callType,
  }) async {
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

  Future<ConversationCall> _patchCall(
    String conversationId,
    String callId,
    String action,
  ) async {
    final response = await apiPatch(
      '/api/v1/direct-conversations/$conversationId/calls/$callId/$action',
      {},
    );
    return ConversationCall.fromJson(response['data'] as Map<String, dynamic>);
  }
}
