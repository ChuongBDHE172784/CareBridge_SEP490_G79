import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/healthRecords/screens/epds_screen.dart';

void main() {
  test('EPDS Vietnamese questionnaire contains exactly ten scored items', () {
    expect(epdsQuestions, hasLength(10));
    for (final question in epdsQuestions) {
      expect(question.options, hasLength(4));
      expect(question.options.map((option) => option.score).toSet(), {
        0,
        1,
        2,
        3,
      });
    }
  });

  test('EPDS total is the sum of all ten selected scores', () {
    expect(calculateEpdsScore(List<int?>.filled(10, 0)), 0);
    expect(calculateEpdsScore(List<int?>.filled(10, 3)), 30);
    expect(calculateEpdsScore([0, 1, 2, 3, 0, 1, 2, 3, 0, 1]), 13);
  });

  test('question ten assigns a positive score to every self-harm response', () {
    final scores = epdsQuestions[9].options
        .map((option) => option.score)
        .toList();
    expect(scores, [3, 2, 1, 0]);
  });

  test('EPDS result guidance is reconstructed for saved history', () {
    expect(epdsLevel(9), 'Nguy cơ hiện tại thấp');
    expect(epdsGuidance(9, 0), contains('Tiếp tục theo dõi'));
    expect(epdsLevel(13), 'Cần được đánh giá chuyên sâu');
    expect(epdsGuidance(13, 0), contains('đánh giá chuyên sâu'));
    expect(epdsGuidance(4, 1), contains('sức khỏe tâm thần ngay'));
  });

  test('stored EPDS answers can be restored for history detail', () {
    final answers = parseEpdsAnswers(
      '{"version":"EPDS_VI_NSW_2023","periodDays":7,"answers":[0,1,2,3,0,1,2,3,0,1]}',
    );

    expect(answers, [0, 1, 2, 3, 0, 1, 2, 3, 0, 1]);
    expect(parseEpdsAnswers('{"answers":[0,1]}'), isNull);
    expect(parseEpdsAnswers('legacy note'), isNull);
  });

  testWidgets('history detail renders a scrollable ten-question answer list', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: EpdsHistoryDetailScreen(
          completedAt: DateTime(2026, 7, 31),
          totalScore: 13,
          question10Score: 1,
          answers: const [0, 1, 2, 3, 0, 1, 2, 3, 0, 1],
        ),
      ),
    );

    expect(find.text('Chi tiết sàng lọc EPDS'), findsOneWidget);
    expect(find.text('Tổng điểm 13/30'), findsOneWidget);
    expect(find.text('Cần được đánh giá chuyên sâu'), findsOneWidget);
    expect(find.textContaining('sức khỏe tâm thần ngay'), findsOneWidget);
    expect(find.byIcon(Icons.check_circle_rounded), findsWidgets);

    await tester.scrollUntilVisible(
      find.textContaining('Câu 10.'),
      500,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.textContaining('Câu 10.'), findsOneWidget);
  });

  testWidgets('EPDS header opens the conditions and guidance note', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: EpdsScreen(journeyId: '00000000-0000-0000-0000-000000000001'),
      ),
    );

    expect(find.text('Sàng lọc tâm trạng EPDS'), findsOneWidget);
    await tester.tap(find.byTooltip('Điều kiện và hướng dẫn EPDS'));
    await tester.pumpAndSettle();

    expect(find.text('Điều kiện và hướng dẫn EPDS'), findsOneWidget);
    expect(find.textContaining('0–9 điểm'), findsOneWidget);
    expect(find.textContaining('Câu 10 có điểm 1–3'), findsOneWidget);
  });
}
