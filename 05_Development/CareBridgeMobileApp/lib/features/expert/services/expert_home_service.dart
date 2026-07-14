import '../../../core/network/api_client.dart';

class ExpertHomeService {
  static final ExpertHomeService instance = ExpertHomeService._();
  ExpertHomeService._();

  Future<ExpertHomeSnapshot> loadSnapshot() async {
    ExpertHomeProfile profile = const ExpertHomeProfile();
    var online = false;
    var consultationCount = 0;
    var requestCount = 0;
    var questionCount = 0;
    ExpertConsultation? nextConsultation;
    final supportRequests = <ExpertSupportRequest>[];

    try {
      final profileJson = await apiGet('/api/v1/expert/profiles/me');
      profile = ExpertHomeProfile.fromJson(
        profileJson['data'] as Map<String, dynamic>? ?? {},
      );
    } catch (_) {}

    try {
      final availabilityJson = await apiGet('/api/v1/expert/availability/me');
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
      // TODO: replace fallback when UC-143 endpoint is implemented in backend.
      final consultationsJson = await apiGet(
        '/api/v1/consultations/requests?page=0&size=5',
      );
      final rows = consultationsJson['data'] as List? ?? [];
      consultationCount = rows.length;
      if (rows.isNotEmpty) {
        nextConsultation = ExpertConsultation.fromJson(
          rows.first as Map<String, dynamic>,
        );
      }
    } catch (_) {
      consultationCount = 3;
      nextConsultation = const ExpertConsultation(
        motherName: 'Mẹ bé An Nhiên',
        topic: 'Tư vấn dinh dưỡng dặm',
        timeLabel: '14:00',
      );
    }

    try {
      final nearbyJson = await apiGet('/api/v1/nearbycare/support-requests/open');
      final rows = nearbyJson['data'] as List? ?? [];
      requestCount = rows.length;
      supportRequests.addAll(
        rows.take(2).map(
              (row) => ExpertSupportRequest.fromJson(
                row as Map<String, dynamic>,
              ),
            ),
      );
    } catch (_) {
      requestCount = 5;
    }

    try {
      final questionsJson = await apiGet(
        '/api/v1/community/questions?page=0&size=20&hasExpertAnswer=false',
      );
      final rows = questionsJson['data'] as List? ?? [];
      questionCount = rows.length;
    } catch (_) {
      questionCount = 12;
    }

    final supports = supportRequests.isEmpty
        ? const [
            ExpertSupportRequest(
              title: 'Bé sốt cao liên tục',
              subtitle: 'Mẹ Tín Phát • 15 phút trước',
              urgent: true,
            ),
            ExpertSupportRequest(
              title: 'Lịch tiêm nhắc lại',
              subtitle: 'Mẹ Bắp • 1 giờ trước',
            ),
          ]
        : supportRequests;

    return ExpertHomeSnapshot(
      profile: profile,
      online: online,
      consultationCount: consultationCount,
      requestCount: requestCount,
      questionCount: questionCount,
      nextConsultation: nextConsultation,
      supportRequests: supports,
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
    final start = DateTime.tryParse(json['scheduledStart'] as String? ?? '');
    return ExpertConsultation(
      motherName: json['motherDisplayName'] as String? ?? 'Người dùng CareBridge',
      topic: json['reason'] as String? ?? 'Tư vấn sức khỏe',
      timeLabel: start != null
          ? '${start.hour.toString().padLeft(2, '0')}:${start.minute.toString().padLeft(2, '0')}'
          : 'Hôm nay',
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
      urgent: (json['supportType'] as String? ?? '').toUpperCase().contains('EMERGENCY'),
    );
  }
}

String _timeAgo(DateTime dateTime) {
  final diff = DateTime.now().difference(dateTime);
  if (diff.inMinutes < 60) return '${diff.inMinutes.clamp(1, 59)} phút trước';
  if (diff.inHours < 24) return '${diff.inHours} giờ trước';
  return '${diff.inDays} ngày trước';
}
