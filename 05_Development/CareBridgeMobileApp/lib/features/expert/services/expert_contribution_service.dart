import 'dart:convert';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

import '../../../core/auth/auth_state.dart';
import '../models/contribution_model.dart';

/// Service for Expert Medical Contribution APIs
class ExpertContributionService {
  ExpertContributionService._();
  static final ExpertContributionService instance =
      ExpertContributionService._();

  static const _basePath = '/api/v1/contributions';

  /// Get base URL using same logic as ApiClient
  String get _baseUrl {
    const envBaseUrl = String.fromEnvironment('API_BASE_URL');
    if (envBaseUrl.isNotEmpty) return envBaseUrl;
    if (kIsWeb) return 'http://localhost:8080';
    if (Platform.isAndroid) return 'http://10.0.2.2:8080';
    return 'http://localhost:8080';
  }

  /// Get token from auth state
  String? get _token => AuthState.instance.accessToken;

  Map<String, String> get _headers => {
    'Content-Type': 'application/json',
    if (_token != null) 'Authorization': 'Bearer $_token',
  };

  /// List my contributions
  Future<PaginatedContributions> listMyContributions({
    int page = 0,
    int size = 10,
  }) async {
    final uri = Uri.parse('$_baseUrl$_basePath/me?page=$page&size=$size');
    final response = await http.get(uri, headers: _headers);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return PaginatedContributions.fromJson(jsonDecode(response.body)['data']);
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Get a single contribution
  Future<Contribution> getContribution(String contributionId) async {
    final uri = Uri.parse('$_baseUrl$_basePath/$contributionId');
    final response = await http.get(uri, headers: _headers);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return Contribution.fromJson(jsonDecode(response.body)['data']);
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Create a new contribution
  Future<Contribution> createContribution(
    CreateContributionRequest request,
  ) async {
    final uri = Uri.parse('$_baseUrl$_basePath');
    final body = jsonEncode(request.toJson());
    final response = await http.post(uri, headers: _headers, body: body);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return Contribution.fromJson(jsonDecode(response.body)['data']);
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Update a contribution
  Future<Contribution> updateContribution(
    String contributionId,
    UpdateContributionRequest request,
  ) async {
    final uri = Uri.parse('$_baseUrl$_basePath/$contributionId');
    final body = jsonEncode(request.toJson());
    final response = await http.put(uri, headers: _headers, body: body);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return Contribution.fromJson(jsonDecode(response.body)['data']);
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Submit a contribution for review
  Future<Contribution> submitContribution(String contributionId) async {
    final uri = Uri.parse('$_baseUrl$_basePath/$contributionId/submit');
    final response = await http.post(uri, headers: _headers, body: '{}');
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return Contribution.fromJson(jsonDecode(response.body)['data']);
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Delete a contribution
  Future<void> deleteContribution(String contributionId) async {
    final uri = Uri.parse('$_baseUrl$_basePath/$contributionId');
    final response = await http.delete(uri, headers: _headers);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return;
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Check if user is eligible to create contributions
  Future<bool> checkEligibility() async {
    final uri = Uri.parse('$_baseUrl$_basePath/eligibility');
    final response = await http.get(uri, headers: _headers);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return jsonDecode(response.body)['data'] as bool;
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Upload a file for contribution (uses multipart upload)
  /// Returns the fileId which can be used in attachments
  Future<String> uploadContributionFile({
    required List<int> bytes,
    required String fileName,
    required String mimeType,
    required String kind, // 'IMAGE' | 'DOCUMENT'
    required String
    purpose, // 'MEDICAL_CONTRIBUTION_IMAGE' | 'MEDICAL_CONTRIBUTION_DOCUMENT'
    required String accessMode, // 'PUBLIC' | 'AUTHENTICATED' | 'PRIVATE'
  }) async {
    final uri = Uri.parse('$_baseUrl/api/v1/files/upload/with-purpose');
    var request = http.MultipartRequest('POST', uri);
    final token = _token;
    if (token != null) {
      request.headers['Authorization'] = 'Bearer $token';
    }
    request.files.add(
      http.MultipartFile.fromBytes(
        'file',
        bytes,
        filename: fileName,
        contentType: MediaType.parse(mimeType),
      ),
    );
    request.fields['kind'] = kind;
    request.fields['purpose'] = purpose;
    request.fields['accessMode'] = accessMode;

    final streamed = await request.send();
    final response = await http.Response.fromStream(streamed);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return jsonDecode(response.body)['data']['fileId'] as String;
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Delete a file uploaded for contribution
  Future<void> deleteContributionFile(String fileId) async {
    final uri = Uri.parse('$_baseUrl/api/v1/files/$fileId');
    final response = await http.delete(uri, headers: _headers);
    _handleUnauthorized(response);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return;
    }
    throw ApiException(response.statusCode, response.body);
  }

  /// Handle 401 responses
  void _handleUnauthorized(http.Response response) {
    if (response.statusCode == 401) {
      AuthState.instance.clearState();
    }
  }
}

/// Generic API exception
class ApiException implements Exception {
  final int statusCode;
  final String message;
  const ApiException(this.statusCode, this.message);
  @override
  String toString() => 'ApiException($statusCode): $message';
}
