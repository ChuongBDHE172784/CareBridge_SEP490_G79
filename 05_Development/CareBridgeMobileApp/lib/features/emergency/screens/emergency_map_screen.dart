import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../core/auth/auth_state.dart';
import '../../aiTriage/models/triage_continuation.dart';
import '../../aiTriage/services/triage_continuation_restore_coordinator.dart';
import '../../aiTriage/services/triage_continuation_store.dart';
import '../../aiTriage/services/triage_service.dart';
import '../models/emergency_session_model.dart';
import '../services/emergency_service.dart';

/// CB-017 — Emergency Map (UC-62, UC-63, UC-64, UC-65)
/// Opens (or resumes) an emergency session on entry, which server-side also
/// triggers the family alert (UC-65, event-driven — no dedicated endpoint).
/// Nearest-facility search (UC-63) has no backend endpoint yet, so the
/// facility shown below is mock data pending that implementation.
class EmergencyMapScreen extends StatefulWidget {
  final EmergencySession? existingSession;
  final EmergencyService? emergencyService;
  final bool triageHandoff;
  final String stage;
  final Future<bool> Function()? emergencyDialer;
  final TriageContinuationRestoreCoordinator? continuationCoordinator;

  const EmergencyMapScreen({
    super.key,
    this.existingSession,
    this.emergencyService,
    this.triageHandoff = false,
    this.stage = 'INFANT',
    this.emergencyDialer,
    this.continuationCoordinator,
  });

  @override
  State<EmergencyMapScreen> createState() => _EmergencyMapScreenState();
}

class _EmergencyMapScreenState extends State<EmergencyMapScreen> {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _onPrimaryContainer = Color(0xFF51271B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceContainerHigh = Color(0xFFFFE2D9);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _error = Color(0xFFBA1A1A);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);

  // Mock nearest facility — UC-63 "Find Nearest Healthcare Facility" has no
  // backend endpoint yet. TODO: replace with real search once implemented.
  static const _facilityName = 'Bệnh viện Nhi Đồng 1';
  static const _facilityAddress = '341 Sư Vạn Hạnh, Quận 10';
  static const _facilityPhone = '02839271119';
  static const _facilityDistanceKm = 1.2;
  static const _facilityEtaMin = 5;
  static const _facilityLat = 10.7626;
  static const _facilityLng = 106.6602;

  late final EmergencyService _emergencyService;
  late final String _stage;
  late final TriageContinuationRestoreCoordinator _continuationCoordinator;
  EmergencySession? _session;
  bool _sendingAlert = false;
  bool _loadingSession = false;
  bool _dialing115 = false;
  bool _sessionLoadFailed = false;
  String? _sessionNotice;
  int _activeSessionRequest = 0;

  bool get _isMaternalStage =>
      const {'PRECONCEPTION', 'PREGNANCY', 'POSTPARTUM'}.contains(_stage);

  bool get _isTriageHandoff =>
      widget.triageHandoff ||
      widget.existingSession?.triggerSource.trim().toUpperCase() == 'AI_TRIAGE';

  bool _isActiveSession(EmergencySession? session) =>
      session?.status.trim().toUpperCase() == 'ACTIVE';

  @override
  void initState() {
    super.initState();
    _stage = widget.stage.trim().toUpperCase();
    if (!const {
      'PRECONCEPTION',
      'PREGNANCY',
      'POSTPARTUM',
      'INFANT',
      'TODDLER',
    }.contains(_stage)) {
      throw ArgumentError.value(widget.stage, 'stage', 'Unsupported stage');
    }
    _emergencyService = widget.emergencyService ?? EmergencyService();
    _continuationCoordinator =
        widget.continuationCoordinator ??
        TriageContinuationRestoreCoordinator(
          store: SecureTriageContinuationStore(),
          gateway: TriageService(),
        );
    final existingSession = widget.existingSession;
    _session = _isActiveSession(existingSession) ? existingSession : null;
    if (_session != null) {
      _sessionNotice =
          'Phiên hỗ trợ khẩn cấp đã mở. Yêu cầu thông báo người thân đang được xử lý.';
    } else if (_isTriageHandoff) {
      _loadTriageSession();
    } else {
      _openManualFlow();
    }
  }

