import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:flutter/foundation.dart';

import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/triage_consent_status.dart';
import '../models/triage_continuation.dart';
import '../models/triage_history_model.dart';
import '../models/triage_result_model.dart';
import '../models/triage_session.dart';
import 'triage_continuation_store.dart';

typedef TriageGetRequest = Future<dynamic> Function(String path);
typedef TriagePostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef TriageContinuationPersistenceFailureHandler =
    void Function(Object error, StackTrace stackTrace);

/// Compatibility reads/consent/continuation retained while mutations move to [TriageSessionService].
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
  // Keyed by "userId::operation" (not just userId) so an unrelated background
  // triage call (e.g. a consent/history check) can never mark an in-flight
  // start/continue mutation as stale. Regression: a single successful
  // start/continue could be discarded as "stale" — and shown as a generic
  // send failure — whenever any other TriageService call for the same user
  // happened to race it, even though the two operations were independent.
  static final Map<String, int> _latestRequestSequenceByUser = {};

  final TriageContinuationStore _continuationStore;
  final TriageGetRequest _getRequest;
  final TriagePostRequest _postRequest;
  final TriageContinuationPersistenceFailureHandler?
  _onContinuationPersistenceFailure;

  Future<TriageResult> getResult(String sessionId) async {
    final requestContext = _captureContinuationContext('getResult');
    final data = await _getRequest(
      '/api/v1/triage/intake/$sessionId',
    ).timeout(_requestTimeout);
    final payload = data['data'] as Map<String, dynamic>;
    final result = TriageResult.fromJson(payload);
    _throwIfStale(requestContext);
    _persistContinuationBestEffort(payload, requestContext);
    return result;
  }

  Future<TriageConsentStatus> getConsentStatus() async {
    final requestContext = _captureContinuationContext('getConsentStatus');
    final data = await _getRequest(
      '/api/v1/triage/consent',
    ).timeout(_requestTimeout);
    final status = TriageConsentStatus.fromJson(
      Map<String, dynamic>.from(data['data'] as Map),
    );
    _throwIfStale(requestContext);
    return status;
  }

  Future<List<TriageHistoryItem>> listHistory() async {
    final requestContext = _captureContinuationContext('listHistory');
    final data = await _getRequest(
      '/api/v1/triage/intake',
    ).timeout(_requestTimeout);
    final rawItems = data['data'];
    if (rawItems is! List) {
      throw const FormatException('Invalid triage history response');
    }
    _throwIfStale(requestContext);
    return rawItems
        .whereType<Map>()
        .map(
          (item) => TriageHistoryItem.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<TriageConsentStatus> acceptConsent({
    required String policyVersion,
    String locale = 'vi',
  }) async {
    final requestContext = _captureContinuationContext('acceptConsent');
    final data = await _postRequest('/api/v1/triage/consent/accept', {
      'policyVersion': policyVersion,
      'locale': locale,
    }).timeout(_requestTimeout);
    final status = TriageConsentStatus.fromJson(
      Map<String, dynamic>.from(data['data'] as Map),
    );
    _throwIfStale(requestContext);
    return status;
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

  _ContinuationRequestContext? _captureContinuationContext(String operation) {
    final userId = AuthState.instance.userId;
    if (userId == null || userId.isEmpty) return null;
    final key = _sequenceKey(userId, operation);
    final requestSequence = (_latestRequestSequenceByUser[key] ?? 0) + 1;
    _latestRequestSequenceByUser[key] = requestSequence;
    return _ContinuationRequestContext(
      userId: userId,
      operation: operation,
      generation: _continuationStore.generationFor(userId),
      requestSequence: requestSequence,
    );
  }

  static String _sequenceKey(String userId, String operation) =>
      '$userId::$operation';

  bool _isCurrent(_ContinuationRequestContext requestContext) {
    final userId = requestContext.userId;
    final key = _sequenceKey(userId, requestContext.operation);
    return AuthState.instance.userId == userId &&
        requestContext.generation == _continuationStore.generationFor(userId) &&
        requestContext.requestSequence == _latestRequestSequenceByUser[key];
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

class TriageConsentRequiredFailure implements Exception {
  const TriageConsentRequiredFailure();
}

typedef TriageSessionGetRequest = Future<dynamic> Function(String path);
typedef TriageSessionPostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef TriageSessionDeleteRequest = Future<dynamic> Function(String path);

class TriageSessionStaleVersionFailure implements Exception {
  const TriageSessionStaleVersionFailure();
}

class TriageSessionUnavailableFailure implements Exception {
  const TriageSessionUnavailableFailure([this.cause]);
  final Object? cause;
}

/// Canonical deterministic triage-session client used by the conversational UI.
///
/// Consent/history helpers remain on [TriageService] while legacy callers are being retired; both
/// classes live in this file so the feature has one HTTP boundary instead of versioned clients.
class TriageSessionService {
  TriageSessionService({
    TriageSessionGetRequest? getRequest,
    TriageSessionPostRequest? postRequest,
    TriageSessionDeleteRequest? deleteRequest,
  }) : _get = getRequest ?? apiGet,
       _post = postRequest ?? apiPost,
       _delete = deleteRequest ?? ((path) => apiDelete(path));

  static const _base = '/api/v1/triage/sessions';
  static const _timeout = Duration(seconds: 17);
  final TriageSessionGetRequest _get;
  final TriageSessionPostRequest _post;
  final TriageSessionDeleteRequest _delete;
  final Map<String, _PendingTriageMutation> _pendingStarts = {};
  final Map<String, _PendingTriageMutation> _pendingContinues = {};

  Future<TriageSession> start({
    required String message,
    required String selectedTarget,
    required String selectedStage,
    String? profileId,
    Map<String, dynamic> lifecycleBinding = const {},
  }) async {
    final fingerprint = jsonEncode({
      'message': message,
      'target': selectedTarget,
      'stage': selectedStage,
      'profileId': profileId,
      'lifecycleBinding': lifecycleBinding,
    });
    _prunePending(_pendingStarts);
    final pending = _pendingStarts.putIfAbsent(
      fingerprint,
      () => _PendingTriageMutation(),
    );
    final result = await _call(
      () => _post(_base, {
        'profileId': profileId,
        'selectedTarget': selectedTarget,
        'selectedStage': selectedStage,
        'journeyContext': const <String, dynamic>{},
        'message': message,
        'messageId': pending.messageId,
        'requestId': pending.requestId,
        'consentContext': const <String, dynamic>{},
        'signals': const <String, dynamic>{},
        'measurements': const <String, dynamic>{},
        ...lifecycleBinding,
      }),
    );
    if (identical(_pendingStarts[fingerprint], pending)) {
      _pendingStarts.remove(fingerprint);
    }
    return result;
  }

  Future<TriageSession> continueSession({
    required TriageSession session,
    required String message,
    List<TriageAnswer> answers = const [],
  }) async {
    final encodedAnswers = answers.map((answer) => answer.toJson()).toList();
    final fingerprint = jsonEncode({
      'sessionId': session.sessionId,
      'version': session.stateVersion,
      'message': message,
      'answers': encodedAnswers,
    });
    _prunePending(_pendingContinues);
    final pending = _pendingContinues.putIfAbsent(
      fingerprint,
      () => _PendingTriageMutation(),
    );
    final result = await _call(
      () => _post('$_base/${session.sessionId}/messages', {
        'sessionId': session.sessionId,
        'expectedStateVersion': session.stateVersion,
        'message': message,
        'messageId': pending.messageId,
        'requestId': pending.requestId,
        'answers': encodedAnswers,
        'signals': const <String, dynamic>{},
        'measurements': const <String, dynamic>{},
      }),
    );
    if (identical(_pendingContinues[fingerprint], pending)) {
      _pendingContinues.remove(fingerprint);
    }
    return result;
  }

  Future<TriageSession> get(String sessionId) =>
      _call(() => _get('$_base/$sessionId'));

  Future<TriageSession> cancel(TriageSession session) => _call(
    () => _delete(
      '$_base/${session.sessionId}?expectedStateVersion=${session.stateVersion}',
    ),
  );

  Future<TriageSession> _call(Future<dynamic> Function() request) async {
    try {
      final envelope = await request().timeout(_timeout);
      if (envelope is! Map || envelope['data'] is! Map) {
        throw const FormatException('Invalid triage session envelope');
      }
      return TriageSession.fromJson(
        Map<String, dynamic>.from(envelope['data'] as Map),
      );
    } on ApiException catch (error) {
      if (error.statusCode == 409 &&
          error.errorCode == 'TRIAGE_CONSENT_REQUIRED') {
        throw const TriageConsentRequiredFailure();
      }
      if (error.statusCode == 409 &&
          error.errorCode == 'TRIAGE_STATE_VERSION_CONFLICT') {
        throw const TriageSessionStaleVersionFailure();
      }
      throw TriageSessionUnavailableFailure(error);
    } on TriageSessionStaleVersionFailure {
      rethrow;
    } catch (error) {
      throw TriageSessionUnavailableFailure(error);
    }
  }

  void _prunePending(Map<String, _PendingTriageMutation> pending) {
    final cutoff = DateTime.now().subtract(const Duration(minutes: 5));
    pending.removeWhere((_, value) => value.createdAt.isBefore(cutoff));
    while (pending.length >= 32) {
      pending.remove(pending.keys.first);
    }
  }
}

class _PendingTriageMutation {
  _PendingTriageMutation()
    : requestId = _newClientRequestId(),
      messageId = _newClientRequestId(),
      createdAt = DateTime.now();

  final String requestId;
  final String messageId;
  final DateTime createdAt;
}

class _ContinuationRequestContext {
  const _ContinuationRequestContext({
    required this.userId,
    required this.operation,
    required this.generation,
    required this.requestSequence,
  });

  final String userId;
  final String operation;
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
