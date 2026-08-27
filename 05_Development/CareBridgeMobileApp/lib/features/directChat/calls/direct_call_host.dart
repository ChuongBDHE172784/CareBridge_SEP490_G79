import 'dart:async';

import 'package:flutter/material.dart';

import '../../../core/auth/auth_state.dart';
import '../models/zego_join_credentials.dart';
import 'call_recording_consent_dialog.dart';
import 'conversation_signal_hub.dart';
import 'direct_call_api.dart';
import 'direct_call_coordinator.dart';
import 'direct_call_state.dart';
import 'ringtone_player.dart';
import 'zego_express_call_room.dart';

class DirectCallScope extends InheritedWidget {
  const DirectCallScope({
    super.key,
    required this.coordinator,
    required super.child,
  });

  final DirectCallCoordinator coordinator;

  static DirectCallCoordinator of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<DirectCallScope>();
    if (scope == null) {
      throw StateError('DirectCallHost is not mounted');
    }
    return scope.coordinator;
  }

  @override
  bool updateShouldNotify(DirectCallScope oldWidget) =>
      oldWidget.coordinator != coordinator;
}

class DirectCallHost extends StatefulWidget {
  const DirectCallHost({
    super.key,
    required this.child,
    this.coordinator,
    this.manageAuthenticatedSession = true,
  });

  final Widget child;
  final DirectCallCoordinator? coordinator;
  final bool manageAuthenticatedSession;

  @override
  State<DirectCallHost> createState() => _DirectCallHostState();
}

