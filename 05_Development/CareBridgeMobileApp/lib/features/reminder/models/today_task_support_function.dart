enum TodayTaskSupportFunctionCode {
  healthRecords,
  appointments,
  reminders,
  journey,
  babyCare,
  expertConsultation,
  contentLibrary,
  aiTriage,
}

extension TodayTaskSupportFunctionCodeValue on TodayTaskSupportFunctionCode {
  String get apiValue => switch (this) {
    TodayTaskSupportFunctionCode.healthRecords => 'HEALTH_RECORDS',
    TodayTaskSupportFunctionCode.appointments => 'APPOINTMENTS',
    TodayTaskSupportFunctionCode.reminders => 'REMINDERS',
    TodayTaskSupportFunctionCode.journey => 'JOURNEY',
    TodayTaskSupportFunctionCode.babyCare => 'BABY_CARE',
    TodayTaskSupportFunctionCode.expertConsultation => 'EXPERT_CONSULTATION',
    TodayTaskSupportFunctionCode.contentLibrary => 'CONTENT_LIBRARY',
    TodayTaskSupportFunctionCode.aiTriage => 'AI_TRIAGE',
  };
}

class TodayTaskSupportFunctionCodeApi {
  static TodayTaskSupportFunctionCode? fromApi(String? value) => switch (value
      ?.trim()
      .toUpperCase()) {
    'HEALTH_RECORDS' => TodayTaskSupportFunctionCode.healthRecords,
    'APPOINTMENTS' => TodayTaskSupportFunctionCode.appointments,
    'REMINDERS' => TodayTaskSupportFunctionCode.reminders,
    'JOURNEY' => TodayTaskSupportFunctionCode.journey,
    'BABY_CARE' => TodayTaskSupportFunctionCode.babyCare,
    'EXPERT_CONSULTATION' => TodayTaskSupportFunctionCode.expertConsultation,
    'CONTENT_LIBRARY' => TodayTaskSupportFunctionCode.contentLibrary,
    'AI_TRIAGE' => TodayTaskSupportFunctionCode.aiTriage,
    _ => null,
  };
}

/// A client-owned catalogue of safe destinations for checklist support.
///
/// Routes are deliberately constants rather than values supplied by the API,
/// so a checklist payload cannot make the mobile app navigate to an arbitrary
/// location.
class TodayTaskSupportFunction {
  final TodayTaskSupportFunctionCode code;
  final String label;
  final String route;

  const TodayTaskSupportFunction({
    required this.code,
    required this.label,
    required this.route,
  });

  static const healthRecords = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.healthRecords,
    label: 'Hồ sơ sức khỏe',
    route: '/health-records',
  );
  static const appointments = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.appointments,
    label: 'Lịch hẹn',
    route: '/appointments/calendar',
  );
  static const reminders = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.reminders,
    label: 'Lịch nhắc',
    route: '/reminder-schedules',
  );
  static const journey = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.journey,
    label: 'Hành trình',
    route: '/mother-home?tab=1',
  );
  static const babyCare = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.babyCare,
    label: 'Chăm sóc bé',
    route: '/baby-care-hub',
  );
  static const expertConsultation = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.expertConsultation,
    label: 'Tư vấn chuyên gia',
    route: '/experts',
  );
  static const contentLibrary = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.contentLibrary,
    label: 'Thư viện nội dung',
    route: '/content',
  );
  static const aiTriage = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.aiTriage,
    label: 'Sàng lọc AI',
    route: '/triage/intake',
  );

  static const values = <TodayTaskSupportFunction>[
    healthRecords,
    appointments,
    reminders,
    journey,
    babyCare,
    expertConsultation,
    contentLibrary,
    aiTriage,
  ];

  String get apiValue => code.apiValue;

  static TodayTaskSupportFunction? fromApi(String? value) {
    final code = TodayTaskSupportFunctionCodeApi.fromApi(value);
    return switch (code) {
      TodayTaskSupportFunctionCode.healthRecords => healthRecords,
      TodayTaskSupportFunctionCode.appointments => appointments,
      TodayTaskSupportFunctionCode.reminders => reminders,
      TodayTaskSupportFunctionCode.journey => journey,
      TodayTaskSupportFunctionCode.babyCare => babyCare,
      TodayTaskSupportFunctionCode.expertConsultation => expertConsultation,
      TodayTaskSupportFunctionCode.contentLibrary => contentLibrary,
      TodayTaskSupportFunctionCode.aiTriage => aiTriage,
      null => null,
    };
  }

  static TodayTaskSupportFunction? fromJson(dynamic value) {
    final code = value is String
        ? value
        : value is Map
        ? value['code'] as String?
        : null;
    return fromApi(code);
  }
}
