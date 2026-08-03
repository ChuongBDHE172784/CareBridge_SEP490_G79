import 'dart:async';
import 'dart:io';

import 'package:flutter/foundation.dart';
import 'package:flutter_foreground_task/flutter_foreground_task.dart';

import '../../../core/auth/auth_state.dart';
import '../../privacy/models/privacy_model.dart';
import '../../privacy/services/privacy_service.dart';
import '../models/safety_config_model.dart';
import 'fall_detection_sensor_service.dart';
import 'safety_service.dart';

typedef SafetyConfigLoader = Future<SafetyConfig> Function();
typedef SafetyConsentLoader = Future<List<ConsentGrant>> Function();
typedef SafetyPermissionRequester = Future<bool> Function();

abstract class SafetyForegroundGateway {
  Future<bool> isRunning();

  Future<void> start({required bool locationSharingAllowed});

  Future<void> stop();
}

class SafetyForegroundServiceCoordinator {
  SafetyForegroundServiceCoordinator._({
    required SafetyForegroundGateway gateway,
    required bool Function() isAuthenticated,
    required SafetyConfigLoader loadConfig,
    required SafetyConsentLoader loadConsents,
    required bool Function() platformSupported,
    required bool Function() platformAndroid,
    required SafetyPermissionRequester requestAndroidPermissions,
  }) : _gateway = gateway,
       _isAuthenticated = isAuthenticated,
       _loadConfig = loadConfig,
       _loadConsents = loadConsents,
       _platformSupported = platformSupported,
       _platformAndroid = platformAndroid,
       _requestAndroidPermissions = requestAndroidPermissions;

  factory SafetyForegroundServiceCoordinator.forTesting({
    required SafetyForegroundGateway gateway,
    required bool Function() isAuthenticated,
    required SafetyConfigLoader loadConfig,
    required SafetyConsentLoader loadConsents,
    bool platformSupported = true,
    bool platformAndroid = true,
    SafetyPermissionRequester? requestAndroidPermissions,
  }) => SafetyForegroundServiceCoordinator._(
    gateway: gateway,
    isAuthenticated: isAuthenticated,
    loadConfig: loadConfig,
    loadConsents: loadConsents,
    platformSupported: () => platformSupported,
    platformAndroid: () => platformAndroid,
    requestAndroidPermissions: requestAndroidPermissions ?? () async => true,
  );

  static final SafetyForegroundServiceCoordinator instance =
      SafetyForegroundServiceCoordinator._(
        gateway: _FlutterSafetyForegroundGateway(),
        isAuthenticated: () => AuthState.instance.isAuthenticated,
        loadConfig: SafetyService().getConfig,
        loadConsents: PrivacyService.instance.listConsents,
        platformSupported: () =>
            !kIsWeb && (Platform.isAndroid || Platform.isIOS),
        platformAndroid: () => !kIsWeb && Platform.isAndroid,
        requestAndroidPermissions: _requestAndroidForegroundPermissions,
      );

  final SafetyForegroundGateway _gateway;
  final bool Function() _isAuthenticated;
  final SafetyConfigLoader _loadConfig;
  final SafetyConsentLoader _loadConsents;
  final bool Function() _platformSupported;
  final bool Function() _platformAndroid;
  final SafetyPermissionRequester _requestAndroidPermissions;
  final StreamController<SafetyEvent> _eventController =
      StreamController<SafetyEvent>.broadcast();

  Future<void>? _reconcileInFlight;
  bool _initialized = false;
  bool _isRunning = false;

  Stream<SafetyEvent> get detectedEvents => _eventController.stream;
  bool get isRunning => _isRunning;
  bool get isSupported => _platformSupported();

  void initialize() {
    if (_initialized || !_platformSupported()) return;
    _initialized = true;
    FlutterForegroundTask.init(
      androidNotificationOptions: AndroidNotificationOptions(
        channelId: 'carebridge_safety_monitoring',
        channelName: 'Giám sát an toàn CareBridge',
        channelDescription:
            'Thông báo thường trực khi CareBridge đang theo dõi cảm biến an toàn.',
        onlyAlertOnce: true,
        visibility: NotificationVisibility.VISIBILITY_PUBLIC,
      ),
      iosNotificationOptions: const IOSNotificationOptions(
        showNotification: false,
        playSound: false,
      ),
      foregroundTaskOptions: ForegroundTaskOptions(
        eventAction: ForegroundTaskEventAction.repeat(30000),
        autoRunOnBoot: false,
        autoRunOnMyPackageReplaced: true,
        allowWakeLock: true,
        allowWifiLock: false,
        allowAutoRestart: true,
        stopWithTask: false,
      ),
    );
    FlutterForegroundTask.addTaskDataCallback(_onTaskData);
    AuthState.instance.addListener(_onAuthStateChanged);
  }

  Future<void> reconcile() {
    final current = _reconcileInFlight;
    if (current != null) return current;
    final operation = _reconcileInternal();
    _reconcileInFlight = operation;
    return operation.whenComplete(() {
      if (identical(_reconcileInFlight, operation)) {
        _reconcileInFlight = null;
      }
    });
  }

