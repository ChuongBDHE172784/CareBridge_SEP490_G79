import 'package:flutter_test/flutter_test.dart';
import 'package:flutter/foundation.dart';
import 'package:untitled/core/notifications/fcm_service.dart';

// MEDI-FL-09 — this codebase has no Firebase Messaging platform-channel mocking
// infrastructure anywhere (verified: no existing test exercises FirebaseMessaging.instance),
// so this exercises the routing DECISION that fcm_service.dart's _handleTap delegates to
// (extracted as FcmService.resolveTapRoute specifically for this reason) rather than driving
// FirebaseMessaging.onMessageOpenedApp/getInitialMessage end-to-end.
void main() {
  test(
    'MESSAGE type with a conversationId resolves to the direct-chat deep link',
    () {
      final route = FcmService.resolveTapRoute({
        'type': 'MESSAGE',
        'conversationId': 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      });
      expect(route, '/direct-chat/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa');
    },
  );

  test('cold-start equivalent: same MESSAGE payload resolves identically', () {
    // initTapHandling()'s getInitialMessage() branch and the onMessageOpenedApp branch both
    // funnel into the same resolveTapRoute — a second, differently-labeled call documents
    // that both paths share (and are covered by) the same decision logic.
    final route = FcmService.resolveTapRoute({
      'type': 'MESSAGE',
      'conversationId': 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
    });
    expect(route, '/direct-chat/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa');
  });

  test('REMINDER type resolves to appointment reminder detail', () {
    final route = FcmService.resolveTapRoute({
      'type': 'REMINDER',
      'reminderId': 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    });
    expect(route, '/reminders/detail/cccccccc-cccc-4ccc-8ccc-cccccccccccc');
  });

  test('REMINDER_SCHEDULE type resolves to alarm schedule detail', () {
    final route = FcmService.resolveTapRoute({
      'type': 'REMINDER_SCHEDULE',
      'scheduleId': 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
    });
    expect(route, '/reminder-schedules/cccccccc-cccc-4ccc-8ccc-cccccccccccc');
  });

  test('REMINDER type rejects a malformed reminder identifier', () {
    expect(
      FcmService.resolveTapRoute({
        'type': 'REMINDER',
        'reminderId': '../admin',
      }),
      isNull,
    );
  });

  test('appointment reminder payload resolves to appointment detail', () {
    final route = FcmService.resolveTapRoute({
      'type': 'REMINDER',
      'referenceType': 'APPOINTMENT',
      'reminderId': 'dddddddd-dddd-4ddd-8ddd-dddddddddddd',
    });
    expect(route, '/appointments/detail/dddddddd-dddd-4ddd-8ddd-dddddddddddd');
  });

  test('MESSAGE type without a conversationId resolves to no route', () {
    final route = FcmService.resolveTapRoute({'type': 'MESSAGE'});
    expect(route, isNull);
  });

  test('EMERGENCY_ALERT still resolves to its existing route (regression)', () {
    final route = FcmService.resolveTapRoute({
      'type': 'EMERGENCY_ALERT',
      'sessionId': 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
    });
    expect(route, '/emergency/alert/bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb');
  });

  test('valid foreground emergency is handled immediately', () {
    expect(
      FcmService.shouldOpenForegroundEmergency({
        'type': 'EMERGENCY_ALERT',
        'sessionId': 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
      }),
      isTrue,
    );
    expect(
      FcmService.shouldOpenForegroundEmergency({
        'type': 'EMERGENCY_ALERT',
        'sessionId': 'not-a-uuid',
      }),
      isFalse,
    );
  });

  test('malformed route identifiers are rejected', () {
    expect(
      FcmService.resolveTapRoute({
        'type': 'MESSAGE',
        'conversationId': '../admin',
      }),
      isNull,
    );
    expect(
      FcmService.resolveTapRoute({'type': 'MESSAGE', 'conversationId': 123}),
      isNull,
    );
  });

  test('unknown type resolves to no route', () {
    final route = FcmService.resolveTapRoute({'type': 'SOMETHING_ELSE'});
    expect(route, isNull);
  });

  test('platform registration labels Web separately from Android and iOS', () {
    expect(FcmService.platformNameForTesting(isWeb: true), 'WEB');
    expect(
      FcmService.platformNameForTesting(
        isWeb: false,
        platform: TargetPlatform.android,
      ),
      'ANDROID',
    );
    expect(
      FcmService.platformNameForTesting(
        isWeb: false,
        platform: TargetPlatform.iOS,
      ),
      'IOS',
    );
  });

  test('malformed consultation request identifier is rejected', () {
    expect(
      FcmService.resolveTapRoute({
        'type': 'CONSULTATION_REQUEST',
        'requestId': '../admin',
      }),
      isNull,
    );
  });

  test('cold-start route waits for readiness and flushes exactly once', () {
    final service = FcmService();
    final navigated = <String>[];
    service.queueRouteForTesting('/emergency/alert/session');

    service.flushPendingRoute();
    expect(service.pendingRouteForTesting, isNotNull);

    service.markNavigationReady(navigate: navigated.add);
    service.markNavigationReady(navigate: navigated.add);

    expect(navigated, ['/emergency/alert/session']);
    expect(service.pendingRouteForTesting, isNull);
  });
}
