import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';

interface ZegoRoomInstancePort {
  autoLeaveRoomWhenOnlySelfInRoom: boolean;
  joinRoom(config: Record<string, unknown>): void;
  destroy(): void;
}

export interface ZegoPrebuiltModulePort {
  ZegoUIKitPrebuilt: {
    OneONoneCall: unknown;
    generateKitTokenForProduction(
      appId: number,
      token: string,
      roomId: string,
      userId: string,
      displayName: string
    ): string;
    create(kitToken: string): ZegoRoomInstancePort;
  };
}

interface MountZegoRoomSessionOptions {
  call: ConversationCall;
  credentials: ZegoJoinCredentials;
  container: HTMLElement;
  onJoin(): void;
  onLeave(): void;
  onError?(error: unknown): void;
  loadModule?: () => Promise<ZegoPrebuiltModulePort>;
}

const loadOfficialModule = () =>
  import('@zegocloud/zego-uikit-prebuilt') as unknown as Promise<ZegoPrebuiltModulePort>;

export function mountZegoRoomSession({
  call,
  credentials,
  container,
  onJoin,
  onLeave,
  onError,
  loadModule = loadOfficialModule,
}: MountZegoRoomSessionOptions): () => void {
  let disposed = false;
  let room: ZegoRoomInstancePort | null = null;

  void loadModule()
    .then(({ ZegoUIKitPrebuilt }) => {
      if (disposed) return;
      const kitToken = ZegoUIKitPrebuilt.generateKitTokenForProduction(
        credentials.appId,
        credentials.token,
        credentials.roomId,
        credentials.userId,
        credentials.displayName
      );
      room = ZegoUIKitPrebuilt.create(kitToken);
      room.autoLeaveRoomWhenOnlySelfInRoom = false;
      room.joinRoom({
        container,
        scenario: { mode: ZegoUIKitPrebuilt.OneONoneCall },
        maxUsers: 2,
        showPreJoinView: false,
        turnOnMicrophoneWhenJoining: true,
        turnOnCameraWhenJoining: call.callType === 'VIDEO',
        useFrontFacingCamera: true,
        showMyMicrophoneToggleButton: true,
        showMyCameraToggleButton: call.callType === 'VIDEO',
        showAudioVideoSettingsButton: true,
        showScreenSharingButton: false,
        showTextChat: false,
        showUserList: false,
        showRoomDetailsButton: false,
        showLayoutButton: false,
        sharedLinks: [],
        lowerLeftNotification: {
          showTextChat: false,
          showUserJoinAndLeave: false,
        },
        showLeavingView: false,
        showLeaveRoomConfirmDialog: true,
        onJoinRoom: () => {
          if (!disposed) onJoin();
        },
        onLeaveRoom: () => {
          if (!disposed) onLeave();
        },
      });
    })
    .catch((error) => {
      if (!disposed) onError?.(error);
    });

  return () => {
    disposed = true;
    room?.destroy();
  };
}