  Future<void> _reconcileInternal() async {
    if (!_platformSupported() || !_isAuthenticated()) {
      await _stopIfRunning();
      return;
    }

    try {
      final results = await Future.wait<Object>([
        _loadConfig(),
        _loadConsents(),
      ]);
      final config = results[0] as SafetyConfig;
      final consents = results[1] as List<ConsentGrant>;
      if (!_isAuthenticated()) {
        await _stopIfRunning();
        return;
      }
      final sensorConsent = _hasActiveConsent(
        consents,
        dataType: 'SENSOR_DATA',
        purpose: 'CREATE',
      );
      if (!config.fallDetectionEnabled ||
          !config.sensorPermissionGranted ||
          !sensorConsent) {
        await _stopIfRunning();
        return;
      }

      if (!await _gateway.isRunning()) {
        // MF-14 deliberately does not request background-location access.
        await _gateway.start(locationSharingAllowed: false);
      }
      _isRunning = true;
    } catch (error) {
      debugPrint(
        '[SafetyForegroundServiceCoordinator] reconciliation failed: $error',
      );
      await _stopIfRunning();
    }
  }

  Future<bool> requestRequiredPermissions() async {
    if (!_platformSupported()) return false;
    if (!_platformAndroid()) return true;
    return _requestAndroidPermissions();
  }

  Future<void> stop() => _stopIfRunning();

  Future<void> _stopIfRunning() async {
    if (await _gateway.isRunning()) await _gateway.stop();
    _isRunning = false;
  }

  bool _hasActiveConsent(
    List<ConsentGrant> consents, {
    required String dataType,
    required String purpose,
  }) => consents.any(
    (grant) =>
        grant.isActive &&
        grant.dataType == dataType &&
        grant.purpose == purpose,
  );

  void _onTaskData(Object data) {
    if (data is! Map) return;
    final normalized = Map<String, dynamic>.from(data);
    if (normalized['type'] != 'safety_event') return;
    final payload = normalized['event'];
    if (payload is! Map) return;
    _eventController.add(
      SafetyEvent.fromJson(Map<String, dynamic>.from(payload)),
    );
  }

  void _onAuthStateChanged() => unawaited(reconcile());
}

Future<bool> _requestAndroidForegroundPermissions() async {
  var notificationPermission =
      await FlutterForegroundTask.checkNotificationPermission();
  if (notificationPermission != NotificationPermission.granted) {
    notificationPermission =
        await FlutterForegroundTask.requestNotificationPermission();
  }
  if (!await FlutterForegroundTask.isIgnoringBatteryOptimizations) {
    await FlutterForegroundTask.requestIgnoreBatteryOptimization();
  }
  return notificationPermission == NotificationPermission.granted;
}

class _FlutterSafetyForegroundGateway implements SafetyForegroundGateway {
  @override
  Future<bool> isRunning() => FlutterForegroundTask.isRunningService;

  @override
  Future<void> start({required bool locationSharingAllowed}) async {
    final result = await FlutterForegroundTask.startService(
      serviceId: 14136,
      serviceTypes: const [ForegroundServiceTypes.health],
      notificationTitle: 'CareBridge đang giám sát an toàn',
      notificationText: 'Nhấn để mở màn hình an toàn.',
      notificationInitialRoute: '/safety',
      callback: startSafetyForegroundTask,
    );
    if (result case ServiceRequestFailure(:final error)) throw error;
  }

  @override
  Future<void> stop() async {
    final result = await FlutterForegroundTask.stopService();
    if (result case ServiceRequestFailure(:final error)) throw error;
  }
}

@pragma('vm:entry-point')
void startSafetyForegroundTask() {
  FlutterForegroundTask.setTaskHandler(_SafetyForegroundTaskHandler());
}

class _SafetyForegroundTaskHandler extends TaskHandler {
  final FallDetectionSensorService _sensorService =
      FallDetectionSensorService.instance;
  StreamSubscription<SafetyEvent>? _eventSubscription;
  bool _validating = false;

  @override
  Future<void> onStart(DateTime timestamp, TaskStarter starter) async {
    await AuthState.instance.init();
    _eventSubscription = _sensorService.detectedEvents.listen(_publishEvent);
    await _validateEligibility();
  }

  void _publishEvent(SafetyEvent event) {
    FlutterForegroundTask.sendDataToMain({
      'type': 'safety_event',
      'event': event.toJson(),
    });
    unawaited(
      FlutterForegroundTask.updateService(
        notificationTitle: 'Có dấu hiệu nghi ngờ ngã hoặc va chạm',
        notificationText: 'Nhấn để xác nhận bạn có an toàn hay không.',
        notificationInitialRoute: '/safety',
      ),
    );
  }

  @override
  void onRepeatEvent(DateTime timestamp) {
    if (_validating) return;
    _validating = true;
    unawaited(_validateEligibility().whenComplete(() => _validating = false));
  }

  Future<void> _validateEligibility() async {
    try {
      if (!AuthState.instance.isAuthenticated) {
        await _stopTask();
        return;
      }
      final config = await SafetyService().getConfig();
      final consents = await PrivacyService.instance.listConsents();
      final sensorConsent = consents.any(
        (grant) =>
            grant.isActive &&
            grant.dataType == 'SENSOR_DATA' &&
            grant.purpose == 'CREATE',
      );
      if (!config.fallDetectionEnabled ||
          !config.sensorPermissionGranted ||
          !sensorConsent) {
        await _stopTask();
        return;
      }
      // Foreground health monitoring must not initiate background GPS reads.
      await _sensorService.start(locationSharingAllowed: false);
    } catch (error) {
      debugPrint(
        '[SafetyForegroundTaskHandler] eligibility validation failed: $error',
      );
      await _stopTask();
    }
  }

  Future<void> _stopTask() async {
    await _sensorService.stop();
    await FlutterForegroundTask.stopService();
  }

  @override
  Future<void> onDestroy(DateTime timestamp, bool isTimeout) async {
    await _eventSubscription?.cancel();
    _eventSubscription = null;
    await _sensorService.stop();
  }

  @override
  void onNotificationPressed() {
    FlutterForegroundTask.launchApp('/safety');
  }
}
