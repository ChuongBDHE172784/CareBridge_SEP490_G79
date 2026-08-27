import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:geolocator/geolocator.dart';

import '../models/safety_config_model.dart';
import '../models/imu_diagnostics_model.dart';
import '../../emergency/services/emergency_service.dart';
import 'imu_fall_detector.dart';
import 'safety_demo_mode.dart';
import 'safety_service.dart';
import 'safety_permission_service.dart';
import '../../privacy/services/privacy_service.dart';

typedef AccelerometerEvents = Stream<AccelerometerEvent> Function();
typedef GyroscopeEvents = Stream<GyroscopeEvent> Function();
typedef CloseStaleSafetyEvent = Future<void> Function(SafetyEvent event);

/// Dịch vụ quản lý luồng cảm biến phần cứng (Gia tốc kế, Con quay hồi chuyển) & đồng bộ định vị GPS khi phát hiện té ngã.
///
/// **Nhiệm vụ chính:**
/// 1. Lắng nghe cảm biến gia tốc và con quay hồi chuyển với chu kỳ ~20ms (50Hz - Game Interval).
/// 2. Đưa các mẫu IMU vào [ImuFallDetector] để phân tích theo máy trạng thái 3 pha.
/// 3. Định kỳ cập nhật tọa độ vị trí GPS (khi có sự đồng thuận Consent & quyền từ người dùng).
/// 4. Gửi sự kiện té ngã đã được xác nhận [FallCandidate] lên Backend (`/api/v1/safety/imu-data`) kèm tọa độ định vị.
/// 5. Quản lý trạng thái chẩn đoán IMU (Diagnostics) và bảo vệ bằng bộ đếm Watchdog.
class FallDetectionSensorService {
  FallDetectionSensorService._({
    AccelerometerEvents? accelerometerEvents,
    GyroscopeEvents? gyroscopeEvents,
    DateTime Function()? now,
    SafetyService? safetyService,
    Future<void> Function()? refreshLocation,
    CloseStaleSafetyEvent? closeStaleEvent,
  }) : _accelerometerEvents =
           accelerometerEvents ??
           (() => accelerometerEventStream(samplingPeriod: _samplingPeriod)),
       _gyroscopeEvents =
           gyroscopeEvents ??
           (() => gyroscopeEventStream(samplingPeriod: _samplingPeriod)),
       _now = now ?? DateTime.now,
       _safetyService = safetyService ?? SafetyService(),
       _refreshLocationOverride = refreshLocation,
       _closeStaleEventOverride = closeStaleEvent;

  @visibleForTesting
  factory FallDetectionSensorService.forTesting({
    required AccelerometerEvents accelerometerEvents,
    required GyroscopeEvents gyroscopeEvents,
    required DateTime Function() now,
    SafetyService? safetyService,
    Future<void> Function()? refreshLocation,
    CloseStaleSafetyEvent? closeStaleEvent,
  }) => FallDetectionSensorService._(
    accelerometerEvents: accelerometerEvents,
    gyroscopeEvents: gyroscopeEvents,
    now: now,
    safetyService: safetyService,
    refreshLocation: refreshLocation,
    closeStaleEvent: closeStaleEvent,
  );

  /// Singleton instance của dịch vụ cảm biến phát hiện ngã.
  static final FallDetectionSensorService instance =
      FallDetectionSensorService._();

  /// Chu kỳ lấy mẫu cảm biến phần cứng (SensorInterval.gameInterval ~20ms, tương đương tần số 50Hz).
  static const _samplingPeriod = SensorInterval.gameInterval;

  final AccelerometerEvents _accelerometerEvents;
  final GyroscopeEvents _gyroscopeEvents;
  final DateTime Function() _now;
  final SafetyService _safetyService;
  final Future<void> Function()? _refreshLocationOverride;
  final CloseStaleSafetyEvent? _closeStaleEventOverride;

