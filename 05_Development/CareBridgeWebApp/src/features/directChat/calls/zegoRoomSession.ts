import type { ConversationCall, ZegoJoinCredentials } from '../models/directConversation';
import { uploadCallRecording } from '../services/directChatApi';

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
  let mediaRecorder: MediaRecorder | null = null;
  let recordingStream: MediaStream | null = null;
  const recordedChunks: Blob[] = [];
  let callStartTime: number | null = null;
  let recordingFinalizePromise: Promise<void> | null = null;
  let leaveRequested = false;

  // Insert PDPA overlay badge in container
  const pdpaBanner = document.createElement('div');
  pdpaBanner.style.position = 'absolute';
  pdpaBanner.style.top = '12px';
  pdpaBanner.style.left = '50%';
  pdpaBanner.style.transform = 'translateX(-50%)';
  pdpaBanner.style.zIndex = '9999';
  pdpaBanner.style.display = 'flex';
  pdpaBanner.style.alignItems = 'center';
  pdpaBanner.style.gap = '8px';
  pdpaBanner.style.padding = '6px 14px';
  pdpaBanner.style.background = 'rgba(15, 23, 42, 0.85)';
  pdpaBanner.style.color = '#fff';
  pdpaBanner.style.borderRadius = '999px';
  pdpaBanner.style.fontSize = '12px';
  pdpaBanner.style.fontWeight = '500';
  pdpaBanner.style.boxShadow = '0 4px 12px rgba(0,0,0,0.3)';
  pdpaBanner.style.backdropFilter = 'blur(6px)';
  pdpaBanner.style.border = '1px solid rgba(239, 68, 68, 0.4)';
  pdpaBanner.innerHTML = `
    <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#ef4444;"></span>
    <span style="color:#f87171; font-weight:700; font-size:11px;">REC (PDPA)</span>
    <span style="color:#94a3b8;">|</span>
    <span>Cuộc gọi được ghi âm/ghi hình nhằm đảm bảo chất lượng tư vấn y tế</span>
  `;
  if (container && typeof container.appendChild === 'function') {
    container.style.position = 'relative';
    container.appendChild(pdpaBanner);
  }

  const startRecording = async () => {
    try {
      if (typeof MediaRecorder === 'undefined' || !navigator.mediaDevices?.getUserMedia) return;
      recordingStream = await navigator.mediaDevices.getUserMedia({
        audio: true,
        video: call.callType === 'VIDEO',
      });
      if (disposed) {
        recordingStream.getTracks().forEach((track) => track.stop());
        recordingStream = null;
        return;
      }
      const mimeType = call.callType === 'VIDEO' ? 'video/webm;codecs=vp8,opus' : 'audio/webm;codecs=opus';
      const options = MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported(mimeType) ? { mimeType } : undefined;
      mediaRecorder = new MediaRecorder(recordingStream, options);
      mediaRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) recordedChunks.push(e.data);
      };
      mediaRecorder.start(1000);
      callStartTime = Date.now();
    } catch (e) {
      console.warn('[zegoRoomSession] Media recording could not start:', e);
    }
  };

  const stopRecordingAndUpload = (): Promise<void> => {
    if (recordingFinalizePromise) return recordingFinalizePromise;
    if (!mediaRecorder) return Promise.resolve();

    const recorder = mediaRecorder;
    recordingFinalizePromise = (async () => {
      try {
        if (recorder.state !== 'inactive') {
          await new Promise<void>((resolve) => {
            recorder.onstop = () => resolve();
            recorder.stop();
          });
        }

        if (recordedChunks.length > 0 && call.conversationId && call.callId) {
          const mimeType = call.callType === 'VIDEO' ? 'video/webm' : 'audio/webm';
          const blob = new Blob(recordedChunks, { type: mimeType });
          const durationSecs = callStartTime
            ? Math.max(1, Math.round((Date.now() - callStartTime) / 1000))
            : undefined;
          await uploadCallRecording(
            call.conversationId,
            call.callId,
            blob,
            durationSecs,
            true
          );
        }
      } catch (error) {
        console.warn('[zegoRoomSession] Stop/upload recording failed:', error);
      } finally {
        recordingStream?.getTracks().forEach((track) => track.stop());
        recordingStream = null;
        mediaRecorder = null;
      }
    })();
    return recordingFinalizePromise;
  };

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
          if (!disposed) {
            void startRecording();
            onJoin();
          }
        },
        onLeaveRoom: () => {
          if (!disposed && !leaveRequested) {
            leaveRequested = true;
            void stopRecordingAndUpload().finally(() => {
              if (!disposed) onLeave();
            });
          }
        },
      });
    })
    .catch((error) => {
      if (!disposed) onError?.(error);
    });

  return () => {
    disposed = true;
    if (typeof pdpaBanner.remove === 'function') {
      pdpaBanner.remove();
    }
    void stopRecordingAndUpload();
    room?.destroy();
  };
}
