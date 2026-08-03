import '../../../core/network/api_client.dart';
import '../models/reminder_schedule_model.dart';

typedef ReminderScheduleGet = Future<dynamic> Function(String path);
typedef ReminderScheduleWrite =
    Future<dynamic> Function(String path, Map<String, dynamic> body);
typedef ReminderScheduleDelete = Future<dynamic> Function(String path);

class ReminderScheduleService {
  ReminderScheduleService({
    ReminderScheduleGet? getRequest,
    ReminderScheduleWrite? postRequest,
    ReminderScheduleWrite? patchRequest,
    ReminderScheduleDelete? deleteRequest,
  }) : _getRequest = getRequest ?? apiGet,
       _postRequest = postRequest ?? apiPost,
       _patchRequest = patchRequest ?? apiPatch,
       _deleteRequest = deleteRequest ?? apiDelete;

  static final instance = ReminderScheduleService();

  final ReminderScheduleGet _getRequest;
  final ReminderScheduleWrite _postRequest;
  final ReminderScheduleWrite _patchRequest;
  final ReminderScheduleDelete _deleteRequest;

  Future<List<ReminderSchedule>> list() async {
    final raw = await _getRequest('/api/v1/reminder-schedules');
    final value = _data(raw);
    if (value is! List) {
      throw const FormatException('Schedule list response is invalid');
    }
    return value
        .whereType<Map>()
        .map(
          (item) => ReminderSchedule.fromJson(Map<String, dynamic>.from(item)),
        )
        .toList(growable: false);
  }

  Future<ReminderSchedule> get(String scheduleId) async {
    final raw = await _getRequest('/api/v1/reminder-schedules/$scheduleId');
    return ReminderSchedule.fromJson(_object(_data(raw)));
  }

  Future<ReminderSchedule> create({
    required String title,
    required List<String> times,
    required String timeZone,
    ReminderScheduleRecurrence recurrence = ReminderScheduleRecurrence.none,
    DateTime? startDate,
    DateTime? endDate,
    bool active = true,
  }) async {
    final raw = await _postRequest('/api/v1/reminder-schedules', {
      'title': title,
      'times': times,
      'timeZone': timeZone,
      'recurrence': recurrence.apiValue,
      if (startDate != null) 'startDate': _dateOnly(startDate),
      if (endDate != null) 'endDate': _dateOnly(endDate),
      'active': active,
    });
    return ReminderSchedule.fromJson(_object(_data(raw)));
  }

  Future<ReminderSchedule> update(
    String scheduleId, {
    String? title,
    List<String>? times,
    String? timeZone,
    ReminderScheduleRecurrence? recurrence,
    DateTime? startDate,
    DateTime? endDate,
    bool endDateSet = false,
    bool? active,
  }) async {
    final body = <String, dynamic>{
      if (title != null) 'title': title,
      if (times != null) 'times': times,
      if (timeZone != null) 'timeZone': timeZone,
      if (recurrence != null) 'recurrence': recurrence.apiValue,
      if (startDate != null) 'startDate': _dateOnly(startDate),
      if (endDateSet) ...{
        'endDate': endDate == null ? null : _dateOnly(endDate),
        'endDateSet': true,
      } else if (endDate != null)
        'endDate': _dateOnly(endDate),
      if (active != null) 'active': active,
    };
    final raw = await _patchRequest(
      '/api/v1/reminder-schedules/$scheduleId',
      body,
    );
    return ReminderSchedule.fromJson(_object(_data(raw)));
  }

  Future<ReminderSchedule> enable(String scheduleId) =>
      update(scheduleId, active: true);

  Future<ReminderSchedule> disable(String scheduleId) =>
      update(scheduleId, active: false);

  Future<void> delete(String scheduleId) async {
    await _deleteRequest('/api/v1/reminder-schedules/$scheduleId');
  }

  static dynamic _data(dynamic raw) {
    if (raw is Map && raw['data'] != null) return raw['data'];
    return raw;
  }

  static Map<String, dynamic> _object(dynamic value) {
    if (value is! Map)
      throw const FormatException('Schedule response is invalid');
    return Map<String, dynamic>.from(value);
  }

  static String _dateOnly(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}
