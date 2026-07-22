import 'dart:async';

import 'package:flutter/material.dart';
import 'package:zego_express_engine/zego_express_engine.dart';

import '../models/zego_join_credentials.dart';
import 'rtc_error_handling.dart';
import 'rtc_permissions.dart';
import 'rtc_platform_capabilities.dart';

class ZegoExpressCallRoom extends StatefulWidget {
  const ZegoExpressCallRoom({
    super.key,
    required this.credentials,
    required this.isVideo,
    required this.onConnected,
    required this.onReconnecting,
    required this.onError,
    required this.onRenewToken,
    required this.onHangUp,
  });

  final ZegoJoinCredentials credentials;
  final bool isVideo;
  final VoidCallback onConnected;
  final VoidCallback onReconnecting;
  final ValueChanged<String> onError;
  final Future<String?> Function() onRenewToken;
  final Future<void> Function() onHangUp;

  @override
  State<ZegoExpressCallRoom> createState() => _ZegoExpressCallRoomState();
}

class _ZegoExpressCallRoomState extends State<ZegoExpressCallRoom>
    with WidgetsBindingObserver {
  Widget? _localView;
  Widget? _remoteView;
  int? _localViewId;
  int? _remoteViewId;
  String? _remoteStreamId;
  late final String _localStreamId;
  bool _engineCreated = false;
  bool _active = true;
  bool _microphoneMuted = false;
  bool _cameraEnabled = false;
  bool _frontCamera = true;
  bool _speakerEnabled = false;
  String _connectionLabel = 'Đang kết nối…';
  late final RtcPlatformCapabilities _platformCapabilities;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _cameraEnabled = widget.isVideo;
    _speakerEnabled = widget.isVideo;
    _platformCapabilities = RtcPlatformCapabilities.current();
    _localStreamId = 'stream_${widget.credentials.userId}';
    unawaited(_initialize());
  }

  Future<void> _initialize() async {
    var stage = RtcSetupStage.permissions;
    try {
      final permissionError = await requestRtcPermissions(
        isVideo: widget.isVideo,
      );
      if (permissionError != null) {
        throw RtcSetupException(
          category: RtcFailureCategory.permissionDenied,
          userMessage: permissionError,
        );
      }
      if (!_active) return;

      _bindCallbacks();
      stage = RtcSetupStage.engineInitialization;
      await ZegoExpressEngine.createEngineWithProfile(
        ZegoEngineProfile(
          widget.credentials.appId,
          widget.isVideo
              ? ZegoScenario.StandardVideoCall
              : ZegoScenario.StandardVoiceCall,
          appSign: null,
        ),
      );
      _engineCreated = true;
      if (!_active) {
        await _releaseEngine();
        return;
      }

      if (widget.isVideo) {
        stage = RtcSetupStage.canvasCreation;
        final localView = await ZegoExpressEngine.instance.createCanvasView((
          viewId,
        ) {
          _localViewId = viewId;
        });
        if (!_active) return;
        setState(() => _localView = localView);
      }

      stage = RtcSetupStage.roomLogin;
      final roomConfig = ZegoRoomConfig(2, true, widget.credentials.token);
      final result = await ZegoExpressEngine.instance.loginRoom(
        widget.credentials.roomId,
        ZegoUser(widget.credentials.userId, widget.credentials.displayName),
        config: roomConfig,
      );
      if (result.errorCode != 0) {
        throw RtcSetupException(
          category: RtcFailureCategory.roomLogin,
          userMessage: 'Không thể vào phòng RTC (mã ${result.errorCode}).',
          errorCode: result.errorCode,
        );
      }
      if (!_active) return;

      stage = RtcSetupStage.mediaSetup;
      await ZegoExpressEngine.instance.enableCamera(widget.isVideo);
      await ZegoExpressEngine.instance.useFrontCamera(true);
      await ZegoExpressEngine.instance.muteMicrophone(false);
      await _platformCapabilities.setSpeakerRoute(
        enabled: _speakerEnabled,
        setter: ZegoExpressEngine.instance.setAudioRouteToSpeaker,
      );
      if (widget.isVideo && _localViewId != null) {
        await ZegoExpressEngine.instance.startPreview(
          canvas: ZegoCanvas.view(_localViewId!),
        );
      }
      stage = RtcSetupStage.mediaPublish;
      await ZegoExpressEngine.instance.startPublishingStream(_localStreamId);
    } on RtcSetupException catch (error, stackTrace) {
      _debugLogFailure(
        error,
        stackTrace,
        stage: stage,
        category: error.category,
      );
      if (_active) widget.onError(error.userMessage);
      await _releaseEngine();
    } catch (error, stackTrace) {
      final failure = classifyRtcSetupFailure(
        error,
        stage: stage,
        isWeb: _platformCapabilities.isWeb,
      );
      _debugLogFailure(
        error,
        stackTrace,
        stage: stage,
        category: failure.category,
      );
      if (_active) {
        widget.onError(failure.userMessage);
      }
      await _releaseEngine();
    }
  }

  void _bindCallbacks() {
    ZegoExpressEngine
        .onRoomStateChanged = (roomId, reason, errorCode, extendedData) {
      if (!_active || roomId != widget.credentials.roomId) return;
      switch (reason) {
        case ZegoRoomStateChangedReason.Logined:
        case ZegoRoomStateChangedReason.Reconnected:
          if (mounted) setState(() => _connectionLabel = 'Đã kết nối');
          widget.onConnected();
          break;
        case ZegoRoomStateChangedReason.Logining:
        case ZegoRoomStateChangedReason.Reconnecting:
          if (mounted) setState(() => _connectionLabel = 'Đang kết nối lại…');
          widget.onReconnecting();
          break;
        case ZegoRoomStateChangedReason.LoginFailed:
        case ZegoRoomStateChangedReason.ReconnectFailed:
        case ZegoRoomStateChangedReason.KickOut:
          widget.onError(
            errorCode == 0
                ? 'Kết nối RTC đã kết thúc.'
                : 'Kết nối RTC thất bại (mã $errorCode).',
          );
          break;
        case ZegoRoomStateChangedReason.Logout:
        case ZegoRoomStateChangedReason.LogoutFailed:
          break;
      }
    };
    ZegoExpressEngine.onRoomStreamUpdate =
        (roomId, updateType, streams, extendedData) {
          if (!_active || roomId != widget.credentials.roomId) return;
          for (final stream in streams) {
            if (stream.user.userID == widget.credentials.userId) continue;
            if (updateType == ZegoUpdateType.Add) {
              unawaited(_startPlaying(stream.streamID));
            } else if (_remoteStreamId == stream.streamID) {
              unawaited(_stopPlaying(stream.streamID));
            }
          }
        };
    ZegoExpressEngine.onRoomTokenWillExpire = (roomId, _) {
      if (_active && roomId == widget.credentials.roomId) {
        unawaited(_renewToken());
      }
    };
    ZegoExpressEngine.onPublisherStateUpdate =
        (streamId, state, errorCode, extendedData) {
          if (_active &&
              streamId == _localStreamId &&
              state == ZegoPublisherState.NoPublish &&
              errorCode != 0) {
            debugLogRtcSdkError(
              category: RtcFailureCategory.mediaPublish,
              errorCode: errorCode,
            );
            widget.onError('Không thể gửi media (mã $errorCode).');
          }
        };
    ZegoExpressEngine.onPlayerStateUpdate =
        (streamId, state, errorCode, extendedData) {
          if (_active &&
              streamId == _remoteStreamId &&
              state == ZegoPlayerState.NoPlay &&
              errorCode != 0) {
            debugLogRtcSdkError(
              category: RtcFailureCategory.mediaPlay,
              errorCode: errorCode,
            );
            widget.onError('Không thể nhận media (mã $errorCode).');
          }
        };
  }

  Future<void> _startPlaying(String streamId) async {
    try {
      if (!_active || _remoteStreamId == streamId) return;
      if (_remoteStreamId != null) await _stopPlaying(_remoteStreamId!);
      _remoteStreamId = streamId;
      if (!widget.isVideo) {
        await ZegoExpressEngine.instance.startPlayingStream(streamId);
        return;
      }
      final remoteView = await ZegoExpressEngine.instance.createCanvasView((
        viewId,
      ) {
        _remoteViewId = viewId;
      });
      if (!_active || _remoteStreamId != streamId) return;
      if (mounted) setState(() => _remoteView = remoteView);
      if (_remoteViewId != null) {
        await ZegoExpressEngine.instance.startPlayingStream(
          streamId,
          canvas: ZegoCanvas.view(_remoteViewId!),
        );
      }
    } catch (error, stackTrace) {
      _debugLogFailure(
        error,
        stackTrace,
        stage: RtcSetupStage.mediaPlay,
        category: RtcFailureCategory.mediaPlay,
      );
      if (_active) {
        widget.onError(
          'Không thể nhận âm thanh hoặc video từ cuộc gọi. '
          'Hãy kiểm tra kết nối mạng.',
        );
      }
    }
  }

  Future<void> _stopPlaying(String streamId) async {
    await ZegoExpressEngine.instance.stopPlayingStream(streamId);
    if (_remoteViewId != null) {
      await ZegoExpressEngine.instance.destroyCanvasView(_remoteViewId!);
    }
    _remoteStreamId = null;
    _remoteViewId = null;
    if (_active && mounted) setState(() => _remoteView = null);
  }

  Future<void> _renewToken() async {
    final token = await widget.onRenewToken();
    if (!_active || token == null) return;
    await ZegoExpressEngine.instance.renewToken(
      widget.credentials.roomId,
      token,
    );
  }

  Future<void> _toggleMicrophone() async {
    final muted = !_microphoneMuted;
    setState(() => _microphoneMuted = muted);
    await ZegoExpressEngine.instance.muteMicrophone(muted);
  }

  Future<void> _toggleCamera() async {
    final enabled = !_cameraEnabled;
    setState(() => _cameraEnabled = enabled);
    await ZegoExpressEngine.instance.enableCamera(enabled);
  }

  Future<void> _switchCamera() async {
    final front = !_frontCamera;
    setState(() => _frontCamera = front);
    await ZegoExpressEngine.instance.useFrontCamera(front);
  }

  Future<void> _toggleSpeaker() async {
    final enabled = !_speakerEnabled;
    setState(() => _speakerEnabled = enabled);
    await _platformCapabilities.setSpeakerRoute(
      enabled: enabled,
      setter: ZegoExpressEngine.instance.setAudioRouteToSpeaker,
    );
  }

  void _debugLogFailure(
    Object error,
    StackTrace stackTrace, {
    required RtcSetupStage stage,
    required RtcFailureCategory category,
  }) {
    debugLogRtcFailure(
      error: error,
      stackTrace: stackTrace,
      stage: stage,
      category: category,
      sensitiveValues: [
        widget.credentials.token,
        widget.credentials.roomId,
        widget.credentials.userId,
        widget.credentials.displayName,
      ],
    );
  }

  Future<void> _releaseEngine() async {
    if (!_engineCreated) return;
    _engineCreated = false;
    ZegoExpressEngine.onRoomStateChanged = null;
    ZegoExpressEngine.onRoomStreamUpdate = null;
    ZegoExpressEngine.onRoomTokenWillExpire = null;
    ZegoExpressEngine.onPublisherStateUpdate = null;
    ZegoExpressEngine.onPlayerStateUpdate = null;
    try {
      await ZegoExpressEngine.instance.stopPublishingStream();
      if (widget.isVideo) {
        await ZegoExpressEngine.instance.stopPreview();
      }
      if (_remoteStreamId != null) {
        await ZegoExpressEngine.instance.stopPlayingStream(_remoteStreamId!);
      }
      if (_localViewId != null) {
        await ZegoExpressEngine.instance.destroyCanvasView(_localViewId!);
      }
      if (_remoteViewId != null) {
        await ZegoExpressEngine.instance.destroyCanvasView(_remoteViewId!);
      }
      await ZegoExpressEngine.instance.logoutRoom(widget.credentials.roomId);
    } finally {
      await ZegoExpressEngine.destroyEngine();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_engineCreated || !widget.isVideo) return;
    if (state == AppLifecycleState.paused ||
        state == AppLifecycleState.inactive) {
      unawaited(ZegoExpressEngine.instance.enableCamera(false));
    } else if (state == AppLifecycleState.resumed && _cameraEnabled) {
      unawaited(ZegoExpressEngine.instance.enableCamera(true));
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _active = false;
    unawaited(_releaseEngine());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.black,
      child: SafeArea(
        child: Stack(
          children: [
            Positioned.fill(
              child: widget.isVideo
                  ? ColoredBox(
                      color: Colors.black,
                      child:
                          _remoteView ??
                          const Center(
                            child: Icon(
                              Icons.person,
                              color: Colors.white54,
                              size: 96,
                            ),
                          ),
                    )
                  : const Center(
                      child: Icon(
                        Icons.phone_in_talk,
                        color: Colors.white70,
                        size: 112,
                      ),
                    ),
            ),
            if (widget.isVideo && _localView != null)
              Positioned(
                top: 24,
                right: 16,
                width: 120,
                height: 170,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: ColoredBox(color: Colors.black54, child: _localView),
                ),
              ),
            Positioned(
              top: 16,
              left: 16,
              right: 16,
              child: Center(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: Colors.black54,
                    borderRadius: BorderRadius.circular(999),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 8,
                    ),
                    child: Text(
                      _connectionLabel,
                      style: const TextStyle(color: Colors.white),
                    ),
                  ),
                ),
              ),
            ),
            Positioned(
              left: 12,
              right: 12,
              bottom: 24,
              child: Wrap(
                alignment: WrapAlignment.center,
                spacing: 12,
                runSpacing: 12,
                children: [
                  _ControlButton(
                    icon: _microphoneMuted ? Icons.mic_off : Icons.mic,
                    label: _microphoneMuted ? 'Bật mic' : 'Tắt mic',
                    onPressed: _toggleMicrophone,
                  ),
                  if (widget.isVideo)
                    _ControlButton(
                      icon: _cameraEnabled
                          ? Icons.videocam
                          : Icons.videocam_off,
                      label: _cameraEnabled ? 'Tắt camera' : 'Bật camera',
                      onPressed: _toggleCamera,
                    ),
                  if (widget.isVideo)
                    _ControlButton(
                      icon: Icons.cameraswitch,
                      label: 'Đổi camera',
                      onPressed: _switchCamera,
                    ),
                  if (_platformCapabilities.showsSpeakerRouteControl)
                    _ControlButton(
                      icon: _speakerEnabled ? Icons.volume_up : Icons.hearing,
                      label: _speakerEnabled ? 'Loa ngoài' : 'Tai nghe',
                      onPressed: _toggleSpeaker,
                    ),
                  _ControlButton(
                    icon: Icons.call_end,
                    label: 'Kết thúc',
                    color: Colors.red,
                    onPressed: widget.onHangUp,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ControlButton extends StatelessWidget {
  const _ControlButton({
    required this.icon,
    required this.label,
    required this.onPressed,
    this.color = const Color(0xFF4A403C),
  });

  final IconData icon;
  final String label;
  final Future<void> Function() onPressed;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: label,
      child: IconButton.filled(
        style: IconButton.styleFrom(
          backgroundColor: color,
          foregroundColor: Colors.white,
          minimumSize: const Size(56, 56),
          maximumSize: const Size(56, 56),
        ),
        onPressed: () => unawaited(onPressed()),
        icon: Icon(icon),
      ),
    );
  }
}
