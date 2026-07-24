import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/core/notifications/fcm_service.dart';
import 'package:untitled/features/consultation/screens/consultation_request_form_screen.dart';
import 'package:untitled/features/consultation/services/consultation_request_refresh_bus.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/expert/screens/expert_public_profile_screen.dart';
import 'package:untitled/features/notification/models/notification_model.dart';
import 'package:untitled/features/notification/routing/consultation_notification_routing.dart';
import 'package:untitled/features/notification/screens/notification_center_screen.dart';
import 'package:untitled/features/notification/services/notification_service.dart';

class _ProfileService extends DirectChatService {
  final bool eligible;
  _ProfileService(this.eligible);

  @override
  Future<Map<String, dynamic>> getExpertProfile(String expertProfileId) async =>
      {
        'expertProfileId': expertProfileId,
        'displayName': 'BS. Bình',
        'professionalTitle': 'Bác sĩ',
        'specialty': 'Nhi khoa',
        'verificationStatus': 'APPROVED',
        'consultationEligible': eligible,
      };

  @override
  Future<DirectConversation> findOrCreateConversation(String expertProfileId) =>
      throw UnimplementedError();
}

class _NotificationService extends NotificationService {
  final List<NotificationRecord> records;
  _NotificationService(this.records);

  @override
  Future<List<NotificationRecord>> getNotifications({
    String? type,
    int page = 0,
    int size = 50,
  }) async => records;

  @override
  Future<void> markAsRead(String notificationId) async {}
}

NotificationRecord _consultationNotification() => NotificationRecord(
  id: 'notification-1',
  userId: 'user-1',
  type: 'CONSULTATION',
  title: 'Yêu cầu tư vấn mới',
  body: 'Bạn có một yêu cầu tư vấn mới',
  referenceId: '123e4567-e89b-42d3-a456-426614174000',
  referenceType: 'CONSULTATION_REQUEST',
  status: 'SENT',
  createdAt: DateTime.utc(2026, 7, 16),
);

void main() {
  late DirectChatService originalChat;
  late NotificationService originalNotifications;

  setUp(() {
    originalChat = DirectChatService.instance;
    originalNotifications = NotificationService.instance;
  });
  tearDown(() {
    DirectChatService.instance = originalChat;
    NotificationService.instance = originalNotifications;
  });

  // CONREQ-FL-01
  testWidgets('eligible profile enables both chat and consultation CTAs', (
    tester,
  ) async {
    DirectChatService.instance = _ProfileService(true);
    final router = GoRouter(
      initialLocation: '/expert/public/expert-1',
      routes: [
        GoRoute(
          path: '/expert/public/:id',
          builder: (_, state) => ExpertPublicProfileScreen(
            expertProfileId: state.pathParameters['id']!,
          ),
        ),
      ],
    );
    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, 'Trò chuyện'))
          .onPressed,
      isNotNull,
    );
    await tester.tap(find.text('Yêu cầu tư vấn'));
    await tester.pumpAndSettle();
    expect(find.byType(ConsultationRequestFormScreen), findsOneWidget);
  });

  // CONREQ-FL-17
  testWidgets('approved but ineligible profile disables both CTAs', (
    tester,
  ) async {
    DirectChatService.instance = _ProfileService(false);
    await tester.pumpWidget(
      const MaterialApp(
        home: ExpertPublicProfileScreen(expertProfileId: 'expert-1'),
      ),
    );
    await tester.pumpAndSettle();
    expect(
      tester
          .widget<FilledButton>(find.widgetWithText(FilledButton, 'Trò chuyện'))
          .onPressed,
      isNull,
    );
    expect(
      tester
          .widget<OutlinedButton>(
            find.widgetWithText(OutlinedButton, 'Yêu cầu tư vấn'),
          )
          .onPressed,
      isNull,
    );
  });

  // CONREQ-FL-11
  test('foreground consultation push publishes refresh event', () async {
    var refreshes = 0;
    final sub = ConsultationRequestRefreshBus.events.listen((_) => refreshes++);
    FcmService.handleForegroundDataForTesting({
      'type': 'CONSULTATION_REQUEST',
      'requestId': '123e4567-e89b-42d3-a456-426614174000',
    });
    await Future<void>.delayed(Duration.zero);
    await sub.cancel();
    expect(refreshes, 1);
  });

  // CONREQ-FL-12
  test('consultation push resolves to request detail route', () {
    expect(
      FcmService.resolveTapRoute({
        'type': 'CONSULTATION_REQUEST',
        'requestId': '123e4567-e89b-42d3-a456-426614174000',
      }),
      '/consultation-requests/123e4567-e89b-42d3-a456-426614174000',
    );
  });

  // CONREQ-FL-15
  testWidgets('notification center recognizes CONSULTATION record type', (
    tester,
  ) async {
    NotificationService.instance = _NotificationService([
      _consultationNotification(),
    ]);
    await tester.pumpWidget(
      const MaterialApp(home: NotificationCenterScreen()),
    );
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.medical_services_outlined), findsOneWidget);
  });

  // CONREQ-FL-16
  test('notification routing uses referenceType and referenceId', () {
    expect(
      resolveNotificationRoute(_consultationNotification()),
      '/consultation-requests/123e4567-e89b-42d3-a456-426614174000',
    );
  });
}
