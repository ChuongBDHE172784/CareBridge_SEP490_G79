/** Client-side shape of the minimal Firestore payload (ADR-DCC-004 §1) — the exact 5 fields
 * the backend ever writes. Signal-only: never trust anything beyond "something changed";
 * the client always reconciles via REST (GET /timeline), never renders this directly. */
export interface ConversationEventSignal {
  eventId: string;
  eventType: string;
  conversationId: string;
  resourceId: string;
  occurredAt: string; // ISO instant, converted from the epoch-millis wire value
}

export function parseConversationEventSignal(raw: unknown): ConversationEventSignal | null {
  if (typeof raw !== 'object' || raw === null) return null;
  const value = raw as Record<string, unknown>;
  if (
    typeof value.eventId !== 'string' ||
    typeof value.eventType !== 'string' ||
    typeof value.conversationId !== 'string' ||
    typeof value.resourceId !== 'string' ||
    typeof value.occurredAt !== 'number'
  ) {
    return null;
  }
  return {
    eventId: value.eventId,
    eventType: value.eventType,
    conversationId: value.conversationId,
    resourceId: value.resourceId,
    occurredAt: new Date(value.occurredAt).toISOString(),
  };
}
