import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ConsultationCallAdminSummary } from '../models/consultationCall';
import CallRecordingPlayerModal from './CallRecordingPlayerModal';

const getRecordingUrl = vi.fn();

vi.mock('../services/consultationCallApi', () => ({
  getCallRecordingPresignedUrl: (...args: unknown[]) => getRecordingUrl(...args),
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

describe('CallRecordingPlayerModal recording deletion', () => {
  let pauseSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.clearAllMocks();
    getRecordingUrl.mockResolvedValue('https://example.test/recording.mp3');
    pauseSpy = vi.spyOn(HTMLMediaElement.prototype, 'pause').mockImplementation(() => undefined);
  });

  afterEach(() => {
    cleanup();
    pauseSpy.mockRestore();
  });

  it('pauses the active media before requesting deletion confirmation', async () => {
    const onDeleteRequest = vi.fn();
    render(
      <CallRecordingPlayerModal
        call={call}
        onClose={vi.fn()}
        onDeleteRequest={onDeleteRequest}
      />
    );

    await waitFor(() => expect(document.querySelector('audio')).not.toBeNull());
    fireEvent.click(screen.getByRole('button', { name: /Xóa bản ghi/ }));

    expect(pauseSpy).toHaveBeenCalledTimes(1);
    expect(onDeleteRequest).toHaveBeenCalledWith(call);
    expect(pauseSpy.mock.invocationCallOrder[0]).toBeLessThan(
      onDeleteRequest.mock.invocationCallOrder[0]
    );
  });
});
