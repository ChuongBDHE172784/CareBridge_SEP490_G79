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
  Future<void> updateVaccination(
    String babyId,
    String vaccinationId,
    Map<String, dynamic> payload,
  ) async {
    await apiPatch(
      '/api/v1/vaccination/babies/$babyId/records/$vaccinationId',
      payload,
    );
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

  // UC-233: Delete vaccination record
  Future<void> deleteVaccination(String babyId, String vaccinationId) async {
    await apiDelete(
      '/api/v1/vaccination/babies/$babyId/records/$vaccinationId',
    );
  }

  // UC-229: Add vaccination record
  Future<void> addVaccinationRecord(
    String babyId,
    Map<String, dynamic> payload,
  ) async {
    await apiPost('/api/v1/vaccination/babies/$babyId/records', payload);
  }
}
