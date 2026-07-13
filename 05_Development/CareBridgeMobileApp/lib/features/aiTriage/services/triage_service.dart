import '../../../core/network/api_client.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_result_model.dart';

class TriageService {
  // UC-61: Owner-only read of a completed intake session's risk result.
  Future<TriageResult> getResult(String sessionId) async {
    final data = await apiGet('/api/v1/triage/intake/$sessionId');
    return TriageResult.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    final data = await apiPost('/api/v1/triage/intake/conversation/start', {
      'initialText': initialText,
      'currentIntake': currentIntake,
    });
    return IntakeFlowResponse.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<IntakeFlowResponse> continueConversation({
    required String intakeSessionId,
    required Map<String, dynamic> currentIntake,
    required Map<String, dynamic> newAnswers,
    required int round,
  }) async {
    final data = await apiPost('/api/v1/triage/intake/conversation/continue', {
      'intakeSessionId': intakeSessionId,
      'currentIntake': currentIntake,
      'messages': const [],
      'newAnswers': newAnswers,
      'round': round,
    });
    return IntakeFlowResponse.fromJson(data['data'] as Map<String, dynamic>);
  }
}
