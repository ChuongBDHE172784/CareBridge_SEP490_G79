import '../../../core/network/api_client.dart';
import '../models/vaccination_model.dart';

class VaccinationService {
  // UC-228: Get vaccination detail
  Future<VaccinationRecord> getVaccination(String vaccinationId) async {
    final data = await apiGet('/api/v1/vaccinations/$vaccinationId');
    return VaccinationRecord.fromJson(data['data'] as Map<String, dynamic>);
  }

  // UC-230/231: Update vaccination info
  Future<void> updateVaccination(String vaccinationId, Map<String, dynamic> payload) async {
    await apiPost('/api/v1/vaccinations/$vaccinationId', payload);
  }

  // UC-232: Reschedule vaccination
  Future<void> rescheduleVaccination(String vaccinationId, DateTime newDate) async {
    await apiPost('/api/v1/vaccinations/$vaccinationId/reschedule', {
      'newPlannedDate': newDate.toIso8601String(),
    });
  }

  // UC-233: Delete vaccination record
  Future<void> deleteVaccination(String vaccinationId) async {
    await apiDelete('/api/v1/vaccinations/$vaccinationId');
  }

  // UC-229: Add vaccination record
  Future<void> addVaccinationRecord(String babyId, Map<String, dynamic> payload) async {
    await apiPost('/api/v1/vaccination/babies/$babyId/records', payload);
  }
}