  final SafetyPermissionService _permissionService = SafetyPermissionService();
  final StreamController<SafetyEvent> _eventController =
      StreamController<SafetyEvent>.broadcast();
  final StreamController<ImuDiagnosticsSnapshot> _diagnosticsController =
      StreamController<ImuDiagnosticsSnapshot>.broadcast();
  final StreamController<SensorSelfTestResult> _sensorSelfTestController =
      StreamController<SensorSelfTestResult>.broadcast();
  final Set<String> _publishedEventIds = <String>{};

  StreamSubscription<AccelerometerEvent>? _accelerometerSubscription;
  StreamSubscription<GyroscopeEvent>? _gyroscopeSubscription;
  final ImuFallDetector _detector = ImuFallDetector();
  final SafetySensorSelfTestDetector _sensorSelfTestDetector =
      SafetySensorSelfTestDetector();
  GyroscopeEvent? _latestGyroscope;
  bool _running = false;
  int _runGeneration = 0;
  int _responseGeneration = 0;
  bool _alertResponseInProgress = false;
  DateTime? _alertResponseStartedAt;
  FallCandidate? _pendingCandidateAfterResponse;
  bool _locationSharingAllowed = false;
  Position? _latestPosition;
  DateTime? _locationReadAt;
  Future<void>? _locationRefreshInFlight;
  Timer? _diagnosticsLifecycleTimer;
  DateTime? _lastAccelerometerAt;
  DateTime? _lastSamplingDiagnosticsPublishedAt;
  double? _sampleRateHz;
  ImuDiagnosticsSnapshot? _latestDiagnostics;
  String? _accelerometerStreamError;
  String? _gyroscopeStreamError;

  /// Giới hạn tần suất phát chẩn đoán (tối đa 4 lần/giây = 250ms/lần) để tránh nghẽn Event Bus.
  static const _diagnosticsThrottle = Duration(milliseconds: 250);

  /// Dữ liệu cảm biến bị coi là quá hạn (stale) nếu trễ hơn 2 giây so với đồng hồ hệ thống.
  static const _staleAfter = Duration(seconds: 2);

  /// Bộ đếm thời gian giám sát tối đa (3 phút): nếu giao diện cảnh báo bị lỗi/tắt đột ngột mà không rearm,
  /// dịch vụ sẽ tự động kích hoạt lại bộ phát hiện té ngã để đảm bảo an toàn cho người dùng.
  static const alertResponseWatchdog = Duration(minutes: 3);

  bool get isRunning => _running;
  Stream<SafetyEvent> get detectedEvents => _eventController.stream;
  Stream<SensorSelfTestResult> get sensorSelfTestResults =>
      _sensorSelfTestController.stream;
  Stream<ImuDiagnosticsSnapshot> get diagnostics => safetyDiagnosticsEnabled
      ? _diagnosticsController.stream
      : const Stream.empty();

  @visibleForTesting
  bool get diagnosticsLifecycleActiveForTesting =>
      _diagnosticsLifecycleTimer?.isActive ?? false;

  @visibleForTesting
  void checkDiagnosticsFreshnessForTesting() => _checkDiagnosticsFreshness();

