// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({ search: vi.fn(), navigate: vi.fn() }));
vi.mock('react-router-dom', () => ({ useNavigate: () => harness.navigate }));
vi.mock('../services/securityIncidentApi', () => ({ searchSecurityIncidents: harness.search }));

import SecurityIncidentListPage from './SecurityIncidentListPage';

describe('security incident list', () => {
  beforeEach(() => {
    harness.navigate.mockReset();
    harness.search.mockReset().mockResolvedValue({
      content: [{ id: 12, eventType: 'LOGIN_FAILED', userId: null, ipAddress: '203.0.113.7', severity: 'HIGH', status: 'OPEN', details: null, correlationId: null, reviewedBy: null, reviewedAt: null, occurredAt: '2026-08-12T08:00:00Z' }],
      totalElements: 1, totalPages: 1, number: 0,
    });
  });
  afterEach(cleanup);

  it('uses canonical backend statuses and opens an investigation', async () => {
    render(<SecurityIncidentListPage />);
    fireEvent.click(await screen.findByRole('button', { name: 'Mở điều tra' }));
    expect(harness.navigate).toHaveBeenCalledWith('/admin/security/incidents/12/investigate');
    fireEvent.change(screen.getByLabelText('Lọc trạng thái'), { target: { value: 'UNDER_REVIEW' } });
    await waitFor(() => expect(harness.search).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'UNDER_REVIEW' })));
  });

  it('filters the loaded page by case id', async () => {
    render(<SecurityIncidentListPage />);
    await screen.findByText('SEC-12');
    fireEvent.change(screen.getByPlaceholderText('Tìm Case ID, loại, IP...'), { target: { value: 'SEC-999' } });
    expect(screen.getByText('Không có sự cố phù hợp với bộ lọc hiện tại.')).toBeTruthy();
  });
});
