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
  messageBody?: string,
  messageType: 'TEXT' | 'IMAGE' | 'FILE' | 'LOCATION' = 'TEXT',
  attachmentId?: string,
  location?: { latitude: number; longitude: number; label?: string }
): Promise<TimelineItem> {
  const { data } = await apiClient.post(`/api/v1/direct-conversations/${conversationId}/messages`, {
    clientMessageId,
    ...(messageBody !== undefined ? { messageBody } : {}),
    messageType,
    ...(attachmentId ? { attachmentId } : {}),
    ...(location ? {
      locationLatitude: location.latitude,
      locationLongitude: location.longitude,
      ...(location.label ? { locationLabel: location.label } : {}),
    } : {}),
  });
  return data.data;
}

export interface DirectChatAttachment {
  fileId: string;
  originalName: string;
  mimeType: string;
  presignedUrl: string;
}

export async function uploadAttachment(
  conversationId: string,
  file: File,
  kind: 'IMAGE' | 'DOCUMENT'
): Promise<DirectChatAttachment> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await apiClient.post(
    `/api/v1/direct-conversations/${conversationId}/attachments?kind=${kind}`,
    form,
    { headers: { 'Content-Type': undefined } }
  );
  return data.data;
}

export async function viewAttachment(conversationId: string, messageId: string): Promise<DirectChatAttachment> {
  const { data } = await apiClient.get(
    `/api/v1/direct-conversations/${conversationId}/messages/${messageId}/attachment`
  );
  return data.data;
}

export async function recallMessage(conversationId: string, messageId: string): Promise<void> {
  await apiClient.patch(`/api/v1/direct-conversations/${conversationId}/messages/${messageId}/recall`);
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
