import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/auth/blocked_account_state.dart';
import 'package:untitled/core/constants/support_contact.dart';
import 'package:untitled/features/auth/screens/blocked_account_screen.dart';

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    FlutterSecureStorage.setMockInitialValues({});
  });

  tearDown(() {
    AuthState.instance.clearBlockedReason();
  });

  group('BlockedAccountScreen', () {
    testWidgets(
      'shows disabled title and message when reason is ACCOUNT_DISABLED',
      (tester) async {
        AuthState.instance.clearBlockedReason();
        await AuthState.instance.clearWithReason('ACCOUNT_DISABLED');

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
      'shows administrative reason and the customer-support contact',
      (tester) async {
        await AuthState.instance.clearWithBlockedAccount(
          const BlockedAccountState(
            code: 'ACCOUNT_ADMIN_LOCKED',
            lockType: 'ADMIN',
            reason: 'Vi phạm quy định cộng đồng',
          ),
        );

        await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
        await tester.pump();

        expect(
          find.text('Tài khoản bị khóa bởi quản trị viên'),
          findsOneWidget,
        );
        expect(find.text('Vi phạm quy định cộng đồng'), findsOneWidget);
        // The appeal form is gone; support contact is the only route back.
        expect(find.text('CẦN MỞ LẠI TÀI KHOẢN?'), findsOneWidget);
        expect(find.text(supportEmail), findsOneWidget);
        expect(find.text('Nội dung khiếu nại'), findsNothing);
      },
    );

    testWidgets('shows locked title and message when reason is ACCOUNT_LOCKED', (
      tester,
    ) async {
      await AuthState.instance.clearWithReason('ACCOUNT_LOCKED');

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

    testWidgets('does not offer support contact for a temporary lock', (
      tester,
    ) async {
      await AuthState.instance.clearWithBlockedAccount(
        const BlockedAccountState(
          code: 'ACCOUNT_TEMPORARILY_LOCKED',
          lockType: 'TEMPORARY',
        ),
      );

      await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
      await tester.pump();

      // It clears by itself, so sending the user to support would be wrong.
      expect(find.text('CẦN MỞ LẠI TÀI KHOẢN?'), findsNothing);
    });

    testWidgets('clearBlockedReason clears the blocked state', (tester) async {
      await AuthState.instance.clearWithReason('ACCOUNT_DISABLED');
      expect(AuthState.instance.blockedReason, 'ACCOUNT_DISABLED');

      AuthState.instance.clearBlockedReason();
      expect(AuthState.instance.blockedReason, isNull);
    });
  });
}
