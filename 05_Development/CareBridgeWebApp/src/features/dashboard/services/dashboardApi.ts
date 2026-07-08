import apiClient from '../../../shared/api/apiClient';
import type { CommunityDashboardResponse, ImpactReportResponse, DashboardDateRange } from '../models/dashboard';

// CommunityDashboardController / ImpactReportController return the DTO directly
// (ResponseEntity.ok(response)) — no ApiResponse envelope, unlike the red-flag-rules endpoints.
export async function fetchCommunityDashboard(range: DashboardDateRange): Promise<CommunityDashboardResponse> {
  const res = await apiClient.get<CommunityDashboardResponse>('/api/v1/admin/community/dashboard', {
    params: { from: range.from, to: range.to },
  });
  return res.data;
}

export async function fetchImpactReport(range: DashboardDateRange): Promise<ImpactReportResponse> {
  const res = await apiClient.get<ImpactReportResponse>('/api/v1/admin/impact-report', {
    params: { from: range.from, to: range.to },
  });
  return res.data;
}
