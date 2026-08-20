import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';
import { uploadCallRecording } from '../services/directChatApi';
import { mountZegoRoomSession, type ZegoPrebuiltModulePort } from './zegoRoomSession';

vi.mock('../services/directChatApi', () => ({
  uploadCallRecording: vi.fn(),
}));

const uploadCallRecordingMock = vi.mocked(uploadCallRecording);

const call: ConversationCall = {
  callId: 'call-1',
  conversationId: 'conversation-1',
  initiatedByUserId: 'mother-1',
  callType: 'VOICE',
  callStatus: 'ANSWERED',
  initiatedAt: '2026-07-16T00:00:00Z',
  answeredAt: '2026-07-16T00:01:00Z',
  endedAt: null,
  durationSeconds: null,
};

const credentials: ZegoJoinCredentials = {
  appId: 123,
  roomId: 'cb_room',
  userId: 'u_mother',
  displayName: 'Mother',
  token: 'server-token',
  expiresAt: '2026-07-16T01:00:00Z',
};

function fakeModule() {
  const destroy = vi.fn();
  const joinRoom = vi.fn();
  const create = vi.fn(() => ({
    autoLeaveRoomWhenOnlySelfInRoom: true,
    joinRoom,
    destroy,
  }));
  const generateKitTokenForProduction = vi.fn(() => 'kit-token');
  const module: ZegoPrebuiltModulePort = {
    ZegoUIKitPrebuilt: {
      OneONoneCall: 'one-on-one',
      create,
      generateKitTokenForProduction,
    },
  };
  return { module, create, destroy, joinRoom, generateKitTokenForProduction };
}

describe('mountZegoRoomSession', () => {
  afterEach(() => {
    vi.clearAllMocks();
    vi.unstubAllGlobals();
  });

  it('destroys the room and tracks when the host unmounts', async () => {
    const fake = fakeModule();
    const dispose = mountZegoRoomSession({
      call,
      credentials,
      container: {} as HTMLElement,
      loadModule: async () => fake.module,
      onJoin: vi.fn(),
      onLeave: vi.fn(),
    });
    await Promise.resolve();

    dispose();

    expect(fake.destroy).toHaveBeenCalledOnce();
    expect(fake.joinRoom).toHaveBeenCalledWith(
      expect.objectContaining({
        turnOnCameraWhenJoining: false,
        showMyCameraToggleButton: false,
        showTextChat: false,
      })
    );
    expect(fake.generateKitTokenForProduction).toHaveBeenCalledWith(
      123,
      'server-token',
      'cb_room',
      'u_mother',
      'Mother'
    );
  });

  it('does not create a room if unmounted while the SDK chunk is loading', async () => {
    const fake = fakeModule();
    let resolveModule!: (module: ZegoPrebuiltModulePort) => void;
    const dispose = mountZegoRoomSession({
      call,
      credentials,
      container: {} as HTMLElement,
      loadModule: () => new Promise((resolve) => {
        resolveModule = resolve;
      }),
      onJoin: vi.fn(),
      onLeave: vi.fn(),
    });

    dispose();
    resolveModule(fake.module);
    await Promise.resolve();

    expect(fake.create).not.toHaveBeenCalled();
  });

  it('keeps the PDPA recording notice outside the SDK-owned container', async () => {
    const fake = fakeModule();
    const roomHost = document.createElement('div');
    const sdkContainer = document.createElement('div');
    roomHost.appendChild(sdkContainer);

    const dispose = mountZegoRoomSession({
      call,
      credentials,
      container: sdkContainer,
      loadModule: async () => fake.module,
      onJoin: vi.fn(),
      onLeave: vi.fn(),
    });
    await Promise.resolve();

    sdkContainer.replaceChildren();

    const notice = roomHost.querySelector('[data-call-recording-notice="true"]');
    expect(notice).not.toBeNull();
    expect(notice?.parentElement).toBe(roomHost);
    expect(notice?.textContent).toContain(
      'Cuộc gọi này sẽ được ghi âm/ghi hình nhằm đảm bảo chất lượng tư vấn y tế (Tuân thủ PDPA)'
    );

    dispose();
    expect(roomHost.querySelector('[data-call-recording-notice="true"]')).toBeNull();
  });

  it('flushes the final recorder chunk and finishes one upload before leaving', async () => {
    const fake = fakeModule();
    const stopTrack = vi.fn();
    const getUserMedia = vi.fn().mockResolvedValue({
      getTracks: () => [{ stop: stopTrack }],
    });
    vi.stubGlobal('navigator', { mediaDevices: { getUserMedia } });
    let recorderTimeslice: number | undefined;

    class FakeMediaRecorder {
      static isTypeSupported = vi.fn(() => true);
      state: RecordingState = 'inactive';
      ondataavailable: ((event: BlobEvent) => void) | null = null;
      onstop: ((event: Event) => void) | null = null;

      start(timeslice?: number) {
        recorderTimeslice = timeslice;
        this.state = 'recording';
      }

      stop() {
        this.state = 'inactive';
        queueMicrotask(() => {
          this.ondataavailable?.({ data: new Blob(['recording-data']) } as BlobEvent);
          this.onstop?.(new Event('stop'));
        });
      }
    }
    vi.stubGlobal('MediaRecorder', FakeMediaRecorder);

    let finishUpload!: (result: ConversationCall) => void;
    uploadCallRecordingMock.mockReturnValue(
      new Promise<ConversationCall>((resolve) => {
        finishUpload = resolve;
      })
    );
    const onLeave = vi.fn();
    const dispose = mountZegoRoomSession({
      call,
      credentials,
      container: document.createElement('div'),
      loadModule: async () => fake.module,
      onJoin: vi.fn(),
      onLeave,
    });
    await Promise.resolve();

    const roomConfig = fake.joinRoom.mock.calls[0][0] as {
      onJoinRoom(): void;
      onLeaveRoom(): void;
    };
    roomConfig.onJoinRoom();
    await Promise.resolve();
    await Promise.resolve();

    expect(recorderTimeslice).toBeUndefined();
    expect(getUserMedia).toHaveBeenCalledWith({
      audio: expect.objectContaining({
        autoGainControl: true,
        echoCancellation: true,
        noiseSuppression: true,
        sampleRate: { ideal: 48_000 },
      }),
      video: false,
    });

    roomConfig.onLeaveRoom();
    roomConfig.onLeaveRoom();
    await Promise.resolve();
    await Promise.resolve();

    expect(uploadCallRecordingMock).toHaveBeenCalledOnce();
    expect(uploadCallRecordingMock).toHaveBeenCalledWith(
      'conversation-1',
      'call-1',
      expect.any(Blob),
      expect.any(Number),
      true
    );
    expect(onLeave).not.toHaveBeenCalled();

    finishUpload(call);
    await Promise.resolve();
    await Promise.resolve();

    expect(onLeave).toHaveBeenCalledOnce();
    dispose();
    await Promise.resolve();
    expect(uploadCallRecordingMock).toHaveBeenCalledOnce();
    expect(stopTrack).toHaveBeenCalledOnce();
  });
});
