import 'dart:async';

import 'package:flutter/services.dart';

import '../models/health_metric_model.dart';
import 'health_metric_service.dart';

class WatchMetricImportResult {
  final String metricType;
  final double value;
  final DateTime measuredAt;
  final String deviceName;

  const WatchMetricImportResult({
    required this.metricType,
    required this.value,
    required this.measuredAt,
    required this.deviceName,
  });
}

class WatchMetricImportService {
  static const _methodChannel = MethodChannel(
    'com.carebridge.app/watch_metrics',
  );
  static const _eventChannel = EventChannel(
    'com.carebridge.app/watch_metric_stream',
  );

  final HealthMetricService _healthMetricService;
  final Set<String> _seenEventIds = {};
  StreamSubscription<dynamic>? _subscription;
  String? _journeyId;
  Future<void> Function(WatchMetricImportResult result)? _onImported;
  void Function(String message)? _onError;

  WatchMetricImportService({HealthMetricService? healthMetricService})
    : _healthMetricService = healthMetricService ?? HealthMetricService();

  void start({
    required String journeyId,
    Future<void> Function(WatchMetricImportResult result)? onImported,
    void Function(String message)? onError,
  }) {
    _journeyId = journeyId;
    _onImported = onImported;
    _onError = onError;
    _subscription ??= _eventChannel.receiveBroadcastStream().listen(
      (event) => _handleEvent(event),
      onError: (_) => _onError?.call('Không thể nhận dữ liệu từ đồng hồ.'),
    );
  }

  Future<bool> openGadgetbridge() async {
    final opened = await _methodChannel.invokeMethod<bool>(
      'openGadgetbridge',
    );
    return opened ?? false;
  }

  Future<void> drainQueuedEvents() async {
    final events = await _methodChannel.invokeMethod<List<dynamic>>(
      'drainQueuedEvents',
    );
    if (events == null) return;
    for (final event in events) {
      await _handleEvent(event);
    }
  }

  void dispose() {
    _subscription?.cancel();
    _subscription = null;
  }

  Future<void> _handleEvent(Object? event) async {
    final journeyId = _journeyId;
    if (journeyId == null || event is! Map) return;
    final payload = Map<String, dynamic>.from(event);
    final rawType = payload['type']?.toString();
    final mapped = _mapType(rawType);
    if (mapped == null) return;

    final value = _number(payload['value']);
    if (value == null || !_isPlausible(mapped.metricType, value)) {
      _onError?.call('Dữ liệu đồng hồ không hợp lệ.');
      return;
    }

    final timestamp = _integer(payload['timestamp']);
    final measuredAt = timestamp == null
        ? DateTime.now()
        : DateTime.fromMillisecondsSinceEpoch(timestamp).toLocal();
    final deviceName = payload['deviceName']?.toString() ?? 'Unknown Device';
    final eventId = [
      mapped.metricType,
      measuredAt.millisecondsSinceEpoch,
      value,
      deviceName,
    ].join(':');
    if (!_seenEventIds.add(eventId)) return;

    await _healthMetricService.addMetric(
      journeyId,
      AddMetricRequest(
        metricType: mapped.metricType,
        valueNumeric: value,
        unit: mapped.unit,
        measuredAt: measuredAt,
        sourceType: 'DEVICE',
        context: {
          'sourceApp': 'Gadgetbridge',
          'sourceType': 'SMART_WATCH',
          'deviceName': deviceName,
          'rawType': rawType,
          'rawAction': payload['action']?.toString(),
          'ingestedAt': DateTime.now().toUtc().toIso8601String(),
          if (mapped.metricType == 'MATERNAL_HEART_RATE')
            'measurementState': 'UNKNOWN',
        },
      ),
    );

    await _onImported?.call(
      WatchMetricImportResult(
        metricType: mapped.metricType,
        value: value,
        measuredAt: measuredAt,
        deviceName: deviceName,
      ),
    );
  }

  _WatchMetricMapping? _mapType(String? rawType) {
    switch (rawType) {
      case 'HEART_RATE':
      case 'MATERNAL_HEART_RATE':
        return const _WatchMetricMapping('MATERNAL_HEART_RATE', 'bpm');
      case 'STRESS':
        return const _WatchMetricMapping('STRESS', 'điểm');
      default:
        return null;
    }
  }

  bool _isPlausible(String metricType, double value) {
    if (metricType == 'MATERNAL_HEART_RATE') {
      return value >= 30 && value <= 250;
    }
    if (metricType == 'STRESS') {
      return value >= 0 && value <= 100;
    }
    return false;
  }

  double? _number(Object? value) {
    if (value is num) return value.toDouble();
    return double.tryParse(value?.toString() ?? '');
  }

  int? _integer(Object? value) {
    if (value is num) return value.toInt();
    return int.tryParse(value?.toString() ?? '');
  }
}

class _WatchMetricMapping {
  final String metricType;
  final String unit;

  const _WatchMetricMapping(this.metricType, this.unit);
}
