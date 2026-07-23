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
    var questionCount = 0;
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
      questionCount: questionCount,
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
  final int questionCount;
  final List<ExpertSupportRequest> supportRequests;

  const ExpertHomeSnapshot({
    required this.profile,
    required this.online,
    required this.questionCount,
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
