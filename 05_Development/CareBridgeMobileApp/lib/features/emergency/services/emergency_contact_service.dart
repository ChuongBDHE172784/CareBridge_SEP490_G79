import '../../../core/network/api_client.dart';
import '../models/emergency_contact_model.dart';

class EmergencyContactService {
  Future<EmergencyContact?> getContact() async {
    try {
      final data = await apiGet('/api/v1/emergency/contact');
      return EmergencyContact.fromJson(data['data'] as Map<String, dynamic>);
    } on ApiException catch (e) {
      if (e.statusCode == 404) return null;
      rethrow;
    }
  }

  Future<EmergencyContact> saveContact(EmergencyContact contact) async {
    final data = await apiPut(
      '/api/v1/emergency/contact',
      contact.toRequestJson(),
    );
    return EmergencyContact.fromJson(data['data'] as Map<String, dynamic>);
  }
}
