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
    );
  }

  bool get isUnread => status != 'READ';
}
