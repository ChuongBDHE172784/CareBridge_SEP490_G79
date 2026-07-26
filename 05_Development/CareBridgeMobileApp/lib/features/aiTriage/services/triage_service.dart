import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/triage_continuation.dart';
import '../models/triage_intake_flow_model.dart';
import '../models/triage_result_model.dart';
import 'triage_continuation_store.dart';

typedef TriageGetRequest = Future<dynamic> Function(String path);
typedef TriagePostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef TriageContinuationPersistenceFailureHandler =
    void Function(Object error, StackTrace stackTrace);

class TriageService implements TriageContinuationGateway {
  TriageService({
    TriageContinuationStore? continuationStore,
    TriageGetRequest? getRequest,
    TriagePostRequest? postRequest,
    TriageContinuationPersistenceFailureHandler?
    onContinuationPersistenceFailure,
  }) : _continuationStore =
           continuationStore ?? SecureTriageContinuationStore(),
       _getRequest = getRequest ?? ((path) => apiGet(path)),
       _postRequest = postRequest ?? ((path, body) => apiPost(path, body)),
       _onContinuationPersistenceFailure = onContinuationPersistenceFailure;

  static const _requestTimeout = Duration(
    seconds: int.fromEnvironment('AI_TRIAGE_TIMEOUT_SECONDS', defaultValue: 8),
  );
  static final Map<String, int> _latestRequestSequenceByUser = {};

  String? _pendingStartRequestId;
  String? _pendingStartFingerprint;
  final TriageContinuationStore _continuationStore;
  final TriageGetRequest _getRequest;
  final TriagePostRequest _postRequest;
  final TriageContinuationPersistenceFailureHandler?
  _onContinuationPersistenceFailure;

  Future<TriageResult> getResult(String sessionId) async {
    final requestContext = _captureContinuationContext();
    final data = await _getRequest(
      '/api/v1/triage/intake/$sessionId',
    ).timeout(_requestTimeout);
    final payload = data['data'] as Map<String, dynamic>;
    final result = TriageResult.fromJson(payload);
    _throwIfStale(requestContext);
    _persistContinuationBestEffort(payload, requestContext);
    return result;
  }

  Future<IntakeFlowResponse> startConversation({
    required String initialText,
    required Map<String, dynamic> currentIntake,
  }) async {
    final requestContext = _captureContinuationContext();
    final stage = currentIntake['stage']?.toString() ?? 'INFANT';
    final fingerprint = jsonEncode({
      'userId': requestContext?.userId,
      'initialText': initialText,
      'currentIntake': currentIntake,
    });
    if (_pendingStartFingerprint != fingerprint) {
      _pendingStartFingerprint = fingerprint;
      _pendingStartRequestId = _newClientRequestId();
    }
    final requestId = _pendingStartRequestId!;
    final requestIntake = Map<String, dynamic>.from(currentIntake);
    final lifecycleBinding = <String, dynamic>{};
    for (final key in const [
      'journeyId',
      'originDashboard',
      'originReferenceId',
      'babyProfileId',
    ]) {
      final value = requestIntake.remove(key);
      if (value != null) lifecycleBinding[key] = value;
    }
    final data = await _postRequest(
      '/api/v1/triage/intake/conversation/start',
      {
        'initialText': initialText,
        'stage': stage,
        ...lifecycleBinding,
        'currentIntake': {...requestIntake, 'stage': stage},
        'clientRequestId': requestId,
      },
    ).timeout(_requestTimeout);
    final payload = data['data'] as Map<String, dynamic>;
    final response = IntakeFlowResponse.fromJson(payload);
    _throwIfStale(requestContext);
    _persistContinuationBestEffort(payload, requestContext);
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
    final requestContext = _captureContinuationContext();
    final data =
        await _postRequest('/api/v1/triage/intake/conversation/continue', {
          'intakeSessionId': intakeSessionId,
          'currentIntake': currentIntake,
          'messages': const [],
          'newAnswers': newAnswers,
          'round': round,
        }).timeout(_requestTimeout);
    final payload = data['data'] as Map<String, dynamic>;
    final response = IntakeFlowResponse.fromJson(payload);
    _throwIfStale(requestContext);
    _persistContinuationBestEffort(payload, requestContext);
    return response;
  }

