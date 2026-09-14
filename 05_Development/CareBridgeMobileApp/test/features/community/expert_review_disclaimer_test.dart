import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/expert_reviewer_model.dart';
import 'package:untitled/features/community/widgets/expert_review_disclaimer_card.dart';

void main() {
  group('MedicalContentDisclaimerBanner & ExpertReviewerCard Tests', () {
    testWidgets('MedicalContentDisclaimerBanner renders disclaimer and opens modal', (
      tester,
    ) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: MedicalContentDisclaimerBanner(contentType: 'bài viết'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Thông tin và sản phẩm gợi ý trong bài viết chỉ mang tính chất tham khảo'),
        findsOneWidget,
      );
      expect(find.textContaining('Xem thêm'), findsOneWidget);

      // Tap "Xem thêm"
      await tester.tap(find.textContaining('Xem thêm'));
      await tester.pumpAndSettle();

      // Verify modal appears with medical warning
      expect(find.text('Khuyến cáo y khoa'), findsOneWidget);
      expect(find.text('Đã hiểu'), findsOneWidget);

      // Dismiss modal
      await tester.ensureVisible(find.text('Đã hiểu'));
      await tester.tap(find.text('Đã hiểu'));
      await tester.pumpAndSettle();
      expect(find.text('Khuyến cáo y khoa'), findsNothing);
    });

    testWidgets('ExpertReviewerCard renders doctor info, badge, and opens details modal', (
      tester,
    ) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: ExpertReviewerCard(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Bác sĩ'), findsOneWidget);
      expect(find.text('Nguyễn Huỳnh Kim Ngân'), findsOneWidget);
      expect(find.text('Đã kiểm duyệt nội dung'), findsOneWidget);
      expect(
        find.textContaining('Bệnh viện Thống Nhất'),
        findsOneWidget,
      );
      expect(find.text('Xem thêm thông tin'), findsOneWidget);

      // Tap "Xem thêm thông tin"
      await tester.tap(find.text('Xem thêm thông tin'));
      await tester.pumpAndSettle();

      // Verify details modal
      expect(find.text('Thông tin chuyên môn & Công tác'), findsOneWidget);
      expect(find.text('Bệnh viện Thống Nhất TP.HCM'), findsOneWidget);
      expect(find.text('Đóng'), findsOneWidget);

      // Close modal
      await tester.ensureVisible(find.text('Đóng'));
      await tester.tap(find.text('Đóng'));
      await tester.pumpAndSettle();
      expect(find.text('Thông tin chuyên môn & Công tác'), findsNothing);
    });

    testWidgets('ExpertReviewerCard renders real database expert reviewer model correctly', (
      tester,
    ) async {
      final reviewer = ExpertReviewer(
        expertId: '6e2acae8-9e1e-4aa6-b951-9ab4740e8a27',
        name: 'BS Đỗ Hải Long',
        professionalTitle: 'BS.CKII',
        specialty: 'Sản khoa',
        workplace: 'Bệnh viện Từ Dũ',
        bio: 'Theo dõi thai kỳ nguy cơ cao, tư vấn tiền sản và chăm sóc sau sinh.',
        verificationStatus: 'Đã kiểm duyệt nội dung',
        approvedAt: DateTime(2026, 9, 14),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: SingleChildScrollView(
              child: ExpertReviewerCard(reviewer: reviewer),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('BS.CKII'), findsOneWidget);
      expect(find.text('BS Đỗ Hải Long'), findsOneWidget);
      expect(find.text('Đã kiểm duyệt nội dung'), findsOneWidget);
      expect(find.textContaining('Theo dõi thai kỳ nguy cơ cao'), findsOneWidget);

      // Tap "Xem thêm thông tin"
      await tester.tap(find.text('Xem thêm thông tin'));
      await tester.pumpAndSettle();

      expect(find.text('Sản khoa'), findsOneWidget);
      expect(find.text('Bệnh viện Từ Dũ'), findsOneWidget);
      expect(find.text('14/09/2026'), findsOneWidget);

      await tester.ensureVisible(find.text('Đóng'));
      await tester.tap(find.text('Đóng'));
      await tester.pumpAndSettle();
    });

    testWidgets('Checklist variant of MedicalContentDisclaimerBanner shows checklist copy', (
      tester,
    ) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: MedicalContentDisclaimerBanner(contentType: 'mục checklist'),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(
        find.textContaining('mục checklist này chỉ mang tính chất tham khảo'),
        findsOneWidget,
      );
    });
  });
}
