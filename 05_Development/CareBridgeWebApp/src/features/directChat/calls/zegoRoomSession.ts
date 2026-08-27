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

  // Mount the PDPA notice outside the SDK-owned container. Zego replaces the
  // container's children while rendering, which used to remove this banner.
  const pdpaBanner = document.createElement('div');
  pdpaBanner.dataset.callRecordingNotice = 'true';
  pdpaBanner.setAttribute('role', 'status');
  pdpaBanner.setAttribute('aria-live', 'polite');
  pdpaBanner.style.position = 'absolute';
  pdpaBanner.style.top = '64px';
  pdpaBanner.style.left = '50%';
  pdpaBanner.style.transform = 'translateX(-50%)';
  pdpaBanner.style.zIndex = '10001';
  pdpaBanner.style.display = 'flex';
  pdpaBanner.style.alignItems = 'center';
  pdpaBanner.style.justifyContent = 'center';
  pdpaBanner.style.gap = '8px';
  pdpaBanner.style.width = 'max-content';
  pdpaBanner.style.maxWidth = 'calc(100% - 32px)';
  pdpaBanner.style.padding = '6px 14px';
  pdpaBanner.style.background = 'rgba(15, 23, 42, 0.85)';
  pdpaBanner.style.color = '#fff';
  pdpaBanner.style.borderRadius = '999px';
  pdpaBanner.style.fontSize = '12px';
  pdpaBanner.style.fontWeight = '500';
  pdpaBanner.style.boxShadow = '0 4px 12px rgba(0,0,0,0.3)';
  pdpaBanner.style.backdropFilter = 'blur(6px)';
  pdpaBanner.style.border = '1px solid rgba(239, 68, 68, 0.4)';
  pdpaBanner.style.pointerEvents = 'none';
  pdpaBanner.style.textAlign = 'center';
  pdpaBanner.innerHTML = `
    <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#ef4444;"></span>
    <span style="color:#f87171; font-weight:700; font-size:11px;">REC</span>
    <span style="color:#94a3b8;">|</span>
    <span>Cuộc gọi này sẽ được ghi âm/ghi hình nhằm đảm bảo chất lượng tư vấn y tế (Tuân thủ PDPA)</span>
  `;
  const pdpaHost = container?.parentElement ?? container;
  if (pdpaHost && typeof pdpaHost.appendChild === 'function') {
    if (pdpaHost === container) container.style.position = 'relative';
    pdpaHost.appendChild(pdpaBanner);
  }

  const startRecording = async () => {
    try {
      if (typeof MediaRecorder === 'undefined' || !navigator.mediaDevices?.getUserMedia) return;
      recordingStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          autoGainControl: true,
          channelCount: { ideal: 1 },
          echoCancellation: true,
          noiseSuppression: true,
          sampleRate: { ideal: 48_000 },
        },
        video: call.callType === 'VIDEO'
          ? {
              frameRate: { ideal: 24, max: 30 },
              height: { ideal: 480 },
              width: { ideal: 640 },
            }
          : false,
      });
      if (disposed) {
        recordingStream.getTracks().forEach((track) => track.stop());
        recordingStream = null;
        return;
      }
      const mimeType = call.callType === 'VIDEO' ? 'video/webm;codecs=vp8,opus' : 'audio/webm;codecs=opus';
      const options: MediaRecorderOptions = {
        audioBitsPerSecond: 96_000,
        ...(call.callType === 'VIDEO' ? { videoBitsPerSecond: 1_500_000 } : {}),
        ...(MediaRecorder.isTypeSupported?.(mimeType) ? { mimeType } : {}),
      };
      mediaRecorder = new MediaRecorder(recordingStream, options);
      mediaRecorder.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) recordedChunks.push(e.data);
      };
      // A timeslice makes Chrome emit independently muxed WebM segments. Joining
      // those segment bytes produces timestamp resets, audio gaps and a truncated
      // container. Let stop() emit one final, valid WebM container instead.
      mediaRecorder.start();
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
