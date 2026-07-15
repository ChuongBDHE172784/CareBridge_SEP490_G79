/// Client-side shape of the minimal Firebase RTDB payload (ADR-DCC-004 §1) — exactly the
/// 5 fields the backend ever writes. Signal-only: never trust anything beyond "something
/// changed"; the client always reconciles via REST (`GET /timeline`), never renders this
/// payload directly.
class ConversationEventSignal {
  final String eventId;
  final String eventType;
  final String conversationId;
  final String resourceId;
  final DateTime occurredAt;

  const ConversationEventSignal({
    required this.eventId,
    required this.eventType,
    required this.conversationId,
    required this.resourceId,
    required this.occurredAt,
  });

  factory ConversationEventSignal.fromSnapshotValue(Map<Object?, Object?> raw) {
    return ConversationEventSignal(
      eventId: raw['eventId'] as String,
      eventType: raw['eventType'] as String,
      conversationId: raw['conversationId'] as String,
      resourceId: raw['resourceId'] as String,
      occurredAt: DateTime.fromMillisecondsSinceEpoch(
        (raw['occurredAt'] as num).toInt(),
        isUtc: true,
      ),
    );
  }
}
