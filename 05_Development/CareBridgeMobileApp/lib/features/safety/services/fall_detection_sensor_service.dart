import 'dart:async';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:geolocator/geolocator.dart';

import '../models/safety_config_model.dart';
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
  GyroscopeEvent? _latestGyroscope;
  DateTime? _lastSentAt;
  bool _sending = false;
  bool _running = false;
  bool _locationSharingAllowed = false;
  Position? _latestPosition;
  DateTime? _locationReadAt;

  bool get isRunning => _running;
  Stream<SafetyEvent> get detectedEvents => _eventController.stream;

  Future<void> start({bool locationSharingAllowed = false}) async {
    _locationSharingAllowed = locationSharingAllowed;
    if (!locationSharingAllowed) {
      _latestPosition = null;
      _locationReadAt = null;
    }
    if (_running) {
      if (_locationSharingAllowed) unawaited(_refreshLocation());
      return;
    }
    _running = true;
    if (_locationSharingAllowed) unawaited(_refreshLocation());
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
    await _accelerometerSubscription?.cancel();
    await _gyroscopeSubscription?.cancel();
    _accelerometerSubscription = null;
    _gyroscopeSubscription = null;
    _latestGyroscope = null;
    _lastSentAt = null;
    _sending = false;
    _locationSharingAllowed = false;
    _latestPosition = null;
    _locationReadAt = null;
  }

  void _handleAccelerometer(AccelerometerEvent event) {
    if (!_running || _sending) return;

    final now = DateTime.now();
    final magnitude = sqrt(
      event.x * event.x + event.y * event.y + event.z * event.z,
    );
    final likelyImpact = (magnitude - 9.81).abs() >= 8.0;
    final elapsed = _lastSentAt == null ? null : now.difference(_lastSentAt!);

    if (!likelyImpact &&
        elapsed != null &&
        elapsed < const Duration(milliseconds: 500)) {
      return;
    }

    _lastSentAt = now;
    _sending = true;
    if (_locationSharingAllowed &&
        (_locationReadAt == null ||
            now.difference(_locationReadAt!) > const Duration(seconds: 30))) {
      unawaited(_refreshLocation());
    }
    final gyro = _latestGyroscope;
    unawaited(
      _safetyService
          .sendImuData(
            accelerometerX: event.x,
            accelerometerY: event.y,
            accelerometerZ: event.z,
            gyroscopeX: gyro?.x ?? 0,
            gyroscopeY: gyro?.y ?? 0,
            gyroscopeZ: gyro?.z ?? 0,
            timestamp: now,
            signalId: '${now.microsecondsSinceEpoch}',
            latitude: _locationSharingAllowed
                ? _latestPosition?.latitude
                : null,
            longitude: _locationSharingAllowed
                ? _latestPosition?.longitude
                : null,
          )
          .then((safetyEvent) {
            if (safetyEvent != null) _eventController.add(safetyEvent);
          })
          .catchError((error, stackTrace) {
            debugPrint('[FallDetectionSensorService] send IMU failed: $error');
          })
          .whenComplete(() {
            _sending = false;
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
