// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ChangePasswordPage from './ChangePasswordPage';
import ForgotPasswordPage from './ForgotPasswordPage';

const harness = vi.hoisted(() => ({
  changePassword: vi.fn(),
  forgotPassword: vi.fn(),
  resetPassword: vi.fn(),
}));

vi.mock('../services/authApi', () => harness);

describe('password recovery pages', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    harness.forgotPassword.mockResolvedValue({ message: 'sent', expiresIn: 900 });
    harness.resetPassword.mockResolvedValue({ message: 'reset' });
    harness.changePassword.mockResolvedValue(undefined);
  });

  afterEach(cleanup);

  it('requests a reset with a single contact and moves to the token form', async () => {
    render(<MemoryRouter initialEntries={['/forgot-password']}><ForgotPasswordPage /></MemoryRouter>);

    fireEvent.change(screen.getByLabelText(/email hoặc số điện thoại/i), {
      target: { value: ' user@example.com ' },
    });
    fireEvent.click(screen.getByRole('button', { name: /gửi hướng dẫn/i }));

    await waitFor(() => expect(harness.forgotPassword).toHaveBeenCalledWith({ contact: 'user@example.com' }));
    expect(await screen.findByLabelText(/mã đặt lại mật khẩu/i)).toBeTruthy();
    expect(screen.getByText(/nếu tài khoản tồn tại/i)).toBeTruthy();
  });

  it('accepts a token from the URL and redirects to login after reset', async () => {
    render(
      <MemoryRouter initialEntries={['/forgot-password?token=url-token']}>
        <Routes>
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/login" element={<div>Đăng nhập lại</div>} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByLabelText(/mã đặt lại mật khẩu/i)).toHaveProperty('value', 'url-token');
    fireEvent.change(screen.getByLabelText(/^mật khẩu mới$/i), { target: { value: 'NewPassword1!' } });
    fireEvent.change(screen.getByLabelText(/xác nhận mật khẩu mới/i), { target: { value: 'NewPassword1!' } });
    fireEvent.click(screen.getByRole('button', { name: /^đặt lại mật khẩu$/i }));

    await waitFor(() => expect(harness.resetPassword).toHaveBeenCalledWith({
      token: 'url-token',
      newPassword: 'NewPassword1!',
      confirmPassword: 'NewPassword1!',
    }));
    expect(await screen.findByText('Đăng nhập lại')).toBeTruthy();
  });

  it('changes the authenticated user password and reports session revocation', async () => {
    render(<ChangePasswordPage />);

    fireEvent.change(document.getElementById('current-password')!, { target: { value: 'CurrentPassword1!' } });
    fireEvent.change(document.getElementById('new-password')!, { target: { value: 'NewPassword1!' } });
    fireEvent.change(document.getElementById('confirm-password')!, { target: { value: 'NewPassword1!' } });
    fireEvent.click(screen.getByRole('button', { name: /đổi mật khẩu/i }));

    await waitFor(() => expect(harness.changePassword).toHaveBeenCalledWith({
      oldPassword: 'CurrentPassword1!',
      newPassword: 'NewPassword1!',
      confirmPassword: 'NewPassword1!',
    }));
    expect(await screen.findByText(/các phiên đăng nhập khác đã được thu hồi/i)).toBeTruthy();
  });

  it('toggles password visibility independently for each settings field', () => {
    render(<ChangePasswordPage />);

    const currentPassword = document.getElementById('current-password') as HTMLInputElement;
    const newPassword = document.getElementById('new-password') as HTMLInputElement;
    const confirmPassword = document.getElementById('confirm-password') as HTMLInputElement;

    expect(currentPassword.type).toBe('password');
    expect(newPassword.type).toBe('password');
    expect(confirmPassword.type).toBe('password');

    fireEvent.click(screen.getByRole('button', { name: /hiện mật khẩu mới/i }));

    expect(currentPassword.type).toBe('password');
    expect(newPassword.type).toBe('text');
    expect(confirmPassword.type).toBe('password');
  });

  it('validates password strength and confirmation match in real time', () => {
    render(<ChangePasswordPage />);

    const currentPassword = document.getElementById('current-password') as HTMLInputElement;
    const newPassword = document.getElementById('new-password') as HTMLInputElement;
    const confirmPassword = document.getElementById('confirm-password') as HTMLInputElement;
    const submitButton = screen.getByRole('button', { name: /đổi mật khẩu/i }) as HTMLButtonElement;

    fireEvent.change(currentPassword, { target: { value: 'CurrentPassword1!' } });
    fireEvent.change(newPassword, { target: { value: 'weak' } });

    expect(screen.getByText('Ít nhất 8 ký tự').closest('li')?.getAttribute('data-met')).toBe('false');
    expect(screen.getByText('Có ít nhất 1 chữ thường').closest('li')?.getAttribute('data-met')).toBe('true');
    expect(submitButton.disabled).toBe(true);

    fireEvent.change(newPassword, { target: { value: 'NewPassword1!' } });

    for (const requirement of screen.getByRole('list', { name: /yêu cầu mật khẩu mới/i }).querySelectorAll('li')) {
      expect(requirement.getAttribute('data-met')).toBe('true');
    }

    fireEvent.change(confirmPassword, { target: { value: 'DifferentPassword1!' } });
    expect(screen.getByTestId('password-match-status').textContent).toContain('chưa khớp');
    expect(submitButton.disabled).toBe(true);

    fireEvent.change(confirmPassword, { target: { value: 'NewPassword1!' } });
    expect(screen.getByTestId('password-match-status').textContent).toContain('đã khớp');
    expect(submitButton.disabled).toBe(false);
  });
});
