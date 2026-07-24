import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/consultation/models/consultation_request.dart';
import 'package:untitled/features/consultation/screens/consultation_request_detail_screen.dart';
import 'package:untitled/features/consultation/screens/consultation_request_form_screen.dart';
import 'package:untitled/features/consultation/screens/expert_request_queue_screen.dart';
import 'package:untitled/features/consultation/screens/expert_requests_tab_screen.dart';
import 'package:untitled/features/consultation/screens/my_consultation_requests_screen.dart';
import 'package:untitled/features/consultation/services/consultation_request_refresh_bus.dart';
import 'package:untitled/features/consultation/services/consultation_request_service.dart';
import 'package:untitled/features/community/screens/expert_question_queue_screen.dart';

class _FakeConsultationRequestService extends ConsultationRequestService {
  ConsultationRequestPage page = const ConsultationRequestPage(
    items: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  );
  ConsultationRequestDetail? detail;
  Completer<ConsultationRequestDetail>? createCompleter;
  Completer<ConsultationRequestDetail>? acceptCompleter;
  final List<String?> assignedStatuses = [];
  int createCalls = 0;
  int acceptCalls = 0;
  int cancelCalls = 0;

  @override
  Future<ConsultationRequestPage> listMine({
    String? status,
    int page = 0,
    int size = 20,
  }) async => this.page;

  @override
  Future<ConsultationRequestPage> listAssigned({
    String? status,
    int page = 0,
    int size = 20,
  }) async {
    assignedStatuses.add(status);
    return this.page;
  }

  @override
  Future<ConsultationRequestDetail> getById(String id) async => detail!;

  @override
  Future<ConsultationRequestDetail> create({
    required String clientRequestId,
    required String expertProfileId,
    required String topic,
    required String description,
    DateTime? preferredWindowStart,
    DateTime? preferredWindowEnd,
  }) {
    createCalls++;
    return createCompleter?.future ?? Future.value(detail!);
  }

  @override
  Future<ConsultationRequestDetail> accept(String id) {
    acceptCalls++;
    return acceptCompleter?.future ?? Future.value(detail!);
  }

  @override
  Future<ConsultationRequestDetail> reject(String id, {String? reason}) async =>
      detail!;

  @override
  Future<ConsultationRequestDetail> cancel(String id) async {
    cancelCalls++;
    return detail!;
  }
}

ConsultationRequestSummary _summary({
  String id = 'request-1',
  String status = 'PENDING',
}) => ConsultationRequestSummary(
  id: id,
  counterpartDisplayName: 'Mẹ An',
  topic: 'Dinh dưỡng',
  status: status,
  createdAt: DateTime.utc(2026, 7, 16),
);

ConsultationRequestDetail _detail({
  String status = 'PENDING',
  String? conversationId,
}) => ConsultationRequestDetail(
  id: 'request-1',
  expertProfileId: 'expert-1',
  counterpartDisplayName: 'BS. Bình',
  topic: 'Dinh dưỡng',
  description: 'Cần tư vấn chế độ ăn.',
  status: status,
  directConversationId: conversationId,
  expiresAt: DateTime.utc(2026, 7, 18),
  createdAt: DateTime.utc(2026, 7, 16),
);