  Future<void> _leaveEmergency() async {
    if (!_isTriageHandoff) {
      if (mounted) Navigator.of(context).pop();
      return;
    }
    final userId = AuthState.instance.userId;
    if (userId == null || userId.isEmpty) {
      if (mounted) Navigator.of(context).pop();
      return;
    }
    TriageContinuationDecision decision;
    try {
      decision = await _continuationCoordinator.restoreForUser(
        userId,
        resumeRedEmergency: false,
      );
    } catch (_) {
      if (mounted) {
        setState(() {
          _sessionLoadFailed = true;
          _sessionNotice =
              'Chưa thể xác nhận nơi quay lại. Phiên hỗ trợ vẫn được giữ nguyên; hãy thử lại.';
        });
      }
      return;
    }
    if (!mounted) return;
    if (decision.requiresRetry) {
      setState(() {
        _sessionLoadFailed = true;
        _sessionNotice =
            'Chưa thể xác nhận nơi quay lại. Phiên hỗ trợ vẫn được giữ nguyên; hãy thử lại.';
      });
      return;
    }
    final location = switch (decision.destination) {
      TriageContinuationDestination.motherJourney =>
        '/mother-home?tab=1&triageReturn=${Uri.encodeQueryComponent(_session?.sessionId ?? DateTime.now().microsecondsSinceEpoch.toString())}',
      TriageContinuationDestination.babyProfile
          when decision.originReferenceId != null =>
        '/babies/detail/${Uri.encodeComponent(decision.originReferenceId!)}',
      TriageContinuationDestination.safeDashboard => '/mother-home',
      _ => null,
    };
    if (location == null) {
      Navigator.of(context).pop();
      return;
    }
    context.go(
      location,
      extra:
          decision.destination == TriageContinuationDestination.motherJourney ||
              decision.destination == TriageContinuationDestination.babyProfile
          ? TriageContinuationArrival(
              userId: userId,
              decision: decision,
              coordinator: _continuationCoordinator,
            )
          : null,
    );
  }

  Future<void> _loadTriageSession() async {
    if (_loadingSession) return;
    final requestUserId = AuthState.instance.userId;
    if (requestUserId == null || requestUserId.isEmpty) {
      setState(() {
        _sessionLoadFailed = true;
        _sessionNotice = 'Vui lòng đăng nhập lại trước khi tải phiên hỗ trợ.';
      });
      return;
    }
    final request = ++_activeSessionRequest;
    setState(() {
      _loadingSession = true;
      _sessionLoadFailed = false;
      _sessionNotice = 'Đang tải phiên hỗ trợ khẩn cấp...';
    });
    try {
      final session = await _emergencyService.getActive();
      if (!mounted || request != _activeSessionRequest) return;
      if (AuthState.instance.userId != requestUserId) {
        setState(() {
          _session = null;
          _sessionLoadFailed = true;
          _sessionNotice =
              'Phiên đăng nhập đã thay đổi. Hãy tải lại phiên hỗ trợ cho tài khoản hiện tại.';
        });
        return;
      }
      if (!_isActiveSession(session)) {
        setState(() {
          _sessionLoadFailed = true;
          _sessionNotice =
              'Chưa thể tải phiên hỗ trợ. Phiên có thể vẫn đang được hệ thống xử lý.';
        });
        return;
      }
      setState(() {
        _session = session;
        _sessionLoadFailed = false;
        _sessionNotice =
            'Phiên hỗ trợ khẩn cấp đã mở. Yêu cầu thông báo người thân đang được xử lý.';
      });
    } catch (_) {
      if (mounted && request == _activeSessionRequest) {
        setState(() {
          _sessionLoadFailed = true;
          _sessionNotice =
              'Không thể tải phiên hỗ trợ lúc này. Bạn có thể thử lại mà không tạo phiên mới.';
        });
      }
    } finally {
      if (mounted && request == _activeSessionRequest) {
        setState(() => _loadingSession = false);
      }
    }
  }