  @override
  Future<TriageContinuationResolution> resolve(String token) async {
    try {
      final data = await _postRequest(
        '/api/v1/triage/intake/continuations/resolve',
        {'token': token},
      ).timeout(_requestTimeout);
      final payload = Map<String, dynamic>.from(
        data['data'] as Map<String, dynamic>,
      );
      payload['token'] = token;
      return TriageContinuationResolution.fromJson(payload);
    } on ApiException catch (error) {
      final code = _errorCode(error.message);
      if (error.statusCode == 404) {
        throw TriageContinuationFailure.notFound(code: code ?? 'TRIAGE-014');
      }
      if (error.statusCode == 409) {
        throw TriageContinuationFailure.conflict(code: code ?? 'TRIAGE-015');
      }
      rethrow;
    }
  }

  @override
  Future<void> acknowledge(String token) async {
    await _postRequest('/api/v1/triage/intake/continuations/acknowledge', {
      'token': token,
    }).timeout(_requestTimeout);
  }

  _ContinuationRequestContext? _captureContinuationContext() {
    final userId = AuthState.instance.userId;
    if (userId == null || userId.isEmpty) return null;
    final requestSequence = (_latestRequestSequenceByUser[userId] ?? 0) + 1;
    _latestRequestSequenceByUser[userId] = requestSequence;
    return _ContinuationRequestContext(
      userId: userId,
      generation: _continuationStore.generationFor(userId),
      requestSequence: requestSequence,
    );
  }

  bool _isCurrent(_ContinuationRequestContext requestContext) {
    final userId = requestContext.userId;
    return AuthState.instance.userId == userId &&
        requestContext.generation == _continuationStore.generationFor(userId) &&
        requestContext.requestSequence == _latestRequestSequenceByUser[userId];
  }

  void _throwIfStale(_ContinuationRequestContext? requestContext) {
    if (requestContext != null && !_isCurrent(requestContext)) {
      throw StateError('Stale triage response ignored');
    }
  }

  void _persistContinuationBestEffort(
    Map<String, dynamic> payload,
    _ContinuationRequestContext? requestContext,
  ) {
    unawaited(() async {
      try {
        await _persistContinuation(payload, requestContext);
      } catch (error, stackTrace) {
        debugPrint(
          '[TriageService] continuation persistence failed: '
          '${error.runtimeType}',
        );
        try {
          _onContinuationPersistenceFailure?.call(error, stackTrace);
        } catch (callbackError) {
          debugPrint(
            '[TriageService] continuation failure handler failed: '
            '${callbackError.runtimeType}',
          );
        }
      }
    }());
  }

  Future<void> _persistContinuation(
    Map<String, dynamic> payload,
    _ContinuationRequestContext? requestContext,
  ) async {
    if (!_isTerminalContinuationPayload(payload)) return;
    final token = payload['continuationToken']?.toString();
    final expiresAt = DateTime.tryParse(
      payload['continuationExpiresAt']?.toString() ?? '',
    );
    final intakeSessionId =
        payload['intakeSessionId']?.toString() ??
        payload['sessionId']?.toString();
    final userId = requestContext?.userId;
    if (token == null ||
        token.isEmpty ||
        expiresAt == null ||
        intakeSessionId == null ||
        intakeSessionId.isEmpty ||
        userId == null ||
        userId.isEmpty ||
        requestContext == null ||
        !_isCurrent(requestContext)) {
      return;
    }
    await _continuationStore.save(
      userId: userId,
      continuation: PendingTriageContinuation(
        token: token,
        intakeSessionId: intakeSessionId,
        expiresAt: expiresAt.toUtc(),
      ),
      generation: requestContext.generation,
    );
  }
}

class _ContinuationRequestContext {
  const _ContinuationRequestContext({
    required this.userId,
    required this.generation,
    required this.requestSequence,
  });

  final String userId;
  final int generation;
  final int requestSequence;
}

bool _isTerminalContinuationPayload(Map<String, dynamic> payload) {
  final status = payload['status']?.toString().toUpperCase();
  return status == 'COMPLETED' || status == 'TRIAGE_COMPLETE';
}

String? _errorCode(String body) {
  try {
    final decoded = jsonDecode(body);
    if (decoded is Map<String, dynamic>) {
      return decoded['code']?.toString() ??
          (decoded['error'] as Map<String, dynamic>?)?['code']?.toString();
    }
  } catch (_) {
    return null;
  }
  return null;
}

String _newClientRequestId() {
  final random = Random.secure();
  return List.generate(32, (_) => random.nextInt(16).toRadixString(16)).join();
}
