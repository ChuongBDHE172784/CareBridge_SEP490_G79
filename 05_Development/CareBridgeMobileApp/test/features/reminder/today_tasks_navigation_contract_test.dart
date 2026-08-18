import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test(
    'Mother Home embeds Today Tasks and View Content uses its canonical screen',
    () {
      final mother = File(
        'lib/features/home/screens/mother_home_screen.dart',
      ).readAsStringSync();
      final content = File(
        'lib/features/community/screens/view_content_screen.dart',
      ).readAsStringSync();

      expect(mother, contains('TodayTasksPanel('));
      expect(mother, contains('AddUserChecklistTaskButton('));
      expect(mother, contains('journeyId: _dashboard!.journeyId'));
      expect(mother, isNot(contains('TodayTasksScreen')));
      expect(content, contains('TodayTasksScreen'));
      expect(content, contains('TodayTasksScreen(journeyId: journeyId)'));
      expect(mother, isNot(contains('PreparationChecklistScreen')));
      expect(content, isNot(contains('PreparationChecklistScreen')));
      expect(mother, isNot(contains('_buildPreparationChecklistSection')));
      expect(mother, isNot(contains('Checklist và việc hôm nay')));
    },
  );

  test(
    'repository has no legacy PreparationChecklistScreen route or import',
    () {
      final allDart = Directory('lib')
          .listSync(recursive: true)
          .whereType<File>()
          .where((file) => file.path.endsWith('.dart'))
          .map((file) => file.readAsStringSync())
          .join('\n');

      expect(allDart, isNot(contains('PreparationChecklistScreen')));
      expect(allDart, isNot(contains('preparation_checklist_screen.dart')));
    },
  );

  test('Mother and Family homes consume the shared Today panel', () {
    final mother = File(
      'lib/features/home/screens/mother_home_screen.dart',
    ).readAsStringSync();
    final family = File(
      'lib/features/home/screens/family_member_home_screen.dart',
    ).readAsStringSync();

    expect(mother, contains('TodayTasksPanel('));
    expect(mother, contains('TodayTasksAudience.mother'));
    expect(family, contains('TodayTasksPanel('));
    expect(family, contains('TodayTasksAudience.family'));
    expect(family, contains('AddUserChecklistTaskButton('));
    expect(family, contains('careGroupId: _selectedCareGroupId'));
  });

  test('checklist task detail route requires TodayTask in state.extra', () {
    final router = File('lib/core/routes/app_router.dart').readAsStringSync();

    expect(router, contains("path: '/checklists/task-detail'"));
    expect(router, contains('final task = state.extra;'));
    expect(router, contains('if (task is! TodayTask)'));
    expect(router, contains('ChecklistTaskDetailScreen('));
    expect(router, contains('task: task,'));
    expect(router, contains("showSupportFunction: audience != 'family',"));
  });

  test('only the canonical Family Home implementation remains', () {
    final legacyFamilyHome = File(
      'lib/features/familySync/screens/family_member_home_screen.dart',
    );
    final canonicalFamilyHome = File(
      'lib/features/home/screens/family_member_home_screen.dart',
    );

    expect(legacyFamilyHome.existsSync(), isFalse);
    expect(canonicalFamilyHome.existsSync(), isTrue);

    final implementations = Directory('lib')
        .listSync(recursive: true)
        .whereType<File>()
        .where((file) => file.path.endsWith('.dart'))
        .where(
          (file) => file.readAsStringSync().contains(
            'class FamilyMemberHomeScreen extends StatefulWidget',
          ),
        )
        .toList(growable: false);
    expect(implementations, hasLength(1));
  });
}
