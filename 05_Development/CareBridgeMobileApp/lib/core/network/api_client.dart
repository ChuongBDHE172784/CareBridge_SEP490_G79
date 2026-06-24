import 'package:http/http.dart' as http;
import 'package:flutter/foundation.dart';
import 'dart:io';
import 'dart:convert';

String get _baseUrl {
  if (kIsWeb) return 'http://localhost:8080';
  if (Platform.isAndroid) return 'http://10.0.2.2:8080';
  return 'http://localhost:8080';
}

Map<String, String> _headers({String? token}) {
  final headers = <String, String>{'Content-Type': 'application/json'};
  if (token != null) headers['Authorization'] = 'Bearer $token';
  return headers;
}

Future<dynamic> apiGet(String path, {String? token}) async {
  final uri = Uri.parse('$_baseUrl$path');
  final response = await http.get(uri, headers: _headers(token: token));
  if (response.statusCode >= 200 && response.statusCode < 300) {
    return jsonDecode(utf8.decode(response.bodyBytes));
  }
  throw ApiException(response.statusCode, response.body);
}

Future<dynamic> apiPost(String path, Map<String, dynamic> body, {String? token}) async {
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
  throw ApiException(response.statusCode, response.body);
}

class ApiException implements Exception {
  final int statusCode;
  final String message;
  ApiException(this.statusCode, this.message);

  @override
  String toString() => 'ApiException($statusCode): $message';
}
