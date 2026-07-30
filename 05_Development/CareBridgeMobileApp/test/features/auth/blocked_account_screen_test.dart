import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/auth/blocked_account_state.dart';
import 'package:untitled/features/auth/screens/blocked_account_screen.dart';

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  tearDown(() {
    AuthState.instance.clearBlockedReason();
  });

  group('BlockedAccountScreen', () {
    testWidgets(
      'shows disabled title and message when reason is ACCOUNT_DISABLED',
      (tester) async {
        AuthState.instance.clearBlockedReason();
        // Set blocked reason synchronously via clearWithReason (sync part runs first)
        // ignore: unawaited_futures
        AuthState.instance.clearWithReason('ACCOUNT_DISABLED');

        await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
        await tester.pump();

        expect(find.text('Tài khoản đã bị vô hiệu hóa'), findsOneWidget);
        expect(
          find.text(
            'Tài khoản đã bị vô hiệu hóa và hiện không thể đăng nhập. Vui lòng liên hệ hỗ trợ nếu bạn cho rằng đây là nhầm lẫn.',
          ),
          findsOneWidget,
        );
        expect(find.text('Quay lại màn hình đăng nhập'), findsOneWidget);
      },
    );

    testWidgets(
      'shows administrative reason and appeal form for an appealable lock',
      (tester) async {
        // ignore: unawaited_futures
        AuthState.instance.clearWithBlockedAccount(
          const BlockedAccountState(
            code: 'ACCOUNT_ADMIN_LOCKED',
            lockType: 'ADMIN',
            reason: 'Vi phạm quy định cộng đồng',
            appealAllowed: true,
            appealToken: 'appeal-token',
          ),
        );

        await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
        await tester.pump();

        expect(
          find.text('Tài khoản bị khóa bởi quản trị viên'),
          findsOneWidget,
        );
        expect(find.text('Vi phạm quy định cộng đồng'), findsOneWidget);
        expect(find.text('Nội dung khiếu nại'), findsOneWidget);
        final submitButton = find.widgetWithText(
          FilledButton,
          'Gửi khiếu nại mở khóa',
          skipOffstage: false,
        );
        expect(submitButton, findsOneWidget);
      },
    );

    testWidgets('shows locked title and message when reason is ACCOUNT_LOCKED', (
      tester,
    ) async {
      // ignore: unawaited_futures
      AuthState.instance.clearWithReason('ACCOUNT_LOCKED');

      await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
      await tester.pump();

      expect(find.text('Tài khoản bị khóa tạm thời'), findsOneWidget);
      expect(
        find.text(
          'Tài khoản tạm thời bị khóa do có nhiều lần đăng nhập không thành công. Vui lòng thử lại sau 15 phút.',
        ),
        findsOneWidget,
      );
      expect(find.text('Quay lại màn hình đăng nhập'), findsOneWidget);
    });

    testWidgets('shows a rejected appeal status without the appeal form', (
      tester,
    ) async {
      // ignore: unawaited_futures
      AuthState.instance.clearWithBlockedAccount(
        const BlockedAccountState(
          code: 'ACCOUNT_ADMIN_LOCKED',
          appealAllowed: false,
          appealStatus: 'REJECTED',
        ),
      );

      await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
      await tester.pump();

      expect(
        find.textContaining('Khiếu nại mở khóa đã bị từ chối'),
        findsOneWidget,
      );
      expect(find.text('Nội dung khiếu nại'), findsNothing);
    });

    testWidgets('clearBlockedReason clears the blocked state', (tester) async {
      // ignore: unawaited_futures
      AuthState.instance.clearWithReason('ACCOUNT_DISABLED');
      expect(AuthState.instance.blockedReason, 'ACCOUNT_DISABLED');

      AuthState.instance.clearBlockedReason();
      expect(AuthState.instance.blockedReason, isNull);
    });
  });
}
