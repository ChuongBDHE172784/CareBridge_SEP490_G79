class ConversationCall {
  final String callId;
  final String conversationId;
  final String initiatedByUserId;
  final String callType;
  final String callStatus;
  final DateTime initiatedAt;
  final DateTime? answeredAt;
  final DateTime? endedAt;
  final int? durationSeconds;

  const ConversationCall({
    required this.callId,
    required this.conversationId,
    required this.initiatedByUserId,
    required this.callType,
    required this.callStatus,
    required this.initiatedAt,
    this.answeredAt,
    this.endedAt,
    this.durationSeconds,
  });

  factory ConversationCall.fromJson(Map<String, dynamic> json) {
    return ConversationCall(
      callId: json['callId'] as String,
      conversationId: json['conversationId'] as String,
      initiatedByUserId: json['initiatedByUserId'] as String,
      callType: json['callType'] as String,
      callStatus: json['callStatus'] as String,
      initiatedAt: DateTime.parse(json['initiatedAt'] as String).toUtc(),
      answeredAt: json['answeredAt'] == null
          ? null
          : DateTime.parse(json['answeredAt'] as String).toUtc(),
      endedAt: json['endedAt'] == null
          ? null
          : DateTime.parse(json['endedAt'] as String).toUtc(),
      durationSeconds: json['durationSeconds'] as int?,
    );
  }
}
