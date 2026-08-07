import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import BlockedAccountPage from './BlockedAccountPage';
import { SUPPORT_EMAIL } from '../../../shared/config/support';

const STORAGE_KEY = 'carebridge.blocked-account';

afterEach(() => {
  cleanup();
  sessionStorage.clear();
});

describe('BlockedAccountPage', () => {
  it('points an administratively locked user at customer support', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      code: 'ACCOUNT_ADMIN_LOCKED',
      reason: 'Vi phạm quy tắc cộng đồng.',
    }));

    render(<MemoryRouter><BlockedAccountPage /></MemoryRouter>);

    expect(screen.getByText('Vi phạm quy tắc cộng đồng.')).toBeTruthy();
    expect(screen.getByText(/Cần mở lại tài khoản\?/)).toBeTruthy();
    expect(screen.getByText(SUPPORT_EMAIL)).toBeTruthy();
    // The in-app appeal workflow was retired.
    expect(screen.queryByLabelText('Nội dung khiếu nại')).toBeNull();
  });

  it('does not send a temporarily locked user to support — the lock clears itself', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      code: 'ACCOUNT_TEMPORARILY_LOCKED',
      lockType: 'TEMPORARY',
      retryAt: '2026-08-06T09:00:00Z',
    }));

    render(<MemoryRouter><BlockedAccountPage /></MemoryRouter>);

    expect(screen.getByText(/Có thể thử lại sau/)).toBeTruthy();
    expect(screen.queryByText(/Cần mở lại tài khoản\?/)).toBeNull();
  });

  it('ignores appeal metadata left over from an older server build', () => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
      code: 'ACCOUNT_ADMIN_LOCKED',
      appealAllowed: true,
      appealToken: 'stale-token',
      appealStatus: 'PENDING',
    }));

    render(<MemoryRouter><BlockedAccountPage /></MemoryRouter>);

    expect(screen.queryByLabelText('Nội dung khiếu nại')).toBeNull();
    expect(screen.getByText(SUPPORT_EMAIL)).toBeTruthy();
  });
});
