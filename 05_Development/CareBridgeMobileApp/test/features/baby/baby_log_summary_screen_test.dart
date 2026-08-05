import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/baby/models/baby_daily_log_model.dart';
import 'package:untitled/features/baby/models/baby_model.dart';
import 'package:untitled/features/baby/screens/baby_log_summary_screen.dart';
import 'package:untitled/features/baby/services/baby_log_service.dart';
import 'package:untitled/features/baby/services/baby_service.dart';

class _FakeBabyService extends BabyService {
  _FakeBabyService(this.profile);

  final BabyProfile profile;

  @override
  Future<List<BabyProfile>> listBabyProfiles() async => [profile];
}

class _FakeLogService extends BabyLogService {
  _FakeLogService(this.log);

  final BabyDailyLog log;

  @override
  Future<BabyLogSummaryResponse> getLogSummary(
    String babyId, {
    String period = '24h',
  }) async => BabyLogSummaryResponse(
    babyId: babyId,
    period: period,
    summaries: const {'FEEDING': LogTypeSummary(count: 1)},
  );

  @override
  Future<List<BabyDailyLog>> getDailyLogs(String babyId) async => [log];
}

void main() {
  testWidgets('canonical journal lists a real log and opens its detail route', (
    tester,
  ) async {
    final baby = BabyProfile(
      id: 'baby-1',
      nickname: 'Bông',
      birthDate: DateTime(2026, 7, 1),
      gender: BabyGender.unknown,
      isActive: true,
    );
    final log = BabyDailyLog(
      id: 'log-1',
      babyId: baby.id,
      logType: LogType.feeding,
      startedAt: DateTime(2026, 8, 5, 9),
      quantity: 120,
      unit: 'ml',
    );
    final router = GoRouter(
      initialLocation: '/babies/baby-1/log-summary',
      routes: [
        GoRoute(
          path: '/babies/:babyId/log-summary',
          builder: (_, _) => BabyLogSummaryScreen(
            babyId: 'baby-1',
            babyService: _FakeBabyService(baby),
            logService: _FakeLogService(log),
          ),
        ),
        GoRoute(
          path: '/babies/:babyId/daily-logs/:logId',
          builder: (_, state) => Scaffold(
            body: Text(
              'detail:${state.pathParameters['babyId']}:${state.pathParameters['logId']}',
            ),
          ),
        ),
      ],
    );
    addTearDown(router.dispose);

    await tester.pumpWidget(MaterialApp.router(routerConfig: router));
    await tester.pumpAndSettle();

    expect(find.byKey(const ValueKey('baby-log-log-1')), findsOneWidget);
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('baby-log-log-1')));
    await tester.pumpAndSettle();
    expect(find.text('detail:baby-1:log-1'), findsOneWidget);
  });
}
