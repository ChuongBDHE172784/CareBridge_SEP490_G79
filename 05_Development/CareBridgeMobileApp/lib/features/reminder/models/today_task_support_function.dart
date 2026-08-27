enum TodayTaskSupportFunctionCode {
  healthRecords,
  maternalHealthMetrics,
  maternalExercises,
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
    TodayTaskSupportFunctionCode.maternalHealthMetrics =>
      'MATERNAL_HEALTH_METRICS',
    TodayTaskSupportFunctionCode.maternalExercises => 'MATERNAL_EXERCISES',
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
    'MATERNAL_HEALTH_METRICS' =>
      TodayTaskSupportFunctionCode.maternalHealthMetrics,
    'MATERNAL_EXERCISES' => TodayTaskSupportFunctionCode.maternalExercises,
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
  final String? route;

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
  static const maternalHealthMetrics = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.maternalHealthMetrics,
    label: 'Đo chỉ số sức khỏe của mẹ',
    route: null,
  );
  static const maternalExercises = TodayTaskSupportFunction(
    code: TodayTaskSupportFunctionCode.maternalExercises,
    label: 'Bài tập cho mẹ',
    route: '/mother-exercise',
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
    route: '/babies',
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
    label: 'AI Nurse',
    route: '/rag/chat',
  );

  static const values = <TodayTaskSupportFunction>[
    healthRecords,
    maternalHealthMetrics,
    maternalExercises,
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
      TodayTaskSupportFunctionCode.maternalHealthMetrics =>
        maternalHealthMetrics,
      TodayTaskSupportFunctionCode.maternalExercises => maternalExercises,
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

  /// Resolves a client-owned destination for a task.
  ///
  /// Maternal health metrics are scoped to a pregnancy journey, so the
  /// journey id comes from the trusted Today context fields rather than from
  /// an API-provided URL. Other support functions retain their static routes.
  String? routeFor({String? careContextType, String? careContextId}) {
    if (code != TodayTaskSupportFunctionCode.maternalHealthMetrics) {
      return route;
    }
    if (careContextType?.trim().toUpperCase() != 'JOURNEY') return null;
    final journeyId = careContextId?.trim();
    if (journeyId == null || journeyId.isEmpty) return null;
    return Uri(
      path: '/journeys/${Uri.encodeComponent(journeyId)}/metrics/trend',
      queryParameters: const {'metricType': 'BMI'},
    ).toString();
  }
}
