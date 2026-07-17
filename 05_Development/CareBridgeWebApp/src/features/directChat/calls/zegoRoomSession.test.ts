import { describe, expect, it, vi } from 'vitest';
import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';
import { mountZegoRoomSession, type ZegoPrebuiltModulePort } from './zegoRoomSession';

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
});
