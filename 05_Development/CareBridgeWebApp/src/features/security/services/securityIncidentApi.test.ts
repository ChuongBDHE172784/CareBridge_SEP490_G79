import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import { addSecurityIncidentNote, resolveSecurityIncident } from './securityIncidentApi';

vi.mock('../../../shared/api/apiClient', () => ({ default: { post: vi.fn(), put: vi.fn() } }));

describe('security incident API contract', () => {
  beforeEach(() => vi.clearAllMocks());

  it('sends noteText expected by the backend', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { noteId: 'n1' } } });
    await addSecurityIncidentNote('12', 'Kiểm tra IP nguồn');
    expect(apiClient.post).toHaveBeenCalledWith('/api/v1/admin/security-events/12/notes', { noteText: 'Kiểm tra IP nguồn' });
  });

  it('persists the complete resolution instead of dropping form fields', async () => {
    vi.mocked(apiClient.put).mockResolvedValue({ data: { data: { id: 12, status: 'RESOLVED' } } });
    const input = { rootCause: 'MISCONFIGURATION', summary: 'Đã sửa cấu hình và xác nhận an toàn.', affectedScope: 'Một session quản trị', remediationTasks: ['Thu hồi session'], notifyAffected: false, confirmed: true };
    await resolveSecurityIncident('12', input);
    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/admin/security-events/12/resolve', input);
  });
});
