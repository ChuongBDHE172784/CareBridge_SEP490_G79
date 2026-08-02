import 'dart:async';
import 'dart:io';

import 'package:uuid/uuid.dart';

import '../../../core/network/api_client.dart';
import '../models/today_task_model.dart';

typedef TodayGetRequest =
    Future<dynamic> Function(String path, {Map<String, dynamic>? queryParams});
typedef TodayPostRequest =
    Future<dynamic> Function(String path, Map<String, dynamic> body);

enum TodayFailureKind { offline, retryable, terminal }

class TodayTasksFailure implements Exception {
  final TodayFailureKind kind;
  final Object cause;
  const TodayTasksFailure(this.kind, this.cause);
}

class TodayTaskService {
  TodayTaskService({
    TodayGetRequest? getRequest,
    TodayPostRequest? postRequest,
    String Function()? clientRequestIdFactory,
    String userTimezone = 'Asia/Ho_Chi_Minh',
  }) : _getRequest =
           getRequest ??
           ((path, {queryParams}) => apiGet(
             path,
             queryParams: queryParams,
             extraHeaders: {'X-User-Timezone': userTimezone},
           )),
       _postRequest = postRequest ?? apiPost,
       _clientRequestIdFactory = clientRequestIdFactory ?? const Uuid().v4;

  static final instance = TodayTaskService();

  final TodayGetRequest _getRequest;
  final TodayPostRequest _postRequest;
  final String Function() _clientRequestIdFactory;

  Future<TodayTasksSnapshot> loadToday({DateTime? date}) async {
    try {
      final effective = date ?? DateTime.now();
      final payload = await _getRequest(
        '/api/v1/tasks/today',
        queryParams: {'date': _dateOnly(effective)},
      );
      final envelope = Map<String, dynamic>.from(payload as Map);
      final raw = envelope['data'] is Map ? envelope['data'] as Map : envelope;
      return TodayTasksSnapshot.fromJson(Map<String, dynamic>.from(raw));
    } catch (error) {
      if (error is TodayTasksFailure) rethrow;
      throw TodayTasksFailure(_classify(error), error);
    }
  }

  Future<Map<String, dynamic>> performAction({
    required TodayTaskKind taskKind,
    required String taskId,
    required TodayTaskAction action,
    TodayTaskSkipReason? reason,
  }) async {
    if (action == TodayTaskAction.skip && reason == null) {
      throw ArgumentError.value(
        reason,
        'reason',
        'SKIP requires a controlled reason',
      );
    }
    final payload = await _postRequest(
      '/api/v1/tasks/${taskKind.apiValue}/$taskId/actions',
      {
        'action': action.apiValue,
        'clientRequestId': _clientRequestIdFactory(),
        'reason': reason?.apiValue,
      },
    );
    final envelope = Map<String, dynamic>.from(payload as Map);
    final raw = envelope['data'] is Map ? envelope['data'] as Map : envelope;
    return Map<String, dynamic>.from(raw);
  }

  Future<Map<String, dynamic>> advanceSequence({
    required String currentInstanceId,
    String? clientRequestId,
  }) async {
    final payload = await _postRequest(
      '/api/v1/checklists/sequences/advance',
      {
        'currentInstanceId': currentInstanceId,
        'clientRequestId': clientRequestId ?? _clientRequestIdFactory(),
      },
    );
    final envelope = Map<String, dynamic>.from(payload as Map);
    final raw = envelope['data'] is Map ? envelope['data'] as Map : envelope;
    return Map<String, dynamic>.from(raw);
  }

  static String _dateOnly(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';

  static TodayFailureKind _classify(Object error) {
    if (error is SocketException || error is TimeoutException) {
      return TodayFailureKind.offline;
    }
    if (error is ApiException) {
      if (error.statusCode == 401 ||
          error.statusCode == 403 ||
          error.statusCode == 404) {
        return TodayFailureKind.terminal;
      }
    }
    return TodayFailureKind.retryable;
  }
}
