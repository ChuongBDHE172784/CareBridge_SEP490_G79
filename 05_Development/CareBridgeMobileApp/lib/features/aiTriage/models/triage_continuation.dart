enum TriageOriginDashboard {
  motherJourney('MOTHER_JOURNEY'),
  babyProfile('BABY_PROFILE');

  const TriageOriginDashboard(this.apiValue);

  final String apiValue;

  static TriageOriginDashboard fromApiValue(String value) => values.firstWhere(
    (candidate) => candidate.apiValue == value,
    orElse: () =>
        throw FormatException('Unknown triage origin dashboard: $value'),
  );
}

enum TriageOriginAction {
  returnToMotherJourney('RETURN_TO_MOTHER_JOURNEY'),
  returnToBabyProfile('RETURN_TO_BABY_PROFILE');

  const TriageOriginAction(this.apiValue);

  final String apiValue;

  static TriageOriginAction fromApiValue(String value) => values.firstWhere(
    (candidate) => candidate.apiValue == value,
    orElse: () => throw FormatException('Unknown triage origin action: $value'),
  );
}

class PendingTriageContinuation {
  const PendingTriageContinuation({
    required this.token,
    required this.intakeSessionId,
    required this.expiresAt,
  });

  final String token;
  final String intakeSessionId;
  final DateTime expiresAt;

  factory PendingTriageContinuation.fromJson(Map<String, dynamic> json) {
    return PendingTriageContinuation(
      token: json['token'] as String,
      intakeSessionId: json['intakeSessionId'] as String,
      expiresAt: DateTime.parse(json['expiresAt'] as String).toUtc(),
    );
  }

  Map<String, dynamic> toJson() => {
    'token': token,
    'intakeSessionId': intakeSessionId,
    'expiresAt': expiresAt.toUtc().toIso8601String(),
  };
}

class TriageContinuationRecoveryNotice {
  const TriageContinuationRecoveryNotice({
    this.message =
        'Điểm quay lại trước đây không còn khả dụng. Bạn đã được đưa về Trang chủ an toàn.',
  });

  final String message;
}

class TriageContinuationResolution {
  const TriageContinuationResolution({
    required this.token,
    required this.intakeSessionId,
    required this.status,
    required this.riskLevel,
    required this.stage,
    required this.originDashboard,
    required this.originReferenceId,
    required this.originAction,
  });

  final String token;
  final String intakeSessionId;
  final String status;
  final String riskLevel;
  final String stage;
  final TriageOriginDashboard originDashboard;
  final String originReferenceId;
  final TriageOriginAction originAction;

  factory TriageContinuationResolution.fromJson(Map<String, dynamic> json) {
    final token = json['token']?.toString() ?? '';
    final intakeSessionId = json['intakeSessionId']?.toString() ?? '';
    final status = json['status']?.toString() ?? '';
    final riskLevel = json['riskLevel']?.toString().toUpperCase() ?? '';
    final stage = json['stage']?.toString().toUpperCase() ?? '';
    final originReferenceId = json['originReferenceId']?.toString() ?? '';
    if (token.isEmpty ||
        intakeSessionId.isEmpty ||
        status != 'COMPLETED' ||
        !{'GREEN', 'YELLOW', 'RED'}.contains(riskLevel) ||
        !{
          'PRECONCEPTION',
          'PREGNANCY',
          'POSTPARTUM',
          'INFANT',
          'TODDLER',
        }.contains(stage) ||
        originReferenceId.isEmpty) {
      throw const FormatException('Invalid triage continuation descriptor');
    }
    final dashboardValue = json['originDashboard']?.toString();
    final actionValue = json['originAction']?.toString();
    if (dashboardValue == null || actionValue == null) {
      throw const FormatException('Missing triage continuation origin');
    }
    final originDashboard = TriageOriginDashboard.fromApiValue(dashboardValue);
    final originAction = TriageOriginAction.fromApiValue(actionValue);
    final actionMatchesDashboard =
        (originDashboard == TriageOriginDashboard.motherJourney &&
            originAction == TriageOriginAction.returnToMotherJourney) ||
        (originDashboard == TriageOriginDashboard.babyProfile &&
            originAction == TriageOriginAction.returnToBabyProfile);
    if (!actionMatchesDashboard) {
      throw const FormatException('Mismatched triage continuation origin');
    }
    return TriageContinuationResolution(
      token: token,
      intakeSessionId: intakeSessionId,
      status: status,
      riskLevel: riskLevel,
      stage: stage,
      originDashboard: originDashboard,
      originReferenceId: originReferenceId,
      originAction: originAction,
    );
  }
}

enum TriageContinuationFailureKind { notFound, conflict }

class TriageContinuationFailure implements Exception {
  const TriageContinuationFailure.notFound({required this.code})
    : status = 404,
      kind = TriageContinuationFailureKind.notFound;

  const TriageContinuationFailure.conflict({required this.code})
    : status = 409,
      kind = TriageContinuationFailureKind.conflict;

  final int status;
  final String code;
  final TriageContinuationFailureKind kind;

  int get statusCode => status;

  @override
  String toString() => 'TriageContinuationFailure($status, $code)';
}

abstract interface class TriageContinuationGateway {
  Future<TriageContinuationResolution> resolve(String token);

  Future<void> acknowledge(String token);
}

enum TriageContinuationDestination {
  none,
  motherJourney,
  babyProfile,
  emergency,
  safeDashboard,
}

class TriageContinuationDecision {
  const TriageContinuationDecision({
    required this.destination,
    required this.continuationToken,
    required this.generation,
    this.originReferenceId,
    this.authoritativeEmergencyId,
    this.riskLevel,
    this.stage,
    this.showRecordedConfirmation = false,
    this.confirmationUsesRiskColorOnly = false,
    this.isRecoverable = false,
    this.requiresRetry = false,
  });

  const TriageContinuationDecision.none()
    : destination = TriageContinuationDestination.none,
      continuationToken = null,
      generation = null,
      originReferenceId = null,
      authoritativeEmergencyId = null,
      riskLevel = null,
      stage = null,
      showRecordedConfirmation = false,
      confirmationUsesRiskColorOnly = false,
      isRecoverable = false,
      requiresRetry = false;

  final TriageContinuationDestination destination;
  final String? continuationToken;
  final int? generation;
  final String? originReferenceId;
  final String? authoritativeEmergencyId;
  final String? riskLevel;
  final String? stage;
  final bool showRecordedConfirmation;
  final bool confirmationUsesRiskColorOnly;
  final bool isRecoverable;
  final bool requiresRetry;
}
