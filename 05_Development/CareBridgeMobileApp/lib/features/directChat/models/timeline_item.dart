enum ChatSendStatus { sending, sent, failed }

/// Unified MESSAGE / CALL_EVENT timeline entry — mirrors backend TimelineItemResponse
/// (TDS §9.2). Optimistic-send fields (clientMessageId, sendStatus) are client-only and
/// never come from the server for a genuinely new item; they're attached locally, then
/// cleared once the server confirms via [copyWith] in [mergeTimelineItems].
class TimelineItem {
  final String kind; // 'MESSAGE' | 'CALL_EVENT'

  // MESSAGE fields
  final String? messageId;
  final String? clientMessageId;
  final String? senderUserId;
  final String? messageType;
  final String? messageBody;
  final DateTime? createdAt;

  // CALL_EVENT fields
  final String? callId;
  final String? callType;
  final String? callStatus;
  final String? initiatedByUserId;
  final int? durationSeconds;
  final DateTime? initiatedAt;
  final DateTime? answeredAt;
  final DateTime? endedAt;

  // client-only
  final ChatSendStatus? sendStatus;

  const TimelineItem({
    required this.kind,
    this.messageId,
    this.clientMessageId,
    this.senderUserId,
    this.messageType,
    this.messageBody,
    this.createdAt,
    this.callId,
    this.callType,
    this.callStatus,
    this.initiatedByUserId,
    this.durationSeconds,
    this.initiatedAt,
    this.answeredAt,
    this.endedAt,
    this.sendStatus,
  });

  factory TimelineItem.fromJson(Map<String, dynamic> json) {
    return TimelineItem(
      kind: json['kind'] as String,
      messageId: json['messageId'] as String?,
      clientMessageId: json['clientMessageId'] as String?,
      senderUserId: json['senderUserId'] as String?,
      messageType: json['messageType'] as String?,
      messageBody: json['messageBody'] as String?,
      createdAt: _parseDate(json['createdAt']),
      callId: json['callId'] as String?,
      callType: json['callType'] as String?,
      callStatus: json['callStatus'] as String?,
      initiatedByUserId: json['initiatedByUserId'] as String?,
      durationSeconds: json['durationSeconds'] as int?,
      initiatedAt: _parseDate(json['initiatedAt']),
      answeredAt: _parseDate(json['answeredAt']),
      endedAt: _parseDate(json['endedAt']),
      sendStatus: ChatSendStatus.sent,
    );
  }

  factory TimelineItem.optimisticMessage({
    required String clientMessageId,
    required String senderUserId,
    required String messageBody,
  }) {
    return TimelineItem(
      kind: 'MESSAGE',
      clientMessageId: clientMessageId,
      senderUserId: senderUserId,
      messageType: 'TEXT',
      messageBody: messageBody,
      createdAt: DateTime.now().toUtc(),
      sendStatus: ChatSendStatus.sending,
    );
  }

  static DateTime? _parseDate(Object? value) =>
      value == null ? null : DateTime.parse(value as String).toUtc();

  /// Stable identity for dedup: MESSAGE items key on messageId (falling back to
  /// clientMessageId before the server has assigned one); CALL_EVENT items key on callId.
  String get dedupKey {
    if (kind == 'CALL_EVENT') return 'CALL:$callId';
    return 'MESSAGE:${messageId ?? clientMessageId}';
  }

  DateTime get sortTimestamp =>
      kind == 'CALL_EVENT' ? (initiatedAt ?? DateTime.now()) : (createdAt ?? DateTime.now());

  TimelineItem copyWith({
    String? messageId,
    ChatSendStatus? sendStatus,
  }) {
    return TimelineItem(
      kind: kind,
      messageId: messageId ?? this.messageId,
      clientMessageId: clientMessageId,
      senderUserId: senderUserId,
      messageType: messageType,
      messageBody: messageBody,
      createdAt: createdAt,
      callId: callId,
      callType: callType,
      callStatus: callStatus,
      initiatedByUserId: initiatedByUserId,
      durationSeconds: durationSeconds,
      initiatedAt: initiatedAt,
      answeredAt: answeredAt,
      endedAt: endedAt,
      sendStatus: sendStatus ?? this.sendStatus,
    );
  }
}

/// Merges a freshly-fetched/reconciled page into the current in-memory list without
/// duplicating optimistic entries: an item matches an existing optimistic ("sending")
/// entry by [TimelineItem.clientMessageId] and replaces it; otherwise items are deduped
/// by [TimelineItem.dedupKey] and the result is sorted by [TimelineItem.sortTimestamp].
List<TimelineItem> mergeTimelineItems(
  List<TimelineItem> existing,
  List<TimelineItem> incoming,
) {
  final byClientMessageId = <String, TimelineItem>{
    for (final item in existing)
      if (item.clientMessageId != null) item.clientMessageId!: item,
  };

  final merged = <String, TimelineItem>{};
  for (final item in existing) {
    merged[item.dedupKey] = item;
  }
  for (final item in incoming) {
    final pendingClientId = item.clientMessageId;
    if (pendingClientId != null && byClientMessageId.containsKey(pendingClientId)) {
      // Reconciles an optimistic "sending" entry with the server-confirmed row —
      // remove the optimistic dedup key so it doesn't linger as a duplicate.
      final optimistic = byClientMessageId[pendingClientId]!;
      merged.remove(optimistic.dedupKey);
    }
    merged[item.dedupKey] = item;
  }

  final result = merged.values.toList()
    ..sort((a, b) => a.sortTimestamp.compareTo(b.sortTimestamp));
  return result;
}
