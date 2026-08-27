import 'dart:ui' show Tristate;

import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/screens/welcome_screen.dart';

const _tagline = 'Đồng hành cùng mẹ trong hành trình làm mẹ tuyệt vời.';
const _disclaimer =
    'Ứng dụng cung cấp thông tin tham khảo, không thay thế tư vấn y tế chuyên nghiệp.';

void main() {
  for (final config in <({String name, Size logicalSize})>[
    (name: 'API35 landscape', logicalSize: Size(914, 411)),
    (name: 'narrow landscape', logicalSize: Size(600, 320)),
  ]) {
    testWidgets(
      '${config.name} at 200 percent keeps content reachable and ordered',
      (tester) async {
        tester.view.physicalSize = config.logicalSize;
        tester.view.devicePixelRatio = 1;
        tester.platformDispatcher.textScaleFactorTestValue = 2.0;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        addTearDown(tester.platformDispatcher.clearTextScaleFactorTestValue);
        final semantics = tester.ensureSemantics();

        await tester.pumpWidget(const MaterialApp(home: WelcomeScreen()));
        await tester.pumpAndSettle();

        expect(tester.takeException(), isNull);
        final scrollFinder = find.byType(SingleChildScrollView);
        expect(scrollFinder, findsOneWidget);
        final scrollable = tester.state<ScrollableState>(
          find.byType(Scrollable),
        );
        expect(scrollable.position.maxScrollExtent, greaterThan(0));

        final create = find.widgetWithText(FilledButton, 'Tạo tài khoản');
        final login = find.widgetWithText(OutlinedButton, 'Đăng nhập');
        final createSemantics = tester.getSemantics(create);
        final loginSemantics = tester.getSemantics(login);
        expect(createSemantics.label, 'Tạo tài khoản');
        expect(loginSemantics.label, 'Đăng nhập');
        expect(
          createSemantics.getSemanticsData().hasAction(SemanticsAction.tap),
          isTrue,
        );
        expect(
          loginSemantics.getSemanticsData().hasAction(SemanticsAction.tap),
          isTrue,
        );

        final semanticsLabels = <String>[];
        void collectSemantics(SemanticsNode node) {
          final label = node.getSemanticsData().label;
          if (label.isNotEmpty) semanticsLabels.add(label);
          node.visitChildren((child) {
            collectSemantics(child);
            return true;
          });
        }

        collectSemantics(tester.getSemantics(scrollFinder));
        final expectedOrder = <String>[
          'CareBridge',
          _tagline,
          'Tạo tài khoản',
          'Đăng nhập',
          'Điều khoản',
          '•',
          'Quyền riêng tư',
          _disclaimer,
        ];
        var priorIndex = -1;
        for (final label in expectedOrder) {
          final index = semanticsLabels.indexOf(label);
          expect(index, greaterThan(priorIndex), reason: 'semantics: $label');
          priorIndex = index;
        }

        await tester.sendKeyEvent(LogicalKeyboardKey.tab);
        await tester.pump();
        expect(
          tester
              .getSemantics(create)
              .getSemanticsData()
              .flagsCollection
              .isFocused,
          Tristate.isTrue,
        );
        await tester.sendKeyEvent(LogicalKeyboardKey.tab);
        await tester.pump();
        expect(
          tester
              .getSemantics(login)
              .getSemanticsData()
              .flagsCollection
              .isFocused,
          Tristate.isTrue,
        );

        scrollable.position.jumpTo(scrollable.position.maxScrollExtent);
        await tester.pumpAndSettle();

        final disclaimer = find.text(_disclaimer);
        expect(disclaimer.hitTestable(), findsOneWidget);
        final disclaimerBounds = tester.getRect(disclaimer);
        expect(disclaimerBounds.top, greaterThanOrEqualTo(0));
        expect(
          disclaimerBounds.bottom,
          lessThanOrEqualTo(config.logicalSize.height),
        );
        expect(tester.takeException(), isNull);
        semantics.dispose();
      },
    );
  }

  testWidgets('all normal-size welcome text meets WCAG AA contrast', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: WelcomeScreen()));

    const canvas = Color(0xFFF6F1EC);
    final create = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, 'Tạo tài khoản'),
    );
    final createBackground = create.style?.backgroundColor?.resolve({});
    final createForeground = create.style?.foregroundColor?.resolve({});
    expect(createBackground, isNotNull);
    expect(createForeground, isNotNull);
    expect(
      _contrastRatio(createForeground!, createBackground!),
      greaterThanOrEqualTo(4.5),
    );

    for (final text in ['Điều khoản', 'Quyền riêng tư', _disclaimer]) {
      final color = tester.widget<Text>(find.text(text)).style?.color;
      expect(color, isNotNull, reason: text);
      expect(
        _contrastRatio(color!, canvas),
        greaterThanOrEqualTo(4.5),
        reason: text,
      );
    }
  });
}

double _contrastRatio(Color foreground, Color background) {
  final foregroundLuminance = foreground.computeLuminance();
  final backgroundLuminance = background.computeLuminance();
  final lighter = foregroundLuminance > backgroundLuminance
      ? foregroundLuminance
      : backgroundLuminance;
  final darker = foregroundLuminance > backgroundLuminance
      ? backgroundLuminance
      : foregroundLuminance;
  return (lighter + 0.05) / (darker + 0.05);
}
