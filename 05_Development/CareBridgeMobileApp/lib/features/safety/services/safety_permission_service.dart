import 'dart:async';

import 'package:geolocator/geolocator.dart';
import 'package:sensors_plus/sensors_plus.dart';

typedef SensorAccessProbe = Future<void> Function();
typedef LocationReader = Future<Position?> Function();

class SafetyPermissionService {
  SafetyPermissionService({
    SensorAccessProbe? sensorProbe,
    LocationReader? locationReader,
  }) : _sensorProbe = sensorProbe ?? _defaultSensorProbe,
       _locationReader = locationReader ?? _defaultLocationReader;

  final SensorAccessProbe _sensorProbe;
  final LocationReader _locationReader;

  Future<bool> attestSensorAccess() async {
    try {
      await _sensorProbe();
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<Position?> readConsentedLocation() => _locationReader();

  static Future<void> _defaultSensorProbe() async {
    await accelerometerEventStream().first.timeout(const Duration(seconds: 3));
  }

  static Future<Position?> _defaultLocationReader() async {
    if (!await Geolocator.isLocationServiceEnabled()) return null;
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied ||
        permission == LocationPermission.deniedForever) {
      return null;
    }
    return Geolocator.getCurrentPosition(
      desiredAccuracy: LocationAccuracy.high,
      timeLimit: const Duration(seconds: 5),
    );
  }
}
