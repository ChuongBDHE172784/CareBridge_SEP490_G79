// @vitest-environment jsdom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { SystemConfiguration } from '../services/systemConfigurationApi';

const harness = vi.hoisted(() => ({
  fetchSystemConfiguration: vi.fn(),
  saveSystemConfiguration: vi.fn(),
}));

vi.mock('../services/systemConfigurationApi', () => ({
  fetchSystemConfiguration: harness.fetchSystemConfiguration,
  saveSystemConfiguration: harness.saveSystemConfiguration,
}));

import SystemConfigurationPage from './SystemConfigurationPage';

function configuration(overrides: Partial<SystemConfiguration> = {}): SystemConfiguration {
  return {
    id: '10000000-0000-0000-0000-000000000001',
    aiModerationEnabled: true,
    maintenanceModeEnabled: false,
    rowVersion: 3,
    updatedBy: '10000000-0000-0000-0000-000000000002',
    updatedAt: '2026-07-28T02:00:00Z',
    ...overrides,
  };
}

describe('system configuration page', () => {
  beforeEach(() => {
    harness.fetchSystemConfiguration.mockReset();
    harness.saveSystemConfiguration.mockReset();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it('shows only operational settings and keeps save disabled while unchanged', async () => {
    harness.fetchSystemConfiguration.mockResolvedValue(configuration());

    render(<SystemConfigurationPage />);

    expect(await screen.findByRole('switch', { name: 'Kiểm duyệt tự động AI' })).toBeTruthy();
    expect(screen.getByRole('switch', { name: 'Chế độ bảo trì hệ thống' })).toBeTruthy();
    expect(screen.queryByText('Slack / Teams Webhook')).toBeNull();
    expect(screen.queryByLabelText('Tải trọng tối đa API (req/s)')).toBeNull();
    expect(screen.queryByText('Đã tinh gọn')).toBeNull();
    expect(screen.getByText('Cấu hình trên màn hình đang đồng bộ với máy chủ.')).toBeTruthy();
    expect(screen.getByText('#3')).toBeTruthy();
    expect((screen.getByRole('button', { name: 'Lưu toàn bộ cấu hình' }) as HTMLButtonElement).disabled).toBe(true);
  });

  it('opens the mechanism guide with an accessible button', async () => {
    harness.fetchSystemConfiguration.mockResolvedValue(configuration());
    render(<SystemConfigurationPage />);
    const user = userEvent.setup();

    const guide = await screen.findByRole('button', { name: 'Giải thích: Kiểm duyệt tự động AI' });
    await user.hover(guide);
    await user.click(guide);

    expect(guide.getAttribute('aria-expanded')).toBe('true');
    const describedBy = guide.getAttribute('aria-describedby');
    expect(describedBy).toBeTruthy();
    expect(document.getElementById(describedBy!)?.textContent).toContain('Hàng đợi được giữ nguyên');

    await user.keyboard('{Escape}');
    expect(guide.getAttribute('aria-expanded')).toBe('false');

    await user.click(guide);
    expect(guide.getAttribute('aria-expanded')).toBe('true');
    await user.click(guide);
    expect(guide.getAttribute('aria-expanded')).toBe('false');
  });

  it('sends only operational flags and rowVersion', async () => {
    harness.fetchSystemConfiguration.mockResolvedValue(configuration());
    harness.saveSystemConfiguration.mockResolvedValue(configuration({ aiModerationEnabled: false, rowVersion: 4 }));
    render(<SystemConfigurationPage />);

    fireEvent.click(await screen.findByRole('switch', { name: 'Kiểm duyệt tự động AI' }));

    expect(screen.getByText('1 thay đổi đang chờ lưu.')).toBeTruthy();
    expect(screen.getByText('Thay đổi chưa tác động đến hệ thống cho đến khi bạn lưu.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Lưu toàn bộ cấu hình' }));

    await waitFor(() => expect(harness.saveSystemConfiguration).toHaveBeenCalledWith({
      aiModerationEnabled: false,
      maintenanceModeEnabled: false,
      rowVersion: 3,
    }));
    expect(await screen.findByText('Đã lưu cấu hình hệ thống thành công.')).toBeTruthy();
    expect(screen.getByText('#4')).toBeTruthy();
  });

  it('requires confirmation before enabling maintenance and discards locally', async () => {
    harness.fetchSystemConfiguration.mockResolvedValue(configuration());
    render(<SystemConfigurationPage />);

    const maintenance = await screen.findByRole('switch', { name: 'Chế độ bảo trì hệ thống' });
    fireEvent.click(maintenance);
    expect(window.confirm).toHaveBeenCalled();
    expect(maintenance.getAttribute('aria-checked')).toBe('true');
    expect(screen.getByText('Sau khi lưu, người dùng thông thường sẽ bị gián đoạn truy cập.')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Hủy thay đổi' }));

    expect(screen.getByRole('switch', { name: 'Chế độ bảo trì hệ thống' }).getAttribute('aria-checked')).toBe('false');
    expect(screen.getByText('Cấu hình trên màn hình đang đồng bộ với máy chủ.')).toBeTruthy();
    expect(harness.fetchSystemConfiguration).toHaveBeenCalledTimes(1);
  });

  it('blocks edits and clears stale notices while a refresh is pending', async () => {
    let finishRefresh!: (value: SystemConfiguration) => void;
    harness.fetchSystemConfiguration
      .mockResolvedValueOnce(configuration())
      .mockImplementationOnce(() => new Promise((resolve) => {
        finishRefresh = resolve;
      }));
    render(<SystemConfigurationPage />);

    const aiToggle = await screen.findByRole('switch', { name: 'Kiểm duyệt tự động AI' });
    fireEvent.click(aiToggle);
    fireEvent.click(screen.getByRole('button', { name: 'Hủy thay đổi' }));
    expect(screen.getByText('Đã hủy các thay đổi chưa lưu.')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Tải lại dữ liệu' }));

    await waitFor(() => {
      expect((aiToggle as HTMLButtonElement).disabled).toBe(true);
      expect(screen.queryByText('Đã hủy các thay đổi chưa lưu.')).toBeNull();
    });

    await act(async () => finishRefresh(configuration({ rowVersion: 4 })));
    await waitFor(() => expect((aiToggle as HTMLButtonElement).disabled).toBe(false));
    expect(screen.getByText('#4')).toBeTruthy();
  });

  it('shows a persistent operational warning when maintenance is already active', async () => {
    harness.fetchSystemConfiguration.mockResolvedValue(configuration({ maintenanceModeEnabled: true }));
    render(<SystemConfigurationPage />);

    expect(await screen.findByText('Hệ thống đang bảo trì; người dùng thông thường hiện không thể truy cập API.')).toBeTruthy();
    expect(screen.getByText('Đang bảo trì')).toBeTruthy();
  });
});
