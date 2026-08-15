import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/screens/journey_setup_screen.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

class _FailingJourneyService extends JourneyService {
  _FailingJourneyService(this.error);

  final ApiException error;

  @override
  Future<CreateJourneyResponse> createJourney(
    CreateJourneyRequest request,
  ) async {
    throw error;
  }

  @override
  Future<void> updateJourney(
    String journeyId,
    UpdateJourneyRequest request,
  ) async {
    throw error;
  }
}

class _SuccessfulJourneyService extends JourneyService {
  @override
  Future<void> updateJourney(
    String journeyId,
    UpdateJourneyRequest request,
  ) async {}
}

class _CapturingJourneyService extends JourneyService {
  CreateJourneyRequest? createdRequest;
  UpdateJourneyRequest? updatedRequest;

  @override
  Future<CreateJourneyResponse> createJourney(
    CreateJourneyRequest request,
  ) async {
    createdRequest = request;
    throw ApiException(400, '{"error":"TEST_CAPTURE"}');
  }

  @override
  Future<void> updateJourney(
    String journeyId,
    UpdateJourneyRequest request,
  ) async {
    updatedRequest = request;
    throw ApiException(400, '{"error":"TEST_CAPTURE"}');
  }
}

Future<void> _submitEdit(WidgetTester tester, ApiException error) async {
  return _submit(tester, error, isEditMode: true);
}

Future<void> _submit(
  WidgetTester tester,
  ApiException error, {
  required bool isEditMode,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      home: JourneySetupScreen(
        journeyId: isEditMode ? '00000000-0000-0000-0000-000000610210' : null,
        isEditMode: isEditMode,
        service: _FailingJourneyService(error),
      ),
    ),
  );

  await _advanceToSubmit(tester);
}

Future<void> _advanceToSubmit(WidgetTester tester) async {
  final methodCard = find.byKey(const Key('dating-method-gestational-age'));
  await tester.ensureVisible(methodCard);
  await tester.tap(methodCard);
  await tester.pumpAndSettle();
  await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
  await tester.pumpAndSettle();
  await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
  await tester.pumpAndSettle();
  await tester.tap(find.widgetWithText(FilledButton, 'Tạo hành trình'));
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 700));
  await tester.pumpAndSettle();
}

Future<void> _chooseCalendarDate(WidgetTester tester) async {
  final today = DateTime.now().day.toString();
  final day = find.text(today);
  expect(day, findsWidgets);
  await tester.tap(day.last);
  await tester.pump();
}

