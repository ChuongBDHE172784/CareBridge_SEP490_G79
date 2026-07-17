import '../../../core/network/api_client.dart';

abstract class ExpertHomeApi {
  Future<dynamic> get(String path);
}

class _DefaultExpertHomeApi implements ExpertHomeApi {
  @override
  Future<dynamic> get(String path) => apiGet(path);
}

class ExpertHomeService {
  static ExpertHomeService instance = ExpertHomeService();

  final ExpertHomeApi api;

  ExpertHomeService({ExpertHomeApi? api})
    : api = api ?? _DefaultExpertHomeApi();

  Future<ExpertHomeSnapshot> loadSnapshot() async {
    ExpertHomeProfile profile = const ExpertHomeProfile();
    var online = false;
    var consultationCount = 0;
    var requestCount = 0;
    var questionCount = 0;
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

    try {
      final questionsJson = await api.get(
        '/api/v1/community/questions?page=0&size=20&hasExpertAnswer=false',
      );
      final rows = questionsJson['data'] as List? ?? [];
      questionCount = rows.length;
    } catch (_) {}

    return ExpertHomeSnapshot(
      profile: profile,
      online: online,
      consultationCount: consultationCount,
      requestCount: requestCount,
      questionCount: questionCount,
      nextConsultation: nextConsultation,
      supportRequests: supportRequests,
    );
  }

  Future<bool> setOnline(bool online) async {
    try {
      // TODO: switch to the implemented UC-142 path when backend exposes it.
      await apiPatch('/api/v1/experts/me/availability/status', {
        'online': online,
      });
      return true;
    } catch (_) {
      return false;
    }
  }
}

class ExpertHomeSnapshot {
  final ExpertHomeProfile profile;
  final bool online;
  final int consultationCount;
  final int requestCount;
  final int questionCount;
  final ExpertConsultation? nextConsultation;
  final List<ExpertSupportRequest> supportRequests;

  const ExpertHomeSnapshot({
    required this.profile,
    required this.online,
    required this.consultationCount,
    required this.requestCount,
    required this.questionCount,
    required this.nextConsultation,
    required this.supportRequests,
  });
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
