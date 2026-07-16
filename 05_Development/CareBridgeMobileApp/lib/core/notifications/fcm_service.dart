import 'dart:async';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:go_router/go_router.dart';
import '../network/api_client.dart';
import '../routes/app_router.dart';

/// Registers this device's FCM token with the backend
/// (POST /api/v1/notifications/device-token) so server-side alerts
/// (e.g. UC-65 family emergency alert) can reach this device for real.
class FcmService {
  // Mutable so widget tests can swap in a fake subclass to drive _handleTap directly.
  static FcmService instance = FcmService();

  StreamSubscription<String>? _refreshSub;
  bool _tapHandlingInitialized = false;

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
    final initialMessage = await FirebaseMessaging.instance.getInitialMessage();
    if (initialMessage != null) _handleTap(initialMessage);
  }

  /// Pure routing decision, factored out of [_handleTap] so it's testable without the
  /// Firebase Messaging platform channel (this codebase has no Firebase test-mocking
  /// infrastructure anywhere yet — see MEDI-FL-09).
  @visibleForTesting
  static String? resolveTapRoute(Map<String, dynamic> data) {
    final sessionId = data['sessionId'];
    if (data['type'] == 'EMERGENCY_ALERT' && sessionId != null) {
      return '/emergency/alert/$sessionId';
    }
    final conversationId = data['conversationId'];
    if (data['type'] == 'MESSAGE' && conversationId != null) {
      return '/direct-chat/$conversationId';
    }
    return null;
  }

  void _handleTap(RemoteMessage message) {
    final route = resolveTapRoute(message.data);
    if (route == null) return;
    final context = rootNavigatorKey.currentContext;
    if (context != null) {
      GoRouter.of(context).push(route);
    }
  }

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
