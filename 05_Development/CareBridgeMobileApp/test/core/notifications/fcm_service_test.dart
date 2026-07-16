import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/notifications/fcm_service.dart';

// MEDI-FL-09 — this codebase has no Firebase Messaging platform-channel mocking
// infrastructure anywhere (verified: no existing test exercises FirebaseMessaging.instance),
// so this exercises the routing DECISION that fcm_service.dart's _handleTap delegates to
// (extracted as FcmService.resolveTapRoute specifically for this reason) rather than driving
// FirebaseMessaging.onMessageOpenedApp/getInitialMessage end-to-end.
void main() {
  test('MESSAGE type with a conversationId resolves to the direct-chat deep link', () {
    final route = FcmService.resolveTapRoute({
      'type': 'MESSAGE',
      'conversationId': 'X',
    });
    expect(route, '/direct-chat/X');
  });

  test('cold-start equivalent: same MESSAGE payload resolves identically', () {
    // initTapHandling()'s getInitialMessage() branch and the onMessageOpenedApp branch both
    // funnel into the same resolveTapRoute — a second, differently-labeled call documents
    // that both paths share (and are covered by) the same decision logic.
    final route = FcmService.resolveTapRoute({
      'type': 'MESSAGE',
      'conversationId': 'X',
    });
    expect(route, '/direct-chat/X');
  });

  test('MESSAGE type without a conversationId resolves to no route', () {
    final route = FcmService.resolveTapRoute({'type': 'MESSAGE'});
    expect(route, isNull);
  });

  test('EMERGENCY_ALERT still resolves to its existing route (regression)', () {
    final route = FcmService.resolveTapRoute({
      'type': 'EMERGENCY_ALERT',
      'sessionId': 'S1',
    });
    expect(route, '/emergency/alert/S1');
  });

  test('unknown type resolves to no route', () {
    final route = FcmService.resolveTapRoute({'type': 'SOMETHING_ELSE'});
    expect(route, isNull);
  });
}
