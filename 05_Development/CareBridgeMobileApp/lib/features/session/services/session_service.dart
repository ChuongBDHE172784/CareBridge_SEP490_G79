import '../../../core/network/api_client.dart';
import '../models/session_model.dart';

class SessionService {
  static final SessionService instance = SessionService._();
  SessionService._();

  Future<List<SessionInfo>> getSessions() async {
    final res = await apiGet('/api/v1/sessions');
    final data = res['data'] as List<dynamic>;
    return data
        .map((e) => SessionInfo.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<void> revokeSession(String sessionId) async {
    await apiDelete('/api/v1/sessions/$sessionId');
  }

  Future<void> revokeAllOtherSessions() async {
    await apiDelete('/api/v1/sessions');
  }
}
