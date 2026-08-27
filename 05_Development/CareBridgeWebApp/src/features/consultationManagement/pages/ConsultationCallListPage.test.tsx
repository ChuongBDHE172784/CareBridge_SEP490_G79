import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ConsultationCallAdminSummary } from '../models/consultationCall';
import ConsultationCallListPage from './ConsultationCallListPage';

const searchCalls = vi.fn();
const deleteRecording = vi.fn();

vi.mock('../services/consultationCallApi', () => ({
  searchConsultationCalls: (...args: unknown[]) => searchCalls(...args),
  deleteConsultationCallRecording: (...args: unknown[]) => deleteRecording(...args),
  getCallRecordingPresignedUrl: vi.fn(),
}));

const call: ConsultationCallAdminSummary = {
  callId: '11111111-1111-1111-1111-111111111111',
  conversationId: '22222222-2222-2222-2222-222222222222',
  callType: 'AUDIO',
  callStatus: 'ENDED',
  initiatedAt: '2026-08-21T10:00:00Z',
  answeredAt: '2026-08-21T10:00:10Z',
  endedAt: '2026-08-21T10:05:00Z',
  durationSeconds: 290,
  recordingFileId: '33333333-3333-3333-3333-333333333333',
  recordingStatus: 'UPLOADED',
  recordedDurationSeconds: 285,
  consentAttested: true,
  initiatedByUserId: '44444444-4444-4444-4444-444444444444',
  initiatedByRole: 'MOTHER',
  motherUserId: null,
  motherName: 'Nguyễn An',
  motherPhone: '0900000000',
  motherEmail: null,
  expertUserId: null,
  expertName: 'Bác sĩ Bình',
  expertSpecialization: 'Sản khoa',
  expertHospital: null,
};

describe('ConsultationCallListPage recording deletion', () => {
  afterEach(cleanup);

  beforeEach(() => {
    vi.clearAllMocks();
    searchCalls.mockResolvedValue({
      content: [call], totalElements: 1, totalPages: 1, size: 10, number: 0,
    });
  });

  it('requires confirmation and cancellation sends no delete request', async () => {
    render(<ConsultationCallListPage />);
    fireEvent.click(await screen.findByRole('button', { name: /Xóa$/ }));
    expect(screen.getByRole('dialog')).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Hủy' }));
    expect(deleteRecording).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('deletes once, disables duplicate submission, and refreshes the list', async () => {
    let resolveDelete!: () => void;
    deleteRecording.mockReturnValue(new Promise<void>((resolve) => { resolveDelete = resolve; }));
    render(<ConsultationCallListPage />);
    fireEvent.click(await screen.findByRole('button', { name: /Xóa$/ }));

    const confirm = screen.getByRole('button', { name: /Xóa bản ghi/ });
    fireEvent.click(confirm);
    expect((confirm as HTMLButtonElement).disabled).toBe(true);
    fireEvent.click(confirm);
    expect(deleteRecording).toHaveBeenCalledTimes(1);

    resolveDelete();
    await waitFor(() => expect(searchCalls).toHaveBeenCalledTimes(2));
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('keeps confirmation open and displays storage errors', async () => {
    deleteRecording.mockRejectedValue({ response: { data: { message: 'R2 tạm thời không khả dụng' } } });
    render(<ConsultationCallListPage />);
    fireEvent.click(await screen.findByRole('button', { name: /Xóa$/ }));
    fireEvent.click(screen.getByRole('button', { name: /Xóa bản ghi/ }));

    expect(await screen.findByText('R2 tạm thời không khả dụng')).toBeTruthy();
    expect(screen.getByRole('dialog')).toBeTruthy();
    expect(searchCalls).toHaveBeenCalledTimes(1);
  });
});
