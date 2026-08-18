import apiClient from '../../../shared/api/apiClient';
import type {
  ConsultationCallAdminSummary,
  ConsultationCallSearchQuery,
  PaginatedConsultationCalls,
} from '../models/consultationCall';

export async function searchConsultationCalls(
  query: ConsultationCallSearchQuery = {}
): Promise<PaginatedConsultationCalls> {
  const params: Record<string, string | number | boolean | undefined> = {
    keyword: query.keyword || undefined,
    callType: query.callType || undefined,
    callStatus: query.callStatus || undefined,
    hasRecording: query.hasRecording !== undefined ? query.hasRecording : undefined,
    fromDate: query.fromDate || undefined,
    toDate: query.toDate || undefined,
    page: query.page ?? 0,
    size: query.size ?? 10,
  };

  const response = await apiClient.get<{
    success: boolean;
    data: ConsultationCallAdminSummary[];
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  }>('/api/v1/admin/consultation-calls', { params });

  const body = response.data;
  return {
    content: body.data || [],
    totalElements: body.totalElements || 0,
    totalPages: body.totalPages || 0,
    size: body.size || (query.size ?? 10),
    number: body.page || (query.page ?? 0),
  };
}

export async function getConsultationCallDetail(
  callId: string
): Promise<ConsultationCallAdminSummary> {
  const response = await apiClient.get<{ data: ConsultationCallAdminSummary }>(
    `/api/v1/admin/consultation-calls/${callId}`
  );
  return response.data.data;
}

export async function getCallRecordingPresignedUrl(
  callId: string
): Promise<string> {
  const response = await apiClient.get<{ data: { url: string } }>(
    `/api/v1/admin/consultation-calls/${callId}/recording-url`
  );
  return response.data.data.url;
}
