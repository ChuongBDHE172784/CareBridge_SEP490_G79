import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/main.dart';

void main() {
  test('a detected real fall routes to safety from another app screen', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'OPEN',
        currentPath: '/mother-home',
      ),
      isTrue,
    );
  });

  test('does not replace the active safety countdown route', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'OPEN',
        currentPath: '/safety',
      ),
      isFalse,
    );
  });

  test('does not route resolved events as active fall alerts', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'CONFIRMED_SAFE',
        currentPath: '/mother-home',
      ),
      isFalse,
    );
  });

  test('does not stack a second safety route while navigation is active', () {
    expect(
      shouldOpenSafetyMonitoringForDetectedEvent(
        eventStatus: 'OPEN',
        currentPath: '/mother-home',
        navigationInFlight: true,
      ),
      isFalse,
    );
  });

  testWidgets('back from an automatic fall alert restores the prior screen', (
    tester,
  ) async {
    final router = GoRouter(
      initialLocation: '/prior-screen',
      routes: [
        GoRoute(
          path: '/prior-screen',
          builder: (_, _) => const Scaffold(body: Text('Prior screen')),
        ),
        GoRoute(
          path: '/safety',
          builder: (_, _) => const Scaffold(body: Text('Fall alert')),
        ),
      ],
    );
    addTearDown(router.dispose);
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(find.text('Prior screen'), findsOneWidget);
    final navigation = pushSafetyMonitoringRoute<void>(router);
    await tester.pumpAndSettle();

    expect(find.text('Fall alert'), findsOneWidget);
    expect(router.canPop(), isTrue);

    router.pop();
    await navigation;
    await tester.pumpAndSettle();

    expect(find.text('Prior screen'), findsOneWidget);
    expect(find.text('Fall alert'), findsNothing);
  });
}
