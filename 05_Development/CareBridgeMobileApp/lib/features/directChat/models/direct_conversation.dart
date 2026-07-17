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
  final String? counterpartDisplayName;
  final String? counterpartAvatarUrl;
  final String? counterpartSpecialty;
  final String? lastMessagePreview;
  final DateTime? lastMessageAt;
  final int unreadCount;
  final String conversationStatus;

  const DirectConversationSummary({
    required this.conversationId,
    required this.counterpartUserId,
    required this.counterpartRole,
    this.lastActivityAt,
    required this.expertAvailable,
    this.counterpartDisplayName,
    this.counterpartAvatarUrl,
    this.counterpartSpecialty,
    this.lastMessagePreview,
    this.lastMessageAt,
    this.unreadCount = 0,
    this.conversationStatus = 'ACTIVE',
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
      counterpartDisplayName: json['counterpartDisplayName'] as String?,
      counterpartAvatarUrl: json['counterpartAvatarUrl'] as String?,
      counterpartSpecialty: json['counterpartSpecialty'] as String?,
      lastMessagePreview: json['lastMessagePreview'] as String?,
      lastMessageAt: json['lastMessageAt'] == null
          ? null
          : DateTime.parse(json['lastMessageAt'] as String).toUtc(),
      unreadCount: json['unreadCount'] as int? ?? 0,
      conversationStatus: json['conversationStatus'] as String? ?? 'ACTIVE',
    );
  }
}

class UnreadSummary {
  final int unreadConversationCount;
  final int totalUnreadMessageCount;

  const UnreadSummary({
    required this.unreadConversationCount,
    required this.totalUnreadMessageCount,
  });

  factory UnreadSummary.fromJson(Map<String, dynamic> json) {
    return UnreadSummary(
      unreadConversationCount: json['unreadConversationCount'] as int? ?? 0,
      totalUnreadMessageCount: json['totalUnreadMessageCount'] as int? ?? 0,
    );
  }
}
