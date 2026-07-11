import '../../../core/network/api_client.dart';

class HealthSummaryService {
  // UC43: Generate health summary
  Future<Map<String, dynamic>> generateSummary(String summaryPeriod, String summaryJson) async {
    final data = await apiPost('/api/v1/health-summaries', {
      'summaryPeriod': summaryPeriod,
      'summaryJson': summaryJson,
    });
    return data['data'] as Map<String, dynamic>;
  }

  // UC44: Share summary with expert
  Future<Map<String, dynamic>> shareSummary(String summaryId, String bookingId) async {
    final data = await apiPost('/api/v1/health-summaries/share', {
      'summaryId': summaryId,
      'bookingId': bookingId,
    });
    return data['data'] as Map<String, dynamic>;
  }
}
