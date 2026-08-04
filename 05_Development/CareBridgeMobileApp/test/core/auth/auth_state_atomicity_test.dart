import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/auth/blocked_account_state.dart';
import 'package:untitled/core/storage/token_storage.dart';

class _ControlledTokenStorage implements TokenStorage {
  final values = <String, String?>{};
  Completer<void>? pendingSave;
  Completer<void>? pendingClear;
  Completer<void>? clearStarted;
  Object? saveFailure;
  Object? clearFailure;
  int clearCalls = 0;
  String? lastExpectedClearUserId;

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) async {
    final blocker = pendingSave;
    if (blocker != null) await blocker.future;
    final failure = saveFailure;
    if (failure != null) throw failure;
    values
      ..['accessToken'] = accessToken
      ..['refreshToken'] = refreshToken
      ..['userId'] = userId
      ..['role'] = role;
  }

  @override
  Future<Map<String, String?>> load() async => Map.of(values);

  @override
  Future<void> clear({String? expectedUserId}) async {
    clearCalls++;
    lastExpectedClearUserId = expectedUserId;
    final started = clearStarted;
    if (started != null && !started.isCompleted) started.complete();
    final blocker = pendingClear;
    if (blocker != null) await blocker.future;
    values.clear();
    final failure = clearFailure;
    if (failure != null) throw failure;
  }
}

