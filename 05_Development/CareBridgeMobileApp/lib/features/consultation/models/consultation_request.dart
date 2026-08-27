class ConsultationRequestSummary {
  final String id;
  final String? counterpartDisplayName;
  final String topic;
  final String status;
  final DateTime createdAt;

  const ConsultationRequestSummary({
    required this.id,
    this.counterpartDisplayName,
    required this.topic,
    required this.status,
    required this.createdAt,
  });

  factory ConsultationRequestSummary.fromJson(Map<String, dynamic> json) {
    return ConsultationRequestSummary(
      id: json['id'] as String,
      counterpartDisplayName: json['counterpartDisplayName'] as String?,
      topic: json['topic'] as String? ?? '',
      status: json['status'] as String? ?? 'PENDING',
      createdAt:
          DateTime.tryParse(json['createdAt'] as String? ?? '') ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
    );
  }
}

class ConsultationRequestDetail {
  final String id;
  final String expertProfileId;
  final String? counterpartDisplayName;
  final String? counterpartAvatarUrl;
  final String topic;
  final String description;
  final DateTime? preferredWindowStart;
  final DateTime? preferredWindowEnd;
  final String status;
  final String? rejectReason;
  final String? directConversationId;
  final DateTime? respondedAt;
  final DateTime expiresAt;
  final DateTime createdAt;

  const ConsultationRequestDetail({
    required this.id,
    required this.expertProfileId,
    this.counterpartDisplayName,
    this.counterpartAvatarUrl,
    required this.topic,
    required this.description,
    this.preferredWindowStart,
    this.preferredWindowEnd,
    required this.status,
    this.rejectReason,
    this.directConversationId,
    this.respondedAt,
    required this.expiresAt,
    required this.createdAt,
  });

  factory ConsultationRequestDetail.fromJson(Map<String, dynamic> json) {
    return ConsultationRequestDetail(
      id: json['id'] as String,
      expertProfileId: json['expertProfileId'] as String,
      counterpartDisplayName: json['counterpartDisplayName'] as String?,
      counterpartAvatarUrl: json['counterpartAvatarUrl'] as String?,
      topic: json['topic'] as String? ?? '',
      description: json['description'] as String? ?? '',
      preferredWindowStart: _date(json['preferredWindowStart']),
      preferredWindowEnd: _date(json['preferredWindowEnd']),
      status: json['status'] as String? ?? 'PENDING',
      rejectReason: json['rejectReason'] as String?,
      directConversationId: json['directConversationId'] as String?,
      respondedAt: _date(json['respondedAt']),
      expiresAt:
          _date(json['expiresAt']) ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
      createdAt:
          _date(json['createdAt']) ??
          DateTime.fromMillisecondsSinceEpoch(0, isUtc: true),
    );
  }

  static DateTime? _date(dynamic value) =>
      value is String ? DateTime.tryParse(value) : null;
}

class ConsultationRequestPage {
  final List<ConsultationRequestSummary> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  const ConsultationRequestPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  factory ConsultationRequestPage.fromJson(Map<String, dynamic> json) {
    final rawItems = (json['data'] ?? json['content']) as List<dynamic>? ?? [];
    return ConsultationRequestPage(
      items: rawItems
          .map(
            (item) => ConsultationRequestSummary.fromJson(
              item as Map<String, dynamic>,
            ),
          )
          .toList(),
      page: (json['page'] as num?)?.toInt() ?? 0,
      size: (json['size'] as num?)?.toInt() ?? 20,
      totalElements:
          (json['totalElements'] as num?)?.toInt() ?? rawItems.length,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 1,
    );
  }
}
