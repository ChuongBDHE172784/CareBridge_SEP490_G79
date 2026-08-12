// @vitest-environment jsdom

import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { AdminExercise } from '../models/adminExercise';

const harness = vi.hoisted(() => ({
  createAdminExercise: vi.fn(),
  updateAdminExercise: vi.fn(),
  toAdminExerciseRequestError: vi.fn(),
  navigate: vi.fn(),
}));

vi.mock('../services/adminExerciseApi', () => ({
  createAdminExercise: harness.createAdminExercise,
  updateAdminExercise: harness.updateAdminExercise,
  toAdminExerciseRequestError: harness.toAdminExerciseRequestError,
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => harness.navigate };
});

import CreatePregnancyExercisePage from './CreatePregnancyExercisePage';

function savedExercise(): AdminExercise {
  return {
    exerciseId: 'exercise-123',
    title: 'Yoga nhẹ nhàng',
    description: 'Bài tập thư giãn',
    trimesterScope: 'ALL',
    difficultyLevel: 'EASY',
    durationMinutes: 15,
    instructionContent: 'Thực hiện chậm rãi.',
    mediaUrl: null,
    safetyWarning: 'Dừng lại nếu thấy chóng mặt.',
    supportsPostureAnalysis: false,
    status: 'DRAFT',
    versionNo: 1,
    createdBy: 'admin-1',
    createdAt: '2026-08-10T00:00:00Z',
    updatedAt: '2026-08-10T00:00:00Z',
  };
}

async function fillRequiredFields() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText(/Tên Bài Tập/), 'Yoga nhẹ nhàng');
  await user.type(screen.getByLabelText(/Hướng dẫn an toàn/), 'Dừng lại nếu thấy chóng mặt.');
  return user;
}

