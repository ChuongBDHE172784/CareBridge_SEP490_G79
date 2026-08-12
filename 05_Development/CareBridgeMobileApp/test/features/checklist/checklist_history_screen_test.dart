import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:untitled/features/checklist/screens/checklist_history_screen.dart';
import 'package:untitled/features/checklist/services/checklist_history_service.dart';

void main() {
  testWidgets('shows read-only grouped history and filters baby tasks', (
    tester,
  ) async {
    final queries = <Map<String, dynamic>?>[];
    final service = ChecklistHistoryService(
      getRequest: (path, {queryParams}) async {
        queries.add(
          queryParams == null ? null : Map<String, dynamic>.from(queryParams),
        );
        return {
          'success': true,
          'data': {
            'items': [
              {
                'checklistInstanceId': 'instance-1',
                'templateName': 'Checklist tháng 1',
                'stage': 'BABY_CARE',
                'targetSubject': 'BABY',
                'careContextType': 'BABY',
                'careContextId': 'baby-1',
                'careContextLabel': 'Bé Na',
                'historicalAt': '2026-09-01T00:00:00Z',
                'historyReasonCode': 'LIFECYCLE_STAGE_OBSOLETE',
                'tasks': [
                  {
                    'taskId': 'task-1',
                    'title': 'Đã cân bé',
                    'status': 'COMPLETED',
                    'displayOrder': 1,
                    'required': true,
                  },
                  {
                    'taskId': 'task-2',
                    'title': 'Chưa đặt lịch tiêm',
                    'status': 'PENDING',
                    'displayOrder': 2,
                    'required': false,
                  },
                ],
              },
            ],
            'page': 0,
            'size': 20,
            'totalElements': 1,
            'totalPages': 1,
          },
        };
      },
    );

    await tester.pumpWidget(
      MaterialApp(home: ChecklistHistoryScreen(service: service)),
    );
    await tester.pumpAndSettle();

    expect(find.text('Checklist tháng 1'), findsOneWidget);
    expect(find.text('Chăm sóc bé'), findsOneWidget);
    expect(find.text('Đã cân bé'), findsOneWidget);
    expect(find.text('Chưa đặt lịch tiêm'), findsOneWidget);
    expect(find.byKey(const Key('save-user-checklist-task')), findsNothing);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Bé'));
    await tester.pumpAndSettle();

    expect(queries.last, containsPair('targetSubject', 'BABY'));
  });

  testWidgets(
    'renders targetless V2 history with neutral recommendation copy',
    (tester) async {
      final service = ChecklistHistoryService(
        getRequest: (path, {queryParams}) async => {
          'success': true,
          'data': {
            'items': [
              {
                'checklistInstanceId': 'v2-history-1',
                'templateName': 'Duy trì thói quen',
                'stage': 'PREGNANCY',
                'targetSubject': null,
                'careContextType': 'JOURNEY',
                'careContextId': 'journey-1',
                'historicalAt': '2026-09-01T00:00:00Z',
                'tasks': [
                  {
                    'taskId': 'v2-history-task',
                    'title': 'Uống đủ nước',
                    'status': 'COMPLETED',
                    'displayOrder': 1,
                    'required': false,
                  },
                ],
              },
            ],
            'page': 0,
            'size': 20,
            'totalElements': 1,
            'totalPages': 1,
          },
        },
      );

      await tester.pumpWidget(
        MaterialApp(home: ChecklistHistoryScreen(service: service)),
      );
      await tester.pumpAndSettle();

      expect(find.text('Duy trì thói quen'), findsOneWidget);
      expect(find.textContaining('Khuyến nghị'), findsOneWidget);
      expect(find.text('Mẹ & bé'), findsNothing);
      expect(find.byIcon(Icons.checklist_rounded), findsOneWidget);
    },
  );

  testWidgets('ignores stale history response after filter changes', (
    tester,
  ) async {
    final first = Completer<dynamic>();
    final second = Completer<dynamic>();
    final queries = <Map<String, dynamic>?>[];
    final service = ChecklistHistoryService(
      getRequest: (path, {queryParams}) {
        queries.add(
          queryParams == null ? null : Map<String, dynamic>.from(queryParams),
        );
        return queries.length == 1 ? first.future : second.future;
      },
    );

    await tester.pumpWidget(
      MaterialApp(home: ChecklistHistoryScreen(service: service)),
    );
    await tester.pump();
    expect(queries, hasLength(1));

    await tester.tap(find.byType(ChoiceChip).at(1));
    await tester.pump();
    expect(queries, hasLength(2));

    second.complete(_historyEnvelope('Baby current', 'BABY'));
    await tester.pumpAndSettle();
    expect(find.text('Baby current'), findsOneWidget);

    first.complete(_historyEnvelope('Mother stale', 'MOTHER'));
    await tester.pumpAndSettle();

    expect(find.text('Baby current'), findsOneWidget);
    expect(find.text('Mother stale'), findsNothing);
  });
}

Map<String, Object?> _historyEnvelope(
  String templateName,
  String targetSubject,
) {
  return {
    'success': true,
    'data': {
      'items': [
        {
          'checklistInstanceId': templateName,
          'templateName': templateName,
          'stage': targetSubject == 'BABY' ? 'BABY_CARE' : 'PREGNANCY',
          'targetSubject': targetSubject,
          'careContextType': targetSubject == 'BABY' ? 'BABY' : 'JOURNEY',
          'careContextId': 'context-id',
          'historicalAt': '2026-09-01T00:00:00Z',
          'historyReasonCode': 'LIFECYCLE_STAGE_OBSOLETE',
          'tasks': [
            {
              'taskId': '$templateName-task',
              'title': '$templateName task',
              'status': 'PENDING',
              'displayOrder': 1,
              'required': true,
            },
          ],
        },
      ],
      'page': 0,
      'size': 20,
      'totalElements': 1,
      'totalPages': 1,
    },
  };
}
