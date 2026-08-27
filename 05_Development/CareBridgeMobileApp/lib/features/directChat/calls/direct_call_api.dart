import '../../../core/network/api_client.dart';
import '../models/conversation_call.dart';
import '../models/zego_join_credentials.dart';

abstract class DirectCallApiPort {
  Future<ConversationCall> initiate(String conversationId, String callType);
  Future<ConversationCall> getCall(String conversationId, String callId);
  Future<List<ConversationCall>> listActiveCalls();
  Future<ConversationCall> markRinging(String conversationId, String callId);
  Future<ConversationCall> answer(String conversationId, String callId);
  Future<ConversationCall> decline(String conversationId, String callId);
  Future<ConversationCall> end(String conversationId, String callId);
  Future<ZegoJoinCredentials> issueJoinCredentials(
    String conversationId,
    String callId,
  );
  Future<ConversationCall> uploadRecording({
    required String conversationId,
    required String callId,
    required String filePath,
    int? recordedDurationSeconds,
    bool consentAttested,
  });
}

class DirectCallApi implements DirectCallApiPort {
  static DirectCallApiPort instance = DirectCallApi();

  @override
  Future<ConversationCall> initiate(
    String conversationId,
    String callType,
  ) async {
    final response = await apiPost(
      '/api/v1/direct-conversations/$conversationId/calls',
      {'callType': callType},
    );
    return ConversationCall.fromJson(response['data'] as Map<String, dynamic>);
  }

  @override
  Future<ConversationCall> getCall(String conversationId, String callId) async {
    final response = await apiGet(
      '/api/v1/direct-conversations/$conversationId/calls/$callId',
    );
    return ConversationCall.fromJson(response['data'] as Map<String, dynamic>);
  }

  @override
  Future<List<ConversationCall>> listActiveCalls() async {
    final response = await apiGet('/api/v1/direct-conversations/calls/active');
    final rows = response['data'] as List? ?? const [];
    return rows
        .cast<Map<String, dynamic>>()
        .map(ConversationCall.fromJson)
        .toList();
  }

  Future<ConversationCall> _patch(
    String conversationId,
    String callId,
    String action,
  ) async {
    final response = await apiPatch(
      '/api/v1/direct-conversations/$conversationId/calls/$callId/$action',
      const {},
    );
    return ConversationCall.fromJson(response['data'] as Map<String, dynamic>);
  }

  @override
  Future<ConversationCall> markRinging(String conversationId, String callId) =>
      _patch(conversationId, callId, 'ringing');

  @override
  Future<ConversationCall> answer(String conversationId, String callId) =>
      _patch(conversationId, callId, 'answer');

  @override
  Future<ConversationCall> decline(String conversationId, String callId) =>
      _patch(conversationId, callId, 'decline');

  @override
  Future<ConversationCall> end(String conversationId, String callId) =>
      _patch(conversationId, callId, 'end');

  @override
  Future<ZegoJoinCredentials> issueJoinCredentials(
    String conversationId,
    String callId,
  ) async {
    final response = await apiPost(
      '/api/v1/direct-conversations/$conversationId/calls/$callId/join-credentials',
      const {},
    );
    return ZegoJoinCredentials.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }

  @override
  Future<ConversationCall> uploadRecording({
    required String conversationId,
    required String callId,
    required String filePath,
    int? recordedDurationSeconds,
    bool consentAttested = true,
  }) async {
    final fields = <String, String>{
      'consentAttested': consentAttested.toString(),
      if (recordedDurationSeconds != null)
        'recordedDurationSeconds': recordedDurationSeconds.toString(),
    };
    final response = await apiMultipart(
      '/api/v1/direct-conversations/$conversationId/calls/$callId/recording',
      fields,
      filePath: filePath,
      fileFieldName: 'file',
    );
    return ConversationCall.fromJson(
      response['data'] as Map<String, dynamic>,
    );
  }
}