describe('CreatePregnancyExercisePage', () => {
  beforeEach(() => {
    harness.createAdminExercise.mockReset();
    harness.updateAdminExercise.mockReset();
    harness.toAdminExerciseRequestError.mockReset();
    harness.navigate.mockReset();
    harness.toAdminExerciseRequestError.mockReturnValue({
      message: 'Không thể lưu bài tập.',
      fieldErrors: {},
    });
  });

  afterEach(() => cleanup());

  it('creates one draft, remains on the form, and defaults posture analysis off', async () => {
    harness.createAdminExercise.mockResolvedValue(savedExercise());
    render(<CreatePregnancyExercisePage />);
    const user = await fillRequiredFields();

    expect(screen.getByRole('switch', { name: 'Phân tích tư thế AI' })
      .getAttribute('aria-checked')).toBe('false');
    expect(screen.queryByText(/Gửi duyệt/)).toBeNull();
    expect(screen.queryByText(/Kéo thả/)).toBeNull();

    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));

    await waitFor(() => expect(harness.createAdminExercise).toHaveBeenCalledTimes(1));
    expect(harness.updateAdminExercise).not.toHaveBeenCalled();
    expect(harness.navigate).not.toHaveBeenCalled();
    expect((await screen.findByRole('status')).textContent).toContain('exercise-123');
    expect((screen.getByLabelText(/Tên Bài Tập/) as HTMLInputElement).value)
      .toBe('Yoga nhẹ nhàng');
  }, 15000);

  it('uses PUT with the returned ID when the saved draft is saved again', async () => {
    harness.createAdminExercise.mockResolvedValue(savedExercise());
    harness.updateAdminExercise.mockResolvedValue({ ...savedExercise(), versionNo: 2 });
    render(<CreatePregnancyExercisePage />);
    const user = await fillRequiredFields();

    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));
    await screen.findByRole('status');
    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));

    await waitFor(() => expect(harness.updateAdminExercise).toHaveBeenCalledTimes(1));
    expect(harness.createAdminExercise).toHaveBeenCalledTimes(1);
    expect(harness.updateAdminExercise).toHaveBeenCalledWith(
      'exercise-123',
      expect.objectContaining({ title: 'Yoga nhẹ nhàng' }),
    );
    expect(harness.navigate).not.toHaveBeenCalled();
  });

  it('persists before navigating to preview with the real exercise ID', async () => {
    harness.createAdminExercise.mockResolvedValue(savedExercise());
    render(<CreatePregnancyExercisePage />);
    const user = await fillRequiredFields();

    await user.click(screen.getByRole('button', { name: 'Lưu & xem trước' }));

    await waitFor(() => expect(harness.createAdminExercise).toHaveBeenCalledTimes(1));
    expect(harness.navigate).toHaveBeenCalledWith('/content/exercises/exercise-123/preview');
  });

  it('preserves fields, shows the backend code, and never navigates after a failed save', async () => {
    const failure = { isAxiosError: true };
    harness.createAdminExercise.mockRejectedValue(failure);
    harness.toAdminExerciseRequestError.mockReturnValue({
      code: 'EXERCISE_POSTURE_NOT_READY',
      message: '[EXERCISE_POSTURE_NOT_READY] Cấu hình tư thế chưa sẵn sàng.',
      fieldErrors: {},
    });
    render(<CreatePregnancyExercisePage />);
    const user = await fillRequiredFields();

    await user.click(screen.getByRole('button', { name: 'Lưu & xem trước' }));

    expect((await screen.findByRole('alert')).textContent).toContain('EXERCISE_POSTURE_NOT_READY');
    expect((screen.getByLabelText(/Tên Bài Tập/) as HTMLInputElement).value)
      .toBe('Yoga nhẹ nhàng');
    expect(harness.navigate).not.toHaveBeenCalled();
  });

  it('blocks invalid title, safety warning, and duration before making a request', async () => {
    render(<CreatePregnancyExercisePage />);
    fireEvent.change(screen.getByLabelText(/Tên Bài Tập/), {
      target: { value: 'T'.repeat(256) },
    });
    fireEvent.change(screen.getByLabelText(/Hướng dẫn an toàn/), {
      target: { value: 'A'.repeat(2001) },
    });
    fireEvent.change(screen.getByLabelText(/Thời lượng/), { target: { value: '181' } });

    fireEvent.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));

    expect(await screen.findByText(/255 ký tự/)).toBeTruthy();
    expect(screen.getByText(/2000 ký tự/)).toBeTruthy();
    expect(screen.getByText(/từ 1 đến 180 phút/)).toBeTruthy();
    expect(harness.createAdminExercise).not.toHaveBeenCalled();
    expect(harness.updateAdminExercise).not.toHaveBeenCalled();
  });

  it('uses a native focusable switch and focuses the first invalid field', async () => {
    const user = userEvent.setup();
    render(<CreatePregnancyExercisePage />);
    const postureSwitch = screen.getByRole('switch', { name: 'Phân tích tư thế AI' });

    postureSwitch.focus();
    expect(postureSwitch.tagName).toBe('BUTTON');
    expect(document.activeElement).toBe(postureSwitch);
    await user.click(postureSwitch);
    expect(postureSwitch.getAttribute('aria-checked')).toBe('true');

    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));
    await waitFor(() => expect(document.activeElement).toBe(screen.getByLabelText(/Tên Bài Tập/)));
    expect(harness.createAdminExercise).not.toHaveBeenCalled();
  });

  it('locks the form while a save request is pending', async () => {
    let resolveSave: (exercise: AdminExercise) => void = () => undefined;
    harness.createAdminExercise.mockReturnValue(new Promise((resolve) => {
      resolveSave = resolve;
    }));
    render(<CreatePregnancyExercisePage />);
    const user = await fillRequiredFields();
    const title = screen.getByLabelText(/Tên Bài Tập/) as HTMLInputElement;

    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));

    expect((title.closest('fieldset') as HTMLFieldSetElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'Hủy' }) as HTMLButtonElement).disabled).toBe(true);
    await act(async () => resolveSave(savedExercise()));
    await screen.findByRole('status');
    expect((title.closest('fieldset') as HTMLFieldSetElement).disabled).toBe(false);
  });

  it('rejects a mismatched identity returned by PUT instead of reporting success', async () => {
    harness.createAdminExercise.mockResolvedValue(savedExercise());
    harness.updateAdminExercise.mockResolvedValue({
      ...savedExercise(),
      exerciseId: 'different-exercise',
      versionNo: 2,
    });
    render(<CreatePregnancyExercisePage />);
    const user = await fillRequiredFields();

    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));
    await screen.findByRole('status');
    await user.click(screen.getByRole('button', { name: 'Lưu bản nháp' }));

    expect((await screen.findByRole('alert')).textContent).toContain('Không thể lưu bài tập');
    expect(screen.queryByRole('status')).toBeNull();
    expect(harness.navigate).not.toHaveBeenCalled();
  });
});
