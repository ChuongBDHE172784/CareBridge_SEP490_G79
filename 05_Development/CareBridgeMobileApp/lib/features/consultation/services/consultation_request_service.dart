import '../../../core/network/api_client.dart';
import '../models/consultation_request.dart';

class ConsultationRequestService {
  static ConsultationRequestService instance = ConsultationRequestService();

  Future<ConsultationRequestPage> listMine({
    String? status,
    int page = 0,
    int size = 20,
  }) async {
    final response = await apiGet(
      '/api/v1/consultation-requests/mine',
      queryParams: {
        if (status != null) 'status': status,
        'page': page,
        'size': size,
      },
    );
    return ConsultationRequestPage.fromJson(response as Map<String, dynamic>);
  }

  Future<ConsultationRequestPage> listAssigned({
    String? status,
    int page = 0,
    int size = 20,
  }) async {
    final response = await apiGet(
      '/api/v1/consultation-requests/assigned',
      queryParams: {
        if (status != null) 'status': status,
        'page': page,
        'size': size,
      },
    );
    return ConsultationRequestPage.fromJson(response as Map<String, dynamic>);
  }

  Future<ConsultationRequestDetail> getById(String id) async {
    final response = await apiGet('/api/v1/consultation-requests/$id');
    return _detail(response);
  }

  Future<ConsultationRequestDetail> create({
    required String clientRequestId,
    required String expertProfileId,
    required String topic,
    required String description,
    DateTime? preferredWindowStart,
    DateTime? preferredWindowEnd,
  }) async {
    final response = await apiPost('/api/v1/consultation-requests', {
      'clientRequestId': clientRequestId,
      'expertProfileId': expertProfileId,
      'topic': topic,
      'description': description,
      if (preferredWindowStart != null)
        'preferredWindowStart': preferredWindowStart.toUtc().toIso8601String(),
      if (preferredWindowEnd != null)
        'preferredWindowEnd': preferredWindowEnd.toUtc().toIso8601String(),
    });
    return _detail(response);
  }

  Future<ConsultationRequestDetail> accept(String id) async {
    return _detail(
      await apiPatch('/api/v1/consultation-requests/$id/accept', {}),
    );
  }

  Future<ConsultationRequestDetail> reject(String id, {String? reason}) async {
    return _detail(
      await apiPatch('/api/v1/consultation-requests/$id/reject', {
        if (reason != null && reason.trim().isNotEmpty) 'reason': reason.trim(),
      }),
    );
  }

  Future<ConsultationRequestDetail> cancel(String id) async {
    return _detail(
      await apiPatch('/api/v1/consultation-requests/$id/cancel', {}),
    );
  }

  Future<int> pendingCount() async {
    final response = await apiGet(
      '/api/v1/consultation-requests/pending-summary',
    );
    final data = (response as Map<String, dynamic>)['data'];
    return ((data as Map<String, dynamic>?)?['pendingCount'] as num?)
            ?.toInt() ??
        0;
  }

  ConsultationRequestDetail _detail(dynamic response) {
    final json = response as Map<String, dynamic>;
    return ConsultationRequestDetail.fromJson(
      (json['data'] ?? json) as Map<String, dynamic>,
    );
  }
}
