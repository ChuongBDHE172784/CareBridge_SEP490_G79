import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:geolocator/geolocator.dart';

import '../models/safety_config_model.dart';
import '../models/imu_diagnostics_model.dart';
import 'imu_fall_detector.dart';
import 'safety_service.dart';
import 'safety_permission_service.dart';
import '../../privacy/services/privacy_service.dart';

typedef AccelerometerEvents = Stream<AccelerometerEvent> Function();
typedef GyroscopeEvents = Stream<GyroscopeEvent> Function();

class FallDetectionSensorService {
  FallDetectionSensorService._({
    AccelerometerEvents? accelerometerEvents,
    GyroscopeEvents? gyroscopeEvents,
    DateTime Function()? now,
  }) : _accelerometerEvents =
           accelerometerEvents ??
           (() => accelerometerEventStream(samplingPeriod: _samplingPeriod)),
       _gyroscopeEvents =
           gyroscopeEvents ??
           (() => gyroscopeEventStream(samplingPeriod: _samplingPeriod)),
       _now = now ?? DateTime.now;

  @visibleForTesting
  factory FallDetectionSensorService.forTesting({
    required AccelerometerEvents accelerometerEvents,
    required GyroscopeEvents gyroscopeEvents,
    required DateTime Function() now,
  }) => FallDetectionSensorService._(
    accelerometerEvents: accelerometerEvents,
    gyroscopeEvents: gyroscopeEvents,
    now: now,
  );

  static final FallDetectionSensorService instance =
      FallDetectionSensorService._();
  static const _samplingPeriod = SensorInterval.gameInterval;

  final AccelerometerEvents _accelerometerEvents;
  final GyroscopeEvents _gyroscopeEvents;
  final DateTime Function() _now;

  final SafetyService _safetyService = SafetyService();
  final SafetyPermissionService _permissionService = SafetyPermissionService();
  final StreamController<SafetyEvent> _eventController =
      StreamController<SafetyEvent>.broadcast();
  final StreamController<ImuDiagnosticsSnapshot> _diagnosticsController =
      StreamController<ImuDiagnosticsSnapshot>.broadcast();

  StreamSubscription<AccelerometerEvent>? _accelerometerSubscription;
  StreamSubscription<GyroscopeEvent>? _gyroscopeSubscription;
  final ImuFallDetector _detector = ImuFallDetector();
  GyroscopeEvent? _latestGyroscope;
  bool _sending = false;
  bool _running = false;
  int _runGeneration = 0;
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

  static const _diagnosticsThrottle = Duration(milliseconds: 250);
  static const _staleAfter = Duration(seconds: 2);

  bool get isRunning => _running;
  Stream<SafetyEvent> get detectedEvents => _eventController.stream;
  Stream<ImuDiagnosticsSnapshot> get diagnostics =>
      kDebugMode ? _diagnosticsController.stream : const Stream.empty();

  @visibleForTesting
  bool get diagnosticsLifecycleActiveForTesting =>
      _diagnosticsLifecycleTimer?.isActive ?? false;

  @visibleForTesting
  void checkDiagnosticsFreshnessForTesting() => _checkDiagnosticsFreshness();

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
    // Epoch-based generations remain monotonic even when iOS recreates the
    // background Dart isolate for a later foreground-task session.
    final wallClockGeneration = _now().toUtc().microsecondsSinceEpoch;
    _runGeneration = wallClockGeneration > _runGeneration
        ? wallClockGeneration
        : _runGeneration + 1;
    _accelerometerStreamError = null;
    _gyroscopeStreamError = null;
    if (kDebugMode) {
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
    _sending = false;
    _locationSharingAllowed = false;
    _latestPosition = null;
    _locationReadAt = null;
    _locationRefreshInFlight = null;
    _lastAccelerometerAt = null;
    _lastSamplingDiagnosticsPublishedAt = null;
    _sampleRateHz = null;
    _accelerometerStreamError = null;
    _gyroscopeStreamError = null;
    if (kDebugMode) {
      _emitDiagnostics(
        ImuDiagnosticsSnapshot.stopped(
          generation: _runGeneration,
          capturedAt: _now().toUtc(),
        ),
        force: true,
      );
    }
  }

  void _handleAccelerometer(AccelerometerEvent event) {
    if (!_running) return;
    _accelerometerStreamError = null;

    final timestamp = event.timestamp.toUtc();
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
    if (kDebugMode) _publishSamplingDiagnostics(sample);
    if (_locationSharingAllowed &&
        (_locationReadAt == null ||
            timestamp.difference(_locationReadAt!) >
                const Duration(seconds: 30))) {
      _scheduleLocationRefresh();
    }
    if (candidate == null || _sending) return;

    _sending = true;
    unawaited(_sendCandidate(candidate, _runGeneration));
  }

  void _handleGyroscope(GyroscopeEvent event) {
    if (!_running) return;
    _gyroscopeStreamError = null;
    _latestGyroscope = event;
  }

  void _publishSamplingDiagnostics(ImuSample sample) {
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
        ),
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
        ),
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
      ),
    );
  }

  void _checkDiagnosticsFreshness() {
    if (!kDebugMode || !_running) return;
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
    if (!kDebugMode) return;
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
    if (!kDebugMode) return;
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

  Future<void> _sendCandidate(
    FallCandidate candidate,
    int runGeneration,
  ) async {
    final impact = candidate.impactSample;
    final position = _locationSharingAllowed ? _latestPosition : null;
    try {
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
            signalId: '${impact.timestamp.microsecondsSinceEpoch}',
            latitude: position?.latitude,
            longitude: position?.longitude,
          );
          if (safetyEvent != null) _eventController.add(safetyEvent);
          return;
        } catch (error) {
          if (attempt == 3) {
            debugPrint(
              '[FallDetectionSensorService] send IMU failed after retry: $error',
            );
            return;
          }
          await Future<void>.delayed(Duration(seconds: attempt));
        }
      }
    } finally {
      _sending = false;
    }
  }

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

  Future<void> _refreshLocation() async {
    try {
      if (!_locationSharingAllowed) {
        _latestPosition = null;
        _locationReadAt = null;
        return;
      }
      // Never keep using coordinates while consent freshness is being checked.
      _latestPosition = null;
      _locationReadAt = null;
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
      final position = await _permissionService.readConsentedLocation();
      if (!_locationSharingAllowed || position == null) {
        _latestPosition = null;
        _locationReadAt = null;
        return;
      }
      _latestPosition = position;
      _locationReadAt = DateTime.now();
    } catch (error) {
      // Fire-and-forget plugin and consent failures must fail closed and must
      // never leave previously-authorized coordinates cached.
      _latestPosition = null;
      _locationReadAt = null;
      debugPrint(
        '[FallDetectionSensorService] location refresh failed: $error',
      );
    }
  }
}
