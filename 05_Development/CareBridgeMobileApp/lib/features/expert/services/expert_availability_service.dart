import '../../../core/network/api_client.dart';
import '../models/expert_availability_slot.dart';

class ExpertAvailabilityService {
  static ExpertAvailabilityService instance = ExpertAvailabilityService();

  Future<List<ExpertAvailabilitySlot>> getPublicAvailability(
    String expertProfileId,
  ) async {
    final response = await apiGet(
      '/api/v1/expert/availability/$expertProfileId',
    );
    final payload = response is Map<String, dynamic>
        ? (response['data'] ?? response['content'])
        : null;
    final rows = payload is List ? payload : const <dynamic>[];
    final slots =
        rows
            .whereType<Map>()
            .map(
              (row) => ExpertAvailabilitySlot.fromJson(
                Map<String, dynamic>.from(row),
              ),
            )
            .where((slot) => slot.isAvailable)
            .toList()
          ..sort((a, b) => a.startAt.compareTo(b.startAt));
    return slots;
  }
}
