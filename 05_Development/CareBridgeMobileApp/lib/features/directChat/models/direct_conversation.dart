class DirectConversation {
  final String conversationId;
  final String motherUserId;
  final String expertUserId;
  final String status;
  final DateTime createdAt;
  final DateTime? lastActivityAt;
  final bool expertAvailable;

  const DirectConversation({
    required this.conversationId,
    required this.motherUserId,
    required this.expertUserId,
    required this.status,
    required this.createdAt,
    this.lastActivityAt,
    required this.expertAvailable,
  });

  factory DirectConversation.fromJson(Map<String, dynamic> json) {
    return DirectConversation(
      conversationId: json['conversationId'] as String,
      motherUserId: json['motherUserId'] as String,
      expertUserId: json['expertUserId'] as String,
      status: json['status'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String).toUtc(),
      lastActivityAt: json['lastActivityAt'] == null
          ? null
          : DateTime.parse(json['lastActivityAt'] as String).toUtc(),
      expertAvailable: json['expertAvailable'] as bool? ?? false,
    );
  }
}

class DirectConversationSummary {
  final String conversationId;
  final String counterpartUserId;
  final String counterpartRole;
  final DateTime? lastActivityAt;
  final bool expertAvailable;

  const DirectConversationSummary({
    required this.conversationId,
    required this.counterpartUserId,
    required this.counterpartRole,
    this.lastActivityAt,
    required this.expertAvailable,
  });

  factory DirectConversationSummary.fromJson(Map<String, dynamic> json) {
    return DirectConversationSummary(
      conversationId: json['conversationId'] as String,
      counterpartUserId: json['counterpartUserId'] as String,
      counterpartRole: json['counterpartRole'] as String,
      lastActivityAt: json['lastActivityAt'] == null
          ? null
          : DateTime.parse(json['lastActivityAt'] as String).toUtc(),
      expertAvailable: json['expertAvailable'] as bool? ?? false,
    );
  }
}
