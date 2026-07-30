import '../../../core/network/api_client.dart';
import '../models/health_record_model.dart';

class HealthRecordService {
  // UC211: Get single health record detail
  Future<HealthRecordDetail> getHealthRecord(String recordId) async {
    final data = await apiGet('/api/v1/health-records/$recordId');
    return HealthRecordDetail.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-40: Partial update health record
  Future<HealthRecord> updateHealthRecord(
    String recordId,
    UpdateHealthRecordRequest request,
  ) async {
    final data = await apiPatch(
      '/api/v1/health-records/$recordId',
      request.toJson(),
    );
    return HealthRecord.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-41: Archive health record (soft-delete)
  Future<void> archiveHealthRecord(String recordId) async {
    await apiPatch('/api/v1/health-records/$recordId/archive', {});
  }

  // UC-40/41/42: List health records visible to the current user.
  Future<List<HealthRecord>> listHealthRecords({int size = 100}) async {
    final data = await apiGet(
      '/api/v1/health-records/timeline',
      queryParams: {'size': size},
    );
    final payload = data['data'];
    final list = payload is Map<String, dynamic>
        ? payload['items'] as List? ?? []
        : payload as List? ?? [];
    return list
        .map((e) => HealthRecord.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<String> uploadHealthRecordAttachment({
    required List<int> bytes,
    required String fileName,
    required String mimeType,
  }) async {
    final upload = MultipartUploadFile(
      fieldName: 'file',
      bytes: bytes,
      fileName: fileName,
      mimeType: mimeType,
    );
    dynamic data;
    try {
      data = await apiMultipart(
        '/api/v1/files/health-records',
        const {},
        files: [upload],
      );
    } on ApiException catch (e) {
      if (e.statusCode != 404 && e.statusCode != 405) rethrow;
      data = await apiMultipart('/api/v1/files', const {}, files: [upload]);
    }
    return data['data']['fileId'].toString();
  }

  Future<HealthRecord> addHealthRecord({
    required String recordType,
    required String title,
    required DateTime recordDate,
    String? facilityName,
    List<String> fileIds = const [],
  }) async {
    final d = recordDate;
    final data = await apiPost('/api/v1/health-records', {
      'recordType': recordType,
      'title': title,
      'recordDate':
          '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}',
      if (facilityName != null && facilityName.trim().isNotEmpty)
        'facilityName': facilityName.trim(),
      if (fileIds.isNotEmpty) 'fileIds': fileIds,
    });
    return HealthRecord.fromJson(data['data'] as Map<String, dynamic>);
  }
}
