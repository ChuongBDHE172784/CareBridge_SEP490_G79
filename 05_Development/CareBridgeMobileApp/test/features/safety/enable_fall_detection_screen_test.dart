import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/familySync/models/care_group_model.dart';
import 'package:untitled/features/familySync/screens/care_groups_screen.dart';
import 'package:untitled/features/familySync/services/care_group_service.dart';
import 'package:untitled/features/safety/models/safety_config_model.dart';
import 'package:untitled/features/safety/screens/enable_fall_detection_screen.dart';
import 'package:untitled/features/safety/services/safety_service.dart';

class _FakeCareGroupService extends CareGroupService {
  final List<CareGroup> groups;
  _FakeCareGroupService({required this.groups});

  @override
  Future<List<CareGroup>> listMyGroups() async => groups;
}

class _FakeSafetyService extends SafetyService {
  final SafetyConfig config;
  _FakeSafetyService({required this.config});

  @override
  Future<SafetyConfig> getConfig() async => config;
}

void main() {
  group('careGroupsHaveAnyFamilyMember unit tests', () {
    test('returns false when user has no care groups', () {
      expect(careGroupsHaveAnyFamilyMember([]), isFalse);
    });

    test('returns false when all groups have only 1 member (owner only)', () {
      final groups = [
        const CareGroup(id: 'g1', groupName: 'Gia đình nội', memberCount: 1),
        const CareGroup(id: 'g2', groupName: 'Gia đình ngoại', memberCount: 1),
      ];
      expect(careGroupsHaveAnyFamilyMember(groups), isFalse);
    });

    test('returns false when group has memberCount > 1 but isActive is false', () {
      final groups = [
        const CareGroup(
          id: 'g1',
          groupName: 'Gia đình cũ',
          memberCount: 3,
          isActive: false,
        ),
      ];
      expect(careGroupsHaveAnyFamilyMember(groups), isFalse);
    });

    test(
      'returns true when only 1 of multiple groups has family members (memberCount > 1)',
      () {
        final groups = [
          const CareGroup(id: 'g1', groupName: 'Gia đình nội', memberCount: 1),
          const CareGroup(id: 'g2', groupName: 'Gia đình ngoại', memberCount: 2),
        ];
        expect(careGroupsHaveAnyFamilyMember(groups), isTrue);
      },
    );

    test('returns true when members list contains an accepted non-owner member', () {
      final groups = [
        const CareGroup(
          id: 'g1',
          groupName: 'Gia đình',
          memberCount: 2,
          members: [
            CareGroupMember(
              memberId: 'm1',
              displayName: 'Mẹ',
              memberRole: 'OWNER',
              inviteStatus: 'ACCEPTED',
            ),
            CareGroupMember(
              memberId: 'm2',
              displayName: 'Chồng',
              memberRole: 'MEMBER',
              familyRelationshipRole: 'CHONG',
              inviteStatus: 'ACCEPTED',
            ),
          ],
        ),
      ];
      expect(careGroupsHaveAnyFamilyMember(groups), isTrue);
    });
  });

  group('EnableFallDetectionScreen widget tests', () {
    const testConfig = SafetyConfig(
      fallDetectionEnabled: false,
      sensitivityLevel: 'MEDIUM',
      emergencyAutoAlert: true,
      locationSharingEnabled: false,
      countdownSeconds: 30,
      sensorPermissionGranted: false,
    );

    testWidgets(
      'when user has no family members, turning on switch shows modal dialog and allows navigation to care groups',
      (tester) async {
        final fakeCareGroupService = _FakeCareGroupService(
          groups: [
            const CareGroup(id: 'g1', groupName: 'Gia đình nhỏ', memberCount: 1),
          ],
        );
        final fakeSafetyService = _FakeSafetyService(config: testConfig);

        await tester.pumpWidget(
          MaterialApp(
            home: EnableFallDetectionScreen(
              careGroupService: fakeCareGroupService,
              safetyService: fakeSafetyService,
            ),
          ),
        );
        await tester.pumpAndSettle();

        // Screen loaded: switch should be initialized to false because user has no family members
        final switchFinder = find.byKey(const Key('auto-family-alert-switch'));
        expect(switchFinder, findsOneWidget);
        final initialSwitch = tester.widget<Switch>(switchFinder);
        expect(initialSwitch.value, isFalse);

        // Tap the switch to turn it ON
        await tester.ensureVisible(switchFinder);
        await tester.tap(switchFinder);
        await tester.pumpAndSettle();

        // Switch stays false
        final afterTapSwitch = tester.widget<Switch>(switchFinder);
        expect(afterTapSwitch.value, isFalse);

        // Modal dialog is shown
        expect(find.text('Chưa có người thân'), findsOneWidget);
        expect(
          find.textContaining('Bạn chưa có người thân nào trong nhóm gia đình'),
          findsOneWidget,
        );
        expect(
          find.byKey(const Key('no-family-modal-cancel-button')),
          findsOneWidget,
        );
        expect(
          find.byKey(const Key('no-family-modal-navigate-care-group-button')),
          findsOneWidget,
        );

        // Tap "Nhóm chăm sóc" button
        await tester.tap(
          find.byKey(const Key('no-family-modal-navigate-care-group-button')),
        );
        await tester.pumpAndSettle();

        // CareGroupsScreen should now be navigated to
        expect(find.byType(CareGroupsScreen), findsOneWidget);
      },
    );

    testWidgets(
      'when user has at least 1 group with family members, turning on switch enables it without modal',
      (tester) async {
        final fakeCareGroupService = _FakeCareGroupService(
          groups: [
            const CareGroup(id: 'g1', groupName: 'Gia đình nội', memberCount: 1),
            const CareGroup(id: 'g2', groupName: 'Gia đình ngoại', memberCount: 2),
          ],
        );
        // Initial config has emergencyAutoAlert = false so user starts with switch off
        final fakeSafetyService = _FakeSafetyService(
          config: const SafetyConfig(
            fallDetectionEnabled: false,
            sensitivityLevel: 'MEDIUM',
            emergencyAutoAlert: false,
            locationSharingEnabled: false,
            countdownSeconds: 30,
            sensorPermissionGranted: false,
          ),
        );

        await tester.pumpWidget(
          MaterialApp(
            home: EnableFallDetectionScreen(
              careGroupService: fakeCareGroupService,
              safetyService: fakeSafetyService,
            ),
          ),
        );
        await tester.pumpAndSettle();

        final switchFinder = find.byKey(const Key('auto-family-alert-switch'));
        expect(switchFinder, findsOneWidget);
        final initialSwitch = tester.widget<Switch>(switchFinder);
        expect(initialSwitch.value, isFalse);

        // Tap the switch to turn it ON
        await tester.ensureVisible(switchFinder);
        await tester.tap(switchFinder);
        await tester.pumpAndSettle();

        // Switch turns to true
        final afterTapSwitch = tester.widget<Switch>(switchFinder);
        expect(afterTapSwitch.value, isTrue);

        // No modal dialog is shown
        expect(find.text('Chưa có người thân'), findsNothing);
      },
    );
  });
}
