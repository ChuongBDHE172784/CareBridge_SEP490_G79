import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/core/network/api_client.dart';
import 'package:untitled/features/familySync/models/care_group_model.dart';
import 'package:untitled/features/familySync/models/family_permission_model.dart';
import 'package:untitled/features/familySync/screens/care_groups_screen.dart';
import 'package:untitled/features/familySync/screens/manage_family_permission_screen.dart';
import 'package:untitled/features/familySync/services/care_group_service.dart';

void main() {
  group('Mother health sharing permissions', () {
    test('parses two new metric flags with deny-by-default semantics', () {
      final legacy = FamilyPermission.fromJson({
        'memberId': 'member-1',
        'careGroupId': 'group-1',
        'calendar': false,
        'logs': false,
        'alerts': false,
        'records': false,
        'updatedAt': '2026-08-03T00:00:00Z',
      });
      final current = FamilyPermission.fromJson({
        'memberId': 'member-1',
        'careGroupId': 'group-1',
        'calendar': false,
        'logs': false,
        'alerts': false,
        'records': false,
        'checklistView': true,
        'checklistComplete': false,
        'quickNoteBloodPressure': true,
        'quickNoteBloodGlucose': true,
        'updatedAt': '2026-08-03T00:00:00Z',
      });

      expect(legacy.quickNoteBloodPressure, isFalse);
      expect(legacy.quickNoteBloodGlucose, isFalse);
      expect(current.quickNoteBloodPressure, isTrue);
      expect(current.quickNoteBloodGlucose, isTrue);
      expect(current.checklistView, isTrue);
      expect(current.checklistComplete, isFalse);
    });

    testWidgets('shows six real metric permissions and saves select all', (
      tester,
    ) async {
      final service = _FakeCareGroupService(permission: _permission());
      await tester.pumpWidget(
        _app(
          ManageFamilyPermissionScreen(
            groupId: 'group-1',
            member: _member(),
            service: service,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.drag(find.byType(ListView), const Offset(0, -520));
      await tester.pumpAndSettle();

      expect(find.text('Chỉ số sức khỏe'), findsOneWidget);
      for (final label in const [
        'Chỉ số BMI',
        'Cử động thai',
        'Huyết áp',
        'Nước',
        'Sàng lọc EPDS',
        'Đường huyết',
      ]) {
        expect(find.text(label), findsOneWidget);
      }
      expect(
        tester
            .widget<FilledButton>(
              find.byKey(const Key('permission-save-button')),
            )
            .onPressed,
        isNull,
      );

      await tester.tap(find.byKey(const Key('health-metrics-select-all')));
      await tester.pump();
      expect(find.text('6/6 chỉ số đang chia sẻ'), findsOneWidget);

      await tester.tap(find.byKey(const Key('permission-save-button')));
      await tester.pumpAndSettle();

      expect(service.lastUpdate?['quickNotes'], isTrue);
      expect(service.lastUpdate?['quickNoteWeight'], isTrue);
      expect(service.lastUpdate?['quickNoteFetalMovement'], isTrue);
      expect(service.lastUpdate?['quickNoteBloodPressure'], isTrue);
      expect(service.lastUpdate?['quickNoteHydration'], isTrue);
      expect(service.lastUpdate?['quickNoteEpds'], isTrue);
      expect(service.lastUpdate?['quickNoteBloodGlucose'], isTrue);
      expect(service.lastUpdate?['checklistView'], isFalse);
      expect(service.lastUpdate?['checklistComplete'], isFalse);
    });

    testWidgets('renders a safe retry state when permission loading fails', (
      tester,
    ) async {
      final service = _FakeCareGroupService(
        permission: _permission(),
        failPermissionLoads: 1,
      );
      await tester.pumpWidget(
        _app(
          ManageFamilyPermissionScreen(
            groupId: 'group-1',
            member: _member(),
            service: service,
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('permission-load-error')), findsOneWidget);
      expect(find.textContaining('Exception'), findsNothing);
      await tester.tap(find.byKey(const Key('permission-retry-button')));
      await tester.pumpAndSettle();
      await tester.drag(find.byType(ListView), const Offset(0, -520));
      await tester.pumpAndSettle();
      expect(find.text('Chỉ số sức khỏe'), findsOneWidget);
    });

    testWidgets('turning off parent clears every metric permission', (
      tester,
    ) async {
      final service = _FakeCareGroupService(
        permission: FamilyPermission(
          memberId: 'member-1',
          careGroupId: 'group-1',
          calendar: false,
          logs: false,
          alerts: false,
          records: false,
          quickNotes: true,
          quickNoteWeight: true,
          quickNoteFetalMovement: true,
          quickNoteBloodPressure: true,
          quickNoteHydration: true,
          quickNoteEpds: true,
          quickNoteBloodGlucose: true,
          updatedAt: DateTime(2026, 8, 3),
        ),
      );
      await tester.pumpWidget(
        _app(
          ManageFamilyPermissionScreen(
            groupId: 'group-1',
            member: _member(),
            service: service,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.drag(find.byType(ListView), const Offset(0, -520));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('health-metrics-parent-switch')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('permission-save-button')));
      await tester.pumpAndSettle();

      expect(service.lastUpdate?['quickNotes'], isFalse);
      expect(
        service.lastUpdate?.entries
            .where((entry) => entry.key != 'quickNotes')
            .every((entry) => entry.value == false),
        isTrue,
      );
    });

    testWidgets('failed save keeps changes dirty and allows retry', (
      tester,
    ) async {
      final service = _FakeCareGroupService(
        permission: _permission(),
        failSaves: 1,
      );
      await tester.pumpWidget(
        _app(
          ManageFamilyPermissionScreen(
            groupId: 'group-1',
            member: _member(),
            service: service,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.drag(find.byType(ListView), const Offset(0, -520));
      await tester.pumpAndSettle();

      await tester.tap(find.byKey(const Key('health-metrics-select-all')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('permission-save-button')));
      await tester.pumpAndSettle();

      expect(service.updateCalls, 1);
      expect(
        tester
            .widget<FilledButton>(
              find.byKey(const Key('permission-save-button')),
            )
            .onPressed,
        isNotNull,
      );

      await tester.tap(find.byKey(const Key('permission-save-button')));
      await tester.pumpAndSettle();

      expect(service.updateCalls, 2);
      expect(service.lastUpdate?['quickNoteBloodPressure'], isTrue);
      expect(service.lastUpdate?['quickNoteBloodGlucose'], isTrue);
    });
  });

  group('Mother care groups', () {
    testWidgets('renders truthful empty state', (tester) async {
      await tester.pumpWidget(
        _app(
          CareGroupsScreen(service: _FakeCareGroupService(groups: const [])),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('care-groups-empty')), findsOneWidget);
      expect(find.text('Bạn chưa có nhóm chăm sóc'), findsOneWidget);
    });

    testWidgets('renders API group data without appointment mock', (
      tester,
    ) async {
      final group = CareGroup(
        id: 'group-1',
        groupName: 'Gia đình An',
        memberCount: 1,
        members: [_member()],
      );
      await tester.pumpWidget(
        _app(CareGroupsScreen(service: _FakeCareGroupService(groups: [group]))),
      );
      await tester.pumpAndSettle();

      expect(find.text('Gia đình An'), findsOneWidget);
      expect(find.text('1 thành viên'), findsOneWidget);
      expect(find.textContaining('Khám thai tuần 28'), findsNothing);
    });

    testWidgets('renders retry state after API error', (tester) async {
      final service = _FakeCareGroupService(
        groups: const [],
        failGroupLoads: 1,
      );
      await tester.pumpWidget(_app(CareGroupsScreen(service: service)));
      await tester.pumpAndSettle();

      expect(find.byKey(const Key('care-groups-error')), findsOneWidget);
      await tester.tap(find.byKey(const Key('care-groups-retry-button')));
      await tester.pumpAndSettle();
      expect(find.byKey(const Key('care-groups-empty')), findsOneWidget);
    });

    testWidgets('creates new care group without controller assertion error', (
      tester,
    ) async {
      final service = _FakeCareGroupService(groups: const []);
      await tester.pumpWidget(_app(CareGroupsScreen(service: service)));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Tạo nhóm đầu tiên'));
      await tester.pumpAndSettle();

      expect(find.text('Tạo nhóm chăm sóc'), findsOneWidget);
      await tester.enterText(find.byType(TextField), 'Gia đình Lan');
      await tester.tap(find.widgetWithText(FilledButton, 'Tạo nhóm'));
      await tester.pumpAndSettle();

      expect(service.createdGroupName, 'Gia đình Lan');
      expect(find.text('Đã tạo nhóm chăm sóc.'), findsOneWidget);
    });

    testWidgets('removes a deleted care group from the visible list', (
      tester,
    ) async {
      final group = CareGroup(
        id: 'group-delete',
        groupName: 'Nhóm cần xóa',
        memberCount: 1,
        members: [_member()],
      );
      final service = _FakeCareGroupService(groups: [group]);
      await tester.pumpWidget(_app(CareGroupsScreen(service: service)));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Tùy chọn nhóm'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Xóa nhóm'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Xóa nhóm'));
      await tester.pumpAndSettle();

      expect(service.deletedGroupId, 'group-delete');
      expect(find.text('Nhóm cần xóa'), findsNothing);
      expect(find.text('Đã xóa nhóm chăm sóc.'), findsOneWidget);
      expect(find.byKey(const Key('care-groups-empty')), findsOneWidget);
    });

    testWidgets('keeps group visible when it still has accepted members', (
      tester,
    ) async {
      final group = CareGroup(
        id: 'group-with-members',
        groupName: 'Nhóm còn thành viên',
        memberCount: 2,
        members: [_member()],
      );
      final service = _FakeCareGroupService(
        groups: [group],
        deleteError: ApiException(
          409,
          '{"error":{"code":"FAM-069"},"message":"Remove members first"}',
        ),
      );
      await tester.pumpWidget(_app(CareGroupsScreen(service: service)));
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Tùy chọn nhóm'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Xóa nhóm'));
      await tester.pumpAndSettle();
      await tester.tap(find.widgetWithText(FilledButton, 'Xóa nhóm'));
      await tester.pumpAndSettle();

      expect(service.deletedGroupId, 'group-with-members');
      expect(find.text('Nhóm còn thành viên'), findsOneWidget);
      expect(find.text('Đã xóa nhóm chăm sóc.'), findsNothing);
      expect(
        find.text(
          'Hãy xóa tất cả thành viên khác trước khi xóa nhóm chăm sóc.',
        ),
        findsOneWidget,
      );
    });
  });
}

Widget _app(Widget child) => MaterialApp(home: child);

CareGroupMember _member() => const CareGroupMember(
  memberId: 'member-1',
  displayName: 'Minh Anh',
  memberRole: 'MEMBER',
  inviteStatus: 'ACCEPTED',
);

FamilyPermission _permission() => FamilyPermission(
  memberId: 'member-1',
  careGroupId: 'group-1',
  calendar: false,
  logs: false,
  alerts: false,
  records: false,
  quickNotes: true,
  updatedAt: DateTime(2026, 8, 3),
);

class _FakeCareGroupService extends CareGroupService {
  _FakeCareGroupService({
    FamilyPermission? permission,
    this.groups = const [],
    this.failPermissionLoads = 0,
    this.failGroupLoads = 0,
    this.failSaves = 0,
    this.deleteError,
  }) : permission = permission ?? _permission();

  final FamilyPermission permission;
  final List<CareGroup> groups;
  int failPermissionLoads;
  int failGroupLoads;
  int failSaves;
  final Object? deleteError;
  int updateCalls = 0;
  Map<String, bool?>? lastUpdate;

  String? createdGroupName;
  String? deletedGroupId;

  @override
  Future<Map<String, dynamic>> createCareGroup(
    String groupName, {
    String? description,
  }) async {
    createdGroupName = groupName;
    return {'id': 'group-new', 'groupName': groupName};
  }

  @override
  Future<List<CareGroup>> listMyGroups() async {
    if (failGroupLoads-- > 0) throw Exception('private server detail');
    return groups;
  }

  @override
  Future<void> deleteCareGroup(String groupId) async {
    deletedGroupId = groupId;
    if (deleteError != null) throw deleteError!;
  }

  @override
  Future<FamilyPermission> getFamilyPermission(
    String groupId,
    String memberId,
  ) async {
    if (failPermissionLoads-- > 0) throw Exception('private server detail');
    return permission;
  }

  @override
  Future<FamilyPermission> updateFamilyPermission(
    String groupId,
    String memberId, {
    bool? calendar,
    bool? logs,
    bool? alerts,
    bool? records,
    bool? checklistView,
    bool? checklistComplete,
    bool? quickNotes,
    bool? quickNoteWeight,
    bool? quickNoteHydration,
    bool? quickNoteEpds,
    bool? quickNoteFetalMovement,
    bool? quickNoteBloodPressure,
    bool? quickNoteBloodGlucose,
  }) async {
    updateCalls += 1;
    lastUpdate = {
      'checklistView': checklistView,
      'checklistComplete': checklistComplete,
      'quickNotes': quickNotes,
      'quickNoteWeight': quickNoteWeight,
      'quickNoteHydration': quickNoteHydration,
      'quickNoteEpds': quickNoteEpds,
      'quickNoteFetalMovement': quickNoteFetalMovement,
      'quickNoteBloodPressure': quickNoteBloodPressure,
      'quickNoteBloodGlucose': quickNoteBloodGlucose,
    };
    if (failSaves-- > 0) throw Exception('private server detail');
    return FamilyPermission(
      memberId: memberId,
      careGroupId: groupId,
      calendar: calendar ?? permission.calendar,
      logs: logs ?? permission.logs,
      alerts: alerts ?? permission.alerts,
      records: records ?? permission.records,
      checklistView: checklistView ?? permission.checklistView,
      checklistComplete: checklistComplete ?? permission.checklistComplete,
      quickNotes: quickNotes ?? permission.quickNotes,
      quickNoteWeight: quickNoteWeight ?? permission.quickNoteWeight,
      quickNoteHydration: quickNoteHydration ?? permission.quickNoteHydration,
      quickNoteEpds: quickNoteEpds ?? permission.quickNoteEpds,
      quickNoteFetalMovement:
          quickNoteFetalMovement ?? permission.quickNoteFetalMovement,
      quickNoteBloodPressure:
          quickNoteBloodPressure ?? permission.quickNoteBloodPressure,
      quickNoteBloodGlucose:
          quickNoteBloodGlucose ?? permission.quickNoteBloodGlucose,
      updatedAt: DateTime(2026, 8, 3),
    );
  }
}
