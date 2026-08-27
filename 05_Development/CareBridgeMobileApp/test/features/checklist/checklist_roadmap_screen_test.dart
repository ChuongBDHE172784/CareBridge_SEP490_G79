import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/models/checklist_roadmap_model.dart';
import 'package:untitled/features/checklist/screens/checklist_roadmap_screen.dart';
import 'package:untitled/features/checklist/services/checklist_roadmap_service.dart';
import 'package:untitled/features/journey/models/journey_model.dart';
import 'package:untitled/features/journey/services/journey_service.dart';

class _FakeRoadmapService extends ChecklistRoadmapService {
  final List<ChecklistRoadmapMilestone> milestones;

  _FakeRoadmapService(this.milestones);

  @override
  Future<List<ChecklistRoadmapMilestone>> loadRoadmap({
    int currentWeek = 24,
    String stage = 'PREGNANCY',
  }) async {
    return milestones;
  }
}

class _FakeJourneyService extends JourneyService {
  final int pregnancyWeek;

  _FakeJourneyService({this.pregnancyWeek = 12});

  @override
  Future<JourneyDashboard> getDashboard({String? journeyId}) async {
    return JourneyDashboard(
      journeyId: 'j-1',
      journeyType: 'PREGNANCY',
      status: 'ACTIVE_PREGNANCY',
      sourceWeekNumber: pregnancyWeek,
      completedGestationalWeek: pregnancyWeek - 1,
    );
  }
}

void main() {
  final sampleMilestones = [
    ChecklistRoadmapMilestone(
      id: 'm-1',
      title: 'Khám và xét nghiệm 20 tuần đầu',
      description: 'Khám lần đầu',
      stage: 'PREGNANCY',
      startWeek: 1,
      endWeek: 20,
      status: ChecklistMilestoneStatus.current,
      tasks: [
        ChecklistRoadmapTask(
          id: 't-1',
          title: 'Khám thai lần 1',
          category: 'Khám thai',
          isRequired: true,
          completed: false,
          dueWeek: 1,
        ),
      ],
    ),
    ChecklistRoadmapMilestone(
      id: 'm-2',
      title: 'Theo dõi và siêu âm tuần 21 - 25',
      description: 'Siêu âm hình thái học',
      stage: 'PREGNANCY',
      startWeek: 21,
      endWeek: 25,
      status: ChecklistMilestoneStatus.upcoming,
      tasks: [
        ChecklistRoadmapTask(
          id: 't-2',
          title: 'Siêu âm 4D',
          category: 'Siêu âm',
          isRequired: true,
          completed: false,
          dueWeek: 21,
        ),
      ],
    ),
    ChecklistRoadmapMilestone(
      id: 'm-0',
      title: 'Chuẩn bị trước mang thai',
      description: 'Tiền thai kỳ',
      stage: 'PRE_PREGNANCY',
      startWeek: 1,
      endWeek: 1,
      status: ChecklistMilestoneStatus.completed,
      tasks: [
        ChecklistRoadmapTask(
          id: 't-0',
          title: 'Bổ sung axit folic',
          category: 'Dinh dưỡng',
          isRequired: true,
          completed: true,
          dueWeek: 1,
        ),
      ],
    ),
  ];

  Widget buildTestWidget({
    ChecklistRoadmapService? roadmapService,
    JourneyService? journeyService,
  }) {
    return MaterialApp(
      home: ChecklistRoadmapScreen(
        service: roadmapService ?? _FakeRoadmapService(sampleMilestones),
        journeyService: journeyService ?? _FakeJourneyService(pregnancyWeek: 12),
      ),
    );
  }

  testWidgets('renders ChecklistRoadmapScreen with header and all milestone cards', (tester) async {
    await tester.pumpWidget(buildTestWidget());
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('checklist-roadmap-screen')), findsOneWidget);
    expect(find.text('Lộ trình checklist'), findsOneWidget);
    expect(find.textContaining('Tuần thứ 12'), findsOneWidget);
    expect(find.text('Khám và xét nghiệm 20 tuần đầu'), findsOneWidget);
    expect(find.text('Theo dõi và siêu âm tuần 21 - 25'), findsOneWidget);
  });

  testWidgets('filters milestones by tabs correctly', (tester) async {
    await tester.pumpWidget(buildTestWidget());
    await tester.pumpAndSettle();

    // Click Tương lai
    await tester.tap(find.byKey(const Key('roadmap-filter-upcoming')));
    await tester.pumpAndSettle();
    expect(find.text('Theo dõi và siêu âm tuần 21 - 25'), findsOneWidget);
    expect(find.text('Khám và xét nghiệm 20 tuần đầu'), findsNothing);

    // Click Tuần hiện tại
    await tester.tap(find.byKey(const Key('roadmap-filter-current')));
    await tester.pumpAndSettle();
    expect(find.text('Khám và xét nghiệm 20 tuần đầu'), findsOneWidget);
    expect(find.text('Theo dõi và siêu âm tuần 21 - 25'), findsNothing);

    // Click Đã qua
    await tester.tap(find.byKey(const Key('roadmap-filter-completed')));
    await tester.pumpAndSettle();
    expect(find.text('Chuẩn bị trước mang thai'), findsOneWidget);
    expect(find.text('Khám và xét nghiệm 20 tuần đầu'), findsNothing);
  });
}
