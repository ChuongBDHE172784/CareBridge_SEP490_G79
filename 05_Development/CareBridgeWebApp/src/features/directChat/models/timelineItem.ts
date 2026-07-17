export type ChatSendStatus = 'sending' | 'sent' | 'failed';

/** Unified MESSAGE / CALL_EVENT timeline entry — mirrors backend TimelineItemResponse (TDS §9.2). */
export interface TimelineItem {
  kind: 'MESSAGE' | 'CALL_EVENT';

  // MESSAGE fields
  messageId?: string;
  clientMessageId?: string;
  senderUserId?: string;
  messageType?: string;
  messageBody?: string;
  createdAt?: string; // ISO instant

  // CALL_EVENT fields
  callId?: string;
  callType?: string;
  callStatus?: string;
  initiatedByUserId?: string;
  durationSeconds?: number;
  initiatedAt?: string;
  answeredAt?: string;
  endedAt?: string;

  // client-only
  sendStatus?: ChatSendStatus;
}

export function optimisticMessage(params: {
  clientMessageId: string;
  senderUserId: string;
  messageBody: string;
}): TimelineItem {
  return {
    kind: 'MESSAGE',
    clientMessageId: params.clientMessageId,
    senderUserId: params.senderUserId,
    messageType: 'TEXT',
    messageBody: params.messageBody,
    createdAt: new Date().toISOString(),
    sendStatus: 'sending',
  };
}

function dedupKey(item: TimelineItem): string {
  if (item.kind === 'CALL_EVENT') return `CALL:${item.callId}`;
  return `MESSAGE:${item.messageId ?? item.clientMessageId}`;
}

function sortTimestamp(item: TimelineItem): number {
  const raw = item.kind === 'CALL_EVENT' ? item.initiatedAt : item.createdAt;
  return raw ? new Date(raw).getTime() : Date.now();
}

/**
 * Merges a freshly-fetched/reconciled page into the current list without duplicating
 * optimistic entries: an incoming item matching an existing "sending" entry by
 * clientMessageId replaces it; everything else is deduped by dedupKey and re-sorted.
 */
export function mergeTimelineItems(existing: TimelineItem[], incoming: TimelineItem[]): TimelineItem[] {
  const byClientMessageId = new Map<string, TimelineItem>();
  for (const item of existing) {
    if (item.clientMessageId) byClientMessageId.set(item.clientMessageId, item);
  }

  const merged = new Map<string, TimelineItem>();
  for (const item of existing) {
    merged.set(dedupKey(item), item);
  }
  for (const item of incoming) {
    const pendingClientId = item.clientMessageId;
    if (pendingClientId && byClientMessageId.has(pendingClientId)) {
      const optimistic = byClientMessageId.get(pendingClientId)!;
      merged.delete(dedupKey(optimistic));
    }
    merged.set(dedupKey(item), item);
  }

  return Array.from(merged.values()).sort((a, b) => sortTimestamp(a) - sortTimestamp(b));
}
