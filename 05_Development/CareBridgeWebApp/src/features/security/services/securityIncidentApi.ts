import apiClient from '../../../shared/api/apiClient';

export type IncidentStatus = 'OPEN' | 'UNDER_REVIEW' | 'RESOLVED' | 'FALSE_POSITIVE';

export interface SecurityEvent {
  id: number;
  eventType: string;
  userId: string | null;
  ipAddress: string | null;
  severity: string;
  status: IncidentStatus;
  details: string | null;
  correlationId: string | null;
  reviewedBy: string | null;
  reviewedAt: string | null;
  occurredAt: string;
}

export interface SecurityEventNote {
  noteId: string;
  eventId: number;
  authorId: string;
  noteText: string;
  createdAt: string;
}

export interface IncidentPage {
  content: SecurityEvent[];
  totalElements: number;
  totalPages: number;
  number: number;
}

interface ApiEnvelope<T> { data: T; }

export async function searchSecurityIncidents(params: {
  page: number;
  size?: number;
  severity?: string;
  status?: string;
  eventType?: string;
}): Promise<IncidentPage> {
  const query = new URLSearchParams({ page: String(params.page), size: String(params.size ?? 10) });
  if (params.severity) query.set('severity', params.severity);
  if (params.status) query.set('status', params.status);
  if (params.eventType) query.set('eventType', params.eventType);
  const response = await apiClient.get<ApiEnvelope<IncidentPage>>(`/api/v1/admin/security-events?${query}`);
  return response.data.data;
}

export async function getSecurityIncident(eventId: string): Promise<SecurityEvent> {
  const response = await apiClient.get<ApiEnvelope<SecurityEvent>>(`/api/v1/admin/security-events/${eventId}`);
  return response.data.data;
}

export async function getSecurityIncidentTimeline(correlationId: string): Promise<SecurityEvent[]> {
  const response = await apiClient.get<ApiEnvelope<SecurityEvent[]>>(
    `/api/v1/admin/security-events/timeline?correlationId=${encodeURIComponent(correlationId)}`,
  );
  return response.data.data;
}

export async function getSecurityIncidentNotes(eventId: string): Promise<SecurityEventNote[]> {
  const response = await apiClient.get<ApiEnvelope<SecurityEventNote[]>>(`/api/v1/admin/security-events/${eventId}/notes`);
  return response.data.data;
}

export async function addSecurityIncidentNote(eventId: string, noteText: string): Promise<SecurityEventNote> {
  const response = await apiClient.post<ApiEnvelope<SecurityEventNote>>(
    `/api/v1/admin/security-events/${eventId}/notes`,
    { noteText },
  );
  return response.data.data;
}

export async function startSecurityIncidentReview(eventId: string): Promise<SecurityEvent> {
  const response = await apiClient.put<ApiEnvelope<SecurityEvent>>(
    `/api/v1/admin/security-events/${eventId}/review`,
    { status: 'UNDER_REVIEW' },
  );
  return response.data.data;
}

export async function resolveSecurityIncident(eventId: string, input: {
  rootCause: string;
  summary: string;
  affectedScope: string;
  remediationTasks: string[];
  notifyAffected: boolean;
  confirmed: boolean;
}): Promise<SecurityEvent> {
  const response = await apiClient.put<ApiEnvelope<SecurityEvent>>(
    `/api/v1/admin/security-events/${eventId}/resolve`, input,
  );
  return response.data.data;
}