  /// Khởi động luồng giám sát cảm biến IMU và cơ chế làm mới định vị GPS (nếu được cấp phép).
  Future<void> start({bool locationSharingAllowed = false}) async {
    _locationSharingAllowed = locationSharingAllowed;
    if (!locationSharingAllowed) {
      _latestPosition = null;
      _locationReadAt = null;
    }
    if (_running) {
      if (_locationSharingAllowed) _scheduleLocationRefresh();
      return;
    }
    _running = true;
    // Epoch-based generations duy trì tính đơn điệu ngay cả khi iOS tái tạo Background Dart Isolate
    final wallClockGeneration = _now().toUtc().microsecondsSinceEpoch;
    _runGeneration = wallClockGeneration > _runGeneration
        ? wallClockGeneration
        : _runGeneration + 1;
    _accelerometerStreamError = null;
    _gyroscopeStreamError = null;
    _sensorSelfTestDetector.reset();
    if (safetyDiagnosticsEnabled) {
      _lastAccelerometerAt = null;
      _sampleRateHz = null;
      _emitDiagnostics(
        ImuDiagnosticsSnapshot.awaiting(
          generation: _runGeneration,
          capturedAt: _now().toUtc(),
        ),
        force: true,
      );
      _diagnosticsLifecycleTimer?.cancel();
      _diagnosticsLifecycleTimer = Timer.periodic(
        const Duration(seconds: 1),
        (_) => _checkDiagnosticsFreshness(),
      );
    }
    if (_locationSharingAllowed) _scheduleLocationRefresh();
    try {
      // Đăng ký nhận stream con quay hồi chuyển
      _gyroscopeSubscription = _gyroscopeEvents().listen(
        _handleGyroscope,
        onError: (error, stackTrace) {
          debugPrint('[FallDetectionSensorService] gyroscope error: $error');
          _gyroscopeStreamError = 'Con quay hồi chuyển: $error';
          _publishCurrentSensorError();
        },
        onDone: () {
          _gyroscopeStreamError = 'Luồng con quay hồi chuyển đã dừng.';
          _publishCurrentSensorError();
        },
        cancelOnError: false,
      );
      // Đăng ký nhận stream gia tốc kế
      _accelerometerSubscription = _accelerometerEvents().listen(
        _handleAccelerometer,
        onError: (error, stackTrace) {
          debugPrint(
            '[FallDetectionSensorService] accelerometer error: $error',
          );
          _accelerometerStreamError = 'Gia tốc kế: $error';
          _publishCurrentSensorError();
        },
        onDone: () {
          _accelerometerStreamError = 'Luồng gia tốc kế đã dừng.';
          _publishCurrentSensorError();
        },
        cancelOnError: false,
      );
    } catch (error) {
      await _cancelSensorSubscriptions();
      _diagnosticsLifecycleTimer?.cancel();
      _diagnosticsLifecycleTimer = null;
      _running = false;
      _publishSensorError('Không thể khởi tạo luồng cảm biến: $error');
      rethrow;
    }
  }

  /// Dừng giám sát cảm biến IMU và giải phóng các đăng ký cảm biến/timer.
  Future<void> stop() async {
    if (!_running &&
        _diagnosticsLifecycleTimer == null &&
        _accelerometerSubscription == null &&
        _gyroscopeSubscription == null) {
      return;
    }
    _running = false;
    _runGeneration++;
    _diagnosticsLifecycleTimer?.cancel();
    _diagnosticsLifecycleTimer = null;
    await _cancelSensorSubscriptions();
    _latestGyroscope = null;
    _detector.reset();
    _responseGeneration++;
    _alertResponseInProgress = false;
    _alertResponseStartedAt = null;
    _pendingCandidateAfterResponse = null;
    _publishedEventIds.clear();
    _locationSharingAllowed = false;
    _latestPosition = null;
    _locationReadAt = null;
    _locationRefreshInFlight = null;
    _lastAccelerometerAt = null;
    _lastSamplingDiagnosticsPublishedAt = null;
    _sampleRateHz = null;
    _accelerometerStreamError = null;
    _gyroscopeStreamError = null;
    if (safetyDiagnosticsEnabled) {
      _emitDiagnostics(
        ImuDiagnosticsSnapshot.stopped(
          generation: _runGeneration,
          capturedAt: _now().toUtc(),
        ),
        force: true,
      );
    }
  }

