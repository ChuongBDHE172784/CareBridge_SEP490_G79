class NotificationPreference {
  final String type;
  final bool? pushEnabled;
  final bool? emailEnabled;
  final bool? inAppEnabled;

  const NotificationPreference({
    required this.type,
    this.pushEnabled,
    this.emailEnabled,
    this.inAppEnabled,
  });

  factory NotificationPreference.fromJson(Map<String, dynamic> json) {
    return NotificationPreference(
      type: json['notificationType'] as String? ?? '',
      pushEnabled: json['pushEnabled'] as bool?,
      emailEnabled: json['emailEnabled'] as bool?,
      inAppEnabled: json['inAppEnabled'] as bool?,
    );
  }

  Map<String, dynamic> toJson() => {
    'notificationType': type,
    'pushEnabled': pushEnabled,
    'emailEnabled': emailEnabled,
    'inAppEnabled': inAppEnabled,
  };

  NotificationPreference copyWith({
    bool? pushEnabled,
    bool? emailEnabled,
    bool? inAppEnabled,
  }) {
    return NotificationPreference(
      type: type,
      pushEnabled: pushEnabled ?? this.pushEnabled,
      emailEnabled: emailEnabled ?? this.emailEnabled,
      inAppEnabled: inAppEnabled ?? this.inAppEnabled,
    );
  }
}

class NotificationRecord {
  final String id;
  final String userId;
  final String type;
  final String title;
  final String body;
  final String? referenceId;
  final String? referenceType;
  final String status;
  final DateTime createdAt;
  final DateTime? sentAt;
  final Map<String, dynamic>? metadata;

  NotificationRecord({
    required this.id,
    required this.userId,
    required this.type,
    required this.title,
    required this.body,
    this.referenceId,
    this.referenceType,
    required this.status,
    required this.createdAt,
    this.sentAt,
    this.metadata,
  });

  factory NotificationRecord.fromJson(Map<String, dynamic> json) {
    return NotificationRecord(
      id: json['id'] as String,
      userId: json['userId'] as String,
      type: json['type'] as String? ?? 'GENERAL',
      title: json['title'] as String? ?? '',
      body: json['body'] as String? ?? '',
      referenceId: json['referenceId'] as String?,
      referenceType: json['referenceType'] as String?,
      status: json['status'] as String? ?? 'SENT',
      createdAt: DateTime.parse(json['createdAt'] as String),
      sentAt: json['sentAt'] != null
          ? DateTime.parse(json['sentAt'] as String)
          : null,
      metadata: json['metadata'] as Map<String, dynamic>?,
    );
  }

  bool get isUnread => status != 'READ';
}
