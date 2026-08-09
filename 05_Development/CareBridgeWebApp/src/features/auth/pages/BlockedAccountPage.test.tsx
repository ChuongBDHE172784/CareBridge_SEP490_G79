import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import BlockedAccountPage from './BlockedAccountPage';

const STORAGE_KEY = 'carebridge.blocked-account';

afterEach(() => {
  cleanup();
  sessionStorage.clear();
});
describe('BlockedAccountPage', () => {
  it('shows the submitted appeal status when a new login reports a pending appeal', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      code: 'ACCOUNT_ADMIN_LOCKED',
      reason: 'Vi phạm quy tắc cộng đồng.',
      appealAllowed: false,
      appealPending: true,
    }));

    render(<MemoryRouter><BlockedAccountPage /></MemoryRouter>);

    expect(screen.getByText(/Khiếu nại đã được gửi/)).toBeTruthy();
    expect(screen.queryByLabelText('Nội dung khiếu nại')).toBeNull();
  });

  it('shows the rejection status instead of allowing another appeal for the same lock episode', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      code: 'ACCOUNT_ADMIN_LOCKED',
      appealAllowed: false,
      appealStatus: 'REJECTED',
    }));

    render(<MemoryRouter><BlockedAccountPage /></MemoryRouter>);

    expect(screen.getByText(/Khiếu nại mở khóa đã bị từ chối/)).toBeTruthy();
    expect(screen.queryByLabelText('Nội dung khiếu nại')).toBeNull();
  });
});
