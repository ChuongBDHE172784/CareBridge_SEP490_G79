import 'dart:async';
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

enum _RefreshOutcome { refreshed, tokenInvalid, networkError }

/// Credential snapshot for a request that deliberately freezes an access
/// token. A late response may only affect this exact account session.
class _RequestSessionIdentity {
  const _RequestSessionIdentity({
    required this.accountId,
    required this.accessToken,
    required this.refreshToken,
  });

  final String accountId;
  final String accessToken;
  final String? refreshToken;

  bool get isCurrent {
    final auth = AuthState.instance;
    return auth.userId == accountId &&
        auth.accessToken == accessToken &&
        auth.refreshToken == refreshToken;
  }
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
    accountId: accountId,
    accessToken: accessToken,
    refreshToken: auth.refreshToken,
  );
}

const _sessionChangedBody = '{"error":"AUTH_SESSION_CHANGED"}';

Completer<_RefreshOutcome>? _refreshLock;

/// Attempts to get a new access token using the stored refresh token.
/// - refreshed: new tokens saved, callers should retry
/// - tokenInvalid: refresh token expired/revoked, caller should logout
/// - networkError: transient failure, caller should NOT logout (keep session)
Future<_RefreshOutcome> _tryRefresh() async {
  final refresh = AuthState.instance.refreshToken;
  if (refresh == null) {
    debugPrint('[ApiClient] _tryRefresh: no refresh token → tokenInvalid');
    return _RefreshOutcome.tokenInvalid;
  }

  if (_refreshLock != null) return await _refreshLock!.future;

  _refreshLock = Completer<_RefreshOutcome>();
  try {
    debugPrint('[ApiClient] _tryRefresh: calling POST /auth/refresh');
    final uri = Uri.parse('$_baseUrl/api/v1/auth/refresh');
    final response = await http.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'refreshToken': refresh}),
    );
    debugPrint('[ApiClient] _tryRefresh: status=${response.statusCode}');
    if (response.statusCode >= 200 && response.statusCode < 300) {
      final data = jsonDecode(utf8.decode(response.bodyBytes));
      final payload = data['data'] as Map<String, dynamic>;
      await AuthState.instance.setTokens(
        accessToken: payload['accessToken'] as String,
        refreshToken: payload['refreshToken'] as String,
        userId: AuthState.instance.userId ?? '',
        role: AuthState.instance.role ?? '',
      );
      debugPrint('[ApiClient] _tryRefresh: SUCCESS → new tokens saved');
      _refreshLock!.complete(_RefreshOutcome.refreshed);
      return _RefreshOutcome.refreshed;
    }
    // 400/401/403 from refresh endpoint = token is invalid/revoked
    final outcome =
        (response.statusCode == 400 ||
            response.statusCode == 401 ||
            response.statusCode == 403)
        ? _RefreshOutcome.tokenInvalid
        : _RefreshOutcome.networkError;
    debugPrint(
      '[ApiClient] _tryRefresh: FAILED → outcome=$outcome body=${response.body}',
    );
    _refreshLock!.complete(outcome);
    return outcome;
  } catch (e) {
    // Network/IO exception = don't clear session, user may be offline
    debugPrint(
      '[ApiClient] _tryRefresh: EXCEPTION → $e → networkError (session kept)',
    );
    _refreshLock!.complete(_RefreshOutcome.networkError);
    return _RefreshOutcome.networkError;
  } finally {
    _refreshLock = null;
  }
}

Future<void> _handle401(http.Response response) async {
  final blockedState = parseAccountBlockedState(response);
  if (blockedState != null) {
    debugPrint('[ApiClient] account blocked → code=${blockedState.code}');
    unawaited(AuthState.instance.clearWithBlockedAccount(blockedState));
  } else {
    debugPrint(
      '[ApiClient] _handle401: clearing session (real 401 with valid token)',
    );
    unawaited(AuthState.instance.clear());
  }
}

