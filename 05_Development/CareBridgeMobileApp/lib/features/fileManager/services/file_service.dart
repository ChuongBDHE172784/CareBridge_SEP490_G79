import 'package:universal_io/io.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/file_model.dart';

class FileService {
  static const Duration _uploadTimeout = Duration(seconds: 90);

  // UC167: Upload file as multipart
  Future<UserFile> uploadFile(File file, {String? title}) async {
    final token = AuthState.instance.accessToken;
    final uri = Uri.parse('$apiBaseUrl/api/v1/files');
    final request = http.MultipartRequest('POST', uri);
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    request.files.add(await http.MultipartFile.fromPath('file', file.path));
    if (title != null) request.fields['title'] = title;

    final streamed = await request.send().timeout(_uploadTimeout);
    final body = await streamed.stream.bytesToString();
    if (streamed.statusCode < 200 || streamed.statusCode >= 300) {
      throw ApiException(streamed.statusCode, body);
    }
    final json = jsonDecode(body) as Map<String, dynamic>;
    return UserFile.fromJson(json['data'] as Map<String, dynamic>);
  }

  // UC-168: List files owned by the current user.
  Future<List<UserFile>> listMyFiles({FileCategory? filter}) async {
    final query = filter == null
        ? ''
        : '?category=${filter.name.toUpperCase()}';
    final data = await apiGet('/api/v1/files$query');
    final list = data['data'] as List? ?? [];
    return list
        .map((e) => UserFile.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // UC-168: Get file details and presigned URL
  Future<ViewFileResponse> getFile(String fileId) async {
    final data = await apiGet('/api/v1/files/$fileId');
    return ViewFileResponse.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-169: Delete file
  Future<void> deleteFile(String fileId) async {
    await apiDelete('/api/v1/files/$fileId');
  }
}
