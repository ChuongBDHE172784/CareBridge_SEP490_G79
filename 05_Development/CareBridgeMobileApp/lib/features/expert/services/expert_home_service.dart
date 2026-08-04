import 'dart:convert';

import '../../../core/network/api_client.dart';

abstract class ExpertHomeApi {
  Future<dynamic> get(String path);

  Future<dynamic> patch(String path, Map<String, dynamic> body) =>
      apiPatch(path, body);
}

class _DefaultExpertHomeApi implements ExpertHomeApi {
  @override
  Future<dynamic> get(String path) => apiGet(path);

  @override
  Future<dynamic> patch(String path, Map<String, dynamic> body) =>
      apiPatch(path, body);
}

class ExpertHomeService {
  static ExpertHomeService instance = ExpertHomeService();

  final ExpertHomeApi api;

  ExpertHomeService({ExpertHomeApi? api})
    : api = api ?? _DefaultExpertHomeApi();

  Future<ExpertHomeSnapshot> loadSnapshot() async {
    ExpertHomeProfile profile = const ExpertHomeProfile();
    var online = false;
    var requestCount = 0;
    ExpertConsultation? nextConsultation;
    final supportRequests = <ExpertSupportRequest>[];

    try {
      final profileJson = await api.get('/api/v1/expert/profiles/me');
      profile = ExpertHomeProfile.fromJson(
        profileJson['data'] as Map<String, dynamic>? ?? {},
      );
    } catch (_) {}

    try {
      final availabilityJson = await api.get('/api/v1/expert/availability/me');
      final rows = availabilityJson['data'] as List? ?? [];
      final now = DateTime.now().toUtc();
      online = rows.cast<Map<String, dynamic>>().any((row) {
        final start = DateTime.tryParse(row['startAt'] as String? ?? '');
        final end = DateTime.tryParse(row['endAt'] as String? ?? '');
        final status = row['status'] as String? ?? '';
        return status == 'AVAILABLE' &&
            start != null &&
            end != null &&
            !now.isBefore(start) &&
            !now.isAfter(end);
      });
    } catch (_) {}

    try {
      final summaryJson = await api.get(
        '/api/v1/consultation-requests/pending-summary',
      );
      requestCount =
          ((summaryJson['data'] as Map<String, dynamic>?)?['pendingCount']
                  as num?)
              ?.toInt() ??
          0;
      final requestsJson = await api.get(
        '/api/v1/consultation-requests/assigned?status=PENDING&page=0&size=5',
      );
      final rows =
          (requestsJson['data'] ?? requestsJson['content']) as List? ?? [];
      if (rows.isNotEmpty) {
        nextConsultation = ExpertConsultation.fromJson(
          rows.first as Map<String, dynamic>,
        );
      }
    } catch (_) {}

    try {
      final nearbyJson = await api.get(
        '/api/v1/nearbycare/support-requests/open',
      );
      final rows = nearbyJson['data'] as List? ?? [];
      supportRequests.addAll(
        rows
            .take(2)
            .map(
              (row) =>
                  ExpertSupportRequest.fromJson(row as Map<String, dynamic>),
            ),
      );
    } catch (_) {}

    return ExpertHomeSnapshot(
      profile: profile,
      online: online,
      requestCount: requestCount,
      nextConsultation: nextConsultation,
      supportRequests: supportRequests,
    );
  }

  Future<OnlineStatusUpdateResult> setOnline(bool online) async {
    try {
      final response = await api
          .patch('/api/v1/expert/online-status', {'online': online})
          .timeout(const Duration(seconds: 15));
      return OnlineStatusUpdateResult.success(
        online: _responseOnlineStatus(response) ?? online,
        message:
            _responseMessage(response) ??
            (online
                ? 'Đã bật trạng thái Trực tuyến'
                : 'Đã chuyển sang Ngoại tuyến'),
      );
    } catch (error) {
      return OnlineStatusUpdateResult.failure(
        message: friendlyApiError(
          error,
          fallback: 'Không thể cập nhật trạng thái. Vui lòng thử lại.',
        ),
      );
    }
  }
}

class OnlineStatusUpdateResult {
  final bool success;
  final bool? online;
  final String message;

  const OnlineStatusUpdateResult._({
    required this.success,
    required this.online,
    required this.message,
  });

  const OnlineStatusUpdateResult.success({
    required bool online,
    required String message,
  }) : this._(success: true, online: online, message: message);

  const OnlineStatusUpdateResult.failure({required String message})
    : this._(success: false, online: null, message: message);
}

class ExpertHomeSnapshot {
  final ExpertHomeProfile profile;
  final bool online;
  final int requestCount;
  final ExpertConsultation? nextConsultation;
  final List<ExpertSupportRequest> supportRequests;

  const ExpertHomeSnapshot({
    required this.profile,
    required this.online,
    this.requestCount = 0,
    this.nextConsultation,
    required this.supportRequests,
  });

  ExpertHomeSnapshot copyWith({
    ExpertHomeProfile? profile,
    bool? online,
    int? requestCount,
    ExpertConsultation? nextConsultation,
    List<ExpertSupportRequest>? supportRequests,
  }) {
    return ExpertHomeSnapshot(
      profile: profile ?? this.profile,
      online: online ?? this.online,
      requestCount: requestCount ?? this.requestCount,
      nextConsultation: nextConsultation ?? this.nextConsultation,
      supportRequests: supportRequests ?? this.supportRequests,
    );
  }
}

