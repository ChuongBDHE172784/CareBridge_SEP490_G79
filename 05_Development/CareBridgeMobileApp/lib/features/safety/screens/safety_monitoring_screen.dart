import 'dart:async';

import 'package:flutter/material.dart';
import '../models/imu_diagnostics_model.dart';
import '../models/safety_config_model.dart';
import '../services/imu_fall_detector.dart';
import '../services/safety_demo_mode.dart';
import '../services/safety_foreground_service.dart';
import '../services/safety_service.dart';
import '../widgets/disable_fall_detection_sheet.dart';
import '../widgets/safety_countdown_sheet.dart';
import 'enable_fall_detection_screen.dart';
import '../../emergency/screens/emergency_contacts_screen.dart';
import '../../emergency/services/emergency_service.dart';
import '../../emergency/models/emergency_session_model.dart';

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
}) {
  for (final event in events) {
    if (event.status == 'OPEN' && event.id != excludingId) return event;
  }
  return null;
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
  static const _surfaceContainerHighest = Color(0xFFFADCD3);
  static const _surfaceContainer = Color(0xFFFFE9E3);
  static const _surfaceVariant = Color(0xFFFADCD3);
  static const _onSurface = Color(0xFF271812);
  static const _onSurfaceVariant = Color(0xFF524440);
  static const _errorContainer = Color(0xFFFFDAD6);
  static const _onErrorContainer = Color(0xFF93000A);
  static const _secondaryContainer = Color(0xFFF6DACF);
  static const _onSecondaryContainer = Color(0xFF735E56);
  static const _tertiary = Color(0xFF625D59);

  final _safetyService = SafetyService();
  final _emergencyService = EmergencyService();
  final _foregroundCoordinator = SafetyForegroundServiceCoordinator.instance;
  SafetyConfig? _config;
  List<SafetyEvent> _events = const [];
  EmergencySession? _activeEmergency;
  StreamSubscription<SafetyEvent>? _detectedEventSubscription;
  StreamSubscription<ImuDiagnosticsSnapshot>? _diagnosticsSubscription;
  ImuDiagnosticsSnapshot? _imuDiagnostics;
  Timer? _demoRecoveryTimer;
  Timer? _demoGestureArmTimer;
  int? _demoGestureGeneration;
  int _lastDemoGestureSequence = 0;
  final SafetyRealEventQueue _pendingRealEvents = SafetyRealEventQueue();
  String? _countdownEventId;
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
    _diagnosticsSubscription?.cancel();
    _demoRecoveryTimer?.cancel();
    _demoGestureArmTimer?.cancel();
    super.dispose();
  }

  void _onDiagnosticsSnapshot(ImuDiagnosticsSnapshot snapshot) {
    if (!mounted) return;
    setState(() => _imuDiagnostics = snapshot);
    _handleDemoGesture(snapshot);
    if (!safetyDemoMode || snapshot.state != ImuSamplingState.stopped) return;
    if (_demoRecoveryTimer?.isActive ?? false) return;
    _demoRecoveryTimer = Timer(const Duration(seconds: 1), () {
      if (mounted) unawaited(_foregroundCoordinator.reconcile());
    });
  }

  void _handleDemoGesture(ImuDiagnosticsSnapshot snapshot) {
    if (!safetyDemoMode) return;
    if (_demoGestureGeneration != snapshot.generation) {
      _demoGestureGeneration = snapshot.generation;
      _lastDemoGestureSequence = snapshot.demoGestureSequence;
      return;
    }
    final gestureDetected =
        snapshot.demoGestureSequence > _lastDemoGestureSequence;
    _lastDemoGestureSequence = snapshot.demoGestureSequence;
    if (!gestureDetected || !(_demoGestureArmTimer?.isActive ?? false)) {
      return;
    }
    _demoGestureArmTimer?.cancel();
    setState(() {});
    unawaited(_runSafeSimulation());
  }

  void _armDemoGesture() {
    final diagnostics = _imuDiagnostics;
    final eligible = isSafeFallSimulationEligible(
      config: _config,
      coordinatorRunning: _foregroundCoordinator.isRunning,
      diagnostics: diagnostics,
      now: DateTime.now().toUtc(),
    );
    if (!safetyDemoMode || !eligible || _countdownEventId != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('IMU phải đang nhận mẫu trước khi bật cử chỉ demo.'),
        ),
      );
      return;
    }

    _demoGestureGeneration = diagnostics?.generation;
    _lastDemoGestureSequence = diagnostics?.demoGestureSequence ?? 0;
    _demoGestureArmTimer?.cancel();
    _demoGestureArmTimer = Timer(const Duration(seconds: 8), () {
      if (!mounted) return;
      setState(() {});
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã hết 8 giây chờ cử chỉ demo.')),
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
      final activeEmergency = await _emergencyService.getActive();
      await _foregroundCoordinator.reconcile();
      if (mounted) {
        setState(() {
          _config = config;
          _events = events;
          _imuSensorActive = _foregroundCoordinator.isRunning;
          _activeEmergency = activeEmergency;
        });
        SafetyEvent? pending;
        for (final event in events) {
          if (event.status == 'OPEN') {
            pending = event;
            break;
          }
        }
        if (pending != null) unawaited(_showCountdown(pending));
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
    _pendingRealEvents.enqueue(event);
    setState(() {
      _events = [event, ..._events.where((item) => item.id != event.id)];
    });
    unawaited(_showCountdown(event));
  }

  Future<void> _showCountdown(
    SafetyEvent event, {
    bool simulated = false,
  }) async {
    final deadline = event.countdownDeadlineAt;
    if (!mounted ||
        event.status != 'OPEN' ||
        deadline == null ||
        _countdownEventId != null) {
      return;
    }
    _countdownEventId = event.id;
    if (!simulated) _pendingRealEvents.remove(event.id);
    final result = await showModalBottomSheet<SafetyCountdownResult>(
      context: context,
      isDismissible: false,
      enableDrag: false,
      isScrollControlled: true,
      builder: (_) => SafetyCountdownSheet(event: event, simulated: simulated),
    );
    try {
      await dispatchSafetyCountdownResult(
        result: result,
        simulated: simulated,
        onSafe: () => _confirmEventSafe(
          event,
          note: 'Người dùng xác nhận an toàn trong thời gian đếm ngược.',
        ),
        onFalsePositive: (reasonCode, reason) =>
            _reportEventFalsePositive(event, note: '$reasonCode: $reason'),
        onEmergency: () => _safetyService.sendEmergencyAlertForEvent(event.id),
      );
      if (simulated && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã kết thúc mô phỏng cục bộ. Không có cảnh báo hay dữ liệu nào được gửi.',
            ),
          ),
        );
      }
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Không thể ghi nhận phản hồi an toàn: $error'),
          ),
        );
      }
    } finally {
      if (!simulated && mounted) await _load();
      _countdownEventId = null;
      if (mounted) {
        final next =
            _pendingRealEvents.takeNext(excludingId: event.id) ??
            selectNextOpenSafetyEvent(_events, excludingId: event.id);
        if (next != null) unawaited(_showCountdown(next));
      }
    }
  }

  Future<void> _runSafeSimulation() async {
    if (!safetyDiagnosticsEnabled) return;
    final eligible = isSafeFallSimulationEligible(
      config: _config,
      coordinatorRunning: _foregroundCoordinator.isRunning,
      diagnostics: _imuDiagnostics,
      now: DateTime.now().toUtc(),
    );
    if (!eligible || _countdownEventId != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Hãy bật phát hiện ngã, quyền cảm biến và dịch vụ IMU trước khi mô phỏng.',
          ),
        ),
      );
      return;
    }

    final detector = ImuFallDetector();
    FallCandidate? candidate;
    final startedAt = DateTime.now().toUtc();
    for (final sample in ImuFallDetector.canonicalSimulationSamples(
      startedAt,
    )) {
      candidate = detector.addSample(sample) ?? candidate;
    }
    if (candidate == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Mô phỏng bị bộ phát hiện từ chối: '
              '${detector.latestDecision.reason.label}',
            ),
          ),
        );
      }
      return;
    }

    final event = SafetyEvent(
      id: 'local-simulation-${startedAt.microsecondsSinceEpoch}',
      eventType: 'SUSPECTED_FALL',
      magnitude: candidate.impactSample.accelerationMagnitude,
      status: 'OPEN',
      detectedAt: startedAt,
      countdownDeadlineAt: DateTime.now().toUtc().add(
        Duration(seconds: (_config?.countdownSeconds ?? 30).clamp(1, 300)),
      ),
      notes: 'Local debug simulation; never persisted or transmitted.',
    );
    await _showCountdown(event, simulated: true);
  }

  Future<void> _confirmEventSafe(SafetyEvent event, {String? note}) async {
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
    if (_activeEmergency?.sessionId == sessionId && mounted) {
      setState(() => _activeEmergency = null);
    }
  }

  Future<void> _resolveActiveEmergency() async {
    final active = _activeEmergency;
    if (active == null) return;
    try {
      await _emergencyService.resolve(active.sessionId);
      if (mounted) setState(() => _activeEmergency = null);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể kết thúc phiên khẩn cấp: $error')),
        );
      }
    }
  }

  Future<void> _onFallDetectionToggle(bool enable) async {
    if (enable) {
      final activated = await Navigator.of(context).push<bool>(
        MaterialPageRoute(builder: (_) => const EnableFallDetectionScreen()),
      );
      if (activated == true) await _load();
    } else {
      final disabled = await showDisableFallDetectionSheet(context);
      if (disabled == true) {
        await _foregroundCoordinator.stop();
        await _load();
      }
    }
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

  Future<void> _triggerManualEmergency() async {
    try {
      await _emergencyService.openFlow(triggerSource: 'MANUAL');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Đã kích hoạt hỗ trợ khẩn cấp và gửi cảnh báo người thân',
            ),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Không thể kích hoạt hỗ trợ khẩn cấp: $e')),
        );
      }
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
                      const Text(
                        'Giám sát an toàn',
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w600,
                          color: _onSurface,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Theo dõi hoạt động và phát hiện sự cố theo thời gian thực.',
                        style: TextStyle(
                          fontSize: 14,
                          color: _onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 24),
                      _buildStatusCard(fallDetectionEnabled),
                      if (safetyDiagnosticsEnabled) ...[
                        const SizedBox(height: 16),
                        _buildImuDiagnosticsCard(),
                      ],
                      if (_activeEmergency != null) ...[
                        const SizedBox(height: 16),
                        _buildActiveEmergencyCard(),
                      ],
                      const SizedBox(height: 24),
                      _buildSosButtons(),
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
          SizedBox(
            width: 40,
            height: 40,
            child: IconButton(
              padding: EdgeInsets.zero,
              icon: const Icon(Icons.arrow_back, color: _primary, size: 20),
              onPressed: () => Navigator.of(context).pop(),
            ),
          ),
          const SizedBox(width: 8),
          Container(
            width: 40,
            height: 40,
            decoration: const BoxDecoration(
              color: _surfaceContainerHighest,
              shape: BoxShape.circle,
            ),
            child: const Center(
              child: Text(
                'N',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  color: _primary,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              'CareBridge',
              style: TextStyle(
                fontSize: 32,
                fontWeight: FontWeight.w700,
                color: _primary,
                letterSpacing: -0.5,
              ),
            ),
          ),
          IconButton(
            icon: const Icon(Icons.notifications_outlined, color: _primary),
            onPressed: () {},
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.personal_injury_outlined, color: _tertiary),
                  const SizedBox(width: 8),
                  const Text(
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
          const SizedBox(height: 12),
          if (safetyDemoMode) ...[
            FilledButton.icon(
              key: const Key('arm-safety-demo-gesture'),
              onPressed: _armDemoGesture,
              icon: Icon(
                _demoGestureArmTimer?.isActive ?? false
                    ? Icons.sensors
                    : Icons.sports_handball_outlined,
              ),
              label: Text(
                _demoGestureArmTimer?.isActive ?? false
                    ? 'ĐÃ SẴN SÀNG — HÃY VUNG MÁY'
                    : 'Sẵn sàng vung điện thoại',
              ),
            ),
            if (_demoGestureArmTimer?.isActive ?? false) ...[
              const SizedBox(height: 6),
              const Text(
                'Giơ máy cao, vung mạnh từ trên xuống rồi giữ chắc. Không thả máy.',
                key: Key('safety-demo-gesture-instruction'),
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700),
              ),
            ],
            const SizedBox(height: 8),
          ],
          OutlinedButton.icon(
            key: const Key('run-safe-fall-simulation'),
            onPressed: _runSafeSimulation,
            icon: const Icon(Icons.science_outlined),
            label: const Text('Mô phỏng ngã an toàn'),
          ),
        ],
      ),
    );
  }

  Widget _buildActiveEmergencyCard() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: _errorContainer,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Row(
        children: [
          const Icon(Icons.emergency, color: _onErrorContainer),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Phiên hỗ trợ khẩn cấp đang hoạt động',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Text('Chỉ kết thúc khi bạn xác nhận hiện đã an toàn.'),
              ],
            ),
          ),
          TextButton(
            onPressed: _resolveActiveEmergency,
            child: const Text('Tôi an toàn'),
          ),
        ],
      ),
    );
  }

  Widget _buildSosButtons() {
    return Row(
      children: [
        Expanded(
          child: _BentoButton(
            icon: Icons.emergency,
            label: 'Gọi khẩn cấp SOS',
            background: _errorContainer,
            foreground: _onErrorContainer,
            onTap: _triggerManualEmergency,
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _BentoButton(
            icon: Icons.contacts,
            label: 'Liên hệ người thân',
            background: _secondaryContainer,
            foreground: _onSecondaryContainer,
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => const EmergencyContactsScreen(),
              ),
            ),
          ),
        ),
      ],
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
              onPressed: _load,
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
          ...eventDisplays.map(
            (e) => InkWell(
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
            ),
          ),
      ],
    );
  }

  _SafetyEventDisplay _toEventDisplay(SafetyEvent event) {
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

class _BentoButton extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color background;
  final Color foreground;
  final VoidCallback onTap;

  const _BentoButton({
    required this.icon,
    required this.label,
    required this.background,
    required this.foreground,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(28),
      child: Container(
        height: 120,
        decoration: BoxDecoration(
          color: background,
          borderRadius: BorderRadius.circular(28),
          boxShadow: const [
            BoxShadow(
              color: Color(0x0F5A463F),
              blurRadius: 20,
              offset: Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: foreground, size: 32),
            const SizedBox(height: 8),
            Text(
              label,
              textAlign: TextAlign.center,
              style: TextStyle(color: foreground, fontWeight: FontWeight.w600),
            ),
          ],
        ),
      ),
    );
  }
}
