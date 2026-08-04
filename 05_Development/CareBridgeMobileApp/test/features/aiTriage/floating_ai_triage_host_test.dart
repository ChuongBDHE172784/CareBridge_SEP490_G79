import 'dart:async';

import 'package:untitled/features/aiTriage/widgets/floating_ai_triage_host.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  Widget buildHost({
    required ValueNotifier<int> authChanges,
    required ValueNotifier<int> navigationChanges,
    required bool Function() isAuthenticated,
    required String? Function() currentRole,
    required String Function() currentPath,
    required Future<void> Function() onOpen,
    ValueNotifier<int>? modalChanges,
    bool Function()? hasModal,
  }) {
    return MaterialApp(
      home: FloatingAiTriageHost(
        authListenable: authChanges,
        navigationListenable: navigationChanges,
        modalListenable: modalChanges,
        isAuthenticated: isAuthenticated,
        currentRole: currentRole,
        currentPath: currentPath,
        hasModal: hasModal ?? () => false,
        onOpen: onOpen,
        child: const Scaffold(body: Text('Nội dung ứng dụng')),
      ),
    );
  }

  testWidgets('shows for Mother and Family but not unsupported roles', (
    tester,
  ) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);
    var role = 'MOTHER';

    await tester.pumpWidget(
      buildHost(
        authChanges: authChanges,
        navigationChanges: navigationChanges,
        isAuthenticated: () => true,
        currentRole: () => role,
        currentPath: () => '/',
        onOpen: () async {},
      ),
    );
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsOneWidget);

    role = 'FAMILY';
    authChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsOneWidget);

    role = 'EXPERT';
    authChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsNothing);
  });

  testWidgets('hides on triage, onboarding, and emergency routes', (
    tester,
  ) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);
    var path = '/';

    await tester.pumpWidget(
      buildHost(
        authChanges: authChanges,
        navigationChanges: navigationChanges,
        isAuthenticated: () => true,
        currentRole: () => 'FAMILY',
        currentPath: () => path,
        onOpen: () async {},
      ),
    );
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsOneWidget);

    path = '/triage/intake';
    navigationChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsNothing);

    path = '/emergency/map';
    navigationChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsNothing);

    path = '/journey-setup';
    navigationChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsNothing);

    path = '/postpartum-safety-help';
    navigationChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsNothing);
  });

  testWidgets('hides while a modal route is active', (tester) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);
    final modalChanges = ValueNotifier(0);
    var hasModal = false;

    await tester.pumpWidget(
      buildHost(
        authChanges: authChanges,
        navigationChanges: navigationChanges,
        modalChanges: modalChanges,
        isAuthenticated: () => true,
        currentRole: () => 'FAMILY',
        currentPath: () => '/',
        hasModal: () => hasModal,
        onOpen: () async {},
      ),
    );
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsOneWidget);

    hasModal = true;
    modalChanges.value++;
    await tester.pump();
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsNothing);
  });

  testWidgets('defers observer notifications received during a build', (
    tester,
  ) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);
    final observer = FloatingAiTriageRouteObserver();
    late StateSetter rebuildChild;
    var notifyDuringBuild = false;

    await tester.pumpWidget(
      MaterialApp(
        home: FloatingAiTriageHost(
          authListenable: authChanges,
          navigationListenable: navigationChanges,
          modalListenable: observer,
          isAuthenticated: () => true,
          currentRole: () => 'MOTHER',
          currentPath: () => '/',
          hasModal: () => observer.hasPopupRoute,
          onOpen: () async {},
          child: StatefulBuilder(
            builder: (context, setState) {
              rebuildChild = setState;
              if (notifyDuringBuild) {
                observer.didPush(
                  PageRouteBuilder<void>(
                    pageBuilder: (_, __, ___) => const SizedBox.shrink(),
                  ),
                  null,
                );
              }
              return const Scaffold(body: Text('Nội dung ứng dụng'));
            },
          ),
        ),
      ),
    );

    notifyDuringBuild = true;
    rebuildChild(() {});
    await tester.pump();
    await tester.pump();

    expect(tester.takeException(), isNull);
    expect(find.byKey(const Key('floating-ai-triage-robot')), findsOneWidget);
  });

  testWidgets('tap opens triage and drag remains inside the safe viewport', (
    tester,
  ) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);
    var opens = 0;

    await tester.pumpWidget(
      buildHost(
        authChanges: authChanges,
        navigationChanges: navigationChanges,
        isAuthenticated: () => true,
        currentRole: () => 'MOTHER',
        currentPath: () => '/content',
        onOpen: () async => opens++,
      ),
    );

    final robot = find.byKey(const Key('floating-ai-triage-robot'));
    await tester.tap(robot);
    expect(opens, 1);

    await tester.drag(robot, const Offset(-2000, -2000));
    await tester.pumpAndSettle();
    var rect = tester.getRect(robot);
    expect(rect.left, greaterThanOrEqualTo(0));
    expect(rect.top, greaterThanOrEqualTo(0));

    await tester.drag(robot, const Offset(2000, 2000));
    await tester.pumpAndSettle();
    rect = tester.getRect(robot);
    final viewport = tester.view.physicalSize / tester.view.devicePixelRatio;
    expect(rect.right, lessThanOrEqualTo(viewport.width));
    expect(rect.bottom, lessThanOrEqualTo(viewport.height));
  });

  testWidgets(
    'hides while AI navigation is pending, restores, and ignores duplicate taps',
    (tester) async {
      final authChanges = ValueNotifier(0);
      final navigationChanges = ValueNotifier(0);
      final navigation = Completer<void>();
      var opens = 0;

      await tester.pumpWidget(
        buildHost(
          authChanges: authChanges,
          navigationChanges: navigationChanges,
          isAuthenticated: () => true,
          currentRole: () => 'FAMILY',
          currentPath: () => '/',
          onOpen: () {
            opens++;
            return navigation.future;
          },
        ),
      );

      final robot = find.byKey(const Key('floating-ai-triage-robot'));
      final onTap = tester.widget<GestureDetector>(robot).onTap!;

      onTap();
      onTap();
      await tester.pump();

      expect(opens, 1);
      expect(robot, findsNothing);

      await tester.pump(const Duration(seconds: 1));
      expect(robot, findsNothing);

      navigation.complete();
      await tester.pump();
      await tester.pump();

      expect(robot, findsOneWidget);
    },
  );

  testWidgets('restores after AI navigation fails', (tester) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);
    final navigation = Completer<void>();

    await tester.pumpWidget(
      buildHost(
        authChanges: authChanges,
        navigationChanges: navigationChanges,
        isAuthenticated: () => true,
        currentRole: () => 'MOTHER',
        currentPath: () => '/',
        onOpen: () => navigation.future,
      ),
    );

    final robot = find.byKey(const Key('floating-ai-triage-robot'));
    await tester.tap(robot);
    await tester.pump();
    expect(robot, findsNothing);

    navigation.completeError(StateError('navigation failed'));
    await tester.pump();
    await tester.pump();

    expect(tester.takeException(), isNull);
    expect(robot, findsOneWidget);
  });

  testWidgets('production builder placement stays valid on web hover', (
    tester,
  ) async {
    final authChanges = ValueNotifier(0);
    final navigationChanges = ValueNotifier(0);

    await tester.pumpWidget(
      MaterialApp(
        builder: (context, child) => FloatingAiTriageHost(
          authListenable: authChanges,
          navigationListenable: navigationChanges,
          isAuthenticated: () => true,
          currentRole: () => 'MOTHER',
          currentPath: () => '/mother-home',
          onOpen: () async {},
          child: child!,
        ),
        home: const Scaffold(body: Text('Nội dung ứng dụng')),
      ),
    );
    await tester.pumpAndSettle();

    final robot = find.byKey(const Key('floating-ai-triage-robot'));
    expect(robot, findsOneWidget);
    await tester.sendEventToBinding(
      PointerHoverEvent(position: tester.getCenter(robot)),
    );
    await tester.pump(const Duration(seconds: 1));

    expect(tester.takeException(), isNull);
  });
}
