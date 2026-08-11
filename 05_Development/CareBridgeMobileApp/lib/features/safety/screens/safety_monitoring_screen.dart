import 'dart:async';

import 'package:flutter/material.dart';
import '../../../core/network/api_client.dart';
import '../models/imu_diagnostics_model.dart';
import '../models/safety_config_model.dart';
import '../services/safety_demo_mode.dart';
import '../services/safety_foreground_service.dart';
import '../services/safety_service.dart';
import '../widgets/safety_countdown_sheet.dart';
import 'enable_fall_detection_screen.dart';
import '../../emergency/services/emergency_service.dart';

Future<void> dispatchSafetyCountdownResult({
  required SafetyCountdownResult? result,
  required bool simulated,
  required Future<void> Function() onSafe,
  required Future<void> Function(String? reasonCode, String? reason)
  onFalsePositive,
  required Future<void> Function() onEmergency,
}) async {
  if (simulated || result == null) return;
  switch (result.action) {
    case SafetyCountdownAction.safe:
      await onSafe();
    case SafetyCountdownAction.falsePositive:
      await onFalsePositive(result.reasonCode, result.reason);
    case SafetyCountdownAction.help:
    case SafetyCountdownAction.timeout:
      await onEmergency();
  }
}

SafetyEvent? selectNextOpenSafetyEvent(
  Iterable<SafetyEvent> events, {
  required String excludingId,
  Set<String> suppressedIds = const {},
}) {
  for (final event in events) {
    if (isPendingSafetyCountdown(event) &&
        event.id != excludingId &&
        !suppressedIds.contains(event.id)) {
      return event;
    }
  }
  return null;
}

bool isPendingSafetyCountdown(SafetyEvent event) =>
    event.status == 'OPEN' || event.status == 'TEST_OPEN';

bool shouldReleaseSafetyCountdownOwnership({
  required String? activeEventId,
  required String completedEventId,
}) => activeEventId == completedEventId;

bool isLikelyDuplicateFallEvent(SafetyEvent first, SafetyEvent second) {
  if (first.id == second.id ||
      first.eventType == 'SENSOR_SELF_TEST' ||
      second.eventType == 'SENSOR_SELF_TEST') {
    return false;
  }
  final firstAt = first.detectedAt;
  final secondAt = second.detectedAt;
  if (firstAt == null || secondAt == null) return false;
  return firstAt.difference(secondAt).abs() <= const Duration(seconds: 10);
}

typedef ReportDuplicateFalsePositive =
    Future<SafetyEvent> Function(String eventId, {String? note});

Future<SafetyEvent> closeDuplicateFallEvent({
  required SafetyEvent event,
  required ReportDuplicateFalsePositive reportFalsePositive,
  required Future<void> Function(SafetyEvent event) resolveEmergency,
}) async {
  final updated = await reportFalsePositive(
    event.id,
    note: 'Tự động gộp bản ghi trùng của cùng một lần phát hiện ngã.',
  );
  await resolveEmergency(updated);
  return updated;
}

Future<void> dispatchSensorSelfTestCountdownResult({
  required SafetyCountdownResult? result,
  required Future<void> Function() onSafe,
  required Future<void> Function(String? reasonCode, String? reason)
  onFalsePositive,
  required Future<void> Function(String outcome) onComplete,
}) async {
  if (result == null) return;
  switch (result.action) {
    case SafetyCountdownAction.safe:
      await onSafe();
    case SafetyCountdownAction.falsePositive:
      await onFalsePositive(result.reasonCode, result.reason);
    case SafetyCountdownAction.help:
      await onComplete('NEED_HELP');
    case SafetyCountdownAction.timeout:
      await onComplete('TIMEOUT');
  }
}

bool isSafeFallSimulationEligible({
  required SafetyConfig? config,
  required bool coordinatorRunning,
  required ImuDiagnosticsSnapshot? diagnostics,
  required DateTime now,
}) =>
    (config?.fallDetectionEnabled ?? false) &&
    (config?.sensorPermissionGranted ?? false) &&
    coordinatorRunning &&
    diagnostics?.state == ImuSamplingState.sampling &&
    diagnostics!.ageAt(now) <= const Duration(seconds: 2);

bool isSensorSelfTestEligible({
  required SafetyConfig? config,
  required bool coordinatorRunning,
}) =>
    (config?.fallDetectionEnabled ?? false) &&
    (config?.sensorPermissionGranted ?? false) &&
    coordinatorRunning;

bool shouldAcceptSensorSelfTestResult({
  required bool armed,
  required DateTime? armedAt,
  required DateTime detectedAt,
}) => armed && armedAt != null && !detectedAt.toUtc().isBefore(armedAt.toUtc());