  /// Xử lý mẫu gia tốc kế: kết hợp với mẫu con quay hồi chuyển gần nhất thành [ImuSample] và đưa vào bộ phân tích.
  void _handleAccelerometer(AccelerometerEvent event) {
    if (!_running) return;
    _accelerometerStreamError = null;

    final timestamp = event.timestamp.toUtc();
    // Bỏ qua và reset nếu dữ liệu bị trễ hơn 2 giây
    if (_now().toUtc().difference(timestamp) > _staleAfter) {
      _detector.reset();
      return;
    }
    _expireStuckAlertResponse(timestamp);
    final gyro = _latestGyroscope;
    final sample = ImuSample(
      accelerometerX: event.x,
      accelerometerY: event.y,
      accelerometerZ: event.z,
      gyroscopeX: gyro?.x ?? 0,
      gyroscopeY: gyro?.y ?? 0,
      gyroscopeZ: gyro?.z ?? 0,
      timestamp: timestamp,
      gyroscopeTimestamp: gyro?.timestamp.toUtc(),
    );
    final candidate = _detector.addSample(sample);
    final sensorSelfTestDetected = _sensorSelfTestDetector.addSample(sample);
    if (sensorSelfTestDetected) {
      _sensorSelfTestController.add(
        SensorSelfTestResult(
          sequence: _sensorSelfTestDetector.sequence,
          detectedAt: sample.timestamp,
          accelerationMagnitude: sample.accelerationMagnitude,
          gyroscopeMagnitude: sample.gyroscopeMagnitude,
        ),
      );
    }
    if (safetyDiagnosticsEnabled) {
      _publishSamplingDiagnostics(sample, force: sensorSelfTestDetected);
    }
    // Định kỳ 30 giây làm mới tọa độ GPS nếu được phép chia sẻ vị trí
    if (_locationSharingAllowed &&
        (_locationReadAt == null ||
            timestamp.difference(_locationReadAt!) >
                const Duration(seconds: 30))) {
      _scheduleLocationRefresh();
    }
    if (candidate == null) return;
    // Nếu màn hình cảnh báo đang mở thì giữ lại ứng viên ngã chờ phản hồi
    if (_alertResponseInProgress) {
      _pendingCandidateAfterResponse = candidate;
      return;
    }
    // Gửi sự kiện ngã lên Backend
    unawaited(_sendCandidate(candidate, _runGeneration, _responseGeneration));
  }

  void _expireStuckAlertResponse(DateTime sampleAt) {
    final startedAt = _alertResponseStartedAt;
    if (!_alertResponseInProgress || startedAt == null) return;
    if (sampleAt.difference(startedAt) <= alertResponseWatchdog) return;
    debugPrint(
      '[FallDetectionSensorService] alert response never rearmed; '
      'resuming fall detection.',
    );
    _alertResponseInProgress = false;
    _alertResponseStartedAt = null;
    _pendingCandidateAfterResponse = null;
    _detector.reset();
  }

  /// Cập nhật giá trị mẫu con quay hồi chuyển mới nhất.
  void _handleGyroscope(GyroscopeEvent event) {
    if (!_running) return;
    _gyroscopeStreamError = null;
    _latestGyroscope = event;
  }

  /// Bắt đầu phiên hiển thị cảnh báo (tạm dừng thu nạp cảnh báo mới để tránh pop-up chồng chéo).
  void beginAlertResponse() {
    if (!_running) return;
    _responseGeneration++;
    _alertResponseInProgress = true;
    _alertResponseStartedAt = _now().toUtc();
    _pendingCandidateAfterResponse = null;
    _detector.reset();
  }

  /// Tái kích hoạt bộ phát hiện té ngã sau khi người dùng phản hồi cảnh báo (Tôi vẫn ổn / Báo nhầm).
  void rearmAfterAlertResponse(DateTime respondedAt) {
    if (!_running) return;
    final pendingCandidate = _pendingCandidateAfterResponse;
    _pendingCandidateAfterResponse = null;
    _alertResponseInProgress = false;
    _alertResponseStartedAt = null;
    _detector.rearmAfterAlertResponse(respondedAt);
    if (pendingCandidate != null &&
        pendingCandidate.impactSample.timestamp.isAfter(respondedAt.toUtc())) {
      unawaited(
        _sendCandidate(pendingCandidate, _runGeneration, _responseGeneration),
      );
    }
  }

