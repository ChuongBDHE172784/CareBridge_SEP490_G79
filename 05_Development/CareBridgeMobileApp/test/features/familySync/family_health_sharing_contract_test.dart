import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/familySync/models/care_group_model.dart';
import 'package:untitled/features/familySync/screens/care_group_detail_screen.dart';
import 'package:untitled/features/familySync/screens/family_quick_note_history_screen.dart';
import 'package:untitled/features/familySync/services/care_group_service.dart';
import 'package:untitled/features/familySync/services/family_home_service.dart';
import 'package:untitled/features/healthRecords/models/health_metric_model.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';

void main() {
  group('Family health sharing contract', () {
    test('six health permissions default to deny and parse explicitly', () {
      final legacy = FamilyHomePermission.fromJson({
        'calendar': true,
        'logs': false,
        'alerts': true,
        'records': false,
      });
      expect(legacy.sharedHealthMetricCount, 0);

      final permission = FamilyHomePermission.fromJson({
        'calendar': true,
        'logs': false,
        'alerts': true,
        'records': false,
        'quickNotes': true,
        'quickNoteWeight': true,
        'quickNoteFetalMovement': true,
        'quickNoteBloodPressure': true,
        'quickNoteHydration': true,
        'quickNoteEpds': true,
        'quickNoteBloodGlucose': true,
      });

      expect(permission.sharedHealthMetricCount, 6);
      expect(permission.quickNoteBloodPressure, isTrue);
      expect(permission.quickNoteBloodGlucose, isTrue);
    });

    test('parses group-scoped real health metric projection', () {
      final detail = FamilyHomeGroupDetail.fromJson({
        'careGroupId': 'group-a',
        'motherDisplayName': 'Nguyễn Lan',
        'todayReminders': <Object?>[],
        'alerts': <Object?>[],
        'memberCount': 0,
        'members': <Object?>[],
        'relationshipRole': 'SISTER',
        'customRelationshipRole': null,
        'permissionScope': {
          'calendar': false,
          'logs': false,
          'alerts': false,
          'records': false,
          'quickNotes': true,
          'quickNoteBloodPressure': true,
        },
        'sharedDataSummary': {'totalItems': 0, 'categories': <Object?>[]},
        'healthMetricSummaries': [
          {
            'metricType': 'BLOOD_PRESSURE',
            'valueNumeric': 118,
            'valueSecondary': 76,
            'unit': 'mmHg',
            'measuredAt': '2026-08-03T02:15:00Z',
            'measurementContext': null,
            'recordCount': 1,
          },
        ],
      });

      expect(detail.healthMetricSummaries, hasLength(1));
      final pressure = detail.healthMetricSummaries.single;
      expect(pressure.metricType, 'BLOOD_PRESSURE');
      expect(pressure.valueDisplay, '118/76');
      expect(pressure.unit, 'mmHg');
      expect(pressure.measuredAt, DateTime.parse('2026-08-03T02:15:00Z'));
    });

    test(
      'shared metric without observation remains an honest no-data item',
      () {
        final metric = FamilyHomeHealthMetricSummary.fromJson({
          'metricType': 'WEIGHT',
          'valueNumeric': null,
          'valueSecondary': null,
          'unit': 'kg',
          'measuredAt': null,
          'recordCount': 0,
        });

        expect(metric.hasData, isFalse);
        expect(metric.valueDisplay, isNull);
      },
    );

    testWidgets('overview shows real values and honest no-data state', (
      tester,
    ) async {
      final snapshot = FamilyHomeSnapshot.fromJson(_dashboardWithMetrics());
      await tester.pumpWidget(
        MaterialApp(
          home: FamilyMemberHomeScreen(
            dashboardLoader: ({selectedCareGroupId}) async => snapshot,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(
        find.byKey(const Key('family-dashboard-quick-notes')),
        300,
        scrollable: find.byType(Scrollable).first,
      );

      expect(find.text('Chỉ số sức khỏe'), findsOneWidget);
      expect(find.text('118/76 mmHg'), findsOneWidget);
      expect(find.text('Chưa có dữ liệu'), findsOneWidget);
      expect(
        find.byKey(const Key('family-quick-note-blood_pressure')),
        findsOneWidget,
      );
    });

    testWidgets('family detail remains available when health summary fails', (
      tester,
    ) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CareGroupDetailScreen(
            groupId: 'group-a',
            groupName: 'Nhóm của Lan',
            service: _DetailCareGroupService(),
            dashboardLoader: ({selectedCareGroupId}) async =>
                throw Exception('private server detail'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Nhóm của Lan'), findsOneWidget);
      expect(find.byKey(const Key('care-group-detail-error')), findsNothing);
      expect(
        find.byKey(const Key('family-group-health-error')),
        findsOneWidget,
      );
      expect(find.text('Thử lại'), findsOneWidget);
      expect(find.text('Hành trình của mẹ'), findsNothing);
    });

    testWidgets('history uses newest timestamp and formats blood pressure', (
      tester,
    ) async {
      final newer = MetricDataPoint(
        measuredAt: DateTime(2026, 8, 3, 10),
        valueNumeric: 118,
        valueSecondary: 76,
      );
      final older = MetricDataPoint(
        measuredAt: DateTime(2026, 8, 3, 8),
        valueNumeric: 130,
        valueSecondary: 85,
      );
      await tester.pumpWidget(
        MaterialApp(
          home: FamilyQuickNoteHistoryScreen(
            careGroupId: 'group-a',
            metricType: 'BLOOD_PRESSURE',
            historyLoader:
                ({
                  required careGroupId,
                  required metricType,
                  required from,
                  required to,
                }) async => MetricTrend(
                  metricType: metricType,
                  unit: 'mmHg',
                  dataPoints: [newer, older],
                ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('118/76 mmHg'), findsAtLeastNWidgets(1));
      expect(find.text('130/85 mmHg'), findsOneWidget);
      expect(find.textContaining('Chỉ xem'), findsOneWidget);
    });

    testWidgets('history error retries without exposing exception text', (
      tester,
    ) async {
      var attempts = 0;
      Future<MetricTrend> loader({
        required String careGroupId,
        required String metricType,
        required DateTime from,
        required DateTime to,
      }) async {
        attempts += 1;
        if (attempts == 1) throw Exception('private server detail');
        return MetricTrend(metricType: metricType, dataPoints: const []);
      }

      await tester.pumpWidget(
        MaterialApp(
          home: FamilyQuickNoteHistoryScreen(
            careGroupId: 'group-a',
            metricType: 'EPDS_SCORE',
            historyLoader: loader,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Không thể tải lịch sử được chia sẻ.'), findsOneWidget);
      expect(find.textContaining('Exception'), findsNothing);
      await tester.tap(find.text('Thử lại'));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('family-quick-note-empty')), findsOneWidget);
      expect(find.textContaining('Không phải chẩn đoán'), findsOneWidget);
    });
  });
}

class _DetailCareGroupService extends CareGroupService {
  @override
  Future<CareGroup> getGroupMembers(String groupId) async => CareGroup(
    id: groupId,
    groupName: 'Nhóm của Lan',
    myRole: 'MEMBER',
    memberCount: 1,
    members: const [
      CareGroupMember(
        memberId: 'member-a',
        displayName: 'Nguyễn Lan',
        memberRole: 'OWNER',
        inviteStatus: 'ACCEPTED',
      ),
    ],
  );
}

Map<String, dynamic> _dashboardWithMetrics() => {
  'groups': [
    {
      'id': 'group-a',
      'name': 'Nhóm của Lan',
      'joinedAt': '2026-08-01T00:00:00Z',
      'lastActivityAt': '2026-08-03T02:15:00Z',
      'relationshipRole': 'SISTER',
      'customRelationshipRole': null,
      'permissionScope': _permissionJson,
      'aggregate': {'overdue': 0, 'dueSoon': 0, 'inProgress': 0, 'alerts': 0},
    },
  ],
  'globalAggregate': {'overdue': 0, 'dueSoon': 0, 'inProgress': 0, 'alerts': 0},
  'selectedCareGroupId': 'group-a',
  'selectedGroupDetail': {
    'careGroupId': 'group-a',
    'motherDisplayName': 'Nguyễn Lan',
    'todayReminders': <Object?>[],
    'alerts': <Object?>[],
    'memberCount': 0,
    'members': <Object?>[],
    'relationshipRole': 'SISTER',
    'customRelationshipRole': null,
    'permissionScope': _permissionJson,
    'sharedDataSummary': {'totalItems': 0, 'categories': <Object?>[]},
    'healthMetricSummaries': [
      {
        'metricType': 'WEIGHT',
        'valueNumeric': null,
        'valueSecondary': null,
        'unit': 'kg',
        'measuredAt': null,
        'recordCount': 0,
      },
      {
        'metricType': 'BLOOD_PRESSURE',
        'valueNumeric': 118,
        'valueSecondary': 76,
        'unit': 'mmHg',
        'measuredAt': '2026-08-03T02:15:00Z',
        'recordCount': 1,
      },
    ],
  },
};

const Map<String, dynamic> _permissionJson = {
  'calendar': false,
  'logs': false,
  'alerts': false,
  'records': false,
  'quickNotes': true,
  'quickNoteWeight': true,
  'quickNoteBloodPressure': true,
};
