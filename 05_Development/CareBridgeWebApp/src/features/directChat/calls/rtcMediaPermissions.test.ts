import { describe, expect, it, vi } from 'vitest';
import {
  mediaErrorMessage,
  requestRtcMediaPermission,
  type MediaDevicesPort,
} from './rtcMediaPermissions';

describe('RTC browser permissions', () => {
  it('maps denied and missing-device errors to actionable copy', () => {
    expect(mediaErrorMessage(new DOMException('', 'NotAllowedError'))).toContain(
      'chưa được cấp quyền'
    );
    expect(mediaErrorMessage(new DOMException('', 'NotFoundError'))).toContain(
      'Không tìm thấy'
    );
  });

  it('requests microphone only for voice and stops temporary tracks', async () => {
    const stop = vi.fn();
    const getUserMedia = vi.fn().mockResolvedValue({
      getTracks: () => [{ stop }],
    });

    await requestRtcMediaPermission(false, { getUserMedia } as MediaDevicesPort);

    expect(getUserMedia).toHaveBeenCalledWith({ audio: true, video: false });
    expect(stop).toHaveBeenCalledOnce();
  });

  it('rejects before join when media devices are unavailable', async () => {
    await expect(requestRtcMediaPermission(true, undefined)).rejects.toMatchObject({
      name: 'NotFoundError',
    });
  });
});