/// Applies refresh-then-retry logic when the server returns 401.
/// Returns the retried response (or the original if no retry was done).
/// Throws ApiException and calls _handle401 when the session must end.
Future<http.Response> _handleUnauthorized(
  http.Response original,
  String? explicitToken,
  Future<http.Response> Function() retry,
) async {
  if (original.statusCode != 401 || explicitToken != null) return original;

  final outcome = await _tryRefresh();
  switch (outcome) {
    case _RefreshOutcome.refreshed:
      return await retry();
    case _RefreshOutcome.tokenInvalid:
      await _handle401(original);
      throw ApiException(401, original.body);
    case _RefreshOutcome.networkError:
      // Keep session intact; surface the error so the UI can show a retry
      throw ApiException(original.statusCode, original.body);
  }
}

Future<void> _handleBlockedResponse(http.Response response) async {
  final state = parseAccountBlockedState(response);
  if (state != null) {
    await AuthState.instance.clearWithBlockedAccount(state);
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
  response = await _handleUnauthorized(
    response,
    token,
    () => http.get(uri, headers: _headers(extraHeaders: extraHeaders)),
  );
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401) await _handle401(response);
  if (response.statusCode == 403) await _handleBlockedResponse(response);
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPost(
  String path,
  Map<String, dynamic> body, {
  String? token,
  String? expectedAccountId,
  http.Client? client,
}) async {
  final expectedSession = token != null && expectedAccountId != null
      ? _captureExpectedSession(
          accountId: expectedAccountId,
          accessToken: token,
        )
      : null;
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
  response = await _handleUnauthorized(
    response,
    token,
    () => client == null
        ? http.post(uri, headers: _headers(), body: encoded)
        : client.post(uri, headers: _headers(), body: encoded),
  );
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
    await _handle401(response);
  }
  if (response.statusCode == 403) {
    if (expectedSession != null && !expectedSession.isCurrent) {
      throw ApiException(response.statusCode, _sessionChangedBody);
    }
    await _handleBlockedResponse(response);
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPut(
  String path,
  Map<String, dynamic> body, {
  String? token,
}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final encoded = jsonEncode(body);
  var response = await http.put(
    uri,
    headers: _headers(token: token),
    body: encoded,
  );
  response = await _handleUnauthorized(
    response,
    token,
    () => http.put(uri, headers: _headers(), body: encoded),
  );
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401) await _handle401(response);
  if (response.statusCode == 403) await _handleBlockedResponse(response);
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPatch(
  String path,
  Map<String, dynamic> body, {
  String? token,
}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final encoded = jsonEncode(body);
  var response = await http.patch(
    uri,
    headers: _headers(token: token),
    body: encoded,
  );
  response = await _handleUnauthorized(
    response,
    token,
    () => http.patch(uri, headers: _headers(), body: encoded),
  );
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401) await _handle401(response);
  if (response.statusCode == 403) await _handleBlockedResponse(response);
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiDelete(
  String path, {
  String? token,
  Map<String, dynamic>? body,
}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final encoded = body != null ? jsonEncode(body) : null;
  var response = await http.delete(
    uri,
    headers: _headers(token: token),
    body: encoded,
  );
  response = await _handleUnauthorized(
    response,
    token,
    () => http.delete(uri, headers: _headers(), body: encoded),
  );
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return _decodeResponse(response);
  }
  if (response.statusCode == 401) await _handle401(response);
  if (response.statusCode == 403) await _handleBlockedResponse(response);
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
}) async {
  final uri = Uri.parse('$_baseUrl$path');
  var request = http.MultipartRequest('POST', uri);
  for (final entry in fields.entries) {
    request.fields[entry.key] = entry.value;
  }
  final effectiveToken = token ?? AuthState.instance.accessToken;
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
  final auth = AuthState.instance;
  String? effectiveToken2 = token ?? auth.accessToken;
  if (response.statusCode == 401 && effectiveToken2 != null) {
    final outcome = await _tryRefresh();
    if (outcome == _RefreshOutcome.refreshed) {
      return apiMultipart(
        path,
        fields,
        token: null,
        files: files,
        fileFieldName: fileFieldName,
        filePath: filePath,
        fileName: fileName,
        mimeType: mimeType,
      );
    }
    await _handle401(response);
    throw ApiException(401, response.body);
  }
  if (response.statusCode >= 200 && response.statusCode < 300) {
    if (response.body.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  if (response.statusCode == 401) await _handle401(response);
  if (response.statusCode == 403) await _handleBlockedResponse(response);
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
