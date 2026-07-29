import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/directChat/models/direct_conversation.dart';
import 'package:untitled/features/directChat/models/expert_directory_item.dart';
import 'package:untitled/features/directChat/services/direct_chat_service.dart';
import 'package:untitled/features/directChat/services/conversation_refresh_bus.dart';
import 'package:untitled/features/expert/services/expert_home_service.dart';
import 'package:untitled/features/home/screens/expert_app_home_screen.dart';
import 'package:untitled/features/home/screens/expert_home_shell.dart';

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

class _FakeExpertHomeApi implements ExpertHomeApi {
  Completer<dynamic>? patchCompleter;
  Object? patchError;
  Map<String, dynamic>? lastPatchBody;

  @override
  Future<dynamic> get(String path) async {
    if (path.endsWith('/profiles/me')) {
      return {
        'data': {'professionalTitle': 'Chuyên gia dinh dưỡng'},
      };
    }
    if (path.endsWith('/availability/me')) return {'data': <dynamic>[]};
    if (path.contains('pending-summary')) {
      return {
        'data': {'pendingCount': 7},
      };
    }
    if (path.contains('assigned?')) return {'data': <dynamic>[]};
    if (path.contains('support-requests/open')) {
      return {
        'data': [
          {'description': 'Hỗ trợ tại nhà', 'supportType': 'ROUTINE'},
        ],
      };
    }
    if (path.contains('/community/questions')) {
      return {
        'data': [
          {'id': 'q1'},
          {'id': 'q2'},
        ],
      };
    }
    return {'data': <dynamic>[]};
  }

  @override
  Future<dynamic> patch(String path, Map<String, dynamic> body) {
    lastPatchBody = body;
    final error = patchError;
    if (error != null) return Future<dynamic>.error(error);
    return patchCompleter?.future ??
        Future<dynamic>.value({'message': 'Server đã xác nhận'});
  }
}

void main() {
  late DirectChatService originalDirectChat;
  late ExpertHomeService originalExpertHome;

  setUp(() {
    originalDirectChat = DirectChatService.instance;
    originalExpertHome = ExpertHomeService.instance;
  });

  tearDown(() {
    DirectChatService.instance = originalDirectChat;
    ExpertHomeService.instance = originalExpertHome;
  });

  // MEDI-FL-02
  testWidgets(
    'EXPERT bottom nav shows unread badge on Trò chuyện and retains direct chat navigation',
    (tester) async {
      DirectChatService.instance = _FakeDirectChatService(
        unreadConversationCount: 3,
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      expect(find.text('3'), findsOneWidget);
      expect(find.byType(NavigationBar), findsOneWidget);
      expect(find.text('Yêu cầu tư vấn'), findsNothing);
    },
  );

  testWidgets(
    'foreground conversation events refresh the Expert unread badge',
    (tester) async {
      final service = _FakeDirectChatService();
      DirectChatService.instance = service;
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
      DirectChatService.instance = _FakeDirectChatService();
      // The synchronous role/user assignment happens before the first secure-storage await.
      // ignore: unawaited_futures
      AuthState.instance.setTokens(
        accessToken: 'test-access',
        refreshToken: 'test-refresh',
        userId: 'expert-1',
        role: 'EXPERT',
      );

      await tester.pumpWidget(const MaterialApp(home: ExpertHomeShell()));
      await tester.pumpAndSettle();

      for (final label in const ['Tìm Mother', 'Nhắn tin mới']) {
        expect(find.text(label), findsNothing);
      }

      for (final tabLabel in const [
        'Trò chuyện',
        'Yêu cầu',
        'Lịch rảnh',
        'Tài khoản',
        'Tổng quan',
      ]) {
        await tester.tap(find.text(tabLabel).last);
        await tester.pumpAndSettle();
        for (final label in const ['Tìm Mother', 'Nhắn tin mới']) {
          expect(find.text(label), findsNothing);
        }
      }
    },
  );

  testWidgets(
    'online toggle waits for server confirmation and preserves snapshot fields',
    (tester) async {
      final api = _FakeExpertHomeApi()..patchCompleter = Completer<dynamic>();
      ExpertHomeService.instance = ExpertHomeService(api: api);

      await tester.pumpWidget(const MaterialApp(home: ExpertAppHomeScreen()));
      await tester.pumpAndSettle();
      expect(find.text('2'), findsOneWidget);
      expect(find.text('Hỗ trợ tại nhà'), findsOneWidget);
      expect(find.byIcon(Icons.close_rounded), findsOneWidget);

      await tester.tap(find.byKey(const Key('expert-online-toggle')));
      await tester.pump();

      expect(api.lastPatchBody, {'online': true});
      expect(find.byIcon(Icons.check_rounded), findsNothing);
      expect(find.text('2'), findsOneWidget);
      expect(find.text('Hỗ trợ tại nhà'), findsOneWidget);

      api.patchCompleter!.complete({'message': 'Đã bật từ máy chủ'});
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.check_rounded), findsOneWidget);
      expect(find.text('Đã bật từ máy chủ'), findsOneWidget);
      expect(find.text('2'), findsOneWidget);
      expect(find.text('Hỗ trợ tại nhà'), findsOneWidget);
    },
  );

  testWidgets(
    'failed online toggle stays unchanged and explains required location action',
    (tester) async {
      final api = _FakeExpertHomeApi()
        ..patchError = ApiException(
          409,
          '{"message":"Active location share is required"}',
        );
      ExpertHomeService.instance = ExpertHomeService(api: api);

      await tester.pumpWidget(const MaterialApp(home: ExpertAppHomeScreen()));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('expert-online-toggle')));
      await tester.pumpAndSettle();

      expect(find.byIcon(Icons.close_rounded), findsOneWidget);
      expect(find.byIcon(Icons.check_rounded), findsNothing);
      expect(
        find.text(
          'Hãy bật chia sẻ vị trí có thời hạn trước khi chuyển sang Trực tuyến.',
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets('online toggle uses the status confirmed by the server', (
    tester,
  ) async {
    final api = _FakeExpertHomeApi()..patchCompleter = Completer<dynamic>();
    ExpertHomeService.instance = ExpertHomeService(api: api);

    await tester.pumpWidget(const MaterialApp(home: ExpertAppHomeScreen()));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('expert-online-toggle')));
    await tester.pump();

    api.patchCompleter!.complete({
      'message': 'Máy chủ giữ trạng thái Ngoại tuyến',
      'data': {'availabilityStatus': 'OFFLINE'},
    });
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.close_rounded), findsOneWidget);
    expect(find.byIcon(Icons.check_rounded), findsNothing);
    expect(find.text('Máy chủ giữ trạng thái Ngoại tuyến'), findsOneWidget);
  });
}
