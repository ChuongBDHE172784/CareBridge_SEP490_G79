import apiClient from '../../../shared/api/apiClient';
import type {
  ConversationCall,
  DirectConversation,
  DirectConversationSummary,
  TimelinePage,
  ZegoJoinCredentials,
} from '../models/directConversation';
import type { TimelineItem } from '../models/timelineItem';

export async function listMyConversations(): Promise<DirectConversationSummary[]> {
  const { data } = await apiClient.get('/api/v1/direct-conversations');
  return data.data;
}

export async function getConversation(conversationId: string): Promise<DirectConversation> {
  const { data } = await apiClient.get(`/api/v1/direct-conversations/${conversationId}`);
  return data.data;
}

export async function getTimeline(
  conversationId: string,
  params: { after?: string; before?: string; limit?: number } = {}
): Promise<TimelinePage> {
  const { data } = await apiClient.get(`/api/v1/direct-conversations/${conversationId}/timeline`, {
    params: { limit: 30, ...params },
  });
  return data.data;
}

// BR-DCC-005: idempotent under retry with the same clientMessageId.
export async function sendMessage(
  conversationId: string,
  clientMessageId: string,
  messageBody: string
): Promise<TimelineItem> {
  const { data } = await apiClient.post(`/api/v1/direct-conversations/${conversationId}/messages`, {
    clientMessageId,
    messageBody,
  });
  return data.data;
}

export async function initiateCall(
  conversationId: string,
  callType: 'VOICE' | 'VIDEO'
): Promise<ConversationCall> {
  const { data } = await apiClient.post(`/api/v1/direct-conversations/${conversationId}/calls`, {
    callType,
  });
  return data.data;
}

export async function getCall(
  conversationId: string,
  callId: string
): Promise<ConversationCall> {
  const { data } = await apiClient.get(
    `/api/v1/direct-conversations/${conversationId}/calls/${callId}`
  );
  return data.data;
}

export async function listActiveCalls(): Promise<ConversationCall[]> {
  const { data } = await apiClient.get('/api/v1/direct-conversations/calls/active');
  return data.data;
}

async function patchCall(conversationId: string, callId: string, action: string): Promise<ConversationCall> {
  const { data } = await apiClient.patch(
    `/api/v1/direct-conversations/${conversationId}/calls/${callId}/${action}`
  );
  return data.data;
}

export const markRinging = (conversationId: string, callId: string) => patchCall(conversationId, callId, 'ringing');
export const answerCall = (conversationId: string, callId: string) => patchCall(conversationId, callId, 'answer');
export const declineCall = (conversationId: string, callId: string) => patchCall(conversationId, callId, 'decline');
export const endCall = (conversationId: string, callId: string) => patchCall(conversationId, callId, 'end');

export async function issueJoinCredentials(
  conversationId: string,
  callId: string
): Promise<ZegoJoinCredentials> {
  const { data } = await apiClient.post(
    `/api/v1/direct-conversations/${conversationId}/calls/${callId}/join-credentials`
  );
  return data.data;
}
