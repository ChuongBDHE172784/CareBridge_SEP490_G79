import 'dart:async';
import 'dart:convert';
import 'dart:math';

import '../../../core/network/api_client.dart';
import '../models/triage_v2_session.dart';

typedef TriageV2GetRequest = Future<dynamic> Function(String path);
typedef TriageV2PostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef TriageV2DeleteRequest = Future<dynamic> Function(String path);

class TriageV2StaleVersionFailure implements Exception {
  const TriageV2StaleVersionFailure();
}

class TriageV2UnavailableFailure implements Exception {
  const TriageV2UnavailableFailure([this.cause]);
  final Object? cause;
}

class TriageV2Service {
  TriageV2Service({
    TriageV2GetRequest? getRequest,
    TriageV2PostRequest? postRequest,
    TriageV2DeleteRequest? deleteRequest,
  }) : _get = getRequest ?? apiGet,
       _post = postRequest ?? apiPost,
       _delete = deleteRequest ?? ((path) => apiDelete(path));

  static const _base = '/api/internal/v2/triage/sessions';
  static const _timeout = Duration(seconds: 8);
  final TriageV2GetRequest _get;
  final TriageV2PostRequest _post;
  final TriageV2DeleteRequest _delete;
  final Map<String, _PendingMutation> _pendingStarts = {};
  final Map<String, _PendingMutation> _pendingContinues = {};

  Future<TriageV2Session> start({
    required String message,
    required String selectedTarget,
    String? profileId,
  }) async {
    final fingerprint = jsonEncode({
      'message': message,
      'target': selectedTarget,
      'profileId': profileId,
    });
    final pending = _pendingStarts.putIfAbsent(
      fingerprint,
      () => _PendingMutation(fingerprint),
    );
    final result = await _call(
      () => _post(_base, {
        'profileId': profileId,
        'selectedTarget': selectedTarget,
        'journeyContext': const <String, dynamic>{},
        'message': message,
        'messageId': pending.messageId,
        'requestId': pending.requestId,
        // Java is the consent/version authority. An empty context means "validate the
        // account's active canonical consent" and avoids inventing a mobile-only version.
        'consentContext': const <String, dynamic>{},
        'signals': const <String, dynamic>{},
        'measurements': const <String, dynamic>{},
      }),
    );
    if (identical(_pendingStarts[fingerprint], pending)) {
      _pendingStarts.remove(fingerprint);
    }
    return result;
  }

  /// [answers] carries one `{questionId, optionCode}` pair per question answered this round.
  ///
  /// They are identifiers only — the server derives every clinical signal from them, and the app
  /// never sends a clinical value. The whole round travels together because the chat asks up to
  /// three questions at once: splitting them would spend a state version per question and turn one
  /// user action into three independently retryable requests.
  Future<TriageV2Session> continueSession({
    required TriageV2Session session,
    required String message,
    List<TriageV2Answer> answers = const [],
  }) async {
    final encodedAnswers = answers.map((answer) => answer.toJson()).toList();
    final fingerprint = jsonEncode({
      'sessionId': session.sessionId,
      'version': session.stateVersion,
      'message': message,
      'answers': encodedAnswers,
    });
    final pending = _pendingContinues.putIfAbsent(
      fingerprint,
      () => _PendingMutation(fingerprint),
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

  Future<TriageV2Session> get(String sessionId) =>
      _call(() => _get('$_base/$sessionId'));

  Future<TriageV2Session> cancel(TriageV2Session session) => _call(
    () => _delete(
      '$_base/${session.sessionId}?expectedStateVersion=${session.stateVersion}',
    ),
  );

  Future<TriageV2Session> _call(Future<dynamic> Function() request) async {
    try {
      final envelope = await request().timeout(_timeout);
      if (envelope is! Map || envelope['data'] is! Map) {
        throw const FormatException('Invalid Triage V2 envelope');
      }
      return TriageV2Session.fromJson(
        Map<String, dynamic>.from(envelope['data'] as Map),
      );
    } on ApiException catch (error) {
      if (error.statusCode == 409 &&
          error.errorCode == 'TRIAGE_V2_STATE_VERSION_CONFLICT') {
        throw const TriageV2StaleVersionFailure();
      }
      throw TriageV2UnavailableFailure(error);
    } on TriageV2StaleVersionFailure {
      rethrow;
    } catch (error) {
      throw TriageV2UnavailableFailure(error);
    }
  }
}

class _PendingMutation {
  _PendingMutation(this.fingerprint)
    : requestId = _newId(),
      messageId = _newId();
  final String fingerprint;
  final String requestId;
  final String messageId;
}

String _newId() {
  final random = Random.secure();
  return List.generate(32, (_) => random.nextInt(16).toRadixString(16)).join();
}