class _DirectCallHostState extends State<DirectCallHost>
    with WidgetsBindingObserver {
  late final DirectCallCoordinator _coordinator;
  StreamSubscription<DirectCallState>? _stateSubscription;
  DirectCallState _state = const DirectCallState();
  ZegoJoinCredentials? _credentials;
  String? _rtcError;
  bool _sessionStarted = false;
  bool _joining = false;
  bool _confirmingIncomingRecording = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _coordinator =
        widget.coordinator ??
        DirectCallCoordinator(
          api: DirectCallApi.instance,
          signals: ConversationSignalHub.instance.events,
          currentUserId: () => AuthState.instance.userId,
        );
    _state = _coordinator.state;
    _stateSubscription = _coordinator.states.listen((state) {
      if (!mounted) return;
      setState(() {
        _state = state;
        if (state.phase == DirectCallPhase.terminal ||
            state.phase == DirectCallPhase.failed) {
          _credentials = null;
          _joining = false;
        }
        if (state.phase != DirectCallPhase.incoming) {
          _confirmingIncomingRecording = false;
        }
      });
      if (state.phase == DirectCallPhase.outgoing) {
        RingtonePlayer.instance.start('outgoing');
      } else if (state.phase == DirectCallPhase.incoming) {
        RingtonePlayer.instance.start('incoming');
      } else {
        RingtonePlayer.instance.stop();
      }
      if (state.phase == DirectCallPhase.readyToJoin &&
          _credentials == null &&
          !_joining) {
        scheduleMicrotask(_join);
      }
    });
    if (widget.manageAuthenticatedSession) {
      AuthState.instance.addListener(_onAuthChanged);
      scheduleMicrotask(_onAuthChanged);
    }
  }

  Future<void> _onAuthChanged() async {
    if (!mounted) return;
    if (AuthState.instance.isAuthenticated && !_sessionStarted) {
      _sessionStarted = true;
      await ConversationSignalHub.instance.start();
      await _coordinator.start();
    } else if (!AuthState.instance.isAuthenticated && _sessionStarted) {
      _sessionStarted = false;
      unawaited(_coordinator.endCurrent());
      await ConversationSignalHub.instance.stop();
    }
  }

  Future<void> _join() async {
    if (!mounted) return;
    final call = _coordinator.state.call;
    if (_joining || call?.callStatus != 'ANSWERED') return;
    final callId = call!.callId;
    setState(() {
      _joining = true;
      _rtcError = null;
    });
    _coordinator.markJoining();
    try {
      final credentials = await _coordinator.issueJoinCredentials();
      if (!mounted ||
          _coordinator.state.call?.callId != callId ||
          _coordinator.state.call?.callStatus != 'ANSWERED') {
        return;
      }
      setState(() {
        _credentials = credentials;
        _joining = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _joining = false;
        _rtcError = 'Không thể thiết lập kết nối cuộc gọi. Vui lòng thử lại.';
      });
      _coordinator.fail('RTC join credentials failed');
    }
  }

  Future<void> _accept() async {
    setState(() => _confirmingIncomingRecording = false);
    try {
      await _coordinator.answerCurrent();
      if (mounted) await _join();
    } catch (_) {
      if (mounted) {
        setState(() => _rtcError = 'Cuộc gọi đã được xử lý ở thiết bị khác.');
      }
    }
  }

  Future<void> _endAndClose() async {
    setState(() {
      _credentials = null;
      _joining = false;
    });
    await _coordinator.endCurrent();
  }

  Widget _buildRtc(ZegoJoinCredentials credentials) {
    return ZegoExpressCallRoom(
      credentials: credentials,
      conversationId: _state.call?.conversationId ?? '',
      callId: _state.call?.callId ?? '',
      isVideo: _state.isVideo,
      onConnected: _coordinator.markInCall,
      onReconnecting: _coordinator.markReconnecting,
      onError: (message) {
        if (!mounted) return;
        setState(() => _rtcError = message);
        _coordinator.fail('RTC room connection failed');
      },
      onRenewToken: () async {
        final callId = _coordinator.state.call?.callId;
        try {
          final renewed = await _coordinator.issueJoinCredentials();
          if (!mounted || callId != _coordinator.state.call?.callId) {
            return null;
          }
          return renewed.token;
        } catch (_) {
          if (mounted) {
            setState(
              () => _rtcError =
                  'Phiên bảo mật sắp hết hạn. Hãy rời và tham gia lại cuộc gọi.',
            );
          }
          return null;
        }
      },
      onHangUp: _endAndClose,
    );
  }

  Widget _buildOverlay() {
    final call = _state.call;
    if (call == null ||
        _state.phase == DirectCallPhase.idle ||
        _state.phase == DirectCallPhase.terminal) {
      return const SizedBox.shrink();
    }
    final incoming = _state.phase == DirectCallPhase.incoming;
    final ready = _state.phase == DirectCallPhase.readyToJoin;
    final title = call.callType == 'VIDEO'
        ? 'Cuộc gọi video'
        : 'Cuộc gọi thoại';
    final confirmingIncomingRecording =
        incoming && _confirmingIncomingRecording;
    return Positioned(
      left: 16,
      right: 16,
      top: 48,
      child: SafeArea(
        child: Card(
          elevation: 12,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (confirmingIncomingRecording) ...[
                  const CircleAvatar(
                    backgroundColor: Color(0xFFFEE2E2),
                    child: Text(
                      'REC',
                      style: TextStyle(
                        color: Color(0xFFB91C1C),
                        fontSize: 12,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    callRecordingConsentTitle,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    callRecordingConsentMessage,
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    callRecordingConsentNote,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 13),
                  ),
                ] else ...[
                  Text(title, style: Theme.of(context).textTheme.titleLarge),
                  const SizedBox(height: 8),
                  Text(
                    incoming
                        ? 'Cuộc gọi đến'
                        : ready
                        ? 'Cuộc gọi đã được trả lời'
                        : _state.phase == DirectCallPhase.answering
                        ? 'Đang trả lời…'
                        : 'Đang đổ chuông…',
                  ),
                ],
                if (_rtcError != null) ...[
                  const SizedBox(height: 8),
                  Text(_rtcError!, style: const TextStyle(color: Colors.red)),
                ],
                const SizedBox(height: 12),
                Wrap(
                  spacing: 12,
                  children: [
                    if (incoming && !confirmingIncomingRecording)
                      FilledButton(
                        onPressed: () =>
                            setState(() => _confirmingIncomingRecording = true),
                        child: const Text('Chấp nhận'),
                      ),
                    if (incoming && !confirmingIncomingRecording)
                      OutlinedButton(
                        onPressed: _coordinator.declineCurrent,
                        child: const Text('Từ chối'),
                      ),
                    if (confirmingIncomingRecording)
                      OutlinedButton(
                        onPressed: () => setState(
                          () => _confirmingIncomingRecording = false,
                        ),
                        child: const Text('Không đồng ý'),
                      ),
                    if (confirmingIncomingRecording)
                      FilledButton(
                        autofocus: true,
                        onPressed: _accept,
                        child: const Text('Đồng ý'),
                      ),
                    if (ready)
                      FilledButton(
                        onPressed: _joining ? null : _join,
                        child: Text(
                          _joining ? 'Đang kết nối…' : 'Tham gia cuộc gọi',
                        ),
                      ),
                    if (!incoming)
                      OutlinedButton(
                        onPressed: _endAndClose,
                        child: Text(ready ? 'Kết thúc' : 'Huỷ'),
                      ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_coordinator.reconcileActiveCalls());
    } else if (state == AppLifecycleState.detached &&
        shouldCancelOutgoingOnDetach(
          call: _coordinator.state.call,
          currentUserId: AuthState.instance.userId,
        )) {
      unawaited(_coordinator.endCurrent());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    AuthState.instance.removeListener(_onAuthChanged);
    _stateSubscription?.cancel();
    RingtonePlayer.instance.stop();
    if (widget.coordinator == null) {
      unawaited(_coordinator.dispose());
    }
    if (widget.manageAuthenticatedSession) {
      unawaited(ConversationSignalHub.instance.stop());
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return DirectCallScope(
      coordinator: _coordinator,
      child: Stack(
        children: [
          widget.child,
          if (_credentials != null)
            Positioned.fill(child: _buildRtc(_credentials!)),
          if (_credentials == null) _buildOverlay(),
        ],
      ),
    );
  }
}
