import '../../../core/network/api_client.dart';
import '../models/triage_result_model.dart';

class TriageService {
  // UC-61: Owner-only read of a completed intake session's risk result.
  Future<TriageResult> getResult(String sessionId) async {
    final data = await apiGet('/api/v1/triage/intake/$sessionId');
    return TriageResult.fromJson(data['data'] as Map<String, dynamic>);
  }
}
