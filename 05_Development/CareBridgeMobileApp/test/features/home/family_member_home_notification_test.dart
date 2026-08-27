import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/familySync/services/family_home_service.dart';
import 'package:untitled/features/home/screens/family_member_home_screen.dart';

void main() {
  group('Family Member Home Screen Notification Bell', () {
    testWidgets('renders Notification Bell icon instead of old group icon',
        (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: FamilyMemberHomeScreen(
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
      );
      await tester.pumpAndSettle();

      // Verify header no longer has tooltip 'Nhóm chăm sóc'
      expect(find.byTooltip('Nhóm chăm sóc'), findsNothing);

      // Verify new notification bell icon is present
      expect(find.byKey(const Key('family-notification-bell')), findsOneWidget);
      expect(find.byIcon(Icons.notifications), findsOneWidget);
      expect(find.byTooltip('Thông báo'), findsOneWidget);
    });
  });
}