class ExpertHomeProfile {
  final String displayName;
  final String subtitle;

  const ExpertHomeProfile({
    this.displayName = 'CareBridge',
    this.subtitle = 'Chuyên gia CareBridge',
  });

  factory ExpertHomeProfile.fromJson(Map<String, dynamic> json) {
    final title = json['professionalTitle'] as String?;
    final specialty = json['specialty'] as String?;
    return ExpertHomeProfile(
      displayName: 'CareBridge',
      subtitle: title?.isNotEmpty == true
          ? title!
          : specialty?.isNotEmpty == true
          ? specialty!
          : 'Chuyên gia CareBridge',
    );
  }
}

class ExpertConsultation {
  final String motherName;
  final String topic;
  final String timeLabel;

  const ExpertConsultation({
    required this.motherName,
    required this.topic,
    required this.timeLabel,
  });

  factory ExpertConsultation.fromJson(Map<String, dynamic> json) {
    final created = DateTime.tryParse(json['createdAt'] as String? ?? '');
    return ExpertConsultation(
      motherName:
          json['counterpartDisplayName'] as String? ?? 'Người dùng CareBridge',
      topic: json['topic'] as String? ?? 'Tư vấn sức khỏe',
      timeLabel: created == null ? 'Mới' : _timeAgo(created),
    );
  }
}

class ExpertSupportRequest {
  final String title;
  final String subtitle;
  final bool urgent;

  const ExpertSupportRequest({
    required this.title,
    required this.subtitle,
    this.urgent = false,
  });

  factory ExpertSupportRequest.fromJson(Map<String, dynamic> json) {
    final created = DateTime.tryParse(json['createdAt'] as String? ?? '');
    return ExpertSupportRequest(
      title: json['description'] as String? ?? 'Yêu cầu hỗ trợ gần đây',
      subtitle: created == null ? 'Vừa cập nhật' : _timeAgo(created),
      urgent: (json['supportType'] as String? ?? '').toUpperCase().contains(
        'EMERGENCY',
      ),
    );
  }
}

String _timeAgo(DateTime dateTime) {
  final diff = DateTime.now().difference(dateTime);
  if (diff.inMinutes < 60) return '${diff.inMinutes.clamp(1, 59)} phút trước';
  if (diff.inHours < 24) return '${diff.inHours} giờ trước';
  return '${diff.inDays} ngày trước';
}

String friendlyApiError(Object error, {required String fallback}) {
  final raw = error is ApiException ? error.message : error.toString();
  final message = _decodeErrorMessage(raw);
  final normalized = message.toLowerCase();

  if (normalized.contains('consent') ||
      normalized.contains('đồng ý') ||
      normalized.contains('dong y')) {
    return 'Quyền chia sẻ vị trí đã hết hạn. Hãy cấp lại quyền rồi thử lại.';
  }
  if (normalized.contains('location') ||
      normalized.contains('vị trí') ||
      normalized.contains('vi tri')) {
    return 'Hãy bật chia sẻ vị trí có thời hạn trước khi chuyển sang Trực tuyến.';
  }
  if (normalized.contains('profile') ||
      normalized.contains('approved') ||
      normalized.contains('verification') ||
      normalized.contains('hồ sơ') ||
      normalized.contains('ho so')) {
    return 'Hồ sơ chuyên gia cần được phê duyệt trước khi thực hiện thao tác này.';
  }
  if (normalized.contains('overlap') ||
      normalized.contains('conflict') ||
      normalized.contains('trùng') ||
      normalized.contains('chồng lấn')) {
    return 'Khung giờ này trùng với lịch hiện có. Hãy chọn thời gian khác.';
  }
  if (normalized.contains('past') ||
      normalized.contains('future') ||
      normalized.contains('quá khứ')) {
    return 'Khung giờ phải bắt đầu trong tương lai.';
  }
  if (message.isNotEmpty &&
      message != raw &&
      !message.toLowerCase().contains('<html')) {
    return message;
  }
  return fallback;
}

String? _responseMessage(dynamic response) {
  if (response is! Map) return null;
  final message = response['message'];
  return message is String && message.trim().isNotEmpty ? message.trim() : null;
}

bool? _responseOnlineStatus(dynamic response) {
  if (response is! Map) return null;
  final data = response['data'];
  if (data is! Map) return null;
  final status = '${data['availabilityStatus'] ?? ''}'.toUpperCase();
  if (status == 'ONLINE') return true;
  if (status == 'OFFLINE') return false;
  return null;
}

String _decodeErrorMessage(String raw) {
  try {
    final decoded = jsonDecode(raw);
    if (decoded is Map) {
      for (final key in const ['message', 'detail', 'error']) {
        final value = decoded[key];
        if (value is String && value.trim().isNotEmpty) {
          return value.trim();
        }
        if (value is Map) {
          final nested = value['message'];
          if (nested is String && nested.trim().isNotEmpty) {
            return nested.trim();
          }
        }
      }
    }
  } catch (_) {
    // Non-JSON transport errors use the supplied safe fallback.
  }
  return raw.trim();
}
