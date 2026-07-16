import 'dart:async';
import 'dart:convert';
import 'dart:math';

import '../../../core/network/api_client.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_result_model.dart';

class TriageService {
  static const _requestTimeout = Duration(
    seconds: int.fromEnvironment('AI_TRIAGE_TIMEOUT_SECONDS', defaultValue: 8),
  );

  String? _pendingStartRequestId;
  String? _pendingStartFingerprint;

  Future<TriageResult> getResult(String sessionId) async {
    final data = await apiGet(
      '/api/v1/triage/intake/$sessionId',
    ).timeout(_requestTimeout);
    return TriageResult.fromJson(data['data'] as Map<String, dynamic>);
  }

  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    final stage = currentIntake['stage']?.toString() ?? 'INFANT';
    final fingerprint = jsonEncode({
      'initialText': initialText,
      'currentIntake': currentIntake,
    });
    if (_pendingStartFingerprint != fingerprint) {
      _pendingStartFingerprint = fingerprint;
      _pendingStartRequestId = _newClientRequestId();
    }
    final requestId = _pendingStartRequestId!;
    final data = await apiPost('/api/v1/triage/intake/conversation/start', {
      'initialText': initialText,
      'stage': stage,
      'currentIntake': {
        ...currentIntake,
        'stage': stage,
      },
      'clientRequestId': requestId,
    }).timeout(_requestTimeout);
    final response = IntakeFlowResponse.fromJson(
      data['data'] as Map<String, dynamic>,
    );
    if (_pendingStartRequestId == requestId) {
      _pendingStartRequestId = null;
      _pendingStartFingerprint = null;
    }
    return response;
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
    }).timeout(_requestTimeout);
    return IntakeFlowResponse.fromJson(data['data'] as Map<String, dynamic>);
  }
}

String _newClientRequestId() {
  final random = Random.secure();
  return List.generate(32, (_) => random.nextInt(16).toRadixString(16)).join();
}
