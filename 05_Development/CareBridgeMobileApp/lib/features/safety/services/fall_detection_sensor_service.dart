import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:geolocator/geolocator.dart';

import '../models/safety_config_model.dart';
import 'imu_fall_detector.dart';
import 'safety_service.dart';
import 'safety_permission_service.dart';
import '../../privacy/services/privacy_service.dart';

class FallDetectionSensorService {
  FallDetectionSensorService._();

  static final FallDetectionSensorService instance =
      FallDetectionSensorService._();
  static const _samplingPeriod = SensorInterval.gameInterval;

  final SafetyService _safetyService = SafetyService();
  final SafetyPermissionService _permissionService = SafetyPermissionService();
  final StreamController<SafetyEvent> _eventController =
      StreamController<SafetyEvent>.broadcast();

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

  bool get isRunning => _running;
  Stream<SafetyEvent> get detectedEvents => _eventController.stream;

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
    _runGeneration++;
    if (_locationSharingAllowed) _scheduleLocationRefresh();
    _gyroscopeSubscription =
        gyroscopeEventStream(samplingPeriod: _samplingPeriod).listen(
          (event) => _latestGyroscope = event,
          onError: (error, stackTrace) {
            debugPrint('[FallDetectionSensorService] gyroscope error: $error');
          },
          cancelOnError: false,
        );
    _accelerometerSubscription =
        accelerometerEventStream(samplingPeriod: _samplingPeriod).listen(
          _handleAccelerometer,
          onError: (error, stackTrace) {
            debugPrint(
              '[FallDetectionSensorService] accelerometer error: $error',
            );
          },
          cancelOnError: false,
        );
  }

  Future<void> stop() async {
    _running = false;
    _runGeneration++;
    await _accelerometerSubscription?.cancel();
    await _gyroscopeSubscription?.cancel();
    _accelerometerSubscription = null;
    _gyroscopeSubscription = null;
    _latestGyroscope = null;
    _detector.reset();
    _sending = false;
    _locationSharingAllowed = false;
    _latestPosition = null;
    _locationReadAt = null;
    _locationRefreshInFlight = null;
  }

  void _handleAccelerometer(AccelerometerEvent event) {
    if (!_running) return;

    final timestamp = event.timestamp.toUtc();
    final gyro = _latestGyroscope;
    final candidate = _detector.addSample(
      ImuSample(
        accelerometerX: event.x,
        accelerometerY: event.y,
        accelerometerZ: event.z,
        gyroscopeX: gyro?.x ?? 0,
        gyroscopeY: gyro?.y ?? 0,
        gyroscopeZ: gyro?.z ?? 0,
        timestamp: timestamp,
        gyroscopeTimestamp: gyro?.timestamp.toUtc(),
      ),
    );
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
