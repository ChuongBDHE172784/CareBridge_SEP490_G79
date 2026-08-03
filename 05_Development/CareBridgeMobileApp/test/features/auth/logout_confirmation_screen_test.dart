import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/core/storage/token_storage.dart';
import 'package:untitled/features/auth/screens/logout_confirmation_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test(
    'successful logout clears draft and captured auth exactly once',
    () async {
      var draftClears = 0;
      var authClears = 0;

      await clearLocalSessionAfterLogout(
        accountId: 'account-a',
        isCapturedSessionCurrent: () => true,
        clearDraft: (_) async => draftClears++,
        clearAuth: () async => authClears++,
      );

      expect(draftClears, 1);
      expect(authClears, 1);
    },
  );

  test('logout cleanup never clears a replacement account', () async {
    var draftClears = 0;
    var authClears = 0;

    await clearLocalSessionAfterLogout(
      accountId: 'account-a',
      isCapturedSessionCurrent: () => false,
      clearDraft: (_) async => draftClears++,
      clearAuth: () async => authClears++,
    );

    expect(draftClears, 1);
    expect(authClears, 0);
  });

  testWidgets('back, barrier and drag cannot dismiss pending logout', (
    tester,
  ) async {
    final logout = Completer<void>();
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () => showModalBottomSheet<bool>(
                context: context,
                isScrollControlled: true,
                isDismissible: false,
                enableDrag: false,
                builder: (_) =>
                    LogoutConfirmationSheet(onLogout: () => logout.future),
              ),
              child: const Text('Open'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng xuất'));
    await tester.pump();

    await tester.tapAt(const Offset(8, 8));
    await tester.pump();
    expect(find.byType(LogoutConfirmationSheet), findsOneWidget);

    await tester.drag(
      find.byType(LogoutConfirmationSheet),
      const Offset(0, 500),
    );
    await tester.pump();
    expect(find.byType(LogoutConfirmationSheet), findsOneWidget);

    await tester.binding.handlePopRoute();
    await tester.pump();
    expect(find.byType(LogoutConfirmationSheet), findsOneWidget);

    logout.complete();
    await tester.pumpAndSettle();
    expect(find.byType(LogoutConfirmationSheet), findsNothing);
  });

  testWidgets('cancel before submission does not call logout', (tester) async {
    var logoutCalls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () => showModalBottomSheet<bool>(
                context: context,
                isScrollControlled: true,
                builder: (_) => LogoutConfirmationSheet(
                  onLogout: () async => logoutCalls++,
                ),
              ),
              child: const Text('Open'),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Open'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(OutlinedButton, 'Hủy'));
    await tester.pumpAndSettle();

    expect(logoutCalls, 0);
    expect(find.byType(LogoutConfirmationSheet), findsNothing);
  });

  testWidgets('401 logout is treated as completed logout', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: LogoutConfirmationSheet(
            onLogout: () async => throw ApiException(401, ''),
          ),
        ),
      ),
    );

    await tester.tap(find.widgetWithText(FilledButton, 'Đăng xuất'));
    await tester.pumpAndSettle();

    expect(find.byType(LogoutConfirmationSheet), findsNothing);
  });

  test(
    'captured logout generation survives same-session token refresh',
    () async {
      final auth = AuthState.forTesting(storage: _MemoryTokenStorage());
      await auth.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      final generation = auth.sessionGeneration;
      await auth.setTokensIfCurrent(
        expectedGeneration: generation,
        expectedAccessToken: 'access-a',
        expectedRefreshToken: 'refresh-a',
        expectedUserId: 'account-a',
        accessToken: 'access-a-new',
        refreshToken: 'refresh-a-new',
        role: 'MOTHER',
      );

      final cleared = await auth.clearIfCurrentSession(
        generation: generation,
        userId: 'account-a',
      );

      expect(cleared, isTrue);
      expect(auth.isAuthenticated, isFalse);
    },
  );

  test('captured logout generation cannot clear a later login', () async {
    final auth = AuthState.forTesting(storage: _MemoryTokenStorage());
    await auth.setTokens(
      accessToken: 'access-a',
      refreshToken: 'refresh-a',
      userId: 'account-a',
      role: 'MOTHER',
    );
    final oldGeneration = auth.sessionGeneration;
    await auth.setTokens(
      accessToken: 'access-a-relogin',
      refreshToken: 'refresh-a-relogin',
      userId: 'account-a',
      role: 'MOTHER',
    );

    final cleared = await auth.clearIfCurrentSession(
      generation: oldGeneration,
      userId: 'account-a',
    );

    expect(cleared, isFalse);
    expect(auth.accessToken, 'access-a-relogin');
  });
}

class _MemoryTokenStorage implements TokenStorage {
  final values = <String, String?>{};

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) async {
    values
      ..['accessToken'] = accessToken
      ..['refreshToken'] = refreshToken
      ..['userId'] = userId
      ..['role'] = role;
  }

  @override
  Future<Map<String, String?>> load() async => Map.of(values);

  @override
  Future<void> clear({String? expectedUserId}) async => values.clear();
}
