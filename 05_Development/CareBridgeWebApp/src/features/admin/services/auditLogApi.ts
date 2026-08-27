import apiClient from '../../../shared/api/apiClient';
import type { ApiResponse } from '../../auth/models/user';
import type { AuditLogEntry, AuditLogSearchParams } from '../models/auditLog';
import type { PaginatedResult } from '../models/adminUser';

type PageEnvelope<T> = ApiResponse<T[]> & {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

// UC117 — GET /api/v1/admin/audit-logs
export async function searchAuditLogs(
  params: AuditLogSearchParams,
): Promise<PaginatedResult<AuditLogEntry>> {
  const q = new URLSearchParams();
  if (params.userId) q.set('userId', params.userId);
  if (params.action) q.set('action', params.action);
  if (params.fromDate) q.set('fromDate', params.fromDate);
  if (params.toDate) q.set('toDate', params.toDate);
  q.set('page', String(params.page ?? 0));
  q.set('size', String(params.size ?? 20));

  const res = await apiClient.get<PageEnvelope<AuditLogEntry>>(`/api/v1/admin/audit-logs?${q}`);
  const body = res.data;
  return {
    content: body.data ?? [],
    totalElements: body.totalElements ?? 0,
    totalPages: body.totalPages ?? 0,
    page: body.page ?? (params.page ?? 0),
    size: body.size ?? (params.size ?? 20),
  };
}
