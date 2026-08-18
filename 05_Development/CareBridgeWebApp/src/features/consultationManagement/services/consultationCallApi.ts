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

  const response = await apiClient.get<PaginatedConsultationCalls>(
    '/api/v1/admin/consultation-calls',
    { params }
  );
  return response.data;
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
