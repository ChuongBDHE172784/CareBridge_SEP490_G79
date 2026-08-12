// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({ get: vi.fn(), resolve: vi.fn(), navigate: vi.fn() }));
vi.mock('react-router-dom', () => ({ useNavigate: () => harness.navigate, useParams: () => ({ eventId: '12' }) }));
vi.mock('../services/securityIncidentApi', () => ({ getSecurityIncident: harness.get, resolveSecurityIncident: harness.resolve }));
import SecurityIncidentResolutionPage from './SecurityIncidentResolutionPage';

describe('security incident resolution', () => {
  beforeEach(() => {
    harness.resolve.mockReset(); harness.navigate.mockReset();
    harness.get.mockReset().mockResolvedValue({ id: 12, severity: 'HIGH', status: 'UNDER_REVIEW' });
  });
  afterEach(cleanup);

  it('requires complete resolution evidence and explicit confirmation', async () => {
    render(<SecurityIncidentResolutionPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Đóng sự cố' }));
    expect(harness.resolve).not.toHaveBeenCalled();
    expect(screen.getByText('Vui lòng chọn nguyên nhân gốc rễ.')).toBeTruthy();
    expect(screen.getByText('Cần xác nhận trước khi đóng.')).toBeTruthy();
  });

  it('submits all resolution fields after validation', async () => {
    harness.resolve.mockResolvedValue({ id: 12, status: 'RESOLVED', severity: 'HIGH' });
    render(<SecurityIncidentResolutionPage />);
    fireEvent.click(await screen.findByRole('button', { name: /Sai cấu hình/ }));
    fireEvent.change(screen.getByLabelText('2. Phạm vi bị ảnh hưởng'), { target: { value: 'Một session quản trị' } });
    fireEvent.change(screen.getByLabelText('3. Kết luận xử lý'), { target: { value: 'Đã sửa cấu hình và xác nhận không còn truy cập trái phép.' } });
    fireEvent.click(screen.getByText('Thu hồi token hoặc session bị ảnh hưởng'));
    fireEvent.click(screen.getByText('Tôi xác nhận thông tin đầy đủ và chính xác.'));
    fireEvent.click(screen.getByRole('button', { name: 'Đóng sự cố' }));
    await waitFor(() => expect(harness.resolve).toHaveBeenCalledWith('12', expect.objectContaining({ rootCause: 'MISCONFIGURATION', confirmed: true })));
    expect(await screen.findByText('Case SEC-12 đã được đóng')).toBeTruthy();
  });
});
