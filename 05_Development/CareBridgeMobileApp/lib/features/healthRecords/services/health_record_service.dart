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
  Future<List<HealthRecord>> listHealthRecords({RecordType? filter}) async {
    final query = filter == null
        ? ''
        : '?recordType=${filter.name.toUpperCase()}';
    final data = await apiGet('/api/v1/health-records$query');
    final list = data['data'] as List? ?? [];
    return list
        .map((e) => HealthRecord.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