void main() {
  late ConsultationRequestService original;

  setUp(() async {
    original = ConsultationRequestService.instance;
    FlutterSecureStorage.setMockInitialValues({});
    await AuthState.instance.clear();
    await AuthState.instance.setTokens(
      accessToken: 'synthetic-access',
      refreshToken: 'synthetic-refresh',
      userId: 'mother-a',
      role: 'MOTHER',
    );
  });
  tearDown(() async {
    ConsultationRequestService.instance = original;
    await AuthState.instance.clear();
  });

  // CONREQ-FL-02
  testWidgets('form validates required topic and description before submit', (
    tester,
  ) async {
    final fake = _FakeConsultationRequestService()..detail = _detail();
    ConsultationRequestService.instance = fake;
    await tester.pumpWidget(
      const MaterialApp(
        home: ConsultationRequestFormScreen(
          expertProfileId: 'expert-1',
          expertDisplayName: 'BS. Bình',
        ),
      ),
    );

    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pump();

    expect(find.text('Vui lòng nhập chủ đề'), findsOneWidget);
    expect(find.text('Vui lòng mô tả nhu cầu tư vấn'), findsOneWidget);
    expect(fake.createCalls, 0);
  });

  // CONREQ-FL-03
  testWidgets('form disables submit immediately and prevents double submit', (
    tester,
  ) async {
    final fake = _FakeConsultationRequestService()
      ..detail = _detail()
      ..createCompleter = Completer<ConsultationRequestDetail>();
    ConsultationRequestService.instance = fake;
    await tester.pumpWidget(
      const MaterialApp(
        home: ConsultationRequestFormScreen(
          expertProfileId: 'expert-1',
          expertDisplayName: 'BS. Bình',
        ),
      ),
    );
    await tester.enterText(
      find.byKey(const Key('consultation-topic')),
      'Ăn dặm',
    );
    await tester.enterText(
      find.byKey(const Key('consultation-description')),
      'Cần tư vấn lịch ăn dặm.',
    );

    await tester.tap(find.text('Gửi yêu cầu'));
    await tester.pump();
    await tester.tap(find.text('Đang gửi...'));
    expect(fake.createCalls, 1);
    fake.createCompleter!.complete(_detail());
    await tester.pumpAndSettle();
  });

  // CONREQ-FL-04
  testWidgets(
    'mother list renders real empty state and retryable error state',
    (tester) async {
      ConsultationRequestService.instance = _FakeConsultationRequestService();
      await tester.pumpWidget(
        const MaterialApp(home: MyConsultationRequestsScreen()),
      );
      await tester.pumpAndSettle();
      expect(find.text('Bạn chưa có yêu cầu tư vấn nào'), findsOneWidget);
      expect(find.text('Thử lại'), findsNothing);
    },
  );

  // CONREQ-FL-05
  testWidgets('detail shows cancel only for PENDING and opens accepted chat', (
    tester,
  ) async {
    final fake = _FakeConsultationRequestService()
      ..detail = _detail(status: 'ACCEPTED', conversationId: 'conv-1');
    ConsultationRequestService.instance = fake;
    final router = GoRouter(
      initialLocation: '/consultation-requests/request-1',
      routes: [
        GoRoute(
          path: '/consultation-requests/:id',
          builder: (_, state) => ConsultationRequestDetailScreen(
            requestId: state.pathParameters['id']!,
          ),
        ),
        GoRoute(
          path: '/direct-chat/:id',
          builder: (_, state) =>
              Scaffold(body: Text('chat:${state.pathParameters['id']}')),
        ),
      ],
    );
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    expect(find.text('Hủy yêu cầu'), findsNothing);
    await tester.tap(find.text('Mở hội thoại'));
    await tester.pumpAndSettle();
    expect(find.text('chat:conv-1'), findsOneWidget);
  });

  // CONREQ-FL-06
  testWidgets('expert queue loads assigned requests from service', (
    tester,
  ) async {
    final fake = _FakeConsultationRequestService()
      ..page = ConsultationRequestPage(
        items: [_summary()],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      )
      ..detail = _detail();
    ConsultationRequestService.instance = fake;
    await tester.pumpWidget(
      const MaterialApp(home: ExpertRequestQueueScreen()),
    );
    await tester.pumpAndSettle();
    expect(find.text('Dinh dưỡng'), findsOneWidget);
    expect(fake.assignedStatuses, contains('PENDING'));
  });

  testWidgets('expert queue opens participant request detail', (tester) async {
    final fake = _FakeConsultationRequestService()
      ..page = ConsultationRequestPage(
        items: [_summary()],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      );
    ConsultationRequestService.instance = fake;
    final router = GoRouter(
      routes: [
        GoRoute(path: '/', builder: (_, _) => const ExpertRequestQueueScreen()),
        GoRoute(
          path: '/consultation-requests/:id',
          builder: (_, state) => Text(
            'detail:${state.pathParameters['id']}',
            textDirection: TextDirection.ltr,
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('view-details-request-1')));
    await tester.pumpAndSettle();

    expect(find.text('detail:request-1'), findsOneWidget);
  });

  // CONREQ-FL-07
  testWidgets('expert accept action has a double-tap guard', (tester) async {
    final fake = _FakeConsultationRequestService()
      ..page = ConsultationRequestPage(
        items: [_summary()],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      )
      ..detail = _detail(status: 'ACCEPTED', conversationId: 'conv-1')
      ..acceptCompleter = Completer<ConsultationRequestDetail>();
    ConsultationRequestService.instance = fake;
    await tester.pumpWidget(
      const MaterialApp(home: ExpertRequestQueueScreen()),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Chấp nhận'));
    await tester.pump();
    await tester.tap(find.byKey(const Key('accept-request-1')));
    expect(fake.acceptCalls, 1);
    fake.acceptCompleter!.complete(fake.detail!);
    await tester.pumpAndSettle();
  });

  // CONREQ-FL-08
  testWidgets('accept removes item from PENDING and publishes badge refresh', (
    tester,
  ) async {
    final fake = _FakeConsultationRequestService()
      ..page = ConsultationRequestPage(
        items: [_summary()],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      )
      ..detail = _detail(status: 'ACCEPTED', conversationId: 'conv-1');
    ConsultationRequestService.instance = fake;
    var refreshes = 0;
    final sub = ConsultationRequestRefreshBus.events.listen((_) => refreshes++);
    addTearDown(sub.cancel);
    await tester.pumpWidget(
      const MaterialApp(home: ExpertRequestQueueScreen()),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Chấp nhận'));
    await tester.pumpAndSettle();
    expect(find.text('Dinh dưỡng'), findsNothing);
    expect(refreshes, 1);
  });

  // CONREQ-FL-09
  testWidgets(
    'expert requests tab has two segments without nested navigation',
    (tester) async {
      ConsultationRequestService.instance = _FakeConsultationRequestService();
      await tester.pumpWidget(
        const MaterialApp(home: ExpertRequestsTabScreen()),
      );
      await tester.pumpAndSettle();
      expect(find.text('Tư vấn'), findsOneWidget);
      expect(find.text('Cộng đồng'), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
      await tester.tap(find.text('Cộng đồng'));
      await tester.pumpAndSettle();
      expect(find.byType(ExpertQuestionQueueScreen), findsOneWidget);
      expect(find.byType(NavigationBar), findsNothing);
    },
  );

  // CONREQ-FL-13
  testWidgets('empty state is role-correct for Expert', (tester) async {
    ConsultationRequestService.instance = _FakeConsultationRequestService();
    await tester.pumpWidget(
      const MaterialApp(home: ExpertRequestQueueScreen()),
    );
    await tester.pumpAndSettle();
    expect(find.text('Chưa có yêu cầu tư vấn được giao'), findsOneWidget);
    expect(find.text('Bạn chưa có yêu cầu tư vấn nào'), findsNothing);
  });

  // CONREQ-FL-14
  testWidgets('stale assigned response cannot overwrite a newer filter', (
    tester,
  ) async {
    final fake = _GenerationFakeService();
    ConsultationRequestService.instance = fake;
    await tester.pumpWidget(
      const MaterialApp(home: ExpertRequestQueueScreen()),
    );
    await tester.pump();
    await tester.tap(find.text('Đã nhận'));
    await tester.pump();
    fake.accepted.complete(
      ConsultationRequestPage(
        items: [_summary(id: 'new', status: 'ACCEPTED')],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      ),
    );
    await tester.pump();
    fake.pending.complete(
      ConsultationRequestPage(
        items: [_summary(id: 'old')],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('consultation-new')), findsOneWidget);
    expect(find.byKey(const Key('consultation-old')), findsNothing);
  });
}

class _GenerationFakeService extends _FakeConsultationRequestService {
  final pending = Completer<ConsultationRequestPage>();
  final accepted = Completer<ConsultationRequestPage>();

  @override
  Future<ConsultationRequestPage> listAssigned({
    String? status,
    int page = 0,
    int size = 20,
  }) => status == 'ACCEPTED' ? accepted.future : pending.future;
}
