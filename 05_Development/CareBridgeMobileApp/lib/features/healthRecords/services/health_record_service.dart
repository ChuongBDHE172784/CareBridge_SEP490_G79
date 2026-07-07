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
    final data = await apiPatch('/api/v1/health-records/$recordId', request.toJson());
    return HealthRecord.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-41: Archive health record (soft-delete)
  Future<void> archiveHealthRecord(String recordId) async {
    await apiPatch('/api/v1/health-records/$recordId/archive', {});
  }

  // TODO: Replace with GET /api/v1/health-records?journeyId=X when list endpoint available (UC-40/41/42)
  Future<List<HealthRecord>> listHealthRecords({RecordType? filter}) async {
    final all = [
      HealthRecord(
        id: 'hr-1',
        recordType: RecordType.checkup,
        title: 'Khám tổng quát định kỳ',
        recordDate: DateTime(2023, 10, 15),
        facilityName: 'Bệnh viện Nhi Đồng 2',
        isShared: true,
      ),
      HealthRecord(
        id: 'hr-2',
        recordType: RecordType.vaccination,
        title: 'Tiêm Mũi 6 in 1 (Mũi 3)',
        recordDate: DateTime(2023, 10, 2),
        facilityName: 'Dịch vụ tiêm chủng VNVC',
        isShared: false,
      ),
      HealthRecord(
        id: 'hr-3',
        recordType: RecordType.metric,
        title: 'Đo cân nặng chiều cao',
        recordDate: DateTime(2023, 9, 20),
        facilityName: null,
        isShared: false,
      ),
      HealthRecord(
        id: 'hr-4',
        recordType: RecordType.prescription,
        title: 'Đơn thuốc sau khám',
        recordDate: DateTime(2023, 9, 5),
        facilityName: 'Phòng khám Nhi khoa',
        isShared: false,
      ),
    ];
    if (filter == null) return all;
    return all.where((r) => r.recordType == filter).toList();
  }
}
