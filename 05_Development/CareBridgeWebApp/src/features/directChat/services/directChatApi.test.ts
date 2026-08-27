// @vitest-environment jsdom

import { beforeEach, describe, expect, it, vi } from 'vitest';
import apiClient from '../../../shared/api/apiClient';
import { uploadCallRecording } from './directChatApi';

vi.mock('../../../shared/api/apiClient', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('uploadCallRecording', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends the recording as the multipart file part without forcing Content-Type', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      data: { data: { callId: 'call-1' } },
    });
    const recording = new Blob(['recording'], { type: 'video/webm' });

    await uploadCallRecording('conversation-1', 'call-1', recording, 12, true);

    expect(apiClient.post).toHaveBeenCalledOnce();
    const [url, body, config] = vi.mocked(apiClient.post).mock.calls[0];
    expect(url).toBe(
      '/api/v1/direct-conversations/conversation-1/calls/call-1/recording'
    );
    expect(body).toBeInstanceOf(FormData);
    expect((body as FormData).get('file')).toBeInstanceOf(File);
    expect((body as FormData).get('recordedDurationSeconds')).toBe('12');
    expect((body as FormData).get('consentAttested')).toBe('true');
    expect(config).toBeUndefined();
  });
});
