import 'dart:async';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../auth/auth_state.dart';
import '../network/api_client.dart';
import '../routes/app_router.dart';
import '../../features/directChat/services/conversation_refresh_bus.dart';
import '../../features/consultation/services/consultation_request_refresh_bus.dart';

/// Registers this device's FCM token with the backend
/// (POST /api/v1/notifications/device-token) so server-side alerts
/// (e.g. UC-65 family emergency alert) can reach this device for real.
enum FcmRegistrationState { idle, registering, registered, failed }

class FcmService {
  // Mutable so widget tests can swap in a fake subclass to drive _handleTap directly.
  static FcmService instance = FcmService();

  static const String webVapidKey = String.fromEnvironment(
    'FIREBASE_WEB_VAPID_KEY',
  );

  StreamSubscription<String>? _refreshSub;
  int _registrationAttempt = 0;
  bool _tapHandlingInitialized = false;
  String? _pendingRoute;
  bool _navigationReady = false;
  FcmRegistrationState _registrationState = FcmRegistrationState.idle;
  String? _registrationErrorCode;
  static final RegExp _uuidPattern = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
  );

  FcmRegistrationState get registrationState => _registrationState;
  String? get registrationErrorCode => _registrationErrorCode;

  /// Call once after a successful login. Safe to call multiple times —
  /// registering the same token again is a harmless no-op server-side.
  Future<void> registerToken() async {
    final auth = AuthState.instance;
    final accountId = auth.userId;
    final generation = auth.sessionGeneration;
    final attempt = ++_registrationAttempt;
    if (!auth.isAuthenticated || accountId == null || accountId.isEmpty) {
      if (attempt == _registrationAttempt) {
        _setRegistrationFailure('AUTH_SESSION_UNAVAILABLE');
      }
      return;
    }
    _registrationState = FcmRegistrationState.registering;
    _registrationErrorCode = null;
    try {
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission();
      if (kIsWeb && webVapidKey.trim().isEmpty) {
        throw const FcmConfigurationException('WEB_VAPID_KEY_MISSING');
      }
      final token = await messaging.getToken(
        vapidKey: kIsWeb ? webVapidKey.trim() : null,
      );
      if (token == null || token.isEmpty) {
        throw const FcmConfigurationException('FCM_TOKEN_UNAVAILABLE');
      }
      final sent = await _send(
        token,
        generation: generation,
        accountId: accountId,
        registrationAttempt: attempt,
      );
      if (!sent || !_isCurrentRegistration(attempt, generation, accountId)) {
        return;
      }

      _refreshSub?.cancel();
      _refreshSub = messaging.onTokenRefresh.listen(
        (token) => unawaited(
          _send(
            token,
            generation: generation,
            accountId: accountId,
            registrationAttempt: attempt,
          ).then<void>(
            (_) {},
            onError: (Object error, StackTrace _) {
              if (_isCurrentRegistration(attempt, generation, accountId)) {
                _setRegistrationFailure(_errorCode(error));
              }
            },
          ),
        ),
        onError: (Object error, StackTrace _) {
          if (_isCurrentRegistration(attempt, generation, accountId)) {
            _setRegistrationFailure(_errorCode(error));
          }
        },
      );
      if (_isCurrentRegistration(attempt, generation, accountId)) {
        _registrationState = FcmRegistrationState.registered;
      }
    } catch (error) {
      if (_isCurrentRegistration(attempt, generation, accountId)) {
        _setRegistrationFailure(_errorCode(error));
        debugPrint(
          '[FcmService] registerToken failed: code=$_registrationErrorCode',
        );
      }
    }
  }

  /// Deregisters this account's current token before a logout/account switch.
  /// The backend endpoint is owner-scoped; failures are allowed to propagate to
  /// the caller so logout can record/retry them without clearing another account.
  Future<void> deregisterToken({String? token}) async {
    final auth = AuthState.instance;
    final accountId = auth.userId;
    final generation = auth.sessionGeneration;
    final attempt = ++_registrationAttempt;
    final refreshSub = _refreshSub;
    _refreshSub = null;
    await refreshSub?.cancel();
    try {
      if (accountId == null || accountId.isEmpty || !auth.isAuthenticated) {
        return;
      }
      final currentToken =
          token ??
          await FirebaseMessaging.instance.getToken(
            vapidKey: kIsWeb ? webVapidKey.trim() : null,
          );
      if (currentToken == null || currentToken.isEmpty) return;
      if (!_isCurrentRegistration(attempt, generation, accountId)) {
        return;
      }
      await apiDelete(
        '/api/v1/notifications/device-token?token=${Uri.encodeQueryComponent(currentToken)}',
      );
    } finally {
      if (attempt == _registrationAttempt) {
        _registrationState = FcmRegistrationState.idle;
      }
    }
  }

  /// Navigates to the real alert detail screen when the user taps an
  /// "EMERGENCY_ALERT" push (UC-65/UC-141) — call once at app startup.
  /// Handles both a cold start (app launched from the notification) and a
  /// tap while the app is backgrounded.
  Future<void> initTapHandling() async {
    if (_tapHandlingInitialized) return;
    _tapHandlingInitialized = true;
    FirebaseMessaging.onMessageOpenedApp.listen(_handleTap);
    FirebaseMessaging.onMessage.listen((message) {
      final foregroundData = <String, dynamic>{...message.data};
      final notification = message.notification;
      if (notification?.title != null) {
        foregroundData['title'] = notification!.title;
      }
      if (notification?.body != null) {
        foregroundData['body'] = notification!.body;
      }
      if (shouldOpenForegroundEmergency(message.data)) {
        _handleTap(message);
      } else {
        _handleForegroundData(foregroundData);
        _showForegroundReminder(foregroundData);
      }
    });
    final initialMessage = await FirebaseMessaging.instance.getInitialMessage();
    if (initialMessage != null) _handleTap(initialMessage);
  }

  /// Pure routing decision, factored out of [_handleTap] so it's testable without the
  /// Firebase Messaging platform channel (this codebase has no Firebase test-mocking
  /// infrastructure anywhere yet — see MEDI-FL-09).
  @visibleForTesting
  static String? resolveTapRoute(Map<String, dynamic> data) {
    final sessionId = data['sessionId'];
    if (data['type'] == 'EMERGENCY_ALERT' &&
        sessionId is String &&
        _uuidPattern.hasMatch(sessionId)) {
      return '/emergency/alert/${Uri.encodeComponent(sessionId)}';
    }
    final scheduleId =
        data['scheduleId'] ??
        ((data['referenceType'] == 'REMINDER_SCHEDULE')
            ? data['referenceId']
            : null);
    if ((data['type'] == 'REMINDER_SCHEDULE' ||
            data['referenceType'] == 'REMINDER_SCHEDULE') &&
        scheduleId is String &&
        _uuidPattern.hasMatch(scheduleId)) {
      return '/reminder-schedules/${Uri.encodeComponent(scheduleId)}';
    }
    final reminderId = data['reminderId'] ?? data['referenceId'];
    if (data['type'] == 'REMINDER' &&
        data['referenceType'] == 'APPOINTMENT' &&
        reminderId is String &&
        _uuidPattern.hasMatch(reminderId)) {
      return '/appointments/detail/${Uri.encodeComponent(reminderId)}';
    }
    if (data['type'] == 'REMINDER' &&
        reminderId is String &&
        _uuidPattern.hasMatch(reminderId)) {
      return '/reminders/detail/${Uri.encodeComponent(reminderId)}';
    }
    final conversationId = data['conversationId'];
    if (data['type'] == 'MESSAGE' &&
        conversationId is String &&
        _uuidPattern.hasMatch(conversationId)) {
      return '/direct-chat/${Uri.encodeComponent(conversationId)}';
    }
    final requestId = data['requestId'];
    if (data['type'] == 'CONSULTATION_REQUEST' &&
        requestId is String &&
        _uuidPattern.hasMatch(requestId)) {
      return '/consultation-requests/${Uri.encodeComponent(requestId)}';
    }
    return null;
  }

  @visibleForTesting
  static bool shouldOpenForegroundEmergency(Map<String, dynamic> data) =>
      data['type'] == 'EMERGENCY_ALERT' && resolveTapRoute(data) != null;

  static void _handleForegroundData(Map<String, dynamic> data) {
    if (data['type'] == 'MESSAGE') ConversationRefreshBus.notify();
    if (data['type'] == 'CONSULTATION_REQUEST') {
      ConsultationRequestRefreshBus.notify();
    }
  }

  @visibleForTesting
  static void handleForegroundDataForTesting(Map<String, dynamic> data) {
    _handleForegroundData(data);
  }

  void _showForegroundReminder(Map<String, dynamic> data) {
    final type = data['type']?.toString();
    final referenceType = data['referenceType']?.toString();
    final isReminder =
        type == 'REMINDER' ||
        type == 'REMINDER_SCHEDULE' ||
        referenceType == 'REMINDER_SCHEDULE';
    if (!isReminder) return;
    final route = resolveTapRoute(data);
    final context = rootNavigatorKey.currentContext;
    if (!isReminder || route == null || context == null) return;
    final messenger = ScaffoldMessenger.maybeOf(context);
    if (messenger == null) return;
    final title = data['title']?.toString() ?? 'Nhắc lịch';
    final body = data['body']?.toString() ?? 'Bạn có một lịch cần xem.';
    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text('$title\n$body'),
          action: SnackBarAction(
            label: 'Mở',
            onPressed: () => GoRouter.of(context).push(route),
          ),
        ),
      );
  }

  void _handleTap(RemoteMessage message) {
    final route = resolveTapRoute(message.data);
    if (route == null) return;
    _pendingRoute = route;
    flushPendingRoute();
  }

  /// Keeps a one-shot cold-start deep link until both auth restoration and the root
  /// navigator are ready. Firebase's initial message cannot be consumed a second time.
  void flushPendingRoute() {
    _flushPendingRoute();
  }

  void _flushPendingRoute([void Function(String)? navigate]) {
    final route = _pendingRoute;
    if (!_navigationReady || route == null) return;
    final context = navigate == null ? rootNavigatorKey.currentContext : null;
    if (navigate == null && context == null) return;
    _pendingRoute = null;
    if (navigate != null) {
      navigate(route);
    } else {
      GoRouter.of(context!).push(route);
    }
  }

  /// Signals that auth restoration and the root router are ready. Pending
  /// initial-message routes remain queued until this explicit boundary.
  void markNavigationReady({
    @visibleForTesting void Function(String)? navigate,
  }) {
    _navigationReady = true;
    _flushPendingRoute(navigate);
  }

  @visibleForTesting
  String? get pendingRouteForTesting => _pendingRoute;

  @visibleForTesting
  void queueRouteForTesting(String route) => _pendingRoute = route;

  @visibleForTesting
  static String platformNameForTesting({
    bool? isWeb,
    TargetPlatform? platform,
  }) {
    if (isWeb ?? kIsWeb) return 'WEB';
    return (platform ?? defaultTargetPlatform) == TargetPlatform.iOS
        ? 'IOS'
        : 'ANDROID';
  }

  Future<bool> _send(
    String token, {
    required int generation,
    required String accountId,
    required int registrationAttempt,
  }) async {
    if (!_isCurrentRegistration(registrationAttempt, generation, accountId)) {
      return false;
    }
    await apiPost('/api/v1/notifications/device-token', {
      'token': token,
      'platform': _platformName,
    });
    debugPrint('[FcmService] device token registered platform=$_platformName');
    return _isCurrentRegistration(registrationAttempt, generation, accountId);
  }

  bool _isCurrentRegistration(int attempt, int generation, String accountId) {
    return attempt == _registrationAttempt &&
        AuthState.instance.matchesSession(
          generation: generation,
          userId: accountId,
        );
  }

  String get _platformName {
    return platformNameForTesting(
      isWeb: kIsWeb,
      platform: defaultTargetPlatform,
    );
  }

  void _setRegistrationFailure(String code) {
    _registrationState = FcmRegistrationState.failed;
    _registrationErrorCode = code;
  }

  String _errorCode(Object error) {
    if (error is FcmConfigurationException) return error.code;
    if (error is ApiException) return 'HTTP_${error.statusCode}';
    return error.runtimeType.toString();
  }
}

class FcmConfigurationException implements Exception {
  const FcmConfigurationException(this.code);

  final String code;
}
