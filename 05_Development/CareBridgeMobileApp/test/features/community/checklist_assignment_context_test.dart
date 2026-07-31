import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/community/models/checklist_assignment_context.dart';

void main() {
  test('baby-care assignment never forwards a maternal journey', () {
    final context = ChecklistAssignmentContext.resolve(
      templateStage: 'BABY_CARE',
      journeyId: 'maternal-journey',
      lifecycleMode: false,
    );

    expect(context.canAssign, isTrue);
    expect(context.journeyId, isNull);
  });

  test('baby-care assignment is available without a maternal journey', () {
    final context = ChecklistAssignmentContext.resolve(
      templateStage: 'BABY_CARE',
      journeyId: null,
      lifecycleMode: false,
    );

    expect(context.canAssign, isTrue);
  });

  test('maternal template still requires journey outside lifecycle mode', () {
    final context = ChecklistAssignmentContext.resolve(
      templateStage: 'PREGNANCY',
      journeyId: null,
      lifecycleMode: false,
    );

    expect(context.canAssign, isFalse);
  });
}
