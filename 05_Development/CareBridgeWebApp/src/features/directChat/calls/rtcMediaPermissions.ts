export interface MediaStreamTrackPort {
  stop(): void;
}

export interface MediaStreamPort {
  getTracks(): MediaStreamTrackPort[];
}

export interface MediaDevicesPort {
  getUserMedia(constraints: MediaStreamConstraints): Promise<MediaStreamPort>;
}

export function mediaErrorMessage(error: unknown): string {
  const name = error instanceof DOMException ? error.name : '';
  if (name === 'NotAllowedError' || name === 'SecurityError') {
    return 'Trình duyệt chưa được cấp quyền microphone/camera.';
  }
  if (name === 'NotFoundError' || name === 'DevicesNotFoundError') {
    return 'Không tìm thấy microphone/camera phù hợp trên thiết bị.';
  }
  return 'Không thể mở thiết bị thoại/video. Hãy kiểm tra quyền và thử lại.';
}

export async function requestRtcMediaPermission(
  isVideo: boolean,
  mediaDevices: MediaDevicesPort | undefined
): Promise<void> {
  if (!mediaDevices?.getUserMedia) {
    throw new DOMException('Media devices API is unavailable', 'NotFoundError');
  }
  const stream = await mediaDevices.getUserMedia({
    audio: true,
    video: isVideo,
  });
  for (const track of stream.getTracks()) track.stop();
}
