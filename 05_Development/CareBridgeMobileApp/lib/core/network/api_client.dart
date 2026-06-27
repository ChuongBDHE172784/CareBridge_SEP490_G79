import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import '../auth/auth_state.dart';
import 'account_block_parser.dart';

String get _baseUrl {
  if (kIsWeb) return 'http://localhost:8080';
  // 10.0.2.2 only works on AVD emulator; use LAN IP for physical devices
  if (Platform.isAndroid) return 'http://192.168.1.11:8080';
  return 'http://localhost:8080';
}

Map<String, String> _headers({String? token}) {
  final effective = token ?? AuthState.instance.accessToken;
  final headers = <String, String>{'Content-Type': 'application/json'};
  if (effective != null) headers['Authorization'] = 'Bearer $effective';
  return headers;
}

Future<dynamic> apiGet(String path, {String? token}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final response = await http.get(uri, headers: _headers(token: token));
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  if (response.statusCode == 401) {
    unawaited(AuthState.instance.clear());
  } else {
    final code = parseAccountBlockedCode(response);
    if (code != null) unawaited(AuthState.instance.clearWithReason(code));
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPost(String path, Map<String, dynamic> body,
    {String? token}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final response = await http.post(
    uri,
    headers: _headers(token: token),
    body: jsonEncode(body),
  );
  if (response.statusCode >= 200 && response.statusCode < 300) {
    if (response.body.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  if (response.statusCode == 401) {
    unawaited(AuthState.instance.clear());
  } else {
    final code = parseAccountBlockedCode(response);
    if (code != null) unawaited(AuthState.instance.clearWithReason(code));
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPut(String path, Map<String, dynamic> body,
    {String? token}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final response = await http.put(
    uri,
    headers: _headers(token: token),
    body: jsonEncode(body),
  );
  if (response.statusCode >= 200 && response.statusCode < 300) {
    if (response.body.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  if (response.statusCode == 401) {
    unawaited(AuthState.instance.clear());
  } else {
    final code = parseAccountBlockedCode(response);
    if (code != null) unawaited(AuthState.instance.clearWithReason(code));
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiDelete(String path, {String? token}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final response = await http.delete(uri, headers: _headers(token: token));
  if (response.statusCode >= 200 && response.statusCode < 300) {
    if (response.body.isEmpty) return null;
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  if (response.statusCode == 401) {
    unawaited(AuthState.instance.clear());
  } else {
    final code = parseAccountBlockedCode(response);
    if (code != null) unawaited(AuthState.instance.clearWithReason(code));
  }
  throw ApiException(response.statusCode, response.body);
}

class ApiException implements Exception {
  final int statusCode;
  final String message;
  ApiException(this.statusCode, this.message);

  @override
  String toString() => 'ApiException($statusCode): $message';
}
