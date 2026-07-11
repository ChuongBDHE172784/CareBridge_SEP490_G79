import 'dart:async';
import 'dart:math';

import 'package:flutter/foundation.dart';
import 'package:sensors_plus/sensors_plus.dart';

import 'safety_service.dart';

class FallDetectionSensorService {
  FallDetectionSensorService._();

  static final FallDetectionSensorService instance = FallDetectionSensorService._();
  static const _samplingPeriod = SensorInterval.gameInterval;

  final SafetyService _safetyService = SafetyService();

  StreamSubscription<AccelerometerEvent>? _accelerometerSubscription;
  StreamSubscription<GyroscopeEvent>? _gyroscopeSubscription;
  GyroscopeEvent? _latestGyroscope;
  DateTime? _lastSentAt;
  bool _sending = false;
  bool _running = false;

  bool get isRunning => _running;

  Future<void> start() async {
    if (_running) return;
    _running = true;
    _gyroscopeSubscription = gyroscopeEventStream(
      samplingPeriod: _samplingPeriod,
    ).listen(
      (event) => _latestGyroscope = event,
      onError: (error, stackTrace) {
        debugPrint('[FallDetectionSensorService] gyroscope error: $error');
      },
      cancelOnError: false,
    );
    _accelerometerSubscription = accelerometerEventStream(
      samplingPeriod: _samplingPeriod,
    ).listen(
      _handleAccelerometer,
      onError: (error, stackTrace) {
        debugPrint('[FallDetectionSensorService] accelerometer error: $error');
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
  }

  void _handleAccelerometer(AccelerometerEvent event) {
    if (!_running || _sending) return;

    final now = DateTime.now();
    final magnitude = sqrt(event.x * event.x + event.y * event.y + event.z * event.z);
    final likelyImpact = (magnitude - 9.81).abs() >= 8.0;
    final elapsed = _lastSentAt == null ? null : now.difference(_lastSentAt!);

    if (!likelyImpact && elapsed != null && elapsed < const Duration(milliseconds: 500)) {
      return;
    }

    _lastSentAt = now;
    _sending = true;
    final gyro = _latestGyroscope;
    unawaited(_safetyService
        .sendImuData(
          accelerometerX: event.x,
          accelerometerY: event.y,
          accelerometerZ: event.z,
          gyroscopeX: gyro?.x ?? 0,
          gyroscopeY: gyro?.y ?? 0,
          gyroscopeZ: gyro?.z ?? 0,
          timestamp: now,
        )
        .catchError((error, stackTrace) {
      debugPrint('[FallDetectionSensorService] send IMU failed: $error');
    }).whenComplete(() {
      _sending = false;
    }));
  }
}