class SafetyRealEventQueue {
  final List<SafetyEvent> _events = [];

  void enqueue(SafetyEvent event) {
    if (event.status != 'OPEN' || _events.any((item) => item.id == event.id)) {
      return;
    }
    _events.add(event);
  }

  void remove(String eventId) {
    _events.removeWhere((event) => event.id == eventId);
  }

  SafetyEvent? takeNext({required String excludingId}) {
    final index = _events.indexWhere((event) => event.id != excludingId);
    if (index < 0) return null;
    return _events.removeAt(index);
  }
}

/// CB-023 — Safety Monitoring (UC-133..UC-141, UC-176)
/// Hub screen for fall detection, SOS, and safety event history.
/// Pushed from the Hành trình (Journey) tab, hence the small back affordance
/// added to the otherwise back-button-less design header.
class SafetyMonitoringScreen extends StatefulWidget {
  const SafetyMonitoringScreen({super.key});

  @override
  State<SafetyMonitoringScreen> createState() => _SafetyMonitoringScreenState();
}

class _SafetyMonitoringScreenState extends State<SafetyMonitoringScreen>
    with WidgetsBindingObserver {
  static const _primary = Color(0xFF845143);
  static const _primaryContainer = Color(0xFFC98C7B);
  static const _surface = Color(0xFFFFF8F6);
  static const _surfaceContainerLowest = Color(0xFFFFFFFF);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _tertiary = Color(0xFF625D59);

  final _safetyService = SafetyService();
  final _emergencyService = EmergencyService();
  final _foregroundCoordinator = SafetyForegroundServiceCoordinator.instance;
  SafetyConfig? _config;
  List<SafetyEvent> _events = const [];
  StreamSubscription<SafetyEvent>? _detectedEventSubscription;
  StreamSubscription<SensorSelfTestResult>? _sensorSelfTestSubscription;
  StreamSubscription<ImuDiagnosticsSnapshot>? _diagnosticsSubscription;
  ImuDiagnosticsSnapshot? _imuDiagnostics;
  Timer? _demoRecoveryTimer;
  Timer? _demoGestureArmTimer;
  DateTime? _sensorSelfTestArmedAt;
  final SafetyRealEventQueue _pendingRealEvents = SafetyRealEventQueue();
  final Set<String> _suppressedDuplicateEventIds = {};
  final Set<String> _duplicateCleanupInFlight = {};
  String? _countdownEventId;
  SafetyEvent? _countdownEvent;
  bool _loading = true;
  // Local sensor-stream toggle backed by sensors_plus accelerometer/gyroscope
  // streams; backend fall detection remains controlled by SafetyConfig.
  bool _imuSensorActive = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _detectedEventSubscription = _foregroundCoordinator.detectedEvents.listen(
      _onDetectedEvent,
    );
    _sensorSelfTestSubscription = _foregroundCoordinator.sensorSelfTestResults
        .listen(_onSensorSelfTestResult);
    if (safetyDiagnosticsEnabled) {
      _diagnosticsSubscription = _foregroundCoordinator.diagnostics.listen(
        _onDiagnosticsSnapshot,
      );
    }
    _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _detectedEventSubscription?.cancel();
    _sensorSelfTestSubscription?.cancel();
    _diagnosticsSubscription?.cancel();
    _demoRecoveryTimer?.cancel();
    _demoGestureArmTimer?.cancel();
    super.dispose();
  }

  void _onDiagnosticsSnapshot(ImuDiagnosticsSnapshot snapshot) {
    if (!mounted) return;
    setState(() => _imuDiagnostics = snapshot);
    if (!safetyDemoMode || snapshot.state != ImuSamplingState.stopped) return;
    if (_demoRecoveryTimer?.isActive ?? false) return;
    _demoRecoveryTimer = Timer(const Duration(seconds: 1), () {
      if (mounted) unawaited(_foregroundCoordinator.reconcile());
    });
  }

  void _onSensorSelfTestResult(SensorSelfTestResult result) {
    final accepted = shouldAcceptSensorSelfTestResult(
      armed: _demoGestureArmTimer?.isActive ?? false,
      armedAt: _sensorSelfTestArmedAt,
      detectedAt: result.detectedAt,
    );
    if (!mounted || !accepted) return;
    _demoGestureArmTimer?.cancel();
    _sensorSelfTestArmedAt = null;
    setState(() {});
    unawaited(_showDemoFallAlert(result));
  }

  void _armDemoGesture() {
    final eligible = isSensorSelfTestEligible(
      config: _config,
      coordinatorRunning: _foregroundCoordinator.isRunning,
    );
    if (!eligible || _countdownEventId != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Hãy bật phát hiện ngã, quyền cảm biến và dịch vụ IMU trước khi kiểm tra.',
          ),
        ),
      );
      return;
    }

    _sensorSelfTestArmedAt = DateTime.now().toUtc();
    _demoGestureArmTimer?.cancel();
    _demoGestureArmTimer = Timer(const Duration(seconds: 8), () {
      if (!mounted) return;
      _sensorSelfTestArmedAt = null;
      setState(() {});
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Chưa nhận được cử chỉ. Hãy giữ máy yên rồi vung nhanh và thử lại.',
          ),
        ),
      );
    });
    setState(() {});
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      unawaited(_load());
    }
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final config = await _safetyService.getConfig();
      final events = await _safetyService.getSafetyEvents();
      await _foregroundCoordinator.reconcile();
      if (mounted) {
        setState(() {
          _config = config;
          _events = events;
          _imuSensorActive = _foregroundCoordinator.isRunning;
        });
        final active = _countdownEvent;
        if (active != null) {
          for (final event in events) {
            if (event.id != active.id &&
                isPendingSafetyCountdown(event) &&
                isLikelyDuplicateFallEvent(active, event)) {
              _suppressDuplicateEvent(event);
            }
          }
        } else {
          SafetyEvent? pending;
          for (final event in events) {
            if (isPendingSafetyCountdown(event) &&
                !_suppressedDuplicateEventIds.contains(event.id)) {
              pending = event;
              break;
            }
          }
          if (pending != null) {
            for (final event in events) {
              if (event.id != pending.id &&
                  isPendingSafetyCountdown(event) &&
                  isLikelyDuplicateFallEvent(pending, event)) {
                _suppressDuplicateEvent(event);
              }
            }
            unawaited(_showCountdown(pending));
          }
        }
      }
    } catch (_) {
      if (mounted) {
        setState(
          () => _config = const SafetyConfig(
            fallDetectionEnabled: false,
            sensitivityLevel: 'MEDIUM',
            emergencyAutoAlert: true,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  void _onDetectedEvent(SafetyEvent event) {
    if (!mounted) return;
    final active = _countdownEvent;
    if (active != null && isLikelyDuplicateFallEvent(active, event)) {
      _suppressDuplicateEvent(event);
      setState(() {
        _events = [event, ..._events.where((item) => item.id != event.id)];
      });
      return;
    }
    _pendingRealEvents.enqueue(event);
    setState(() {
      _events = [event, ..._events.where((item) => item.id != event.id)];
    });
    unawaited(_showCountdown(event));
  }

  void _suppressDuplicateEvent(SafetyEvent event) {
    _suppressedDuplicateEventIds.add(event.id);
    _pendingRealEvents.remove(event.id);
    if (_duplicateCleanupInFlight.add(event.id)) {
      unawaited(_closeSuppressedDuplicateEvent(event));
    }
  }

  Future<void> _closeSuppressedDuplicateEvent(SafetyEvent event) async {
    try {
      final updated = await closeDuplicateFallEvent(
        event: event,
        reportFalsePositive: _safetyService.reportFalsePositive,
        resolveEmergency: _resolveEventEmergency,
      );
      if (mounted) {
        setState(() {
          _events = [
            updated,
            ..._events.where((item) => item.id != updated.id),
          ];
        });
      }
    } on ApiException catch (error) {
      if (error.statusCode == 409 && error.errorCode == 'SAFETY-010') {
        if (mounted) unawaited(_load());
        return;
      }
      _restoreDuplicateAfterCleanupFailure(event);
    } catch (_) {
      _restoreDuplicateAfterCleanupFailure(event);
    } finally {
      _duplicateCleanupInFlight.remove(event.id);
    }
  }

  void _restoreDuplicateAfterCleanupFailure(SafetyEvent event) {
    _suppressedDuplicateEventIds.remove(event.id);
    _pendingRealEvents.enqueue(event);
  }

  Future<void> _showCountdown(
    SafetyEvent event, {
    bool simulated = false,
    bool presentAsRealAlert = false,
  }) async {
    final deadline = event.countdownDeadlineAt;
    if (!mounted ||
        !isPendingSafetyCountdown(event) ||
        deadline == null ||
        _countdownEventId != null) {
      return;
    }
    _countdownEventId = event.id;
    _countdownEvent = event;
    if (!simulated) _pendingRealEvents.remove(event.id);
    final result = await showModalBottomSheet<SafetyCountdownResult>(
      context: context,
      isDismissible: false,
      enableDrag: false,
      isScrollControlled: true,
      builder: (_) => SafetyCountdownSheet(
        event: event,
        simulated: simulated,
        presentAsRealAlert: presentAsRealAlert,
        onTimeout: () {
          if (!simulated && event.eventType != 'SENSOR_SELF_TEST') {
            unawaited(_safetyService.sendEmergencyAlertForEvent(event.id));
          }
        },
      ),
    );
    _releaseCountdownOwnership(event.id);
    try {
      final isSensorSelfTest = event.eventType == 'SENSOR_SELF_TEST';
      if (isSensorSelfTest) {
        await dispatchSensorSelfTestCountdownResult(
          result: result,
          onSafe: () => _confirmEventSafe(
            event,
            note: 'Diễn tập IMU: người dùng thử thao tác Tôi vẫn ổn.',
          ),
          onFalsePositive: (reasonCode, reason) => _reportEventFalsePositive(
            event,
            note: reason == null || reason.isEmpty
                ? 'Diễn tập IMU: báo phát hiện nhầm'
                : 'Diễn tập IMU: $reason',
          ),
          onComplete: (outcome) =>
              _safetyService.completeSensorSelfTest(event.id, outcome: outcome),
        );
      } else {
        await dispatchSafetyCountdownResult(
          result: result,
          simulated: simulated,
          onSafe: () => _confirmEventSafe(
            event,
            note: 'Người dùng xác nhận an toàn trong thời gian đếm ngược.',
          ),
          onFalsePositive: (reasonCode, reason) => _reportEventFalsePositive(
            event,
            note: reason ?? 'Báo phát hiện nhầm',
          ),
          onEmergency: () =>
              _safetyService.sendEmergencyAlertForEvent(event.id),
        );
      }
      final showDemoEscalation =
          (isSensorSelfTest || (simulated && presentAsRealAlert)) &&
          (result?.action == SafetyCountdownAction.help ||
              result?.action == SafetyCountdownAction.timeout);
      if (showDemoEscalation && mounted) {
        await _showDemoEmergencyEscalation();
      }
    } catch (error) {
      if (error is ApiException &&
          error.statusCode == 409 &&
          error.errorCode == 'SAFETY-010') {
        // Timeout escalation and the user's final safe tap may cross on the
        // wire. The server has already recorded one terminal response; close
        // the local flow and refresh instead of showing a raw conflict.
        if (mounted) {
          try {
            await _load();
          } catch (_) {
            // The response was already persisted; a refresh failure must not
            // turn the handled race back into a visible error.
          }
        }
        return;
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Không thể ghi nhận phản hồi an toàn: $error'),
          ),
        );
      }
    } finally {
      if (!simulated && mounted) await _load();
      _releaseCountdownOwnership(event.id);
      if (mounted) {
        final next =
            _pendingRealEvents.takeNext(excludingId: event.id) ??
            selectNextOpenSafetyEvent(
              _events,
              excludingId: event.id,
              suppressedIds: _suppressedDuplicateEventIds,
            );
        if (next != null) unawaited(_showCountdown(next));
      }
    }
  }

  void _releaseCountdownOwnership(String eventId) {
    if (!shouldReleaseSafetyCountdownOwnership(
      activeEventId: _countdownEventId,
      completedEventId: eventId,
    )) {
      return;
    }
    _countdownEventId = null;
    _countdownEvent = null;
  }

  Future<void> _showDemoEmergencyEscalation() async {
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (dialogContext) => AlertDialog(
        icon: const Icon(Icons.emergency, size: 48, color: _onErrorContainer),
        title: const Text(
          'Không nhận được phản hồi',
          textAlign: TextAlign.center,
        ),
        content: const Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'ĐÂY LÀ DIỄN TẬP — KHÔNG GỬI CẢNH BÁO THẬT',
              textAlign: TextAlign.center,
              style: TextStyle(fontWeight: FontWeight.w800, color: _primary),
            ),
            SizedBox(height: 16),
            _EmergencyEscalationRow(
              icon: Icons.notifications_active_outlined,
              text:
                  'Khi ngã thật: CareBridge liên hệ người thân đã đăng ký để yêu cầu kiểm tra ngay.',
            ),
            SizedBox(height: 14),
            _EmergencyEscalationRow(
              icon: Icons.phone_in_talk_outlined,
              text:
                  'Nếu chưa nhận được hỗ trợ, luồng thật sẽ chuyển sang bước gọi 115.',
            ),
          ],
        ),
        actions: [
          FilledButton.icon(
            key: const Key('close-demo-emergency-escalation'),
            onPressed: () => Navigator.of(dialogContext).pop(),
            icon: const Icon(Icons.check_circle_outline),
            label: const Text('Tôi đã an toàn — đóng cảnh báo'),
          ),
        ],
      ),
    );
  }

  Future<void> _showDemoFallAlert(SensorSelfTestResult result) async {
    if (!mounted) return;
    try {
      final event = await _safetyService.createSensorSelfTestEvent(result);
      if (!mounted) return;
      setState(() {
        _events = [event, ..._events.where((item) => item.id != event.id)];
      });
      await _showCountdown(event);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Không thể bắt đầu diễn tập cảnh báo: $error')),
      );
    }
  }

  Future<void> _confirmEventSafe(SafetyEvent event, {String? note}) async {
    _foregroundCoordinator.rearmFallDetectorAfterResponse();
    final updated = await _safetyService.confirmSafetyCheck(
      event.id,
      note: note,
    );
    await _resolveEventEmergency(updated);
  }

  Future<void> _reportEventFalsePositive(
    SafetyEvent event, {
    String? note,
  }) async {
    _foregroundCoordinator.rearmFallDetectorAfterResponse();
    final updated = await _safetyService.reportFalsePositive(
      event.id,
      note: note,
    );
    await _resolveEventEmergency(updated);
  }

  Future<void> _resolveEventEmergency(SafetyEvent event) async {
    final sessionId = event.emergencySessionId;
    if (sessionId == null || sessionId.isEmpty) return;
    await _emergencyService.resolve(sessionId);
  }

  Future<void> _onFallDetectionToggle([bool? _]) async {
    final activated = await Navigator.of(context).push<bool>(
      MaterialPageRoute(builder: (_) => const EnableFallDetectionScreen()),
    );
    if (activated == true) await _load();
  }

  Future<void> _onImuSensorToggle(bool enable) async {
    if (enable) {
      if (!(_config?.fallDetectionEnabled ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Vui lòng bật phát hiện ngã trước')),
        );
        return;
      }
      await _load();
      if (!_foregroundCoordinator.isRunning && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Cần consent cảm biến còn hiệu lực và quyền cảm biến đã được xác minh.',
            ),
          ),
        );
      }
    } else {
      await _foregroundCoordinator.stop();
    }
    if (mounted) {
      setState(() => _imuSensorActive = _foregroundCoordinator.isRunning);
    }
  }

  @override
  Widget build(BuildContext context) {
    final fallDetectionEnabled = _config?.fallDetectionEnabled ?? false;
    return Scaffold(
      backgroundColor: _surface,
      body: SafeArea(
        child: _loading
            ? const Center(
                child: CircularProgressIndicator(color: _primaryContainer),
              )
            : RefreshIndicator(
                color: _primaryContainer,
                onRefresh: _load,
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      _buildTopBar(),
                      const SizedBox(height: 16),
                      _buildStatusCard(fallDetectionEnabled),
                      const SizedBox(height: 16),
                      _buildSensorSelfTestCard(),
                      if (safetyDiagnosticsEnabled) ...[
                        const SizedBox(height: 16),
                        _buildImuDiagnosticsCard(),
                      ],
                      const SizedBox(height: 24),
                      _buildEventHistory(),
                      const SizedBox(height: 16),
                      _buildPermissionStatus(),
                    ],
                  ),
                ),
              ),
      ),
    );
  }

  Widget _buildTopBar() {
    return SizedBox(
      height: 56,
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.of(context).pop(),
            icon: const Icon(Icons.arrow_back_rounded, color: _primary),
          ),
          const SizedBox(width: 4),
          const Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Giám sát an toàn',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: _primary,
                    letterSpacing: -0.3,
                  ),
                ),
                Text(
                  'Tự động phát hiện té ngã & hỗ trợ khẩn cấp',
                  style: TextStyle(
                    fontFamily: 'Lexend',
                    fontSize: 12,
                    color: _onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatusCard(bool fallDetectionEnabled) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(32),
        border: Border.all(color: _surfaceContainer),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: _primaryContainer.withValues(alpha: 0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.sensors, color: _primary),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Cảm biến IMU',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w600,
                        color: _onSurface,
                      ),
                    ),
                    Row(
                      children: [
                        Container(
                          width: 8,
                          height: 8,
                          decoration: const BoxDecoration(
                            color: _primary,
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 4),
                        Text(
                          _imuSensorActive ? 'Đang hoạt động' : 'Đã tắt',
                          style: const TextStyle(
                            fontSize: 14,
                            color: _primary,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              Switch(
                value: _imuSensorActive,
                activeThumbColor: Colors.white,
                activeTrackColor: _primaryContainer,
                onChanged: _onImuSensorToggle,
              ),
            ],
          ),
          const Divider(height: 24, color: _surfaceVariant),
          InkWell(
            onTap: () => _onFallDetectionToggle(),
            borderRadius: BorderRadius.circular(16),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 4),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Row(
                    children: const [
                      Icon(Icons.personal_injury_outlined, color: _tertiary),
                      SizedBox(width: 8),
                      Text(
                        'Phát hiện ngã (Fall Detection)',
                        style: TextStyle(fontSize: 14, color: _onSurface),
                      ),
                    ],
                  ),
                  Switch(
                    value: fallDetectionEnabled,
                    activeThumbColor: Colors.white,
                    activeTrackColor: _primaryContainer,
                    onChanged: _onFallDetectionToggle,
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSensorSelfTestCard() {
    final armed = _demoGestureArmTimer?.isActive ?? false;
    return Container(
      key: const Key('sensor-self-test-card'),
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: _surfaceContainerLowest,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _surfaceContainer),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Row(
            children: [
              Icon(Icons.health_and_safety_outlined, color: _primary),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Kiểm tra cảm biến IMU',
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          const Text(
            'Kiểm tra gia tốc kế và con quay hồi chuyển bằng một cử chỉ có chủ ý. '
            'Thao tác này không tạo cảnh báo ngã và không liên hệ người thân.',
            style: TextStyle(fontSize: 13, color: _onSurfaceVariant),
          ),
          const SizedBox(height: 14),
          FilledButton.icon(
            key: const Key('arm-sensor-self-test'),
            onPressed: armed ? null : _armDemoGesture,
            icon: Icon(armed ? Icons.sensors : Icons.sports_handball_outlined),
            label: Text(
              armed
                  ? 'ĐÃ SẴN SÀNG — HÃY VUNG MÁY'
                  : 'Bắt đầu kiểm tra cảm biến',
            ),
          ),
          if (armed) ...[
            const SizedBox(height: 8),
            const Text(
              'Giữ máy yên trong chốc lát, giơ cao rồi vung nhanh ít nhất khoảng '
              '50 cm từ trên xuống. Luôn giữ chắc, không thả máy.',
              key: Key('sensor-self-test-instruction'),
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildImuDiagnosticsCard() {
    final diagnostics = _imuDiagnostics;
    final state =
        diagnostics?.state ??
        (_foregroundCoordinator.isRunning
            ? ImuSamplingState.coordinatorRunning
            : ImuSamplingState.stopped);
    final age = diagnostics?.ageAt(DateTime.now().toUtc());
    String metric(double? value, String suffix) =>
        value == null ? '—' : '${value.toStringAsFixed(1)} $suffix';

    return Container(
      key: const Key('imu-diagnostics-card'),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainer,
        borderRadius: BorderRadius.circular(24),
        border: Border.all(color: _primaryContainer.withValues(alpha: 0.45)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Chẩn đoán IMU · $safetyDiagnosticsModeLabel',
            style: TextStyle(fontWeight: FontWeight.w800, color: _primary),
          ),
          if (safetyDemoMode) ...[
            const SizedBox(height: 6),
            const Text(
              'BẢN TRÌNH DIỄN · MÔ PHỎNG KHÔNG GỬI SOS',
              key: Key('safety-demo-mode-banner'),
              style: TextStyle(
                color: _onErrorContainer,
                fontSize: 12,
                fontWeight: FontWeight.w800,
              ),
            ),
          ],
          const SizedBox(height: 8),
          Text(state.label, key: const Key('imu-sampling-state')),
          const SizedBox(height: 6),
          Text(
            'Tần số: ${metric(diagnostics?.sampleRateHz, 'Hz')} · '
            'Tuổi mẫu: ${age == null ? '—' : '${age.inMilliseconds} ms'}',
          ),
          Text(
            'Gia tốc: ${metric(diagnostics?.accelerationMagnitude, 'm/s²')} · '
            'Gyro: ${metric(diagnostics?.gyroscopeMagnitude, 'rad/s')}',
          ),
          Text(
            'Pha: ${diagnostics?.detectorPhase.name ?? 'idle'} · '
            '${diagnostics?.detectorReason.label ?? 'Chưa có quyết định'}',
          ),
          if (diagnostics?.errorMessage case final message?)
            Text(message, style: const TextStyle(color: _onErrorContainer)),
        ],
      ),
    );
  }

  Widget _buildEventHistory() {
    final eventDisplays = _events.take(5).map(_toEventDisplay).toList();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'Lịch sử sự kiện',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w600,
                color: _onSurface,
              ),
            ),
            TextButton(
              onPressed: _showAllEventsHistorySheet,
              child: const Text(
                'Xem tất cả',
                style: TextStyle(color: _primary, fontWeight: FontWeight.w500),
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),
        if (eventDisplays.isEmpty)
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: _surfaceContainerLowest,
              borderRadius: BorderRadius.circular(28),
              boxShadow: const [
                BoxShadow(
                  color: Color(0x0F5A463F),
                  blurRadius: 20,
                  offset: Offset(0, 4),
                ),
              ],
            ),
            child: const Text(
              'Chưa có sự kiện an toàn nào được ghi nhận.',
              style: TextStyle(fontSize: 14, color: _onSurfaceVariant),
            ),
          )
        else
          ...eventDisplays.map(_buildEventCard),
      ],
    );
  }

  Widget _buildEventCard(_SafetyEventDisplay e) {
    return InkWell(
      onTap: e.event == null ? null : () => _showEventActions(e.event!),
      borderRadius: BorderRadius.circular(28),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: _surfaceContainerLowest,
          borderRadius: BorderRadius.circular(28),
          border: Border(left: BorderSide(color: e.color, width: 4)),
          boxShadow: const [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 20,
              offset: Offset(0, 4),
            ),
          ],
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.only(top: 2),
              child: Icon(e.icon, color: e.color, size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          e.title,
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                            color: _onSurface,
                          ),
                        ),
                      ),
                      Text(
                        e.time,
                        style: const TextStyle(
                          fontSize: 12,
                          color: _onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    e.description,
                    style: const TextStyle(
                      fontSize: 14,
                      color: _onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showAllEventsHistorySheet() async {
    List<SafetyEvent> allEvents = _events;
    try {
      final fetched = await _safetyService.getSafetyEvents(size: 50);
      if (fetched.isNotEmpty) {
        allEvents = fetched;
      }
    } catch (_) {
      // Fallback to currently loaded _events on error
    }

    if (!mounted) return;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: _surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (bottomSheetContext) {
        return DraggableScrollableSheet(
          initialChildSize: 0.75,
          minChildSize: 0.4,
          maxChildSize: 0.95,
          expand: false,
          builder: (context, scrollController) {
            final displays = allEvents.map(_toEventDisplay).toList();
            return Padding(
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 16),
              child: Column(
                children: [
                  Container(
                    width: 40,
                    height: 4,
                    decoration: BoxDecoration(
                      color: _onSurfaceVariant.withOpacity(0.3),
                      borderRadius: BorderRadius.circular(2),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Lịch sử sự kiện an toàn',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                          color: _onSurface,
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close),
                        onPressed: () => Navigator.of(bottomSheetContext).pop(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Expanded(
                    child: displays.isEmpty
                        ? const Center(
                            child: Text(
                              'Chưa có sự kiện an toàn nào được ghi nhận.',
                              style: TextStyle(
                                fontSize: 14,
                                color: _onSurfaceVariant,
                              ),
                            ),
                          )
                        : ListView.builder(
                            controller: scrollController,
                            itemCount: displays.length,
                            itemBuilder: (context, index) {
                              return _buildEventCard(displays[index]);
                            },
                          ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  _SafetyEventDisplay _toEventDisplay(SafetyEvent event) {
    if (event.eventType == 'SENSOR_SELF_TEST') {
      final description = switch ((event.status, event.responseType)) {
        ('TEST_OPEN', _) => 'Đang chờ người dùng thử phản hồi cảnh báo.',
        ('CONFIRMED_SAFE', _) => 'Đã thử thao tác “Tôi vẫn ổn”.',
        ('FALSE_POSITIVE', _) => 'Đã thử thao tác báo phát hiện nhầm.',
        ('TIMED_OUT', 'TEST_NEED_HELP') =>
          'Đã thử thao tác “Cần trợ giúp”; không gửi cảnh báo thật.',
        ('TIMED_OUT', 'TEST_TIMEOUT') =>
          'Đã thử tình huống không phản hồi; không gửi cảnh báo thật.',
        _ => 'Đã hoàn tất kiểm tra luồng cảnh báo bằng cảm biến IMU.',
      };
      return _SafetyEventDisplay(
        event: event,
        icon: Icons.science_outlined,
        color: _primary,
        title: 'Diễn tập cảnh báo ngã',
        description: description,
        time: _formatEventTime(event.detectedAt),
      );
    }
    final isOpen = event.status == 'OPEN';
    final isFalsePositive = event.status == 'FALSE_POSITIVE';
    final isSafe = event.status == 'CONFIRMED_SAFE';
    final time = _formatEventTime(event.detectedAt);
    if (isFalsePositive) {
      return _SafetyEventDisplay(
        event: event,
        icon: Icons.info_outline,
        color: _surfaceVariant,
        title: 'Đã báo cáo cảnh báo sai',
        description: event.notes?.isNotEmpty == true
            ? event.notes!
            : 'Sự kiện đã được đánh dấu là phát hiện nhầm.',
        time: time,
      );
    }
    if (isSafe) {
      return _SafetyEventDisplay(
        event: event,
        icon: Icons.check_circle_outline,
        color: _primaryContainer,
        title: 'Đã xác nhận an toàn',
        description: event.notes?.isNotEmpty == true
            ? event.notes!
            : 'Người dùng đã xác nhận không cần hỗ trợ khẩn cấp.',
        time: time,
      );
    }
    return _SafetyEventDisplay(
      event: event,
      icon: isOpen ? Icons.warning_amber_outlined : Icons.emergency_outlined,
      color: isOpen ? const Color(0xFFE8A87C) : _onErrorContainer,
      title: event.eventType == 'SUSPECTED_IMPACT'
          ? 'Phát hiện va chạm nghi ngờ'
          : 'Phát hiện chuyển động bất thường',
      description:
          'Hệ thống ghi nhận gia tốc ${event.magnitude.toStringAsFixed(1)} m/s². Vui lòng kiểm tra tình trạng an toàn.',
      time: time,
    );
  }

  String _formatEventTime(DateTime? value) {
    if (value == null) return '--:--';
    final local = value.toLocal();
    final hour = local.hour.toString().padLeft(2, '0');
    final minute = local.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  Future<void> _showEventActions(SafetyEvent event) async {
    if (event.eventType == 'SENSOR_SELF_TEST') {
      if (event.status == 'TEST_OPEN') await _showCountdown(event);
      return;
    }
    if (event.status != 'OPEN') return;
    await showModalBottomSheet<void>(
      context: context,
      backgroundColor: _surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(32)),
      ),
      builder: (context) {
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 20, 24, 24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Xử lý cảnh báo an toàn',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w700,
                    color: _onSurface,
                  ),
                ),
                const SizedBox(height: 16),
                _EventActionButton(
                  icon: Icons.check_circle_outline,
                  label: 'Tôi vẫn an toàn',
                  onTap: () => _handleEventAction(
                    event,
                    () => _confirmEventSafe(
                      event,
                      note: 'Người dùng xác nhận an toàn từ ứng dụng.',
                    ),
                  ),
                ),
                _EventActionButton(
                  icon: Icons.report_gmailerrorred_outlined,
                  label: 'Báo cáo phát hiện sai',
                  onTap: () => _handleEventAction(
                    event,
                    () => _reportEventFalsePositive(
                      event,
                      note: 'Người dùng báo cáo phát hiện sai.',
                    ),
                  ),
                ),
                _EventActionButton(
                  icon: Icons.emergency_outlined,
                  label: 'Gửi cảnh báo khẩn cấp',
                  color: _onErrorContainer,
                  onTap: () => _handleEventAction(
                    event,
                    () => _safetyService.sendEmergencyAlertForEvent(event.id),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Future<void> _handleEventAction(
    SafetyEvent event,
    Future<void> Function() action,
  ) async {
    Navigator.of(context).pop();
    try {
      await action();
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể cập nhật sự kiện: $e')),
        );
      }
    }
  }

  Widget _buildPermissionStatus() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _surfaceContainer,
        borderRadius: BorderRadius.circular(28),
        boxShadow: const [
          BoxShadow(
            color: Color(0x0F5A463F),
            blurRadius: 20,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          const Icon(Icons.verified_user_outlined, color: _tertiary),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Quyền truy cập thiết bị',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: _onSurface,
                  ),
                ),
                Text(
                  (_config?.sensorPermissionGranted ?? false) &&
                          _imuSensorActive
                      ? 'Cảm biến đã được xác minh và đang hoạt động'
                      : 'Cảm biến chưa được xác minh hoặc chưa hoạt động',
                  style: const TextStyle(fontSize: 12, color: _tertiary),
                ),
              ],
            ),
          ),
          Icon(
            (_config?.sensorPermissionGranted ?? false) && _imuSensorActive
                ? Icons.check_circle
                : Icons.warning_amber,
            color:
                (_config?.sensorPermissionGranted ?? false) && _imuSensorActive
                ? _primary
                : _onErrorContainer,
          ),
        ],
      ),
    );
  }
}

class _EmergencyEscalationRow extends StatelessWidget {
  const _EmergencyEscalationRow({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: const Color(0xFF93000A)),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            text,
            style: const TextStyle(fontWeight: FontWeight.w600),
          ),
        ),
      ],
    );
  }
}

class _SafetyEventDisplay {
  final SafetyEvent? event;
  final IconData icon;
  final Color color;
  final String title;
  final String description;
  final String time;

  const _SafetyEventDisplay({
    this.event,
    required this.icon,
    required this.color,
    required this.title,
    required this.description,
    required this.time,
  });
}

class _EventActionButton extends StatelessWidget {
  const _EventActionButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.color = const Color(0xFF845143),
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: OutlinedButton.icon(
        onPressed: onTap,
        icon: Icon(icon),
        label: Text(label),
        style: OutlinedButton.styleFrom(
          foregroundColor: color,
          minimumSize: const Size.fromHeight(52),
          shape: const StadiumBorder(),
        ),
      ),
    );
  }
}
