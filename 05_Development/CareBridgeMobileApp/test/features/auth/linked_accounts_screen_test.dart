import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/auth/models/linked_account.dart';
import 'package:untitled/features/auth/models/auth_model.dart';
import 'package:untitled/features/auth/screens/account_profile_screen.dart';
import 'package:untitled/features/auth/screens/linked_accounts_screen.dart';

void main() {
  const unlinked = LinkedAccount(provider: 'GOOGLE', linked: false);
  const linked = LinkedAccount(
    provider: 'GOOGLE',
    linked: true,
    email: 'member@example.com',
  );

  Widget subject({
    required Future<LinkedAccount> Function() load,
    Future<LinkedAccount> Function()? link,
  }) {
    return MaterialApp(
      home: LinkedAccountsScreen(loadLinkedAccount: load, onLinkGoogle: link),
    );
  }

  testWidgets('shows loading then the unlinked state', (tester) async {
    final completer = Completer<LinkedAccount>();
    await tester.pumpWidget(subject(load: () => completer.future));

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    completer.complete(unlinked);
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('linked-accounts-screen')), findsOneWidget);
    expect(find.byKey(const Key('google-link-status')), findsOneWidget);
    expect(find.text('Chưa liên kết'), findsOneWidget);
    expect(find.byKey(const Key('link-google-account')), findsOneWidget);
  });

  testWidgets('shows linked email and handles a long address safely', (
    tester,
  ) async {
    const longEmail =
        'a.very.long.google.workspace.account.address@example-company.test';
    await tester.pumpWidget(
      subject(
        load: () async => const LinkedAccount(
          provider: 'GOOGLE',
          linked: true,
          email: longEmail,
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Đã liên kết'), findsOneWidget);
    expect(find.text(longEmail), findsOneWidget);
    expect(tester.takeException(), isNull);
    expect(find.byKey(const Key('link-google-account')), findsNothing);
  });

  testWidgets(
    'disables the link action while linking and refreshes on success',
    (tester) async {
      final completer = Completer<LinkedAccount>();
      await tester.pumpWidget(
        subject(load: () async => unlinked, link: () => completer.future),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('link-google-account')));
      await tester.pump();
      final button = tester.widget<FilledButton>(
        find.byKey(const Key('link-google-account')),
      );
      expect(button.onPressed, isNull);

      completer.complete(linked);
      await tester.pumpAndSettle();

      expect(find.text('Đã liên kết'), findsOneWidget);
      expect(find.text('Đã liên kết tài khoản Google.'), findsOneWidget);
    },
  );

  testWidgets('Google picker cancellation is silent and restores the action', (
    tester,
  ) async {
    await tester.pumpWidget(
      subject(
        load: () async => unlinked,
        link: () async =>
            throw const LinkedAccountException(LinkedAccountFailure.canceled),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('link-google-account')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('linked-account-error')), findsNothing);
    final button = tester.widget<FilledButton>(
      find.byKey(const Key('link-google-account')),
    );
    expect(button.onPressed, isNotNull);
  });

  testWidgets('conflict shows contextual copy without raw server details', (
    tester,
  ) async {
    await tester.pumpWidget(
      subject(
        load: () async => unlinked,
        link: () async =>
            throw const LinkedAccountException(LinkedAccountFailure.conflict),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('link-google-account')));
    await tester.pumpAndSettle();

    expect(
      find.text(LinkedAccountFailure.conflict.userMessage),
      findsOneWidget,
    );
    expect(find.textContaining('raw'), findsNothing);
  });

  testWidgets('load failure offers retry and can recover', (tester) async {
    var attempts = 0;
    await tester.pumpWidget(
      subject(
        load: () async {
          attempts += 1;
          if (attempts == 1) throw Exception('raw backend response');
          return unlinked;
        },
      ),
    );
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('linked-accounts-retry')), findsOneWidget);
    expect(find.textContaining('raw backend'), findsNothing);

    await tester.tap(find.byKey(const Key('linked-accounts-retry')));
    await tester.pumpAndSettle();

    expect(attempts, 2);
    expect(find.text('Chưa liên kết'), findsOneWidget);
  });

  testWidgets('profile exposes the linked accounts navigation entry', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: AccountProfileScreen(
          loadProfile: () async => const UserProfile(
            id: 'user-1',
            name: 'CareBridge Member',
            role: 'MOTHER',
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.byKey(const Key('linked-accounts-menu-item')),
      300,
      scrollable: find.byType(Scrollable).first,
    );

    expect(find.byKey(const Key('linked-accounts-menu-item')), findsOneWidget);
    expect(find.text('Tài khoản liên kết'), findsOneWidget);
  });
}
