import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/features/auth/screens/blocked_account_screen.dart';

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  tearDown(() {
    AuthState.instance.clearBlockedReason();
  });

  group('BlockedAccountScreen', () {
    testWidgets('shows disabled title and message when reason is ACCOUNT_DISABLED', (tester) async {
      AuthState.instance.clearBlockedReason();
      // Set blocked reason synchronously via clearWithReason (sync part runs first)
      // ignore: unawaited_futures
      AuthState.instance.clearWithReason('ACCOUNT_DISABLED');

      await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
      await tester.pump();

      expect(find.text('Tài khoản bị vô hiệu hoá'), findsOneWidget);
      expect(find.text('Quay lại đăng nhập'), findsOneWidget);
    });

    testWidgets('shows locked title and message when reason is ACCOUNT_LOCKED', (tester) async {
      // ignore: unawaited_futures
      AuthState.instance.clearWithReason('ACCOUNT_LOCKED');

      await tester.pumpWidget(_wrap(const BlockedAccountScreen()));
      await tester.pump();

      expect(find.text('Tài khoản bị khoá tạm thời'), findsOneWidget);
      expect(find.text('Quay lại đăng nhập'), findsOneWidget);
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
