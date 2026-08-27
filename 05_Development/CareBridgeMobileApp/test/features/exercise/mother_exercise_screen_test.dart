import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/screens/mother_exercise_screen.dart';

void main() {
  testWidgets(
    'back button returns from Mother Exercise to the previous screen',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => Scaffold(
              body: Center(
                child: FilledButton(
                  onPressed: () => Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => const MotherExerciseScreen(),
                    ),
                  ),
                  child: const Text('Mở bài tập'),
                ),
              ),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Mở bài tập'));
      await tester.pumpAndSettle();

      expect(find.text('Bài tập cho mẹ'), findsOneWidget);
      expect(
        find.byKey(const Key('mother-exercise-back-button')),
        findsOneWidget,
      );
      expect(find.byTooltip('Quay lại'), findsOneWidget);
      expect(find.byTooltip('Lịch sử'), findsOneWidget);

      await tester.tap(find.byKey(const Key('mother-exercise-back-button')));
      await tester.pumpAndSettle();

      expect(find.text('Mở bài tập'), findsOneWidget);
      expect(find.text('Bài tập cho mẹ'), findsNothing);
    },
  );
}
