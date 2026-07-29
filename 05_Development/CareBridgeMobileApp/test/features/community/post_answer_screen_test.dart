import 'package:untitled/features/community/screens/post_answer_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('states that a new answer is published immediately', (
    tester,
  ) async {
    await tester.pumpWidget(
      const MaterialApp(home: PostAnswerScreen(questionId: 'question-1')),
    );

    expect(
      find.text('Câu trả lời của bạn sẽ hiển thị ngay sau khi đăng.'),
      findsOneWidget,
    );
    expect(find.text('Đang chờ kiểm duyệt'), findsNothing);
    expect(find.text('Chụp ảnh'), findsOneWidget);
    expect(find.text('Thư viện'), findsOneWidget);
  });
}
