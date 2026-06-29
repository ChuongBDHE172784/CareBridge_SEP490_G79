import '../../../core/network/api_client.dart';
import '../models/privacy_model.dart';

class PrivacyService {
  static final PrivacyService instance = PrivacyService._();
  PrivacyService._();

  Future<PrivacySettings> getSettings() async {
    final res = await apiGet('/api/v1/privacy-settings/me');
    return PrivacySettings.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<PrivacySettings> updateSettings(PrivacySettings settings) async {
    final res = await apiPut(
        '/api/v1/privacy-settings/me', settings.toUpdateJson());
    return PrivacySettings.fromJson(res['data'] as Map<String, dynamic>);
  }

  Future<List<ConsentGrant>> listConsents() async {
    final res = await apiGet('/api/v1/consent/grants');
    final data = res['data'] as List<dynamic>;
    return data
        .map((e) => ConsentGrant.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> revokeConsent(int consentId) async {
    await apiDelete('/api/v1/consent/grants/$consentId');
  }
}
