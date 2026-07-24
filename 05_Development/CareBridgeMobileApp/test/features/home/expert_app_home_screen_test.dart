import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';
import 'package:untitled/features/home/screens/expert_home_shell.dart';
import 'package:untitled/features/consultation/models/consultation_request.dart';
import 'package:untitled/features/consultation/screens/expert_requests_tab_screen.dart';
import 'package:untitled/features/consultation/services/consultation_request_service.dart';

class _FakeDirectChatService extends DirectChatService {
  int unreadConversationCount;
  _FakeDirectChatService({this.unreadConversationCount = 0});

  @override
  Future<List<DirectConversationSummary>> listMyConversations() async =>
      const [];

  @override
  Future<UnreadSummary> getUnreadSummary() async => UnreadSummary(
    unreadConversationCount: unreadConversationCount,
    totalUnreadMessageCount: unreadConversationCount,
  );

  @override
  Future<ExpertDirectoryPage> getExpertDirectory({
    String? q,
    String? specialty,
    int page = 0,
    int size = 20,
  }) async => const ExpertDirectoryPage(
    experts: [],
    currentPage: 0,
    pageSize: 20,
    totalElements: 0,
    totalPages: 0,
  );
}

class _FakeConsultationService extends ConsultationRequestService {
  @override
  Future<int> pendingCount() async => 0;

  @override
  Future<ConsultationRequestPage> listAssigned({
    String? status,
    int page = 0,
    int size = 20,
  }) async => const ConsultationRequestPage(
    items: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  );
}

void main() {
  late DirectChatService original;
  late ConsultationRequestService originalConsultation;

  tearDown(() {
    DirectChatService.instance = original;
    ConsultationRequestService.instance = originalConsultation;
  });

  // MEDI-FL-02
  testWidgets(
    'EXPERT bottom nav shows unread badge on Trò chuyện; Yêu cầu tab actually navigates (no longer a no-op)',
    (tester) async {
      original = DirectChatService.instance;
      originalConsultation = ConsultationRequestService.instance;
      DirectChatService.instance = _FakeDirectChatService(
        unreadConversationCount: 3,
      );
      ConsultationRequestService.instance = _FakeConsultationService();

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      expect(
        find.text('3'),
        findsOneWidget,
      ); // Badge label on the Trò chuyện destination

      await tester.tap(find.text('Yêu cầu tư vấn').last);
      await tester.pumpAndSettle();

      expect(find.byType(ExpertRequestsTabScreen), findsOneWidget);
      expect(find.text('Tư vấn'), findsOneWidget);
      expect(find.text('Cộng đồng'), findsOneWidget);
      expect(find.byType(NavigationBar), findsOneWidget);
    },
  );

  testWidgets(
    'foreground conversation events refresh the Expert unread badge',
    (tester) async {
      original = DirectChatService.instance;
      originalConsultation = ConsultationRequestService.instance;
      final service = _FakeDirectChatService();
      DirectChatService.instance = service;
      ConsultationRequestService.instance = _FakeConsultationService();
      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      service.unreadConversationCount = 4;
      ConversationRefreshBus.notify();
      await tester.pumpAndSettle();
      expect(find.text('4'), findsOneWidget);
    },
  );

  // MEDI-FL-11
  testWidgets(
    'EXPERT shell has no "find/message Mother" CTA anywhere across its tabs',
    (tester) async {
      original = DirectChatService.instance;
      originalConsultation = ConsultationRequestService.instance;
      DirectChatService.instance = _FakeDirectChatService();
      ConsultationRequestService.instance = _FakeConsultationService();
      FlutterSecureStorage.setMockInitialValues({});
      await AuthState.instance.setTokens(
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        userId: 'expert-1',
        role: 'EXPERT',
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      for (final label in const [
        'Tìm chuyên gia',
        'Tìm Mother',
        'Nhắn tin mới',
      ]) {
        expect(find.text(label), findsNothing);
      }

      // Visit every tab and re-check — IndexedStack keeps them all built, but assert per tab too.
      for (final tabLabel in const [
        'Trò chuyện',
        'Yêu cầu tư vấn',
        'Lịch',
        'Tài khoản',
        'Tổng quan',
      ]) {
        await tester.tap(find.text(tabLabel).last);
        await tester.pumpAndSettle();
        for (final label in const [
          'Tìm chuyên gia',
          'Tìm Mother',
          'Nhắn tin mới',
        ]) {
          expect(find.text(label), findsNothing);
        }
      }
    },
  );
}
