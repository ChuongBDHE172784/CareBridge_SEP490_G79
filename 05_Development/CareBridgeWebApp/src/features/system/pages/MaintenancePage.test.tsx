// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import MaintenancePage from './MaintenancePage';

describe('maintenance page', () => {
  afterEach(cleanup);

  it('explains the outage without implying the session was cleared', () => {
    render(<MaintenancePage />);

    expect(screen.getByRole('heading', { name: 'CareBridge đang bảo trì' })).toBeTruthy();
    expect(screen.getByText(/Phiên đăng nhập của bạn vẫn được giữ nguyên/)).toBeTruthy();
    expect(screen.getByRole('button', { name: 'Kiểm tra lại' })).toBeTruthy();
  });
});
