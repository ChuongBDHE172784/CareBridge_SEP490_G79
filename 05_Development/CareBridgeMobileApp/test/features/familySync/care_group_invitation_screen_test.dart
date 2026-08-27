import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/familySync/models/care_group_model.dart';
import 'package:untitled/features/familySync/screens/care_group_invitation_screen.dart';

void main() {
  group('CareGroupInvitationScreen UI & Logic Tests', () {
    testWidgets('formats title, translates role to Vietnamese, and removes hardcoded permissions/purpose',
        (tester) async {
      const inv = PendingInvitation(
        groupId: 'test-group-id',
        groupName: 'Nhóm gia đình Bé An',
        memberRole: 'MEMBER',
      );

      await tester.pumpWidget(
        const MaterialApp(
          home: CareGroupInvitationScreen(invitation: inv),
        ),
      );
      await tester.pumpAndSettle();

      // Check title formatting (no duplicated 'Nhóm chăm sóc')
      expect(find.text('Bạn đã được mời tham gia Nhóm gia đình Bé An.'), findsOneWidget);
      expect(find.textContaining('nhóm chăm sóc Nhóm'), findsNothing);

      // Check Vietnamese role translation
      expect(find.text('Vai trò: Thành viên'), findsOneWidget);
      expect(find.textContaining('MEMBER'), findsNothing);

      // Verify removal of hardcoded permissions and purpose sections
      expect(find.text('QUYỀN HẠN'), findsNothing);
      expect(find.text('MỤC ĐÍCH'), findsNothing);
      expect(find.text('Xem lịch tiêm chủng'), findsNothing);
      expect(find.text('Hỗ trợ chăm sóc bé'), findsNothing);

      // Verify Action Buttons are present for pending state
      expect(find.text('Chấp nhận'), findsOneWidget);
      expect(find.text('Từ chối'), findsOneWidget);
    });
  });
}