  Future<void> _openManualFlow() async {
    if (_loadingSession) return;
    setState(() {
      _loadingSession = true;
      _sessionLoadFailed = false;
    });
    try {
      final session = await _emergencyService.openFlow(triggerSource: 'MANUAL');
      if (!mounted) return;
      setState(() {
        _session = session;
        _sessionNotice =
            'Phiên hỗ trợ khẩn cấp đã mở. Yêu cầu thông báo người thân đang được xử lý.';
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _sessionLoadFailed = true;
          _sessionNotice =
              'Không thể mở phiên hỗ trợ lúc này. Hãy gọi 115 nếu bạn đang không an toàn.';
        });
      }
    } finally {
      if (mounted) setState(() => _loadingSession = false);
    }
  }

  Future<void> _sendFamilyAlert() async {
    if (_sendingAlert) return;
    final requestUserId = AuthState.instance.userId;
    if (requestUserId == null || requestUserId.isEmpty) {
      setState(() {
        _sessionLoadFailed = true;
        _sessionNotice =
            'Vui lòng đăng nhập lại trước khi xác nhận trạng thái thông báo.';
      });
      return;
    }
    final request = ++_activeSessionRequest;
    setState(() {
      _sendingAlert = true;
      _loadingSession = false;
    });
    try {
      final active = await _emergencyService.getActive();
      if (!mounted || request != _activeSessionRequest) return;
      if (AuthState.instance.userId != requestUserId) {
        setState(() {
          _session = null;
          _sessionLoadFailed = true;
          _sessionNotice =
              'Phiên đăng nhập đã thay đổi. Hãy tải lại phiên hỗ trợ cho tài khoản hiện tại.';
        });
        return;
      }
      if (!_isActiveSession(active)) {
        if (_isTriageHandoff) {
          setState(() {
            _sessionLoadFailed = true;
            _sessionNotice =
                'Chưa thể xác nhận yêu cầu thông báo. Hãy thử lại sau ít phút.';
          });
          return;
        }
        await _openManualFlow();
        if (!mounted || _session == null) return;
      } else {
        setState(() {
          _session = active;
          _sessionLoadFailed = false;
          _sessionNotice =
              'Phiên hỗ trợ đang hoạt động. Yêu cầu thông báo người thân đang được xử lý.';
        });
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            behavior: SnackBarBehavior.floating,
            shape: StadiumBorder(),
            content: Text(
              'Phiên hỗ trợ đã mở; yêu cầu thông báo đang được xử lý.',
            ),
          ),
        );
      }
    } catch (_) {
      if (mounted && request == _activeSessionRequest) {
        setState(() {
          _sessionLoadFailed = true;
          _sessionNotice =
              'Không thể xác nhận trạng thái thông báo lúc này. Phiên hỗ trợ vẫn được giữ nguyên.';
        });
      }
    } finally {
      if (mounted) {
        setState(() => _sendingAlert = false);
      }
    }
  }

  Future<void> _callFacility() async {
    final uri = Uri(scheme: 'tel', path: _facilityPhone);
    await launchUrl(uri);
  }

  Future<void> _call115() async {
    if (_dialing115 || !mounted) return;
    setState(() => _dialing115 = true);
    try {
      final opened =
          await (widget.emergencyDialer?.call() ??
              launchUrl(
                Uri.parse('tel:115'),
                mode: LaunchMode.externalApplication,
              ));
      if (!opened && mounted) {
        setState(() {
          _sessionNotice =
              'Không thể mở ứng dụng gọi. Hãy tự gọi 115 hoặc nhờ người bên cạnh gọi giúp.';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _sessionNotice =
              'Không thể mở ứng dụng gọi. Hãy tự gọi 115 hoặc nhờ người bên cạnh gọi giúp.';
        });
      }
    } finally {
      if (mounted) setState(() => _dialing115 = false);
    }
  }

  Future<void> _openDirections() async {
    final uri = Uri.parse(
      'https://www.google.com/maps/dir/?api=1&destination=$_facilityLat,$_facilityLng',
    );
    await launchUrl(uri, mode: LaunchMode.externalApplication);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: Column(
          children: [
            _buildTopBar(),
            Expanded(
              child: _isMaternalStage
                  ? _buildMaternalEmergencyPanel()
                  : Stack(
                      children: [
                        _buildMapBackground(),
                        Positioned(
                          top: 12,
                          right: 16,
                          child: _buildFamilyAlertButton(),
                        ),
                        Align(
                          alignment: Alignment.bottomCenter,
                          child: _buildFacilitySheet(),
                        ),
                      ],
                    ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 48,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: Row(
          children: [
            SizedBox(
              width: 48,
              height: 48,
              child: IconButton(
                key: const Key('emergency-leave'),
                padding: EdgeInsets.zero,
                icon: const Icon(Icons.arrow_back, color: _primary),
                onPressed: _leaveEmergency,
              ),
            ),
            Expanded(
              child: Text(
                _isMaternalStage ? 'Hỗ trợ khẩn cấp cho mẹ' : 'Bản đồ khẩn cấp',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ),
            const SizedBox(width: 48, height: 48),
          ],
        ),
      ),
    );
  }

  // Stylized static map background — no map SDK dependency in this app yet,
  // so this is a non-interactive placeholder matching the design's palette.
  Widget _buildMaternalEmergencyPanel() {
    return Container(
      color: const Color(0xFFF6F1EC),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(32),
                border: Border.all(color: const Color(0xFFE8DDD6)),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x0F5A463F),
                    blurRadius: 32,
                    offset: Offset(0, 12),
                  ),
                ],
              ),
              child: Column(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: const BoxDecoration(
                      color: Color(0xFFFFDAD6),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.emergency_outlined,
                      color: _error,
                      size: 30,
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Ưu tiên gọi cấp cứu khi không an toàn',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Color(0xFF5A463F),
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 10),
                  const Text(
                    'Gọi 115 ngay nếu có dấu hiệu nặng. Không chờ bản đồ hoặc phiên trực tuyến hoàn tất.',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Color(0xFF9C857C),
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      height: 1.45,
                    ),
                  ),
                  const SizedBox(height: 20),
                  SizedBox(
                    height: 52,
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      key: const Key('emergency-maternal-call-115'),
                      onPressed: _dialing115 ? null : _call115,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _error,
                        foregroundColor: Colors.white,
                        shape: const StadiumBorder(),
                        elevation: 3,
                      ),
                      icon: _dialing115
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.call),
                      label: const Text(
                        'Gọi cấp cứu 115 ngay',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Semantics(
              liveRegion: true,
              container: true,
              label: _sessionNotice ?? 'Trạng thái phiên hỗ trợ khẩn cấp',
              child: Container(
                key: const Key('emergency-session-status'),
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: const Color(0xFFF2EAE4),
                  borderRadius: BorderRadius.circular(24),
                  border: const Border(
                    left: BorderSide(color: _primaryContainer, width: 4),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(
                          _sessionLoadFailed
                              ? Icons.info_outline
                              : Icons.notifications_active_outlined,
                          color: _primary,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            _sessionNotice ??
                                'Yêu cầu mở phiên hỗ trợ đang được xử lý.',
                            style: const TextStyle(
                              color: Color(0xFF5A463F),
                              fontSize: 16,
                              fontWeight: FontWeight.w700,
                              height: 1.4,
                            ),
                          ),
                        ),
                      ],
                    ),
                    if (_sessionLoadFailed) ...[
                      const SizedBox(height: 14),
                      SizedBox(
                        height: 48,
                        child: OutlinedButton.icon(
                          key: const Key('emergency-session-retry'),
                          onPressed: _loadingSession
                              ? null
                              : _isTriageHandoff
                              ? _loadTriageSession
                              : _openManualFlow,
                          style: OutlinedButton.styleFrom(
                            foregroundColor: _primary,
                            shape: const StadiumBorder(),
                            side: const BorderSide(color: _primaryContainer),
                          ),
                          icon: const Icon(Icons.refresh),
                          label: const Text(
                            'Thử tải lại phiên hỗ trợ',
                            style: TextStyle(fontWeight: FontWeight.w800),
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
            SizedBox(
              height: 48,
              child: OutlinedButton.icon(
                key: const Key('emergency-family-alert'),
                onPressed:
                    _sendingAlert || (_loadingSession && !_isTriageHandoff)
                    ? null
                    : _sendFamilyAlert,
                style: OutlinedButton.styleFrom(
                  foregroundColor: _primary,
                  backgroundColor: Colors.white,
                  shape: const StadiumBorder(),
                  side: const BorderSide(color: Color(0xFFE8DDD6)),
                ),
                icon: _sendingAlert
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.sync),
                label: const Text(
                  'Kiểm tra yêu cầu thông báo',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMapBackground() {
    return Container(
      color: const Color(0xFFFBE9DF),
      child: CustomPaint(
        painter: _RoadGridPainter(),
        child: Stack(
          children: [
            Positioned(
              top: 90,
              left: 70,
              child: _buildMarker(
                icon: Icons.local_hospital,
                color: _error,
                label: 'BV Nhi Đồng',
              ),
            ),
            const Positioned(top: 220, right: 60, child: _SecondaryMarker()),
            const Align(
              alignment: Alignment.center,
              child: _UserLocationMarker(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMarker({
    required IconData icon,
    required Color color,
    required String label,
  }) {
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
            boxShadow: const [
              BoxShadow(
                color: Color(0x1F5A463F),
                blurRadius: 24,
                offset: Offset(0, 8),
              ),
            ],
          ),
          child: Icon(icon, color: Colors.white, size: 20),
        ),
        const SizedBox(height: 4),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: _surface,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Text(
            label,
            style: const TextStyle(fontSize: 12, color: _onSurface),
          ),
        ),
      ],
    );
  }

  Widget _buildFamilyAlertButton() {
    return SizedBox(
      height: 48,
      child: ElevatedButton.icon(
        key: const Key('emergency-family-alert'),
        onPressed: _sendingAlert || (_loadingSession && !_isTriageHandoff)
            ? null
            : _sendFamilyAlert,
        style: ElevatedButton.styleFrom(
          backgroundColor: _error,
          foregroundColor: Colors.white,
          shape: const StadiumBorder(),
          padding: const EdgeInsets.symmetric(horizontal: 16),
          elevation: 4,
        ),
        icon: _sendingAlert
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  color: Colors.white,
                  strokeWidth: 2,
                ),
              )
            : const Icon(Icons.campaign, size: 20),
        label: const Text(
          'Kiểm tra yêu cầu thông báo',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
        ),
      ),
    );
  }

  Widget _buildPediatricSessionStatus() {
    return Semantics(
      liveRegion: true,
      container: true,
      label: _sessionNotice ?? 'Trạng thái phiên hỗ trợ khẩn cấp',
      child: Container(
        key: const Key('emergency-session-status'),
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: const Color(0xFFF2EAE4),
          borderRadius: BorderRadius.circular(24),
          border: const Border(
            left: BorderSide(color: _primaryContainer, width: 4),
          ),
          boxShadow: const [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 24,
              offset: Offset(0, 8),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  _sessionLoadFailed
                      ? Icons.info_outline
                      : Icons.hourglass_top_rounded,
                  color: _primary,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    _sessionNotice ??
                        'Yêu cầu mở phiên hỗ trợ đang được xử lý.',
                    style: const TextStyle(
                      color: Color(0xFF5A463F),
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      height: 1.4,
                    ),
                  ),
                ),
              ],
            ),
            if (_sessionLoadFailed) ...[
              const SizedBox(height: 14),
              SizedBox(
                height: 48,
                child: OutlinedButton.icon(
                  key: const Key('emergency-session-retry'),
                  onPressed: _loadingSession
                      ? null
                      : _isTriageHandoff
                      ? _loadTriageSession
                      : _openManualFlow,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _primary,
                    shape: const StadiumBorder(),
                    side: const BorderSide(color: _primaryContainer),
                  ),
                  icon: const Icon(Icons.refresh),
                  label: const Text(
                    'Thử tải lại phiên hỗ trợ',
                    style: TextStyle(fontWeight: FontWeight.w800),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildFacilitySheet() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
      decoration: const BoxDecoration(
        color: _surface,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        boxShadow: [
          BoxShadow(
            color: Color(0x145A463F),
            blurRadius: 30,
            offset: Offset(0, -8),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 48,
            height: 4,
            margin: const EdgeInsets.only(bottom: 16),
            decoration: BoxDecoration(
              color: _surfaceContainerHigh,
              borderRadius: BorderRadius.circular(99),
            ),
          ),
          if (_sessionLoadFailed || _loadingSession) ...[
            _buildPediatricSessionStatus(),
            const SizedBox(height: 16),
          ],
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      _facilityName,
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: const [
                        Icon(
                          Icons.location_on,
                          size: 16,
                          color: _onSurfaceVariant,
                        ),
                        SizedBox(width: 4),
                        Text(
                          _facilityAddress,
                          style: TextStyle(
                            fontSize: 14,
                            color: _onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: _errorContainer,
                  borderRadius: BorderRadius.circular(99),
                ),
                child: Column(
                  children: const [
                    Text(
                      '$_facilityEtaMin',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _onErrorContainer,
                        height: 1,
                      ),
                    ),
                    Text(
                      'phút',
                      style: TextStyle(
                        fontSize: 12,
                        color: _onErrorContainer,
                        height: 1.2,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: _surfaceContainer,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(Icons.directions_car, size: 14, color: _onSurface),
                    SizedBox(width: 4),
                    Text(
                      '$_facilityDistanceKm km',
                      style: TextStyle(fontSize: 12, color: _onSurface),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: _secondaryContainer,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(
                      Icons.check_circle,
                      size: 14,
                      color: _onSecondaryContainer,
                    ),
                    SizedBox(width: 4),
                    Text(
                      'Mở cửa 24/7',
                      style: TextStyle(
                        fontSize: 12,
                        color: _onSecondaryContainer,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: SizedBox(
                  height: 52,
                  child: ElevatedButton.icon(
                    onPressed: _callFacility,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _surfaceContainerHigh,
                      foregroundColor: _onSurface,
                      shape: const StadiumBorder(),
                      elevation: 0,
                    ),
                    icon: const Icon(Icons.call),
                    label: const Text(
                      'Gọi điện',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: SizedBox(
                  height: 52,
                  child: ElevatedButton.icon(
                    onPressed: _openDirections,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _primaryContainer,
                      foregroundColor: _onPrimaryContainer,
                      shape: const StadiumBorder(),
                      elevation: 2,
                    ),
                    icon: const Icon(Icons.directions),
                    label: const Text(
                      'Chỉ đường',
                      style: TextStyle(fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _SecondaryMarker extends StatelessWidget {
  const _SecondaryMarker();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.85),
        shape: BoxShape.circle,
        border: Border.all(color: const Color(0xFFD6C2BD)),
      ),
      child: const Icon(
        Icons.medical_services_outlined,
        color: Color(0xFF845143),
        size: 18,
      ),
    );
  }
}

class _UserLocationMarker extends StatefulWidget {
  const _UserLocationMarker();

  @override
  State<_UserLocationMarker> createState() => _UserLocationMarkerState();
}

class _UserLocationMarkerState extends State<_UserLocationMarker>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 2),
  )..repeat();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 48,
      height: 48,
      child: Stack(
        alignment: Alignment.center,
        children: [
          AnimatedBuilder(
            animation: _controller,
            builder: (_, _) {
              final scale = 0.6 + (_controller.value * 0.6);
              return Opacity(
                opacity: (1 - _controller.value).clamp(0.0, 1.0),
                child: Transform.scale(
                  scale: scale,
                  child: Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: const Color(0xFF845143).withValues(alpha: 0.2),
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              );
            },
          ),
          Container(
            width: 16,
            height: 16,
            decoration: BoxDecoration(
              color: const Color(0xFF845143),
              shape: BoxShape.circle,
              border: Border.all(color: const Color(0xFFFFF8F6), width: 2),
            ),
          ),
        ],
      ),
    );
  }
}

class _RoadGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = const Color(0xFFF1D4CA)
      ..strokeWidth = 3;
    const gap = 56.0;
    for (double x = 0; x < size.width; x += gap) {
      canvas.drawLine(Offset(x, 0), Offset(x, size.height), paint);
    }
    for (double y = 0; y < size.height; y += gap) {
      canvas.drawLine(Offset(0, y), Offset(size.width, y), paint);
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
