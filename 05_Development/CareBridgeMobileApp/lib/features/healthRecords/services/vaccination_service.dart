import '../../../core/network/api_client.dart';
import '../models/vaccination_model.dart';

class VaccinationService {
  Future<List<VaccinationRecord>> listVaccinationRecords(String babyId) async {
    final data = await apiGet('/api/v1/vaccination/babies/$babyId/records');
    final rows = data['data'] as List<dynamic>? ?? const [];
    return rows
        .map((row) => VaccinationRecord.fromJson(row as Map<String, dynamic>))
        .toList();
  }

  Future<VaccinationSchedule> getVaccinationSchedule(String babyId) async {
    final data = await apiGet('/api/v1/vaccination/babies/$babyId/schedule');
    final raw = data is Map && data['data'] is Map ? data['data'] : data;
    return VaccinationSchedule.fromJson(Map<String, dynamic>.from(raw as Map));
  }

  // UC-228: Get vaccination detail
  Future<VaccinationRecord> getVaccination(
    String babyId,
    String vaccinationId,
  ) async {
    final records = await listVaccinationRecords(babyId);
    return records.firstWhere(
      (record) => record.vaccinationId == vaccinationId,
      orElse: () => throw StateError('Vaccination record not found'),
    );
  }

  // UC-230/231: Update vaccination info
  Future<VaccinationRecord> updateVaccination(
    String babyId,
    String vaccinationId,
    Map<String, dynamic> payload,
  ) async {
    final data = await apiPatch(
      '/api/v1/vaccination/babies/$babyId/records/$vaccinationId',
      payload,
    );
    final raw = data is Map && data['data'] is Map ? data['data'] : data;
    return VaccinationRecord.fromJson(Map<String, dynamic>.from(raw as Map));
  }

  // UC-232: Reschedule vaccination
  Future<void> rescheduleVaccination(
    String babyId,
    String vaccinationId,
    DateTime newDate,
  ) async {
    throw UnsupportedError(
      'Rescheduling requires the vaccination schedule workflow.',
    );
  }

  Future<void> markVaccinationCompleted({
    required String babyId,
    required String vaccineName,
    required int doseNumber,
    required DateTime administeredDate,
    String? facilityName,
    String? proofRecordId,
  }) async {
    await apiPost('/api/v1/vaccination/babies/$babyId/completions', {
      'vaccineName': vaccineName,
      'doseNumber': doseNumber,
      'administeredDate': _dateOnly(administeredDate),
      if (facilityName != null && facilityName.trim().isNotEmpty)
        'facilityName': facilityName.trim(),
      'proofRecordId': ?proofRecordId,
    });
  }

  // UC-233: Delete vaccination record
  Future<void> deleteVaccination(String babyId, String vaccinationId) async {
    await apiDelete(
      '/api/v1/vaccination/babies/$babyId/records/$vaccinationId',
    );
  }

  // UC-229: Add vaccination record
  Future<VaccinationRecord> addVaccinationRecord(
    String babyId,
    Map<String, dynamic> payload,
  ) async {
    final data = await apiPost(
      '/api/v1/vaccination/babies/$babyId/records',
      payload,
    );
    final raw = data is Map && data['data'] is Map ? data['data'] : data;
    return VaccinationRecord.fromJson(Map<String, dynamic>.from(raw as Map));
  }

  static String _dateOnly(DateTime value) {
    final local = value.toLocal();
    return '${local.year.toString().padLeft(4, '0')}-'
        '${local.month.toString().padLeft(2, '0')}-'
        '${local.day.toString().padLeft(2, '0')}';
  }
}
