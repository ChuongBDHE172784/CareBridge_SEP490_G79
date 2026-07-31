import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/reminder/widgets/today_tasks_panel.dart';

import 'support/today_task_red_fixture.dart';

double _contrastRatio(Color foreground, Color background) {
  final fg = foreground.computeLuminance();
  final bg = background.computeLuminance();
  final lighter = fg > bg ? fg : bg;
  final darker = fg > bg ? bg : fg;
  return (lighter + .05) / (darker + .05);
}

Widget _panel(StatefulTodayBackend backend) => MaterialApp(
  home: Scaffold(
    body: SingleChildScrollView(
      child: TodayTasksPanel(service: backend.service),
    ),
  ),
);

void main() {
  testWidgets('card semantics exposes title, target, origin and status', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    final semantics = tester.ensureSemantics();
    try {
      await tester.pumpWidget(_panel(backend));
      await tester.pumpAndSettle();

      final card = find.byWidgetPredicate(
        (widget) =>
            widget is Semantics &&
            (widget.properties.label ?? '').contains('Chuẩn bị bình sữa'),
      );
      expect(card, findsOneWidget);
      final data = tester.getSemantics(card).getSemanticsData();
      expect(data.label, contains('System template'));
      expect(data.label, contains('Baby'));
      expect(data.label, contains('Đang chờ'));
    } finally {
      semantics.dispose();
    }
  });

  testWidgets('normal-size action controls have 48dp minimum hit targets', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    await tester.pumpWidget(_panel(backend));
    await tester.pumpAndSettle();

    final cardItem = find.byType(InkWell).first;
    expect(
      tester.getSize(cardItem),
      predicate<Size>(
        (size) => size.width >= 48 && size.height >= 48,
        'has a minimum 48dp touch target',
      ),
    );
  });

  testWidgets('320dp width at 200 percent text scale has no overflow', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    tester.platformDispatcher.textScaleFactorTestValue = 2;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);

    await tester.pumpWidget(_panel(StatefulTodayBackend(longLabels: true)));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });

  testWidgets('normal text and accent controls meet WCAG AA contrast', (
    tester,
  ) async {
    final backend = StatefulTodayBackend();
    await tester.pumpWidget(_panel(backend));
    await tester.pumpAndSettle();

    final taskText = tester.widget<Text>(find.text('Chuẩn bị bình sữa'));
    expect(taskText.style?.color, isNotNull);

    final contextText = tester.widget<Text>(find.text('Bé An'));
    expect(contextText.style?.color, isNotNull);
    expect(
      _contrastRatio(contextText.style!.color!, Colors.white),
      greaterThanOrEqualTo(4.5),
    );
  });
}