  void _publishSamplingDiagnostics(ImuSample sample, {bool force = false}) {
    final previousAt = _lastAccelerometerAt;
    if (previousAt != null && sample.timestamp.isAfter(previousAt)) {
      final micros = sample.timestamp.difference(previousAt).inMicroseconds;
      if (micros > 0) {
        final instantaneous = Duration.microsecondsPerSecond / micros;
        _sampleRateHz = _sampleRateHz == null
            ? instantaneous
            : (_sampleRateHz! * 0.8) + (instantaneous * 0.2);
      }
    } else if (previousAt != null) {
      _sampleRateHz = null;
      _lastSamplingDiagnosticsPublishedAt = null;
    }
    _lastAccelerometerAt = sample.timestamp;
    final streamError = _currentSensorError;
    if (streamError != null) {
      _publishSensorError(streamError);
      return;
    }
    final gyroscopeAt = sample.gyroscopeTimestamp;
    if (gyroscopeAt == null) {
      _emitDiagnostics(
        ImuDiagnosticsSnapshot(
          generation: _runGeneration,
          state: ImuSamplingState.awaitingSamples,
          capturedAt: sample.timestamp,
          sampleRateHz: _sampleRateHz,
          accelerationMagnitude: sample.accelerationMagnitude,
          detectorPhase: _detector.latestDecision.phase,
          detectorReason: ImuDetectorDecisionReason.gyroscopeMissing,
          demoGestureSequence: _sensorSelfTestDetector.sequence,
        ),
        force: force,
      );
      return;
    }
    if (sample.timestamp.difference(gyroscopeAt).abs() >
        ImuFallDetector.maximumGyroscopeAge) {
      _emitDiagnostics(
        ImuDiagnosticsSnapshot(
          generation: _runGeneration,
          state: ImuSamplingState.stale,
          capturedAt: sample.timestamp,
          sampleRateHz: _sampleRateHz,
          accelerationMagnitude: sample.accelerationMagnitude,
          gyroscopeMagnitude: sample.gyroscopeMagnitude,
          detectorPhase: _detector.latestDecision.phase,
          detectorReason: ImuDetectorDecisionReason.gyroscopeStale,
          demoGestureSequence: _sensorSelfTestDetector.sequence,
        ),
        force: force,
      );
      return;
    }
    _emitDiagnostics(
      ImuDiagnosticsSnapshot(
        generation: _runGeneration,
        state: ImuSamplingState.sampling,
        capturedAt: sample.timestamp,
        sampleRateHz: _sampleRateHz,
        accelerationMagnitude: sample.accelerationMagnitude,
        gyroscopeMagnitude: sample.gyroscopeTimestamp == null
            ? null
            : sample.gyroscopeMagnitude,
        detectorPhase: _detector.latestDecision.phase,
        detectorReason: _detector.latestDecision.reason,
        demoGestureSequence: _sensorSelfTestDetector.sequence,
      ),
      force: force,
    );
  }

  void _checkDiagnosticsFreshness() {
    if (!safetyDiagnosticsEnabled || !_running) return;
    final latestAt = _lastAccelerometerAt;
    final now = _now().toUtc();
    if (latestAt == null) {
      _emitDiagnostics(
        ImuDiagnosticsSnapshot.awaiting(
          generation: _runGeneration,
          capturedAt: now,
        ),
        force: true,
      );
      return;
    }
    final latest = _latestDiagnostics;
    if (latest?.state == ImuSamplingState.error &&
        !latestAt.isAfter(latest!.capturedAt)) {
      return;
    }
    if (latestAt.isAfter(now)) {
      _publishSensorError('Dấu thời gian cảm biến nằm trong tương lai.');
      return;
    }
    if (now.difference(latestAt) <= _staleAfter) return;
    _emitDiagnostics(
      ImuDiagnosticsSnapshot(
        generation: _runGeneration,
        state: ImuSamplingState.stale,
        capturedAt: latestAt,
        sampleRateHz: latest?.sampleRateHz,
        accelerationMagnitude: latest?.accelerationMagnitude,
        gyroscopeMagnitude: latest?.gyroscopeMagnitude,
        detectorPhase: latest?.detectorPhase ?? FallDetectionPhase.idle,
        detectorReason:
            latest?.detectorReason ??
            ImuDetectorDecisionReason.awaitingFreeFall,
      ),
      force: true,
    );
  }

