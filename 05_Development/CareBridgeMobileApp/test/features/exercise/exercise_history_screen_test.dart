import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/auth/auth_state.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/core/storage/token_storage.dart';
import 'package:untitled/features/exercise/screens/exercise_history_screen.dart';
import 'package:untitled/features/exercise/services/exercise_service.dart';

void main() {
  testWidgets('renders loaded history without a false trimester tag', (
    tester,
  ) async {
    await _pumpScreen(
      tester,
      _serviceReturning(_responseWith(title: 'Yoga bầu 20 phút')),
    );
    await tester.pumpAndSettle();

    expect(find.text('Yoga bầu 20 phút'), findsOneWidget);
    expect(find.text('Hoàn thành'), findsOneWidget);
    expect(find.text('Tam cá nguyệt 2'), findsOneWidget);
  });

  testWidgets('renders the empty state for canonical empty history', (
    tester,
  ) async {
    await _pumpScreen(tester, _serviceReturning(_emptyResponse()));
    await tester.pumpAndSettle();

    expect(find.text('Chưa có lịch sử bài tập'), findsOneWidget);
    expect(find.textContaining('Không thể kết nối'), findsNothing);
  });

  testWidgets('shows API errors and retries the request', (tester) async {
    var attempts = 0;
    final service = ExerciseService.forTesting(
      get: (_) async {
        attempts++;
        if (attempts == 1) throw ApiException(503, 'unavailable');
        return _responseWith(title: 'Bài tập sau khi thử lại');
      },
    );

    await _pumpScreen(tester, service);
    await tester.pumpAndSettle();
    expect(find.text('Lỗi tải lịch sử (503)'), findsOneWidget);

    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(attempts, 2);
    expect(find.text('Bài tập sau khi thử lại'), findsOneWidget);
  });

  testWidgets('custom auth without a service fails closed visibly', (
    tester,
  ) async {
    final authState = await _authenticatedAuth('custom-auth-user');

    await tester.pumpWidget(
      MaterialApp(home: ExerciseHistoryScreen(authState: authState)),
    );
    await tester.pumpAndSettle();

    expect(find.text('Không thể kết nối. Vui lòng thử lại.'), findsOneWidget);
    expect(find.text('Gần đây'), findsNothing);
    expect(find.byType(CircularProgressIndicator), findsNothing);
  });

  testWidgets('sends only supported trimester scopes from filters', (
    tester,
  ) async {
    final paths = <String>[];
    final service = ExerciseService.forTesting(
      get: (path) async {
        paths.add(path);
        return _emptyResponse();
      },
    );

    await _pumpScreen(tester, service);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tam cá nguyệt 2'));
    await tester.pumpAndSettle();

    expect(paths, hasLength(2));
    expect(paths.first, isNot(contains('trimesterScope=')));
    expect(paths.last, endsWith('&trimesterScope=SECOND'));
    expect(paths.last, isNot(anyOf(contains('YOGA'), contains('WALKING'))));
  });

  testWidgets('discards a stale response after a newer filter load', (
    tester,
  ) async {
    final first = Completer<dynamic>();
    final second = Completer<dynamic>();
    var requestCount = 0;
    final service = ExerciseService.forTesting(
      get: (_) {
        requestCount++;
        return requestCount == 1 ? first.future : second.future;
      },
    );

    await _pumpScreen(tester, service);
    await tester.pump();
    await tester.tap(find.text('Tam cá nguyệt 3'));
    await tester.pump();

    second.complete(_responseWith(title: 'Kết quả mới nhất'));
    await tester.pumpAndSettle();
    expect(find.text('Kết quả mới nhất'), findsOneWidget);

    first.complete(_responseWith(title: 'Kết quả cũ'));
    await tester.pumpAndSettle();
    expect(find.text('Kết quả mới nhất'), findsOneWidget);
    expect(find.text('Kết quả cũ'), findsNothing);
  });

  testWidgets('clears account A history and rejects its late response', (
    tester,
  ) async {
    final lateAccountA = Completer<dynamic>();
    final accountB = Completer<dynamic>();
    var requestCount = 0;
    final service = ExerciseService.forTesting(
      get: (_) {
        requestCount++;
        return switch (requestCount) {
          1 => Future<dynamic>.value(
            _responseWith(title: 'Lịch sử tài khoản A'),
          ),
          2 => lateAccountA.future,
          _ => accountB.future,
        };
      },
    );
    final authState = await _authenticatedAuth('account-a');

    await _pumpScreen(tester, service, authState: authState);
    await tester.pumpAndSettle();
    expect(find.text('Lịch sử tài khoản A'), findsOneWidget);

    await tester.tap(find.text('Tam cá nguyệt 1'));
    await tester.pump();
    await _setAuthenticatedAccount(authState, 'account-b');
    await tester.pump();

    expect(requestCount, 3);
    expect(find.text('Lịch sử tài khoản A'), findsNothing);

    lateAccountA.complete(_responseWith(title: 'Phản hồi muộn tài khoản A'));
    await tester.pump();
    await tester.pump();
    expect(find.text('Phản hồi muộn tài khoản A'), findsNothing);

    accountB.complete(_responseWith(title: 'Lịch sử tài khoản B'));
    await tester.pumpAndSettle();
    expect(find.text('Lịch sử tài khoản B'), findsOneWidget);
    expect(find.text('Phản hồi muộn tài khoản A'), findsNothing);
  });

  testWidgets('replacing the injected service clears and reloads history', (
    tester,
  ) async {
    final authState = await _authenticatedAuth('service-swap-user');
    final replacementResponse = Completer<dynamic>();

    await _pumpScreen(
      tester,
      _serviceReturning(_responseWith(title: 'Dữ liệu service cũ')),
      authState: authState,
    );
    await tester.pumpAndSettle();
    expect(find.text('Dữ liệu service cũ'), findsOneWidget);

    await _pumpScreen(
      tester,
      ExerciseService.forTesting(get: (_) => replacementResponse.future),
      authState: authState,
    );
    await tester.pump();
    expect(find.text('Dữ liệu service cũ'), findsNothing);

    replacementResponse.complete(_responseWith(title: 'Dữ liệu service mới'));
    await tester.pumpAndSettle();
    expect(find.text('Dữ liệu service mới'), findsOneWidget);
  });

  testWidgets('supports large text without layout exceptions', (tester) async {
    await _pumpScreen(
      tester,
      _serviceReturning(
        _responseWith(title: 'Bài tập vận động nhẹ nhàng trong thai kỳ'),
      ),
      textScaler: const TextScaler.linear(2),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
    expect(
      find.text('Bài tập vận động nhẹ nhàng trong thai kỳ'),
      findsOneWidget,
    );
  });

  testWidgets('filter controls meet the minimum tap target', (tester) async {
    await _pumpScreen(tester, _serviceReturning(_emptyResponse()));
    await tester.pumpAndSettle();

    final target = find.byKey(
      const ValueKey<String>('exercise-history-filter-FIRST'),
    );
    expect(target, findsOneWidget);
    expect(tester.getSize(target).height, greaterThanOrEqualTo(44));
  });

  testWidgets('disables every chip animation for reduced motion', (
    tester,
  ) async {
    await _pumpScreen(
      tester,
      _serviceReturning(_emptyResponse()),
      disableAnimations: true,
    );
    await tester.pumpAndSettle();

    final chip = tester.widget<ChoiceChip>(
      find.byKey(const ValueKey<String>('exercise-history-filter-FIRST')),
    );
    final style = chip.chipAnimationStyle;
    expect(style, isNotNull);
    expect(style!.enableAnimation, same(AnimationStyle.noAnimation));
    expect(style.selectAnimation, same(AnimationStyle.noAnimation));
    expect(style.avatarDrawerAnimation, same(AnimationStyle.noAnimation));
    expect(style.deleteDrawerAnimation, same(AnimationStyle.noAnimation));
  });
}

Future<AuthState> _pumpScreen(
  WidgetTester tester,
  ExerciseService service, {
  AuthState? authState,
  TextScaler textScaler = TextScaler.noScaling,
  bool disableAnimations = false,
}) async {
  final authenticatedState =
      authState ?? await _authenticatedAuth('exercise-history-user');
  await tester.pumpWidget(
    MaterialApp(
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(
          textScaler: textScaler,
          disableAnimations: disableAnimations,
        ),
        child: child!,
      ),
      home: ExerciseHistoryScreen(
        service: service,
        authState: authenticatedState,
      ),
    ),
  );
  return authenticatedState;
}

