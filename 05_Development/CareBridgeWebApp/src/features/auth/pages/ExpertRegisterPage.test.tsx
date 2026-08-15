// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

const harness = vi.hoisted(() => ({
  navigate: vi.fn(),
  registerExpert: vi.fn(),
}));

vi.mock('../services/authApi', () => ({
  registerExpert: harness.registerExpert,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>(
    'react-router-dom',
  );
  return { ...actual, useNavigate: () => harness.navigate };
});

import { MemoryRouter } from 'react-router-dom';
import ExpertRegisterPage from './ExpertRegisterPage';

describe('expert registration validation errors', () => {
  beforeEach(() => {
    harness.navigate.mockReset();
    harness.registerExpert.mockReset();
  });

  afterEach(cleanup);

  it('renders backend validation details below the matching field', async () => {
    harness.registerExpert.mockRejectedValue({
      response: {
        data: {
          message: 'Invalid request',
          details: [
            {
              field: 'phone',
              message: 'Invalid Vietnamese phone number',
            },
          ],
        },
      },
    });

    render(
      <MemoryRouter>
        <ExpertRegisterPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText('Họ và tên *'), {
      target: { value: 'Bác sĩ Test' },
    });
    fireEvent.change(screen.getByLabelText('Email *'), {
      target: { value: 'expert@example.com' },
    });
    fireEvent.change(screen.getByLabelText('Số điện thoại *'), {
      target: { value: '+84912345678' },
    });
    fireEvent.change(screen.getByLabelText('Mật khẩu *'), {
      target: { value: 'Password1' },
    });
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu *'), {
      target: { value: 'Password1' },
    });
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký ngay' }));

    const phoneError = await screen.findByText('Invalid Vietnamese phone number');
    const phoneInput = document.getElementById('expert-register-phone');
    expect(phoneError.id).toBe('expert-register-phone-error');
    expect(phoneInput?.getAttribute('aria-describedby'))
      .toBe('expert-register-phone-error');
    expect(phoneInput?.getAttribute('aria-invalid')).toBe('true');
    expect(screen.queryByText('Invalid request')).toBeNull();

    fireEvent.change(phoneInput!, {
      target: { value: '+84912345679' },
    });
    await waitFor(() => {
      expect(screen.queryByText('Invalid Vietnamese phone number')).toBeNull();
    });
  });

  it('blocks registration until the legal consent box is ticked', async () => {
    render(
      <MemoryRouter>
        <ExpertRegisterPage />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByLabelText('Họ và tên *'), {
      target: { value: 'Bác sĩ Test' },
    });
    fireEvent.change(screen.getByLabelText('Email *'), {
      target: { value: 'expert@example.com' },
    });
    fireEvent.change(screen.getByLabelText('Số điện thoại *'), {
      target: { value: '+84912345678' },
    });
    fireEvent.change(screen.getByLabelText('Mật khẩu *'), {
      target: { value: 'Password1' },
    });
    fireEvent.change(screen.getByLabelText('Nhập lại mật khẩu *'), {
      target: { value: 'Password1' },
    });

    // Hồ sơ hợp lệ nhưng chưa chấp thuận: không được gọi API, vì chưa có căn cứ
    // hợp pháp để xử lý dữ liệu cá nhân.
    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký ngay' }));

    const consentError = await screen.findByText(
      'Bạn cần đồng ý với Điều khoản sử dụng và Chính sách bảo mật để tiếp tục.',
    );
    expect(consentError.id).toBe('expert-register-terms-error');
    expect(harness.registerExpert).not.toHaveBeenCalled();

    // Tích vào ô thì lỗi biến mất và yêu cầu được gửi đi.
    harness.registerExpert.mockResolvedValue({ userId: 'u-1', otpExpiresAt: null });
    fireEvent.click(screen.getByRole('checkbox'));
    await waitFor(() => {
      expect(
        screen.queryByText('Bạn cần đồng ý với Điều khoản sử dụng và Chính sách bảo mật để tiếp tục.'),
      ).toBeNull();
    });

    fireEvent.click(screen.getByRole('button', { name: 'Đăng ký ngay' }));
    await waitFor(() => {
      expect(harness.registerExpert).toHaveBeenCalledTimes(1);
    });
  });

  it('points the consent links at the public legal pages', () => {
    render(
      <MemoryRouter>
        <ExpertRegisterPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('link', { name: 'Điều khoản sử dụng' }).getAttribute('href'))
      .toBe('/terms-of-service');
    expect(screen.getByRole('link', { name: 'Chính sách bảo mật' }).getAttribute('href'))
      .toBe('/privacy-policy');
  });
});