void main() {
  testWidgets('LMP, EDD, and gestational age dating choices are shown', (
    tester,
  ) async {
    await tester.pumpWidget(const MaterialApp(home: JourneySetupScreen()));
    await tester.pump();
    expect(find.byKey(const Key('dating-method-lmp')), findsOneWidget);
    expect(find.byKey(const Key('dating-method-due-date')), findsOneWidget);
    expect(
      find.byKey(const Key('dating-method-gestational-age')),
      findsOneWidget,
    );
    expect(find.byKey(const Key('dating-method-conception')), findsNothing);
    expect(find.textContaining('Sẽ quy đổi thành EDD.'), findsNothing);
  });

  testWidgets(
    'gestational age setup calculates correct LMP date and sends LMP dating basis',
    (tester) async {
      final service = _CapturingJourneyService();
      await tester.pumpWidget(
        MaterialApp(
          home: JourneySetupScreen(service: service, refreshSession: () async {}),
        ),
      );
      await tester.pump();

      final card = find.byKey(const Key('dating-method-gestational-age'));
      await tester.ensureVisible(card);
      await tester.tap(card);
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('wheel-gestational-weeks')), findsOneWidget);
      expect(find.byKey(const Key('wheel-gestational-days')), findsOneWidget);
      await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
      await tester.pumpAndSettle();
      expect(
        find.byWidgetPredicate(
          (widget) =>
              widget is RichText &&
              widget.text.toPlainText().contains('12 tuần 0 ngày'),
        ),
        findsOneWidget,
      );
      await tester.tap(find.widgetWithText(FilledButton, 'Tạo hành trình'));
      await tester.pump(const Duration(milliseconds: 700));

      expect(service.createdRequest, isNotNull);
      expect(service.createdRequest!.datingBasis, 'LMP');
      expect(service.createdRequest!.lastMenstrualDate, isNotNull);
      expect(service.createdRequest!.estimatedDueDate, isNull);
      expect(service.createdRequest!.dateSource, 'SELF_REPORTED');
      expect(service.createdRequest!.dateConfidence, 'ESTIMATED');
      expect(service.createdRequest!.notes, contains('Gestational age: 12w 0d'));
    },
  );

  testWidgets('EDD setup sends exactly one EDD date and dating basis', (
    tester,
  ) async {
    final service = _CapturingJourneyService();
    await tester.pumpWidget(
      MaterialApp(
        home: JourneySetupScreen(service: service, refreshSession: () async {}),
      ),
    );
    await tester.pump();

    final card = find.byKey(const Key('dating-method-due-date'));
    await tester.ensureVisible(card);
    await tester.tap(card);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
    await tester.pumpAndSettle();
    await _chooseCalendarDate(tester);
    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Tạo hành trình'));
    await tester.pump(const Duration(milliseconds: 700));

    expect(service.createdRequest, isNotNull);
    expect(service.createdRequest!.datingBasis, 'EDD');
    expect(service.createdRequest!.estimatedDueDate, isNotNull);
    expect(service.createdRequest!.lastMenstrualDate, isNull);
    expect(service.createdRequest!.dateSource, 'SELF_REPORTED');
    expect(service.createdRequest!.dateConfidence, 'ESTIMATED');
  });

  testWidgets('LMP setup sends exactly one LMP date and dating basis', (
    tester,
  ) async {
    final service = _CapturingJourneyService();
    await tester.pumpWidget(
      MaterialApp(
        home: JourneySetupScreen(service: service, refreshSession: () async {}),
      ),
    );
    await tester.pump();

    final card = find.byKey(const Key('dating-method-lmp'));
    await tester.ensureVisible(card);
    await tester.tap(card);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
    await tester.pumpAndSettle();
    await _chooseCalendarDate(tester);
    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp theo'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Tạo hành trình'));
    await tester.pump(const Duration(milliseconds: 700));

    expect(service.createdRequest, isNotNull);
    expect(service.createdRequest!.datingBasis, 'LMP');
    expect(service.createdRequest!.lastMenstrualDate, isNotNull);
    expect(service.createdRequest!.estimatedDueDate, isNull);
    expect(service.createdRequest!.dateSource, 'SELF_REPORTED');
    expect(service.createdRequest!.dateConfidence, 'ESTIMATED');
  });

  testWidgets('consent-invalid 409 shows neutral actionable guidance', (
    tester,
  ) async {
    await _submitEdit(
      tester,
      ApiException(
        409,
        '{"error":"LIFECYCLE_CONSENT_INVALID",'
        '"message":"Your lifecycle consent needs to be reviewed"}',
      ),
    );

    expect(
      find.text(
        'Quyền đồng ý cho hành trình này không còn hiệu lực. '
        'Vui lòng xác nhận lại quyền đồng ý rồi thử lại.',
      ),
      findsOneWidget,
    );
    final semanticsHandle = tester.ensureSemantics();
    final node = tester.getSemantics(
      find.bySemanticsLabel(
        'Quyền đồng ý cho hành trình này không còn hiệu lực. '
        'Vui lòng xác nhận lại quyền đồng ý rồi thử lại.',
      ),
    );
    expect(node.flagsCollection.isLiveRegion, isTrue);
    semanticsHandle.dispose();
  });

  testWidgets('generic update errors keep the existing retry guidance', (
    tester,
  ) async {
    await _submitEdit(
      tester,
      ApiException(
        409,
        '{"error":"JOURNEY-017","message":"Concurrent update"}',
      ),
    );

    expect(
      find.text('Không thể cập nhật hành trình. Vui lòng thử lại.'),
      findsOneWidget,
    );
    expect(find.textContaining('Quyền đồng ý'), findsNothing);
  });

  testWidgets('consent-invalid 409 also shows guidance in create mode', (
    tester,
  ) async {
    await _submit(
      tester,
      ApiException(
        409,
        '{"error":"LIFECYCLE_CONSENT_INVALID",'
        '"message":"Your lifecycle consent needs to be reviewed"}',
      ),
      isEditMode: false,
    );

    expect(
      find.text(
        'Quyền đồng ý cho hành trình này không còn hiệu lực. '
        'Vui lòng xác nhận lại quyền đồng ý rồi thử lại.',
      ),
      findsOneWidget,
    );
    expect(find.text('Bạn đã có một hành trình đang hoạt động.'), findsNothing);
  });

  testWidgets('canonical conflict keeps the existing create guidance', (
    tester,
  ) async {
    await _submit(
      tester,
      ApiException(
        409,
        '{"error":"JOURNEY-015","message":"Active journey exists"}',
      ),
      isEditMode: false,
    );

    expect(
      find.text('Bạn đã có một hành trình đang hoạt động.'),
      findsOneWidget,
    );
    expect(find.textContaining('quyền đồng ý'), findsNothing);
  });

  testWidgets('missing-consent 409 shows guidance in update mode', (
    tester,
  ) async {
    await _submitEdit(
      tester,
      ApiException(
        409,
        '{"error":"LIFECYCLE_CONSENT_REQUIRED",'
        '"message":"Lifecycle consent is required"}',
      ),
    );

    expect(
      find.text(
        'Bạn cần xác nhận quyền đồng ý cho hành trình trước khi tiếp tục.',
      ),
      findsOneWidget,
    );
    expect(find.textContaining('Không thể cập nhật'), findsNothing);
  });

  testWidgets('missing-consent 409 shows guidance in create mode', (
    tester,
  ) async {
    await _submit(
      tester,
      ApiException(
        409,
        '{"error":"LIFECYCLE_CONSENT_REQUIRED",'
        '"message":"Lifecycle consent is required"}',
      ),
      isEditMode: false,
    );

    expect(
      find.text(
        'Bạn cần xác nhận quyền đồng ý cho hành trình trước khi tiếp tục.',
      ),
      findsOneWidget,
    );
    expect(find.text('Bạn đã có một hành trình đang hoạt động.'), findsNothing);
  });

  testWidgets(
    'refresh failure after update still navigates without retry error',
    (tester) async {
      final router = GoRouter(
        initialLocation: '/probe',
        routes: [
          GoRoute(path: '/probe', builder: (_, _) => const Text('probe')),
          GoRoute(
            path: '/setup',
            builder: (_, _) => JourneySetupScreen(
              journeyId: '00000000-0000-0000-0000-000000610210',
              isEditMode: true,
              service: _SuccessfulJourneyService(),
              refreshSession: () async => throw StateError('refresh failed'),
            ),
          ),
        ],
      );
      addTearDown(router.dispose);
      await tester.pumpWidget(MaterialApp.router(routerConfig: router));
      unawaited(router.push('/setup'));
      await tester.pumpAndSettle();

      await _advanceToSubmit(tester);
      expect(find.text('probe'), findsOneWidget);
      expect(find.textContaining('Lỗi kết nối'), findsNothing);
      expect(find.textContaining('Không thể cập nhật'), findsNothing);
    },
  );
}
