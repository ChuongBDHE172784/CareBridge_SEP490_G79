import 'dart:convert';
import 'package:universal_io/io.dart';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';
import 'package:mime/mime.dart';
import '../auth/auth_state.dart';
import 'account_block_parser.dart';

const String _envBaseUrl = String.fromEnvironment('API_BASE_URL');

String get _baseUrl {
  if (_envBaseUrl.isNotEmpty) return _envBaseUrl;
  if (kIsWeb) return 'http://127.0.0.1:8080';
  if (Platform.isAndroid) return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

/// Base URL shared by API-backed media rendered outside the HTTP client.
/// Rich HTML images may contain relative paths; Flutter web must resolve them
/// against the API server rather than the Flutter development server.
String get apiBaseUrl => _baseUrl;

Map<String, String> _headers({
  String? token,
  Map<String, String>? extraHeaders,
}) {
  final effective = token ?? AuthState.instance.accessToken;
  final headers = <String, String>{...?extraHeaders};
  headers['Content-Type'] = 'application/json';
  if (effective != null) headers['Authorization'] = 'Bearer $effective';
  return headers;
}

enum _RefreshOutcome { refreshed, tokenInvalid, networkError, sessionChanged }

class _RefreshResult {
  const _RefreshResult(this.outcome, [this.session]);

  final _RefreshOutcome outcome;
  final _RequestSessionIdentity? session;
}

/// Credential snapshot for a request that deliberately freezes an access
/// token. A late response may only affect this exact account session.
class _RequestSessionIdentity {
  const _RequestSessionIdentity({
    required this.generation,
    required this.accountId,
    required this.accessToken,
    required this.refreshToken,
    required this.role,
  });

  final int generation;
  final String accountId;
  final String accessToken;
  final String? refreshToken;
  final String role;

  bool get isCurrent {
    final auth = AuthState.instance;
    return auth.matchesSession(generation: generation, userId: accountId);
  }

  bool get hasCurrentCredentials {
    final auth = AuthState.instance;
    return isCurrent &&
        auth.accessToken == accessToken &&
        auth.refreshToken == refreshToken &&
        auth.role == role;
  }

  String get lockKey => '$generation\u0000$accountId\u0000$accessToken';
}

_RequestSessionIdentity? _captureExpectedSession({
  required String accountId,
  required String accessToken,
}) {
  final auth = AuthState.instance;
  if (accountId.isEmpty ||
      auth.userId != accountId ||
      auth.accessToken != accessToken) {
    return null;
  }
  return _RequestSessionIdentity(
    generation: auth.sessionGeneration,
    accountId: accountId,
    accessToken: accessToken,
    refreshToken: auth.refreshToken,
    role: auth.role ?? '',
  );
}

const _sessionChangedBody = '{"error":"AUTH_SESSION_CHANGED"}';

final Map<String, Future<_RefreshResult>> _refreshLocks = {};

/// Attempts to get a new access token using the stored refresh token.
/// - refreshed: new tokens saved, callers should retry
/// - tokenInvalid: refresh token expired/revoked, caller should logout
/// - networkError: transient failure, caller should NOT logout (keep session)
Future<_RefreshResult> _tryRefresh(
  _RequestSessionIdentity requestSession,
) async {
  if (!requestSession.isCurrent) {
    return const _RefreshResult(_RefreshOutcome.sessionChanged);
  }
  if (!requestSession.hasCurrentCredentials) {
    final current = AuthState.instance;
    final currentToken = current.accessToken;
    if (currentToken == null) {
      return const _RefreshResult(_RefreshOutcome.sessionChanged);
    }
    final refreshedSession = _captureExpectedSession(
      accountId: requestSession.accountId,
      accessToken: currentToken,
    );
    return refreshedSession == null
        ? const _RefreshResult(_RefreshOutcome.sessionChanged)
        : _RefreshResult(_RefreshOutcome.refreshed, refreshedSession);
  }
  final refresh = requestSession.refreshToken;
  if (refresh == null) {
    debugPrint('[ApiClient] _tryRefresh: no refresh token → tokenInvalid');
    return const _RefreshResult(_RefreshOutcome.tokenInvalid);
  }

  final existing = _refreshLocks[requestSession.lockKey];
  if (existing != null) return existing;
  final future = _performRefresh(requestSession, refresh);
  _refreshLocks[requestSession.lockKey] = future;
  try {
    return await future;
  } finally {
    if (identical(_refreshLocks[requestSession.lockKey], future)) {
      _refreshLocks.remove(requestSession.lockKey);
    }
  }
}

Future<_RefreshResult> _performRefresh(
  _RequestSessionIdentity requestSession,
  String refresh,
) async {
  try {
    debugPrint('[ApiClient] _tryRefresh: calling POST /auth/refresh');
    final uri = Uri.parse('$_baseUrl/api/v1/auth/refresh');
    final response = await http
        .post(
          uri,
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({'refreshToken': refresh}),
        )
        .timeout(const Duration(seconds: 20));
    debugPrint('[ApiClient] _tryRefresh: status=${response.statusCode}');
    if (!requestSession.isCurrent) {
      return const _RefreshResult(_RefreshOutcome.sessionChanged);
    }
    if (!requestSession.hasCurrentCredentials) {
      final currentToken = AuthState.instance.accessToken;
      final refreshedSession = currentToken == null
          ? null
          : _captureExpectedSession(
              accountId: requestSession.accountId,
              accessToken: currentToken,
            );
      return refreshedSession == null
          ? const _RefreshResult(_RefreshOutcome.sessionChanged)
          : _RefreshResult(_RefreshOutcome.refreshed, refreshedSession);
    }
    if (response.statusCode >= 200 && response.statusCode < 300) {
      final data = jsonDecode(utf8.decode(response.bodyBytes));
      final payload = data['data'] as Map<String, dynamic>;
      final accessToken = payload['accessToken'] as String;
      final refreshToken = payload['refreshToken'] as String;
      final published = await AuthState.instance.setTokensIfCurrent(
        expectedGeneration: requestSession.generation,
        expectedAccessToken: requestSession.accessToken,
        expectedRefreshToken: requestSession.refreshToken,
        expectedUserId: requestSession.accountId,
        accessToken: accessToken,
        refreshToken: refreshToken,
        role: requestSession.role,
      );
      if (!published) {
        return const _RefreshResult(_RefreshOutcome.sessionChanged);
      }
      final refreshedSession = _captureExpectedSession(
        accountId: requestSession.accountId,
        accessToken: accessToken,
      );
      if (refreshedSession == null) {
        return const _RefreshResult(_RefreshOutcome.sessionChanged);
      }
      debugPrint('[ApiClient] _tryRefresh: SUCCESS → new tokens saved');
      return _RefreshResult(_RefreshOutcome.refreshed, refreshedSession);
    }
    // 400/401/403 from refresh endpoint = token is invalid/revoked
    final outcome =
        (response.statusCode == 400 ||
            response.statusCode == 401 ||
            response.statusCode == 403)
        ? _RefreshOutcome.tokenInvalid
        : _RefreshOutcome.networkError;
    debugPrint(
      '[ApiClient] _tryRefresh: FAILED → '
      'outcome=$outcome status=${response.statusCode}',
    );
    return _RefreshResult(outcome);
  } catch (e) {
    // Network/IO exception = don't clear session, user may be offline
    debugPrint(
      '[ApiClient] _tryRefresh: EXCEPTION → $e → networkError (session kept)',
    );
    return const _RefreshResult(_RefreshOutcome.networkError);
  }
}

Future<void> _handle401(
  http.Response response,
  _RequestSessionIdentity? requestSession,
) async {
  if (requestSession == null) return;
  if (!requestSession.isCurrent) _throwSessionChanged();
  final blockedState = parseAccountBlockedState(response);
  if (blockedState != null) {
    debugPrint('[ApiClient] account blocked → code=${blockedState.code}');
    final cleared = await _normalizeAuthClearFailure(
      response,
      () => AuthState.instance.clearWithBlockedAccountIfCurrentCredentials(
        generation: requestSession.generation,
        accessToken: requestSession.accessToken,
        refreshToken: requestSession.refreshToken,
        userId: requestSession.accountId,
        role: requestSession.role,
        state: blockedState,
      ),
    );
    if (!cleared) {
      _throwSessionChanged();
    }
  } else {
    debugPrint(
      '[ApiClient] _handle401: clearing session (real 401 with valid token)',
    );
    final cleared = await _normalizeAuthClearFailure(
      response,
      () => AuthState.instance.clearIfCurrentCredentials(
        generation: requestSession.generation,
        accessToken: requestSession.accessToken,
        refreshToken: requestSession.refreshToken,
        userId: requestSession.accountId,
        role: requestSession.role,
      ),
    );
    if (!cleared) _throwSessionChanged();
  }
}

Future<T> _normalizeAuthClearFailure<T>(
  http.Response response,
  Future<T> Function() clear,
) async {
  try {
    return await clear();
  } catch (error) {
    debugPrint('[ApiClient] credential cleanup failed: ${error.runtimeType}');
    throw ApiException(response.statusCode, response.body);
  }
}

Never _throwSessionChanged() => throw ApiException(401, _sessionChangedBody);

/// Applies refresh-then-retry logic when the server returns 401.
/// Returns the retried response (or the original if no retry was done).
/// Throws ApiException and calls _handle401 when the session must end.
Future<http.Response> _handleUnauthorized(
  http.Response original,
  String? explicitToken,
  Future<http.Response> Function() retry,
  _RequestSessionIdentity? requestSession,
) async {
  if (original.statusCode != 401 || explicitToken != null) return original;

  // A request started before logout/account switching can finish after the
  // local session has changed. It must not refresh or clear the new session.
  if (requestSession != null && !requestSession.isCurrent) {
    debugPrint('[ApiClient] ignoring 401 from a stale session request');
    _throwSessionChanged();
  }

  if (requestSession == null) return original;

  final result = await _tryRefresh(requestSession);
  switch (result.outcome) {
    case _RefreshOutcome.refreshed:
      final refreshedSession = result.session;
      if (refreshedSession == null || !refreshedSession.isCurrent) {
        _throwSessionChanged();
      }
      final response = await retry();
      if (!refreshedSession.isCurrent) _throwSessionChanged();
      if (response.statusCode == 401) {
        if (!refreshedSession.hasCurrentCredentials) {
          _throwSessionChanged();
        }
        await _handle401(response, refreshedSession);
        throw ApiException(401, response.body);
      }
      if (response.statusCode == 403) {
        await _handleBlockedResponse(response, refreshedSession);
      }
      return response;
    case _RefreshOutcome.tokenInvalid:
      if (!requestSession.isCurrent) _throwSessionChanged();
      await _handle401(original, requestSession);
      throw ApiException(401, original.body);
    case _RefreshOutcome.networkError:
      // Keep session intact; surface the error so the UI can show a retry
      throw ApiException(original.statusCode, original.body);
    case _RefreshOutcome.sessionChanged:
      _throwSessionChanged();
  }
}

Future<void> _handleBlockedResponse(
  http.Response response,
  _RequestSessionIdentity? requestSession,
) async {
  if (requestSession == null) return;
  if (!requestSession.isCurrent) _throwSessionChanged();
  final state = parseAccountBlockedState(response);
  if (state != null) {
    final cleared = await _normalizeAuthClearFailure(
      response,
      () => AuthState.instance.clearWithBlockedAccountIfCurrentCredentials(
        generation: requestSession.generation,
        accessToken: requestSession.accessToken,
        refreshToken: requestSession.refreshToken,
        userId: requestSession.accountId,
        role: requestSession.role,
        state: state,
      ),
    );
    if (!cleared) _throwSessionChanged();
  }
}

void _ensureSessionStillCurrent(_RequestSessionIdentity? requestSession) {
  if (requestSession != null && !requestSession.isCurrent) {
    _throwSessionChanged();
  }
}

dynamic _decodeResponse(http.Response response) {
  if (response.body.isEmpty) return null;
  try {
    return jsonDecode(utf8.decode(response.bodyBytes));
  } catch (_) {
    try {
      return jsonDecode(utf8.decode(latin1.encode(response.body)));
    } catch (_) {
      return jsonDecode(response.body);
    }
  }
}

Future<dynamic> apiGet(
  String path, {
  String? token,
  Map<String, dynamic>? queryParams,
  Map<String, String>? extraHeaders,
}) async {
  final auth = AuthState.instance;
  final requestToken = token ?? auth.accessToken;
  final requestSession = requestToken == null
      ? null
      : _captureExpectedSession(
          accountId: auth.userId ?? '',
          accessToken: requestToken,
        );
  String queryString = '';
  if (queryParams != null && queryParams.isNotEmpty) {
    queryString =
        '?${queryParams.entries.map((e) => '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value.toString())}').join('&')}';
  }
  final uri = Uri.parse('$_baseUrl$path$queryString');
  var response = await http.get(
    uri,
    headers: _headers(token: token, extraHeaders: extraHeaders),
  );
  if (response.statusCode != 401) {
    _ensureSessionStillCurrent(requestSession);
  }
  response = await _handleUnauthorized(
    response,
    token,
    () => http.get(uri, headers: _headers(extraHeaders: extraHeaders)),
    requestSession,
  );
  _ensureSessionStillCurrent(requestSession);
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401 &&
      token == null &&
      (requestSession == null || requestSession.isCurrent)) {
    await _handle401(response, requestSession);
  }
  if (response.statusCode == 403) {
    await _handleBlockedResponse(response, requestSession);
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPost(
  String path,
  Map<String, dynamic> body, {
  String? token,
  String? expectedAccountId,
  http.Client? client,
}) async {
  final auth = AuthState.instance;
  final requestToken = token ?? auth.accessToken;
  final expectedSession = requestToken == null
      ? null
      : _captureExpectedSession(
          accountId: expectedAccountId ?? auth.userId ?? '',
          accessToken: requestToken,
        );
  if (token != null && expectedAccountId != null && expectedSession == null) {
    throw ApiException(401, _sessionChangedBody);
  }

  final uri = Uri.parse('$_baseUrl$path');
  final encoded = jsonEncode(body);
  var response = client == null
      ? await http.post(
          uri,
          headers: _headers(token: token),
          body: encoded,
        )
      : await client.post(
          uri,
          headers: _headers(token: token),
          body: encoded,
        );
  if (response.statusCode != 401) {
    _ensureSessionStillCurrent(expectedSession);
  }
  response = await _handleUnauthorized(
    response,
    token,
    () => client == null
        ? http.post(uri, headers: _headers(), body: encoded)
        : client.post(uri, headers: _headers(), body: encoded),
    expectedSession,
  );
  _ensureSessionStillCurrent(expectedSession);
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401) {
    if (expectedSession != null) {
      // Explicit-token requests intentionally do not refresh. An ordinary
      // expiry remains a recoverable form error and must not sign the user
      // out, while a late response from a replaced session is surfaced to
      // its original caller without touching the active account.
      if (!expectedSession.isCurrent) {
        throw ApiException(response.statusCode, _sessionChangedBody);
      }
      throw ApiException(response.statusCode, response.body);
    }
    await _handle401(response, expectedSession);
  }
  if (response.statusCode == 403) {
    await _handleBlockedResponse(response, expectedSession);
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPut(
  String path,
  Map<String, dynamic> body, {
  String? token,
}) async {
  final auth = AuthState.instance;
  final requestToken = token ?? auth.accessToken;
  final requestSession = requestToken == null
      ? null
      : _captureExpectedSession(
          accountId: auth.userId ?? '',
          accessToken: requestToken,
        );
  final uri = Uri.parse('$_baseUrl$path');
  final encoded = jsonEncode(body);
  var response = await http.put(
    uri,
    headers: _headers(token: token),
    body: encoded,
  );
  if (response.statusCode != 401) {
    _ensureSessionStillCurrent(requestSession);
  }
  response = await _handleUnauthorized(
    response,
    token,
    () => http.put(uri, headers: _headers(), body: encoded),
    requestSession,
  );
  _ensureSessionStillCurrent(requestSession);
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401 && token == null) {
    await _handle401(response, requestSession);
  }
  if (response.statusCode == 403) {
    await _handleBlockedResponse(response, requestSession);
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPatch(
  String path,
  Map<String, dynamic> body, {
  String? token,
}) async {
  final auth = AuthState.instance;
  final requestToken = token ?? auth.accessToken;
  final requestSession = requestToken == null
      ? null
      : _captureExpectedSession(
          accountId: auth.userId ?? '',
          accessToken: requestToken,
        );
  final uri = Uri.parse('$_baseUrl$path');
  final encoded = jsonEncode(body);
  var response = await http.patch(
    uri,
    headers: _headers(token: token),
    body: encoded,
  );
  if (response.statusCode != 401) {
    _ensureSessionStillCurrent(requestSession);
  }
  response = await _handleUnauthorized(
    response,
    token,
    () => http.patch(uri, headers: _headers(), body: encoded),
    requestSession,
  );
  _ensureSessionStillCurrent(requestSession);
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401 && token == null) {
    await _handle401(response, requestSession);
  }
  if (response.statusCode == 403) {
    await _handleBlockedResponse(response, requestSession);
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiDelete(
  String path, {
  String? token,
  Map<String, dynamic>? body,
}) async {
  final auth = AuthState.instance;
  final requestToken = token ?? auth.accessToken;
  final requestSession = requestToken == null
      ? null
      : _captureExpectedSession(
          accountId: auth.userId ?? '',
          accessToken: requestToken,
        );
  final uri = Uri.parse('$_baseUrl$path');
  final encoded = body != null ? jsonEncode(body) : null;
  var response = await http.delete(
    uri,
    headers: _headers(token: token),
    body: encoded,
  );
  if (response.statusCode != 401) {
    _ensureSessionStillCurrent(requestSession);
  }
  response = await _handleUnauthorized(
    response,
    token,
    () => http.delete(uri, headers: _headers(), body: encoded),
    requestSession,
  );
  _ensureSessionStillCurrent(requestSession);
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401 && token == null) {
    await _handle401(response, requestSession);
  }
  if (response.statusCode == 403) {
    await _handleBlockedResponse(response, requestSession);
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiMultipart(
  String path,
  Map<String, String> fields, {
  String? token,
  List<MultipartUploadFile> files = const [],
  String? fileFieldName,
  String? filePath,
  String? fileName,
  String? mimeType,
  String? expectedAccountId,
}) async {
  final auth = AuthState.instance;
  final effectiveToken = token ?? auth.accessToken;
  final requestSession = effectiveToken == null
      ? null
      : _captureExpectedSession(
          accountId: expectedAccountId ?? auth.userId ?? '',
          accessToken: effectiveToken,
        );
  if (token != null && expectedAccountId != null && requestSession == null) {
    _throwSessionChanged();
  }
  final uri = Uri.parse('$_baseUrl$path');
  var request = http.MultipartRequest('POST', uri);
  for (final entry in fields.entries) {
    request.fields[entry.key] = entry.value;
  }
  if (effectiveToken != null) {
    request.headers['Authorization'] = 'Bearer $effectiveToken';
  }
  for (final upload in files) {
    request.files.add(
      http.MultipartFile.fromBytes(
        upload.fieldName,
        upload.bytes,
        filename: upload.fileName,
        contentType: MediaType.parse(upload.mimeType),
      ),
    );
  }
  if (filePath != null) {
    final file = http.MultipartFile(
      fileFieldName ?? 'file',
      http.ByteStream(File(filePath).openRead()),
      await File(filePath).length(),
      filename: fileName,
      contentType: MediaType.parse(
        mimeType ?? lookupMimeType(filePath) ?? 'application/octet-stream',
      ),
    );
    request.files.add(file);
  }
  final streamed = await request.send();
  final response = await http.Response.fromStream(streamed);
  debugPrint('Multipart $path → ${response.statusCode}');
  if (response.statusCode != 401) {
    _ensureSessionStillCurrent(requestSession);
  }
  if (response.statusCode == 401 && token == null && requestSession != null) {
    if (!requestSession.isCurrent) _throwSessionChanged();
    final result = await _tryRefresh(requestSession);
    if (result.outcome == _RefreshOutcome.refreshed) {
      final refreshedSession = result.session;
      if (refreshedSession == null || !refreshedSession.isCurrent) {
        _throwSessionChanged();
      }
      try {
        return await apiMultipart(
          path,
          fields,
          token: refreshedSession.accessToken,
          expectedAccountId: refreshedSession.accountId,
          files: files,
          fileFieldName: fileFieldName,
          filePath: filePath,
          fileName: fileName,
          mimeType: mimeType,
        );
      } on ApiException catch (error) {
        if (error.statusCode == 401 &&
            error.errorCode != 'AUTH_SESSION_CHANGED' &&
            refreshedSession.isCurrent) {
          await _handle401(
            http.Response(error.message, error.statusCode),
            refreshedSession,
          );
        }
        rethrow;
      }
    }
    if (result.outcome == _RefreshOutcome.sessionChanged ||
        !requestSession.isCurrent) {
      _throwSessionChanged();
    }
    if (result.outcome == _RefreshOutcome.networkError) {
      throw ApiException(401, response.body);
    }
    await _handle401(response, requestSession);
    throw ApiException(401, response.body);
  }
  if (response.statusCode >= 200 && response.statusCode < 300) {
    if (response.body.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  if (response.statusCode == 401) {
    _ensureSessionStillCurrent(requestSession);
    await _handle401(response, token == null ? requestSession : null);
  }
  if (response.statusCode == 403) {
    await _handleBlockedResponse(response, requestSession);
  }
  throw ApiException(response.statusCode, response.body);
}

/// Platform-neutral multipart input. Keeping bytes here lets mobile and web
/// upload camera evidence without relying on `dart:io` paths.
class MultipartUploadFile {
  final String fieldName;
  final List<int> bytes;
  final String fileName;
  final String mimeType;

  const MultipartUploadFile({
    required this.fieldName,
    required this.bytes,
    required this.fileName,
    required this.mimeType,
  });
}

class ApiException implements Exception {
  final int statusCode;
  final String message;
  ApiException(this.statusCode, this.message);

  String? get errorCode {
    try {
      final decoded = jsonDecode(message);
      if (decoded is! Map<String, dynamic>) return null;
      final error = decoded['error'];
      if (error is String && error.isNotEmpty) return error;
      if (error is Map<String, dynamic>) {
        final nestedCode = error['code']?.toString();
        if (nestedCode != null && nestedCode.isNotEmpty) return nestedCode;
      }
      final code = decoded['code']?.toString();
      return code == null || code.isEmpty ? null : code;
    } catch (_) {
      return null;
    }
  }

  @override
  String toString() => 'ApiException($statusCode): $message';
}
