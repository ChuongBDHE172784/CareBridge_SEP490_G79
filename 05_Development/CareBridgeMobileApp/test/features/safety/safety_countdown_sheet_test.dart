import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/widgets/safety_countdown_sheet.dart';

class _FakeCountdownFeedback implements SafetyCountdownFeedback {
  int starts = 0;
  int stops = 0;
  final List<int> pulses = [];

  @override
  void start() => starts++;

  @override
  void pulse(int remainingSeconds) => pulses.add(remainingSeconds);

  @override
  void stop() => stops++;
}

void main() {
  late DateTime now;
  late SafetyEvent event;
  late _FakeCountdownFeedback feedback;

  setUp(() {
    now = DateTime.utc(2026, 7, 28, 10);
    event = SafetyEvent(
      id: 'event-1',
      eventType: 'SUSPECTED_FALL',
      magnitude: 30,
      status: 'OPEN',
      countdownDeadlineAt: now.add(const Duration(seconds: 30)),
    );
    feedback = _FakeCountdownFeedback();
  });

  Future<SafetyCountdownResult?> openSheet(WidgetTester tester) async {
    SafetyCountdownResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () async {
              result = await showModalBottomSheet<SafetyCountdownResult>(
                context: context,
                isDismissible: false,
                enableDrag: false,
                isScrollControlled: true,
                builder: (_) => SafetyCountdownSheet(
                  event: event,
                  feedback: feedback,
                  now: () => now,
                ),
              );
            },
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    return result;
  }

  testWidgets('offers safe, false-positive, and need-help actions', (
    tester,
  ) async {
    await openSheet(tester);

    expect(find.byKey(const Key('safety-countdown-safe')), findsOneWidget);
    expect(
      find.byKey(const Key('safety-countdown-false-positive')),
      findsOneWidget,
    );
    expect(find.byKey(const Key('safety-countdown-help')), findsOneWidget);
    expect(find.text('30'), findsOneWidget);
    expect(
      find.byKey(const Key('safety-countdown-large-timer')),
      findsOneWidget,
    );
    expect(feedback.starts, 1);
  });

  testWidgets('returns the selected false-positive reason', (tester) async {
    SafetyCountdownResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () async {
              result = await showModalBottomSheet<SafetyCountdownResult>(
                context: context,
                isScrollControlled: true,
                builder: (_) => SafetyCountdownSheet(
                  event: event,
                  feedback: feedback,
                  now: () => now,
                ),
              );
            },
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    final falsePositiveButton = find.byKey(
      const Key('safety-countdown-false-positive'),
    );
    await tester.ensureVisible(falsePositiveButton);
    await tester.tap(falsePositiveButton);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('false-positive-reason-exercise')));
    await tester.pumpAndSettle();

    expect(result?.action, SafetyCountdownAction.falsePositive);
    expect(result?.reasonCode, 'EXERCISE');
    expect(feedback.stops, 1);
  });

  testWidgets('times out at 30 seconds and always stops feedback', (
    tester,
  ) async {
    SafetyCountdownResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () async {
              result = await showModalBottomSheet<SafetyCountdownResult>(
                context: context,
                isScrollControlled: true,
                builder: (_) => SafetyCountdownSheet(
                  event: event,
                  feedback: feedback,
                  now: () => now,
                ),
              );
            },
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pump();

    now = now.add(const Duration(seconds: 29));
    await tester.pump(const Duration(seconds: 29));
    expect(result, isNull);

    now = now.add(const Duration(seconds: 1));
    await tester.pump(const Duration(seconds: 1));
    await tester.pumpAndSettle();

    expect(result?.action, SafetyCountdownAction.timeout);
    expect(feedback.pulses, isNotEmpty);
    expect(feedback.stops, 1);
  });

  testWidgets('timeout closes an open false-positive dialog and the sheet', (
    tester,
  ) async {
    SafetyCountdownResult? result;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => ElevatedButton(
            onPressed: () async {
              result = await showModalBottomSheet<SafetyCountdownResult>(
                context: context,
                isDismissible: false,
                enableDrag: false,
                isScrollControlled: true,
                builder: (_) => SafetyCountdownSheet(
                  event: event,
                  feedback: feedback,
                  now: () => now,
                ),
              );
            },
            child: const Text('open'),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    final falsePositiveButton = find.byKey(
      const Key('safety-countdown-false-positive'),
    );
    await tester.ensureVisible(falsePositiveButton);
    await tester.tap(falsePositiveButton);
    await tester.pumpAndSettle();
    expect(find.textContaining('cảnh báo nhầm'), findsOneWidget);

    now = now.add(const Duration(seconds: 30));
    await tester.pump(const Duration(seconds: 30));
    await tester.pumpAndSettle();

    expect(result?.action, SafetyCountdownAction.timeout);
    expect(find.byType(SimpleDialog), findsNothing);
    expect(feedback.stops, 1);
  });

  testWidgets('simulation is unmistakable and does not start alert feedback', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SafetyCountdownSheet(
            event: event,
            simulated: true,
            feedback: feedback,
            now: () => now,
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('MÔ PHỎNG AN TOÀN'), findsOneWidget);
    expect(find.textContaining('không gửi cảnh báo'), findsOneWidget);
    expect(feedback.starts, isZero);
    await tester.pumpWidget(const SizedBox.shrink());
    expect(feedback.stops, isZero);
  });

  testWidgets('demo gesture presents the production fall-alert experience', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SafetyCountdownSheet(
            event: event,
            simulated: true,
            presentAsRealAlert: true,
            feedback: feedback,
            now: () => now,
          ),
        ),
      ),
    );
    await tester.pump();

    expect(find.text('MÔ PHỎNG AN TOÀN'), findsNothing);
    expect(
      find.textContaining('phát hiện dấu hiệu nghi ngờ ngã'),
      findsOneWidget,
    );
    expect(
      find.byKey(const Key('safety-countdown-large-timer')),
      findsOneWidget,
    );
    expect(find.textContaining('gửi cảnh báo cho người thân'), findsOneWidget);
    expect(find.textContaining('bước gọi 115'), findsOneWidget);
    expect(find.text('Tôi vẫn ổn — tắt cảnh báo'), findsOneWidget);
    expect(feedback.starts, 1);

    await tester.pumpWidget(const SizedBox.shrink());
    expect(feedback.stops, 1);
  });

  testWidgets(
    'persisted sensor rehearsal shows real actions with safe banner',
    (tester) async {
      final rehearsal = SafetyEvent(
        id: 'self-test-1',
        eventType: 'SENSOR_SELF_TEST',
        magnitude: 17.2,
        status: 'TEST_OPEN',
        countdownDeadlineAt: now.add(const Duration(seconds: 30)),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SafetyCountdownSheet(
              event: rehearsal,
              feedback: feedback,
              now: () => now,
            ),
          ),
        ),
      );
      await tester.pump();

      expect(
        find.byKey(const Key('sensor-self-test-countdown-banner')),
        findsOneWidget,
      );
      expect(find.textContaining('KHÔNG GỬI CẢNH BÁO THẬT'), findsOneWidget);
      expect(find.text('Tôi vẫn ổn — tắt cảnh báo'), findsOneWidget);
      expect(find.byKey(const Key('safety-countdown-help')), findsOneWidget);
      expect(find.textContaining('luồng thật mới'), findsOneWidget);
      expect(feedback.starts, 1);

      await tester.pumpWidget(const SizedBox.shrink());
      expect(feedback.stops, 1);
    },
  );

  test('SystemSafetyCountdownFeedback lifecycle start, pulse, and stop complete safely', () {
    final systemFeedback = SystemSafetyCountdownFeedback();
    expect(() => systemFeedback.start(), returnsNormally);
    expect(() => systemFeedback.pulse(10), returnsNormally);
    expect(() => systemFeedback.pulse(5), returnsNormally);
    expect(() => systemFeedback.stop(), returnsNormally);
  });
}
