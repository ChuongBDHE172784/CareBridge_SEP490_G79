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
});
