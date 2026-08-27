import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/exercise/models/exercise_model.dart';
import 'package:untitled/features/exercise/screens/exercise_detail_screen.dart';

void main() {
  const plank = ExerciseDetail(
    id: 'plank-id',
    title: 'Plank cơ bản',
    description: 'Giữ thân người thành một đường thẳng.',
    trimesterScope: 'ALL',
    difficultyLevel: 'MEDIUM',
    durationMinutes: 3,
    instructionContent: 'Chống khuỷu tay. Siết nhẹ cơ bụng. Thở đều.',
    safetyWarning: 'Dừng lại nếu thấy đau hoặc khó thở.',
    supportsPostureAnalysis: true,
  );

  test('maps local media only for Bicep Curl and Lunge', () {
    expect(
      ExerciseMediaAssets.forExercise('Cuốn tạ bắp tay (Bicep Curl)'),
      ExerciseMediaAssets.bicepCurl,
    );
    expect(
      ExerciseMediaAssets.forExercise('Lunge bước chùng'),
      ExerciseMediaAssets.lunge,
    );
    expect(ExerciseMediaAssets.forExercise('Plank cơ bản'), isNull);
    expect(ExerciseMediaAssets.forExercise('Squat cơ bản'), isNull);
  });

  test(
    'keeps temporary Plank and Squat media blank even when API media exists',
    () {
      final plankWithServerMedia = ExerciseDetail(
        id: 'plank',
        title: 'Plank cơ bản',
        description: 'Giữ thân người thẳng.',
        trimesterScope: 'ALL',
        difficultyLevel: 'MEDIUM',
        durationMinutes: 3,
        instructionContent: 'Giữ tư thế.',
        mediaUrl: 'https://example.test/plank.mp4',
        safetyWarning: 'Dừng lại nếu khó chịu.',
        supportsPostureAnalysis: true,
      );
      expect(plankWithServerMedia.detailMediaUrl, isNull);
    },
  );

  test('turns instruction content into ordered steps', () {
    expect(plank.instructionSteps, [
      'Chống khuỷu tay.',
      'Siết nhẹ cơ bụng.',
      'Thở đều.',
    ]);
  });

  testWidgets('shows blank-media state, instructions, warning, and CTA', (
    tester,
  ) async {
    var startCount = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: ExerciseDetailScreen(
          exercise: plank,
          onStart: () async => startCount++,
        ),
      ),
    );
    await tester.scrollUntilVisible(
      find.text('Các bước thực hiện'),
      400,
      scrollable: find.byType(Scrollable),
    );
    expect(find.text('Media hướng dẫn sẽ được bổ sung.'), findsOneWidget);
    expect(find.text('Các bước thực hiện'), findsOneWidget);
    expect(find.text('Dừng lại nếu thấy đau hoặc khó thở.'), findsOneWidget);
    await tester.tap(find.byKey(const Key('exercise-detail-start-button')));
    await tester.pump();
    expect(startCount, 1);
  });
}