  void _publishSensorError(String message) {
    if (!safetyDiagnosticsEnabled) return;
    _emitDiagnostics(
      ImuDiagnosticsSnapshot(
        generation: _runGeneration,
        state: ImuSamplingState.error,
        capturedAt: _now().toUtc(),
        detectorPhase: _detector.latestDecision.phase,
        detectorReason: _detector.latestDecision.reason,
        errorMessage: message,
      ),
      force: true,
    );
  }

  String? get _currentSensorError =>
      _accelerometerStreamError ?? _gyroscopeStreamError;

  void _publishCurrentSensorError() {
    final error = _currentSensorError;
    if (error != null) _publishSensorError(error);
  }

  Future<void> _cancelSensorSubscriptions() async {
    final accelerometer = _accelerometerSubscription;
    final gyroscope = _gyroscopeSubscription;
    _accelerometerSubscription = null;
    _gyroscopeSubscription = null;
    try {
      await accelerometer?.cancel();
    } catch (error) {
      debugPrint(
        '[FallDetectionSensorService] accelerometer cancel failed: $error',
      );
    }
    try {
      await gyroscope?.cancel();
    } catch (error) {
      debugPrint(
        '[FallDetectionSensorService] gyroscope cancel failed: $error',
      );
    }
  }

  void _emitDiagnostics(ImuDiagnosticsSnapshot snapshot, {bool force = false}) {
    if (!safetyDiagnosticsEnabled) return;
    final previousPublishedAt = _lastSamplingDiagnosticsPublishedAt;
    if (!force &&
        previousPublishedAt != null &&
        snapshot.capturedAt.difference(previousPublishedAt) <
            _diagnosticsThrottle) {
      return;
    }
    if (!force) _lastSamplingDiagnosticsPublishedAt = snapshot.capturedAt;
    _latestDiagnostics = snapshot;
    _diagnosticsController.add(snapshot);
  }

  /// Gửi ứng viên té ngã [FallCandidate] lên API Backend `/api/v1/safety/imu-data`.
  /// Kèm theo cờ `onDeviceFallConfirmed = true` và tọa độ GPS (Latitude, Longitude) nếu được cấp quyền.
  Future<void> _sendCandidate(
    FallCandidate candidate,
    int runGeneration,
    int responseGeneration,
  ) async {
    final impact = candidate.impactSample;
    final locationReadAt = _locationReadAt;
    // Kiểm tra độ tươi của tọa độ vị trí (phải được đọc trong vòng 30 giây gần nhất)
    final hasFreshLocation =
        _locationSharingAllowed &&
        _latestPosition != null &&
        locationReadAt != null &&
        _now().difference(locationReadAt) <= const Duration(seconds: 30);
    final position = hasFreshLocation ? _latestPosition : null;
    if (_locationSharingAllowed && !hasFreshLocation) {
      _scheduleLocationRefresh();
    }
    // Thử gửi tối đa 3 lần với cơ chế exponential backoff nếu gặp lỗi mạng
    for (var attempt = 1; attempt <= 3; attempt++) {
      if (!_running || _runGeneration != runGeneration) return;
      try {
        final safetyEvent = await _safetyService.sendImuData(
          accelerometerX: impact.accelerometerX,
          accelerometerY: impact.accelerometerY,
          accelerometerZ: impact.accelerometerZ,
          gyroscopeX: impact.gyroscopeX,
          gyroscopeY: impact.gyroscopeY,
          gyroscopeZ: impact.gyroscopeZ,
          timestamp: impact.timestamp,
          onDeviceFallConfirmed: true,
          signalId: '${impact.timestamp.microsecondsSinceEpoch}',
          latitude: position?.latitude,
          longitude: position?.longitude,
        );
        if (safetyEvent != null) {
          // Nếu sự kiện trả về sau khi người dùng đã phản hồi xong -> Tự động đóng lại
          if (responseGeneration != _responseGeneration) {
            await _closeStaleEvent(safetyEvent);
          } else if (_publishedEventIds.add(safetyEvent.id)) {
            _eventController.add(safetyEvent);
          }
        }
        return;
      } catch (error) {
        if (attempt == 3) {
          debugPrint(
            '[FallDetectionSensorService] send IMU failed after retry: $error',
          );
          _publishSensorError('Không gửi được dữ liệu ngã: $error');
          return;
        }
        await Future<void>.delayed(Duration(seconds: attempt));
      }
    }
  }

