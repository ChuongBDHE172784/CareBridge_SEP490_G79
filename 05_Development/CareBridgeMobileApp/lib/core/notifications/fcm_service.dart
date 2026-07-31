import 'dart:async';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:go_router/go_router.dart';
import '../network/api_client.dart';
import '../routes/app_router.dart';
import '../../features/directChat/services/conversation_refresh_bus.dart';
import '../../features/consultation/services/consultation_request_refresh_bus.dart';

/// Registers this device's FCM token with the backend
/// (POST /api/v1/notifications/device-token) so server-side alerts
/// (e.g. UC-65 family emergency alert) can reach this device for real.
class FcmService {
  // Mutable so widget tests can swap in a fake subclass to drive _handleTap directly.
  static FcmService instance = FcmService();

  StreamSubscription<String>? _refreshSub;
  bool _tapHandlingInitialized = false;
  String? _pendingRoute;
  bool _navigationReady = false;
  static final RegExp _uuidPattern = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$',
  );

  /// Call once after a successful login. Safe to call multiple times —
  /// registering the same token again is a harmless no-op server-side.
  Future<void> registerToken() async {
    try {
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission();
      final token = await messaging.getToken();
      if (token != null) await _send(token);

      _refreshSub?.cancel();
      _refreshSub = messaging.onTokenRefresh.listen(_send);
    } catch (e) {
      debugPrint('[FcmService] registerToken failed: $e');
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
      if (shouldOpenForegroundEmergency(message.data)) {
        _handleTap(message);
      } else {
        _handleForegroundData(message.data);
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
    final reminderId = data['reminderId'];
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

  Future<void> _send(String token) async {
    try {
      await apiPost('/api/v1/notifications/device-token', {
        'token': token,
        'platform': defaultTargetPlatform == TargetPlatform.iOS
            ? 'IOS'
            : 'ANDROID',
      });
      debugPrint('[FcmService] device token registered');
    } catch (e) {
      debugPrint('[FcmService] device token registration failed: $e');
    }
  }
}