Future<AuthState> _authenticatedAuth(String userId) async {
  final authState = AuthState.forTesting(storage: _MemoryTokenStorage());
  await _setAuthenticatedAccount(authState, userId);
  return authState;
}

Future<void> _setAuthenticatedAccount(AuthState authState, String userId) {
  return authState.setTokens(
    accessToken: 'access-$userId',
    refreshToken: 'refresh-$userId',
    userId: userId,
    role: 'MOTHER',
  );
}

ExerciseService _serviceReturning(Map<String, dynamic> response) {
  return ExerciseService.forTesting(get: (_) async => response);
}

Map<String, dynamic> _emptyResponse() => <String, dynamic>{
  'data': <dynamic>[],
  'page': 0,
  'size': 20,
  'totalElements': 0,
  'totalPages': 0,
};

Map<String, dynamic> _responseWith({required String title}) =>
    <String, dynamic>{
      'data': <dynamic>[
        <String, dynamic>{
          'exerciseSessionId': '73000000-0000-0000-0000-000000000001',
          'exerciseId': '60000000-0000-0000-0000-000000000003',
          'exerciseTitle': title,
          'sessionStatus': 'COMPLETED',
          'startedAt': '2026-07-26T17:00:00+07:00',
          'actualDurationSeconds': 1200,
          'completionPercent': 100.0,
          'warningCount': 0,
        },
      ],
      'page': 0,
      'size': 20,
      'totalElements': 1,
      'totalPages': 1,
    };

class _MemoryTokenStorage implements TokenStorage {
  Map<String, String?> _tokens = <String, String?>{};

  @override
  Future<void> save({
    required String accessToken,
    required String refreshToken,
    required String userId,
    required String role,
  }) async {
    _tokens = <String, String?>{
      'accessToken': accessToken,
      'refreshToken': refreshToken,
      'userId': userId,
      'role': role,
    };
  }

  @override
  Future<Map<String, String?>> load() async =>
      Map<String, String?>.from(_tokens);

  @override
  Future<void> clear({String? expectedUserId}) async {
    if (expectedUserId != null && _tokens['userId'] != expectedUserId) return;
    _tokens = <String, String?>{};
  }
}
