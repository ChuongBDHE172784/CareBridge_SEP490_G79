import 'dart:convert';

import '../../../core/network/api_client.dart';
import '../models/triage_continuation.dart';

typedef TriagePostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);

/// Compatibility client for resolving emergency continuations created by
/// older AI Triage sessions. New conversations use AI Nurse instead.
class TriageService implements TriageContinuationGateway {
  TriageService({TriagePostRequest? postRequest})
    : _postRequest = postRequest ?? ((path, body) => apiPost(path, body));

  static const _requestTimeout = Duration(seconds: 8);

  final TriagePostRequest _postRequest;

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
