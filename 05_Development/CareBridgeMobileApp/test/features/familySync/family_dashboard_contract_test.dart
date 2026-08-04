import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/familySync/models/family_permission_model.dart';
import 'package:untitled/features/familySync/services/family_home_service.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';

void main() {
  group('Family dashboard contract', () {
    test(
      'parses granular quick-note permission response with safe defaults',
      () {
        final permission = FamilyPermission.fromJson({
          'memberId': 'member-a',
          'careGroupId': 'group-a',
          'calendar': false,
          'logs': false,
          'alerts': true,
          'records': true,
          'quickNotes': true,
          'quickNoteWeight': true,
          'updatedAt': '2026-07-31T00:00:00Z',
        });

        expect(permission.quickNotes, isTrue);
        expect(permission.quickNoteWeight, isTrue);
        expect(permission.quickNoteHydration, isFalse);
        expect(permission.quickNoteEpds, isFalse);
        expect(permission.quickNoteFetalMovement, isFalse);
      },
    );

    test('parses backend groups, global aggregate, and selected detail', () {
      final snapshot = FamilyHomeSnapshot.fromJson(_dashboardJson());

      expect(snapshot.globalAggregate.overdue, 7);
      expect(snapshot.groups, hasLength(2));
      expect(snapshot.groups.first.permissionScope.alerts, isTrue);
      expect(snapshot.groups.first.permissionScope.quickNotes, isTrue);
      expect(snapshot.groups.first.permissionScope.quickNoteWeight, isTrue);
      expect(snapshot.groups.first.permissionScope.quickNoteEpds, isFalse);
      expect(snapshot.selectedCareGroupId, 'group-a');
      expect(snapshot.selectedGroupDetail!.careGroupId, 'group-a');
      expect(snapshot.selectedGroupDetail!.motherDisplayName, 'Nguyễn Lan');
      expect(
        snapshot.selectedGroupDetail!.todayReminders.single.type,
        'APPOINTMENT',
      );
      expect(
        snapshot.selectedGroupDetail!.alerts.single.careGroupId,
        'group-a',
      );
      expect(snapshot.selectedGroupDetail!.members.single.systemRole, 'MEMBER');
      expect(snapshot.selectedGroupDetail!.sharedDataSummary.totalItems, 2);
    });

    testWidgets(
      'overview exposes Community and Content FAQ without duplicating bottom navigation',
      (tester) async {
        await _pumpDashboard(
          tester,
          ({selectedCareGroupId}) async => _snapshot(
            selectedId: 'group-a',
            groups: [_group('group-a', 'Nhóm A')],
            detail: _detail('group-a'),
          ),
        );
        await tester.pumpAndSettle();

        expect(find.text('Cộng đồng'), findsOneWidget);
        expect(find.text('Nội dung & FAQ'), findsOneWidget);
        expect(find.text('Chuyên gia'), findsOneWidget);
        expect(find.text('Trò chuyện'), findsOneWidget);
        expect(find.text('Cộng đồng / FAQ'), findsNothing);
      },
    );

    testWidgets('renders loading state', (tester) async {
      final pending = Completer<FamilyHomeSnapshot>();

      await _pumpDashboard(tester, ({selectedCareGroupId}) => pending.future);

      expect(find.byKey(const Key('family-dashboard-loading')), findsOneWidget);
    });

    testWidgets('renders no-group CTAs', (tester) async {
      await _pumpDashboard(
        tester,
        ({selectedCareGroupId}) async => _noGroupSnapshot(),
      );
      await tester.pump();

      expect(
        find.byKey(const Key('family-dashboard-no-group')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('family-dashboard-join-cta')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('family-dashboard-invitation-cta')),
        findsOneWidget,
      );
    });

    testWidgets('one group renders detail without selector', (tester) async {
      await _pumpDashboard(
        tester,
        ({selectedCareGroupId}) async => _snapshot(
          selectedId: 'group-a',
          groups: [_group('group-a', 'Nhóm A')],
          detail: _detail('group-a'),
        ),
      );
      await tester.pump();

      expect(
        find.byKey(const Key('family-dashboard-group-selector')),
        findsNothing,
      );
      await tester.scrollUntilVisible(
        find.byKey(const Key('family-selected-group-group-a')),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      expect(
        find.byKey(const Key('family-selected-group-group-a')),
        findsOneWidget,
      );
      expect(find.text('Lịch hẹn của Nguyễn Lan'), findsOneWidget);
      expect(
        find.byKey(const Key('family-reminder-reminder-a')),
        findsOneWidget,
      );
      expect(find.text('Quyền được chia sẻ'), findsNothing);
      expect(find.text('Dữ liệu chia sẻ'), findsNothing);
    });

    testWidgets(
      'renders arrow icon for appointment calendar in mother appointments section header',
      (tester) async {
        await _pumpDashboard(
          tester,
          ({selectedCareGroupId}) async => _snapshot(
            selectedId: 'group-a',
            groups: [_group('group-a', 'Nhóm A')],
            detail: _detail('group-a'),
          ),
        );
        await tester.pumpAndSettle();

        final button = find.byKey(
          const Key('family-home-appointment-calendar-button'),
        );
        await tester.scrollUntilVisible(
          button,
          250,
          scrollable: find.byType(Scrollable).first,
        );
        expect(button, findsOneWidget);
        final iconWidget = tester.widget<IconButton>(button);
        expect(
          (iconWidget.icon as Icon).icon,
          equals(Icons.arrow_forward_rounded),
        );
      },
    );

    testWidgets(
      'two groups show selector and changing detail preserves global aggregate',
      (tester) async {
        final requestedIds = <String?>[];
        Future<FamilyHomeSnapshot> loader({String? selectedCareGroupId}) async {
          requestedIds.add(selectedCareGroupId);
          final selectedId = selectedCareGroupId ?? 'group-a';
          return _snapshot(
            selectedId: selectedId,
            groups: [_group('group-a', 'Nhóm A'), _group('group-b', 'Nhóm B')],
            detail: _detail(selectedId),
          );
        }

        await _pumpDashboard(tester, loader);
        await tester.pumpAndSettle();

        await tester.scrollUntilVisible(
          find.byKey(const Key('family-dashboard-group-selector')),
          100.0,
          scrollable: find.byType(Scrollable).first,
        );
        await tester.pumpAndSettle();
        expect(
          find.byKey(const Key('family-dashboard-group-selector')),
          findsOneWidget,
        );
        expect(
          find.descendant(
            of: find.byKey(const Key('family-global-overdue')),
            matching: find.text('7'),
          ),
          findsOneWidget,
        );

        await tester.tap(
          find.byKey(const Key('family-dashboard-group-selector')),
        );
        await tester.pumpAndSettle();
        await tester.tap(find.text('Nhóm B').last);
        await tester.pumpAndSettle();

        expect(requestedIds, [null, 'group-b']);
        expect(
          find.byKey(const Key('family-selected-group-group-b')),
          findsOneWidget,
        );
        expect(
          find.byKey(const Key('family-reminder-reminder-b')),
          findsOneWidget,
        );
        await tester.scrollUntilVisible(
          find.byKey(const Key('family-global-overdue')),
          -250,
          scrollable: find.byType(Scrollable).first,
        );
        expect(
          find.descendant(
            of: find.byKey(const Key('family-global-overdue')),
            matching: find.text('7'),
          ),
          findsOneWidget,
        );
      },
    );

    testWidgets('renders honest empty reminder and alert states', (
      tester,
    ) async {
      await _pumpDashboard(
        tester,
        ({selectedCareGroupId}) async => _snapshot(
          selectedId: 'group-a',
          groups: [_group('group-a', 'Nhóm A')],
          detail: _detail('group-a', empty: true),
        ),
      );
      await tester.pump();

      await tester.scrollUntilVisible(
        find.byKey(const Key('family-dashboard-empty-reminders')),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      expect(
        find.byKey(const Key('family-dashboard-empty-reminders')),
        findsOneWidget,
      );
      await tester.scrollUntilVisible(
        find.byKey(const Key('family-dashboard-empty-alerts')),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      expect(
        find.byKey(const Key('family-dashboard-empty-alerts')),
        findsOneWidget,
      );
      expect(find.text('Quyền được chia sẻ'), findsNothing);
      expect(find.text('Dữ liệu chia sẻ'), findsNothing);
    });

    testWidgets('renders only explicitly shared quick-note histories', (
      tester,
    ) async {
      const permission = FamilyHomePermission(
        calendar: true,
        logs: false,
        alerts: true,
        records: true,
        quickNotes: true,
        quickNoteWeight: true,
        quickNoteHydration: true,
      );
      await _pumpDashboard(
        tester,
        ({selectedCareGroupId}) async => _snapshot(
          selectedId: 'group-a',
          groups: [_group('group-a', 'Nhóm A')],
          detail: _detail('group-a', permission: permission),
        ),
      );
      await tester.pump();

      await tester.scrollUntilVisible(
        find.byKey(const Key('family-dashboard-quick-notes')),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      expect(
        find.byKey(const Key('family-dashboard-quick-notes')),
        findsOneWidget,
      );
      expect(find.byKey(const Key('family-quick-note-weight')), findsOneWidget);
      expect(
        find.byKey(const Key('family-quick-note-hydration')),
        findsOneWidget,
      );
      expect(
        find.byKey(const Key('family-quick-note-epds_score')),
        findsNothing,
      );
      expect(find.text('Chỉ xem'), findsOneWidget);
    });

    testWidgets('renders generic error and retries loader', (tester) async {
      var calls = 0;
      await _pumpDashboard(tester, ({selectedCareGroupId}) async {
        calls++;
        throw Exception('network');
      });
      await tester.pump();

      expect(find.byKey(const Key('family-dashboard-error')), findsOneWidget);
      await tester.tap(find.byKey(const Key('family-dashboard-retry')));
      await tester.pump();
      await tester.pump();
      expect(calls, 2);
    });

    testWidgets('renders permission denied for 403', (tester) async {
      await _pumpDashboard(
        tester,
        ({selectedCareGroupId}) async => throw ApiException(403, 'forbidden'),
      );
      await tester.pump();

      expect(
        find.byKey(const Key('family-dashboard-permission-denied')),
        findsOneWidget,
      );
      expect(find.byKey(const Key('family-dashboard-error')), findsNothing);
    });
  });
}

Future<void> _pumpDashboard(
  WidgetTester tester,
  Future<FamilyHomeSnapshot> Function({String? selectedCareGroupId}) loader,
) {
  return tester.pumpWidget(
    MaterialApp(home: FamilyMemberHomeScreen(dashboardLoader: loader)),
  );
}

FamilyHomeSnapshot _noGroupSnapshot() {
  return const FamilyHomeSnapshot(
    groups: [],
    globalAggregate: FamilyHomeAggregate(
      overdue: 0,
      dueSoon: 0,
      inProgress: 0,
      alerts: 0,
    ),
    selectedCareGroupId: null,
    selectedGroupDetail: null,
  );
}

FamilyHomeSnapshot _snapshot({
  required String selectedId,
  required List<FamilyHomeGroup> groups,
  required FamilyHomeGroupDetail detail,
}) {
  return FamilyHomeSnapshot(
    groups: groups,
    globalAggregate: const FamilyHomeAggregate(
      overdue: 7,
      dueSoon: 5,
      inProgress: 3,
      alerts: 2,
    ),
    selectedCareGroupId: selectedId,
    selectedGroupDetail: detail,
  );
}

FamilyHomeGroup _group(String id, String name) {
  return FamilyHomeGroup(
    id: id,
    name: name,
    joinedAt: DateTime.utc(2026, 7, 1),
    lastActivityAt: DateTime.utc(2026, 7, 29),
    relationshipRole: 'GRANDMOTHER',
    customRelationshipRole: null,
    permissionScope: _permission,
    aggregate: const FamilyHomeAggregate(
      overdue: 1,
      dueSoon: 1,
      inProgress: 1,
      alerts: 1,
    ),
  );
}

FamilyHomeGroupDetail _detail(
  String groupId, {
  bool empty = false,
  FamilyHomePermission permission = _permission,
}) {
  final suffix = groupId == 'group-a' ? 'a' : 'b';
  return FamilyHomeGroupDetail(
    careGroupId: groupId,
    motherDisplayName: 'Nguyễn Lan',
    todayReminders: empty
        ? []
        : [
            FamilyHomeTodayReminder(
              id: 'reminder-$suffix',
              title: 'Khám thai',
              type: 'APPOINTMENT',
              status: 'PENDING',
              scheduledAt: DateTime.utc(2026, 7, 30, 2),
              dueAt: DateTime.utc(2026, 7, 31),
              snoozedUntil: null,
              priority: 3,
            ),
          ],
    alerts: empty
        ? []
        : [
            FamilyHomeAlert(
              id: 'alert-$suffix',
              careGroupId: groupId,
              title: 'Cảnh báo',
              body: 'Cần kiểm tra',
              createdAt: DateTime.utc(2026, 7, 30),
              read: false,
            ),
          ],
    memberCount: 1,
    members: [
      FamilyHomeMember(
        memberId: 'member-$suffix',
        userId: 'user-$suffix',
        displayName: 'Thành viên $suffix',
        systemRole: 'MEMBER',
        relationshipRole: 'GRANDMOTHER',
        customRelationshipRole: null,
        joinedAt: DateTime.utc(2026, 7, 1),
      ),
    ],
    relationshipRole: 'GRANDMOTHER',
    customRelationshipRole: null,
    permissionScope: permission,
    sharedDataSummary: FamilyHomeSharedDataSummary(
      totalItems: empty ? 0 : 2,
      categories: [
        FamilyHomeSharedDataCategory(
          category: 'CALENDAR',
          permitted: true,
          itemCount: empty ? 0 : 1,
        ),
        FamilyHomeSharedDataCategory(
          category: 'ALERTS',
          permitted: true,
          itemCount: empty ? 0 : 1,
        ),
      ],
    ),
  );
}

const _permission = FamilyHomePermission(
  calendar: true,
  logs: false,
  alerts: true,
  checklistView: true,
  records: true,
);

Map<String, dynamic> _dashboardJson() {
  return {
    'groups': [
      {
        'id': 'group-a',
        'name': 'Nhóm A',
        'joinedAt': '2026-07-01T00:00:00Z',
        'lastActivityAt': '2026-07-29T00:00:00Z',
        'relationshipRole': 'GRANDMOTHER',
        'customRelationshipRole': null,
        'permissionScope': {
          'calendar': true,
          'logs': false,
          'alerts': true,
          'checklistView': true,
          'records': true,
          'quickNotes': true,
          'quickNoteWeight': true,
          'quickNoteHydration': false,
          'quickNoteEpds': false,
          'quickNoteFetalMovement': false,
        },
        'aggregate': {'overdue': 1, 'dueSoon': 1, 'inProgress': 1, 'alerts': 1},
      },
      {
        'id': 'group-b',
        'name': 'Nhóm B',
        'joinedAt': '2026-06-01T00:00:00Z',
        'lastActivityAt': null,
        'relationshipRole': 'OTHER',
        'customRelationshipRole': 'Dì',
        'permissionScope': {
          'calendar': false,
          'logs': false,
          'alerts': false,
          'records': false,
        },
        'aggregate': {'overdue': 0, 'dueSoon': 0, 'inProgress': 0, 'alerts': 0},
      },
    ],
    'globalAggregate': {
      'overdue': 7,
      'dueSoon': 5,
      'inProgress': 3,
      'alerts': 2,
    },
    'selectedCareGroupId': 'group-a',
    'selectedGroupDetail': {
      'careGroupId': 'group-a',
      'motherDisplayName': 'Nguyễn Lan',
      'todayReminders': [
        {
          'id': 'reminder-a',
          'title': 'Khám thai',
          'type': 'APPOINTMENT',
          'status': 'PENDING',
          'scheduledAt': '2026-07-30T02:00:00Z',
          'dueAt': '2026-07-30T02:00:00Z',
          'snoozedUntil': null,
          'priority': 3,
        },
      ],
      'alerts': [
        {
          'id': 'alert-a',
          'careGroupId': 'group-a',
          'title': 'Cảnh báo',
          'body': 'Cần kiểm tra',
          'createdAt': '2026-07-30T00:00:00Z',
          'read': false,
        },
      ],
      'memberCount': 1,
      'members': [
        {
          'memberId': 'member-a',
          'userId': 'user-a',
          'displayName': 'Bà An',
          'systemRole': 'MEMBER',
          'relationshipRole': 'GRANDMOTHER',
          'customRelationshipRole': null,
          'joinedAt': '2026-07-01T00:00:00Z',
        },
      ],
      'relationshipRole': 'GRANDMOTHER',
      'customRelationshipRole': null,
      'permissionScope': {
        'calendar': true,
        'logs': false,
        'alerts': true,
        'checklistView': true,
        'records': true,
        'quickNotes': true,
        'quickNoteWeight': true,
        'quickNoteHydration': false,
        'quickNoteEpds': false,
        'quickNoteFetalMovement': false,
      },
      'sharedDataSummary': {
        'totalItems': 2,
        'categories': [
          {'category': 'CALENDAR', 'permitted': true, 'itemCount': 1},
          {'category': 'ALERTS', 'permitted': true, 'itemCount': 1},
        ],
      },
    },
  };
}
