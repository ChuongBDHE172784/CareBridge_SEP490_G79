export interface DirectConversation {
  conversationId: string;
  motherUserId: string;
  expertUserId: string;
  status: string;
  createdAt: string;
  lastActivityAt: string | null;
  // ADR-DCC-007 §4: computed at response time — drives the read-only banner client-side.
  expertAvailable: boolean;
}

export interface DirectConversationSummary {
  conversationId: string;
  counterpartUserId: string;
  counterpartRole: 'MOTHER' | 'EXPERT';
  lastActivityAt: string | null;
  expertAvailable: boolean;
}

export interface TimelinePage {
  items: import('./timelineItem').TimelineItem[];
  nextCursor: string | null;
  hasMoreNewer: boolean;
  previousCursor: string | null;
  hasMoreOlder: boolean;
}

export interface ConversationCall {
  callId: string;
  conversationId: string;
  initiatedByUserId: string;
  callType: string;
  callStatus: string;
  durationSeconds: number | null;
  zegoRoomId: string | null;
  zegoToken: string | null;
}
