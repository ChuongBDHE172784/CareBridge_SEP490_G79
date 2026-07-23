import apiClient from '../../../shared/api/apiClient';
import type { CommunityDashboardResponse, DashboardDateRange } from '../models/dashboard';

// CommunityDashboardController returns the DTO directly
// (ResponseEntity.ok(response)) — no ApiResponse envelope, unlike the red-flag-rules endpoints.
export async function fetchCommunityDashboard(range: DashboardDateRange): Promise<CommunityDashboardResponse> {
  const res = await apiClient.get<CommunityDashboardResponse>('/api/v1/admin/community/dashboard', {
    params: { from: range.from, to: range.to },
  });
  return res.data;
}
