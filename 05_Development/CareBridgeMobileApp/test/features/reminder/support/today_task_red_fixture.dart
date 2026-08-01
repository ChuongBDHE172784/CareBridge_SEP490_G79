import 'dart:async';

import 'package:untitled/features/reminder/services/today_task_service.dart';

/// Deterministic in-memory API used by the Today RED acceptance tests.
/// It deliberately models the server as GET -> POST -> GET, rather than
/// mutating widgets directly, so the tests prove the mobile API contract.
class StatefulTodayBackend {
  StatefulTodayBackend({this.longLabels = false, this.failActions = false}) {
    service = TodayTaskService(
      getRequest: _get,
      postRequest: _post,
      clientRequestIdFactory: () => 'red-client-request-1',
    );
  }

  final bool longLabels;
  final bool failActions;

  late final TodayTaskService service;
  final List<String> paths = <String>[];
  final List<Map<String, dynamic>?> queries = <Map<String, dynamic>?>[];
  final List<Map<String, dynamic>> actionBodies = <Map<String, dynamic>>[];
  Completer<void>? actionGate;
  int getCount = 0;
  int postCount = 0;
  bool completed = false;

  Future<dynamic> _get(String path, {Map<String, dynamic>? queryParams}) async {
    getCount++;
    paths.add(path);
    queries.add(queryParams);
    return <String, dynamic>{'data': _snapshot()};
  }

  Future<dynamic> _post(String path, Map<String, dynamic> body) async {
    postCount++;
    paths.add(path);
    actionBodies.add(Map<String, dynamic>.from(body));
    final gate = actionGate;
    if (gate != null) await gate.future;
    if (failActions) {
      throw StateError('simulated action failure');
    }
    completed = body['action'] == 'COMPLETE';
    return <String, dynamic>{
      'data': <String, dynamic>{
        'taskKind': 'CHECKLIST',
        'taskId': 'today-red-task',
        'action': body['action'],
        'status': completed ? 'COMPLETED' : 'PENDING',
      },
    };
  }

  Map<String, dynamic> _snapshot() {
    final title = longLabels
        ? 'Chuẩn bị kế hoạch chăm sóc hậu sản với nội dung rất dài để kiểm tra co giãn'
        : 'Chuẩn bị bình sữa';
    final group = longLabels
        ? 'Gia đình có tên nhóm chăm sóc dài vượt quá chiều rộng màn hình'
        : 'Gia đình An';
    final context = longLabels
        ? 'Bé có tên hồ sơ chăm sóc dài để kiểm tra textScale 200 phần trăm'
        : 'Bé An';
    final task = <String, dynamic>{
      'taskKind': 'CHECKLIST',
      'taskId': 'today-red-task',
      'title': title,
      'careGroupId': 'red-group',
      'careGroupName': group,
      'careContextType': 'BABY',
      'careContextId': 'red-baby',
      'careContextLabel': context,
      'targetSubject': 'BABY',
      'origin': 'SYSTEM_TEMPLATE',
      'status': completed ? 'COMPLETED' : 'PENDING',
      'timeBucket': 'TODAY',
      'allowedActions': completed ? <String>['REOPEN'] : <String>['COMPLETE'],
      'dueAt': '2026-08-03T08:00:00Z',
    };
    return <String, dynamic>{
      'asOf': '2026-08-03T01:00:00Z',
      'zoneId': 'Asia/Ho_Chi_Minh',
      'horizonDays': 7,
      'sections': <String, dynamic>{
        'overdue': <Map<String, dynamic>>[],
        'today': <Map<String, dynamic>>[task],
        'upcoming': <Map<String, dynamic>>[],
        'unscheduled': <Map<String, dynamic>>[],
      },
      'counts': <String, dynamic>{
        'overdue': 0,
        'today': 1,
        'upcoming': 0,
        'unscheduled': 0,
      },
      'correlationId': 'today-red-correlation',
    };
  }
}
