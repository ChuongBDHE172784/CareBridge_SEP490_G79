import 'dart:io';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../../../core/auth/auth_state.dart';
import '../../../core/network/api_client.dart';
import '../models/file_model.dart';

class FileService {
  String get _baseUrl {
    if (Platform.isAndroid) return 'http://10.0.2.2:8080';
    return 'http://localhost:8080';
  }

  // UC167: Upload file as multipart
  Future<UserFile> uploadFile(File file, {String? title}) async {
    final token = AuthState.instance.accessToken;
    final uri = Uri.parse('$_baseUrl/api/v1/files');
    final request = http.MultipartRequest('POST', uri);
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    request.files.add(await http.MultipartFile.fromPath('file', file.path));
    if (title != null) request.fields['title'] = title;

    final streamed = await request.send();
    final body = await streamed.stream.bytesToString();
    if (streamed.statusCode < 200 || streamed.statusCode >= 300) {
      throw ApiException(streamed.statusCode, body);
    }
    final json = jsonDecode(body) as Map<String, dynamic>;
    return UserFile.fromJson(json['data'] as Map<String, dynamic>);
  }

  // TODO: GET /api/v1/files when list endpoint available (UC-168)
  Future<List<UserFile>> listMyFiles({FileCategory? filter}) async {
    final all = [
      UserFile(
        fileId: 'f-1',
        originalName: 'Siêu âm thai 4D - Tuần 24',
        mimeType: 'image/jpeg',
        fileSizeBytes: 2400000,
        createdAt: DateTime(2024, 5, 12),
        category: FileCategory.ultrasound,
        owner: FileOwner.baby,
        visibility: FileVisibility.private,
      ),
      UserFile(
        fileId: 'f-2',
        originalName: 'Sổ khám thai định kỳ',
        mimeType: 'application/pdf',
        fileSizeBytes: 800000,
        createdAt: DateTime(2024, 5, 8),
        category: FileCategory.medicalRecord,
        owner: FileOwner.mother,
        visibility: FileVisibility.shared,
      ),
      UserFile(
        fileId: 'f-3',
        originalName: 'Ảnh kỷ niệm siêu âm',
        mimeType: 'image/jpeg',
        fileSizeBytes: 1200000,
        createdAt: DateTime(2024, 5, 5),
        category: FileCategory.photo,
        owner: FileOwner.baby,
        visibility: FileVisibility.private,
      ),
    ];
    if (filter == null) return all;
    return all.where((f) => f.category == filter).toList();
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
