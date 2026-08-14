import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:untitled/features/emergency/screens/emergency_map_screen.dart';
import 'package:untitled/features/familySync/services/family_home_service.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';

void main() {
  group('Family Member Emergency Map Navigation', () {
    testWidgets(
      'renders Emergency FAB and shortcut button and navigates to /emergency/map',
      (tester) async {
        Uri? openedUri;
        final router = GoRouter(
          initialLocation: '/',
          routes: [
            GoRoute(
              path: '/',
              builder: (_, _) => FamilyMemberHomeScreen(
                dashboardLoader: ({selectedCareGroupId}) async =>
                    const FamilyHomeSnapshot(
                  groups: [],
                  globalAggregate: FamilyHomeAggregate(
                    overdue: 0,
                    dueSoon: 0,
                    inProgress: 0,
                    alerts: 0,
                  ),
                  selectedCareGroupId: null,
                  selectedGroupDetail: null,
                ),
              ),
            ),
            GoRoute(
              path: '/emergency/map',
              builder: (_, state) {
                openedUri = state.uri;
                return const Scaffold(body: Text('TrackAsia Map'));
              },
            ),
          ],
        );
        addTearDown(router.dispose);

        await tester.pumpWidget(MaterialApp.router(routerConfig: router));
        await tester.pumpAndSettle();

        // 1. Verify FAB is present and styled
        final fabFinder = find.byKey(const Key('family-emergency-map-fab'));
        expect(fabFinder, findsOneWidget);
        expect(tester.getSize(fabFinder), const Size.square(64));

        final fabMaterial = tester.widget<Material>(
          find.descendant(of: fabFinder, matching: find.byType(Material)),
        );
        expect(fabMaterial.shape, isA<CircleBorder>());

        // 2. Verify Shortcut 'Cơ sở y tế' is present
        expect(find.text('Cơ sở y tế'), findsOneWidget);

        // 3. Tap FAB and verify navigation
        await tester.tap(fabFinder);
        await tester.pumpAndSettle();

        expect(openedUri?.path, '/emergency/map');
        expect(openedUri?.queryParameters['mode'], 'manual');
        expect(openedUri?.queryParameters['stage'], 'PREGNANCY');
        expect(find.text('TrackAsia Map'), findsOneWidget);
      },
    );

    testWidgets(
      'resolves emergency stage from mother journey when available',
      (tester) async {
        Uri? openedUri;
        final router = GoRouter(
          initialLocation: '/',
          routes: [
            GoRoute(
              path: '/',
              builder: (_, _) => FamilyMemberHomeScreen(
                dashboardLoader: ({selectedCareGroupId}) async =>
                    FamilyHomeSnapshot(
                  groups: [
                    FamilyHomeGroup(
                      id: 'cg-1',
                      name: 'Gia đình bé Sam',
                      joinedAt: DateTime(2026, 1, 1),
                      lastActivityAt: DateTime(2026, 8, 1),
                      relationshipRole: 'HUSBAND',
                      customRelationshipRole: null,
                      permissionScope: const FamilyHomePermission(
                        calendar: true,
                        logs: true,
                        alerts: true,
                        checklistView: true,
                        records: true,
                      ),
                      aggregate: const FamilyHomeAggregate(
                        overdue: 0,
                        dueSoon: 0,
                        inProgress: 0,
                        alerts: 0,
                      ),
                    ),
                  ],
                  globalAggregate: const FamilyHomeAggregate(
                    overdue: 0,
                    dueSoon: 0,
                    inProgress: 0,
                    alerts: 0,
                  ),
                  selectedCareGroupId: 'cg-1',
                  selectedGroupDetail: FamilyHomeGroupDetail(
                    careGroupId: 'cg-1',
                    motherDisplayName: 'Mẹ Bầu Sam',
                    motherJourney: const FamilyMotherJourney(
                      journeyId: 'j-1',
                      journeyType: 'POSTPARTUM',
                      status: 'ACTIVE',
                    ),
                    todayReminders: const [],
                    alerts: const [],
                    memberCount: 2,
                    members: const [],
                    relationshipRole: 'HUSBAND',
                    customRelationshipRole: null,
                    permissionScope: const FamilyHomePermission(
                      calendar: true,
                      logs: true,
                      alerts: true,
                      checklistView: true,
                      records: true,
                    ),
                    sharedDataSummary: const FamilyHomeSharedDataSummary(
                      totalItems: 0,
                      categories: [],
                    ),
                  ),
                ),
              ),
            ),
            GoRoute(
              path: '/emergency/map',
              builder: (_, state) {
                openedUri = state.uri;
                return const Scaffold(body: Text('TrackAsia Map'));
              },
            ),
          ],
        );
        addTearDown(router.dispose);

        await tester.pumpWidget(MaterialApp.router(routerConfig: router));
        await tester.pumpAndSettle();

        // Tap the shortcut 'Cơ sở y tế'
        await tester.tap(find.text('Cơ sở y tế'));
        await tester.pumpAndSettle();

        expect(openedUri?.path, '/emergency/map');
        expect(openedUri?.queryParameters['mode'], 'manual');
        expect(openedUri?.queryParameters['stage'], 'POSTPARTUM');
        expect(find.text('TrackAsia Map'), findsOneWidget);
      },
    );

    testWidgets(
      'hides family location share button when opening EmergencyMapScreen for family role',
      (tester) async {
        await tester.pumpWidget(
          const MaterialApp(
            home: EmergencyMapScreen(
              showFamilyLocationShare: false,
            ),
          ),
        );
        await tester.pumpAndSettle();

        // 115 emergency call is present
        expect(find.byKey(const Key('emergency-call')), findsOneWidget);
        expect(find.text('Gọi cấp cứu 115'), findsOneWidget);

        // Location share button for family is HIDDEN
        expect(find.byKey(const Key('emergency-family-alert')), findsNothing);
        expect(find.byKey(const Key('family-alert')), findsNothing);
        expect(find.text('Gửi vị trí'), findsNothing);
      },
    );
  });
}