void main() {
  test(
    'setTokens persists before publishing authenticated memory state',
    () async {
      final storage = _ControlledTokenStorage();
      final saveBlocker = Completer<void>();
      storage.pendingSave = saveBlocker;
      final state = AuthState.forTesting(storage: storage);
      var notifications = 0;
      state.addListener(() => notifications++);

      final update = state.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'MOTHER',
      );
      await Future<void>.delayed(Duration.zero);

      expect(state.accessToken, isNull);
      expect(state.userId, isNull);
      expect(state.isAuthenticated, isFalse);
      expect(notifications, 0);

      saveBlocker.complete();
      await update;

      expect(storage.values['userId'], 'account-b');
      expect(state.accessToken, 'access-b');
      expect(state.userId, 'account-b');
      expect(state.isAuthenticated, isTrue);
      expect(notifications, 1);
    },
  );

  test(
    'failed account switch clears durable and memory state then rethrows',
    () async {
      final storage = _ControlledTokenStorage();
      final state = AuthState.forTesting(storage: storage);
      await state.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      storage.saveFailure = StateError('synthetic account cleanup failure');

      await expectLater(
        state.setTokens(
          accessToken: 'access-b',
          refreshToken: 'refresh-b',
          userId: 'account-b',
          role: 'MOTHER',
        ),
        throwsA(isA<StateError>()),
      );

      expect(storage.clearCalls, 1);
      expect(storage.values, isEmpty);
      expect(state.accessToken, isNull);
      expect(state.refreshToken, isNull);
      expect(state.userId, isNull);
      expect(state.role, isNull);
      expect(state.isAuthenticated, isFalse);
      expect(storage.lastExpectedClearUserId, 'account-b');
    },
  );

  test('startup cleanup finishes before a new login is persisted', () async {
    final storage = _ControlledTokenStorage();
    final clearBlocker = Completer<void>();
    final clearStarted = Completer<void>();
    storage
      ..pendingClear = clearBlocker
      ..clearStarted = clearStarted;
    final state = AuthState.forTesting(storage: storage);

    final initialization = state.init();
    await clearStarted.future.timeout(const Duration(seconds: 2));
    final login = state.setTokens(
      accessToken: 'access-b',
      refreshToken: 'refresh-b',
      userId: 'account-b',
      role: 'FAMILY',
    );

    clearBlocker.complete();
    await Future.wait([initialization, login]);

    expect(state.userId, 'account-b');
    expect(storage.values['userId'], 'account-b');
    expect(storage.values['accessToken'], 'access-b');
  });

  test(
    'stale clear finishes before a replacement login is persisted',
    () async {
      final storage = _ControlledTokenStorage();
      final state = AuthState.forTesting(storage: storage);
      await state.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      final generationA = state.sessionGeneration;
      final clearBlocker = Completer<void>();
      storage.pendingClear = clearBlocker;

      final clearA = state.clearIfCurrentSession(
        generation: generationA,
        userId: 'account-a',
      );
      await Future<void>.delayed(Duration.zero);
      final loginB = state.setTokens(
        accessToken: 'access-b',
        refreshToken: 'refresh-b',
        userId: 'account-b',
        role: 'FAMILY',
      );
      await Future<void>.delayed(Duration.zero);

      expect(state.userId, isNull);
      clearBlocker.complete();
      await Future.wait([clearA, loginB]);

      expect(state.userId, 'account-b');
      expect(storage.values['userId'], 'account-b');
      expect(storage.values['accessToken'], 'access-b');
    },
  );

  test('queued stale refresh cannot overwrite a replacement login', () async {
    final storage = _ControlledTokenStorage();
    final state = AuthState.forTesting(storage: storage);
    await state.setTokens(
      accessToken: 'access-a',
      refreshToken: 'refresh-a',
      userId: 'account-a',
      role: 'MOTHER',
    );
    final generationA = state.sessionGeneration;
    final saveBlocker = Completer<void>();
    storage.pendingSave = saveBlocker;

    final loginB = state.setTokens(
      accessToken: 'access-b',
      refreshToken: 'refresh-b',
      userId: 'account-b',
      role: 'FAMILY',
    );
    final staleRefresh = state.setTokensIfCurrent(
      expectedGeneration: generationA,
      expectedAccessToken: 'access-a',
      expectedRefreshToken: 'refresh-a',
      expectedUserId: 'account-a',
      accessToken: 'access-a-new',
      refreshToken: 'refresh-a-new',
      role: 'MOTHER',
    );
    await Future<void>.delayed(Duration.zero);

    saveBlocker.complete();
    expect(await staleRefresh, isFalse);
    await loginB;

    expect(state.userId, 'account-b');
    expect(storage.values['userId'], 'account-b');
    expect(storage.values['accessToken'], 'access-b');
  });

  test('blocked-account cleanup finishes before a replacement login', () async {
    final storage = _ControlledTokenStorage();
    final state = AuthState.forTesting(storage: storage);
    await state.setTokens(
      accessToken: 'access-a',
      refreshToken: 'refresh-a',
      userId: 'account-a',
      role: 'MOTHER',
    );
    final clearBlocker = Completer<void>();
    final clearStarted = Completer<void>();
    storage
      ..pendingClear = clearBlocker
      ..clearStarted = clearStarted;

    final blockedCleanup = state.clearWithBlockedAccount(
      const BlockedAccountState(code: 'ACCOUNT_DISABLED'),
    );
    await clearStarted.future.timeout(const Duration(seconds: 2));
    final loginB = state.setTokens(
      accessToken: 'access-b',
      refreshToken: 'refresh-b',
      userId: 'account-b',
      role: 'FAMILY',
    );

    clearBlocker.complete();
    await Future.wait([blockedCleanup, loginB]);

    expect(state.userId, 'account-b');
    expect(storage.values['userId'], 'account-b');
    expect(storage.values['accessToken'], 'access-b');
  });

  test(
    'queued credential clear cannot erase an in-flight newer refresh',
    () async {
      final storage = _ControlledTokenStorage();
      final state = AuthState.forTesting(storage: storage);
      await state.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      final generation = state.sessionGeneration;
      final saveBlocker = Completer<void>();
      storage.pendingSave = saveBlocker;

      final rotation = state.setTokensIfCurrent(
        expectedGeneration: generation,
        expectedAccessToken: 'access-a',
        expectedRefreshToken: 'refresh-a',
        expectedUserId: 'account-a',
        accessToken: 'access-a-new',
        refreshToken: 'refresh-a-new',
        role: 'MOTHER',
      );
      await Future<void>.delayed(Duration.zero);
      final staleClear = state.clearIfCurrentCredentials(
        generation: generation,
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );

      saveBlocker.complete();
      expect(await rotation, isTrue);
      expect(await staleClear, isFalse);
      expect(state.accessToken, 'access-a-new');
      expect(state.refreshToken, 'refresh-a-new');
      expect(storage.values['accessToken'], 'access-a-new');
    },
  );

  test(
    'credential clear failure leaves memory and durable state fail-closed',
    () async {
      final storage = _ControlledTokenStorage();
      final state = AuthState.forTesting(storage: storage);
      await state.setTokens(
        accessToken: 'access-a',
        refreshToken: 'refresh-a',
        userId: 'account-a',
        role: 'MOTHER',
      );
      final generation = state.sessionGeneration;
      storage.clearFailure = StateError('synthetic ancillary clear failure');

      await expectLater(
        state.clearIfCurrentCredentials(
          generation: generation,
          accessToken: 'access-a',
          refreshToken: 'refresh-a',
          userId: 'account-a',
          role: 'MOTHER',
        ),
        throwsA(isA<StateError>()),
      );

      expect(storage.values, isEmpty);
      expect(state.isAuthenticated, isFalse);
      expect(state.accessToken, isNull);
      expect(state.refreshToken, isNull);
      expect(state.userId, isNull);
      expect(storage.lastExpectedClearUserId, 'account-a');
    },
  );
}
