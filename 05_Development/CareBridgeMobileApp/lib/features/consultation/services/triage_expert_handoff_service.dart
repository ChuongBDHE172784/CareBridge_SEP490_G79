import '../../../core/network/api_client.dart';
import '../models/triage_expert_handoff.dart';

export '../models/triage_expert_handoff.dart';

typedef HandoffGetRequest = Future<dynamic> Function(String path);
typedef HandoffPostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);

class TriageExpertHandoffService {
  TriageExpertHandoffService({
    HandoffGetRequest? getRequest,
    HandoffPostRequest? postRequest,
    Duration? requestTimeout,
  }) : _getRequest = getRequest ?? apiGet,
       _postRequest = postRequest ?? apiPost,
       _requestTimeout = requestTimeout ?? _defaultRequestTimeout;

  static const _defaultRequestTimeout = Duration(
    seconds: int.fromEnvironment(
      'EXPERT_HANDOFF_TIMEOUT_SECONDS',
      defaultValue: 8,
    ),
  );

  static TriageExpertHandoffService instance = TriageExpertHandoffService();

  final HandoffGetRequest _getRequest;
  final HandoffPostRequest _postRequest;
  final Duration _requestTimeout;

  Future<TriageExpertHandoffPreview> getPreview(String intakeSessionId) async {
    final response = await _getRequest(
      '/api/v1/triage/intake/$intakeSessionId/expert-handoff-preview',
    ).timeout(_requestTimeout);
    return TriageExpertHandoffPreview.fromJson(_payload(response));
  }

  Future<TriageExpertHandoffCreateResult> create({
    required String intakeSessionId,
    required String clientRequestId,
    required String expertProfileId,
    required bool consentAccepted,
    required String consentPolicyVersion,
  }) async {
    final response = await _postRequest(
      '/api/v1/triage/intake/$intakeSessionId/expert-handoffs',
      {
        'clientRequestId': clientRequestId,
        'expertProfileId': expertProfileId,
        'consentAccepted': consentAccepted,
        'consentPolicyVersion': consentPolicyVersion,
      },
    ).timeout(_requestTimeout);
    return TriageExpertHandoffCreateResult.fromJson(_payload(response));
  }

  Future<TriageExpertHandoffParticipantContext> getContext(
    String consultationRequestId,
  ) async {
    final response = await _getRequest(
      '/api/v1/consultation-requests/$consultationRequestId/triage-context',
    ).timeout(_requestTimeout);
    return TriageExpertHandoffParticipantContext.fromJson(_payload(response));
  }

  Map<String, dynamic> _payload(dynamic response) {
    if (response is! Map<String, dynamic>) {
      throw const FormatException('Invalid handoff response envelope');
    }
    final data = response['data'] ?? response;
    if (data is! Map<String, dynamic>) {
      throw const FormatException('Invalid handoff response payload');
    }
    return data;
  }
}