  /// Tự động đóng sự kiện cảnh báo cũ nếu người dùng đã xử lý xong phản hồi trước đó.
  Future<void> _closeStaleEvent(SafetyEvent event) async {
    try {
      final override = _closeStaleEventOverride;
      if (override != null) {
        await override(event);
        return;
      }
      final updated = await _safetyService.reportFalsePositive(
        event.id,
        note: 'Tự động đóng cảnh báo IMU cũ sau khi người dùng đã phản hồi.',
      );
      final emergencySessionId = updated.emergencySessionId;
      if (emergencySessionId != null && emergencySessionId.isNotEmpty) {
        await EmergencyService().resolve(emergencySessionId);
      }
    } catch (error) {
      debugPrint(
        '[FallDetectionSensorService] close stale event failed: $error',
      );
    }
  }

  /// Lên lịch tác vụ đọc vị trí ngầm nếu chưa có tác vụ nào đang chạy.
  void _scheduleLocationRefresh() {
    if (_locationRefreshInFlight != null) return;
    final operation = _refreshLocation();
    _locationRefreshInFlight = operation;
    unawaited(
      operation.whenComplete(() {
        if (identical(_locationRefreshInFlight, operation)) {
          _locationRefreshInFlight = null;
        }
      }),
    );
  }

  /// Đọc tọa độ GPS thời gian thực từ cảm biến thiết bị theo tiêu chuẩn bảo mật dữ liệu (PDPA Consent).
  Future<void> _refreshLocation() async {
    final override = _refreshLocationOverride;
    if (override != null) {
      await override();
      return;
    }
    try {
      if (!_locationSharingAllowed) {
        _latestPosition = null;
        _locationReadAt = null;
        return;
      }
      _latestPosition = null;
      _locationReadAt = null;
      // Kiểm tra trạng thái Consent chia sẻ vị trí của người dùng
      final consents = await PrivacyService.instance.listConsents();
      final consentActive = consents.any(
        (grant) =>
            grant.isActive &&
            grant.dataType == 'LOCATION' &&
            grant.purpose == 'SHARE',
      );
      if (!consentActive) {
        _locationSharingAllowed = false;
        _latestPosition = null;
        _locationReadAt = null;
        return;
      }
      // Đọc tọa độ GPS từ dịch vụ định vị
      final position = await _permissionService.readConsentedLocation();
      if (!_locationSharingAllowed || position == null) {
        _latestPosition = null;
        _locationReadAt = null;
        return;
      }
      _latestPosition = position;
      _locationReadAt = DateTime.now();
    } catch (error) {
      _latestPosition = null;
      _locationReadAt = null;
      debugPrint(
        '[FallDetectionSensorService] location refresh failed: $error',
      );
    }
  }
}
