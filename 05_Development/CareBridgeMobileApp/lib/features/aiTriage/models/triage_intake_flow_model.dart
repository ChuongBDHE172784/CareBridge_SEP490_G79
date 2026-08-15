import 'triage_result_model.dart';

class IntakeQuestion {
  final String questionKey;
  final String text;
  final String answerType;
  final List<IntakeQuestionOption> options;

  const IntakeQuestion({
    required this.questionKey,
    required this.text,
    required this.answerType,
    this.options = const [],
  });

  factory IntakeQuestion.fromJson(Map<String, dynamic> json) {
    return IntakeQuestion(
      questionKey: json['questionKey']?.toString() ?? '',
      text: json['text']?.toString() ?? '',
      answerType: json['answerType']?.toString() ?? 'TEXT',
      options: (json['options'] as List<dynamic>? ?? const [])
          .whereType<Map>()
          .map(
            (item) =>
                IntakeQuestionOption.fromJson(Map<String, dynamic>.from(item)),
          )
          .toList(),
    );
  }
}

class IntakeQuestionOption {
  const IntakeQuestionOption({required this.code, required this.label});

  final String code;
  final String label;

  factory IntakeQuestionOption.fromJson(Map<String, dynamic> json) =>
      IntakeQuestionOption(
        code: json['optionCode']?.toString() ?? '',
        label: json['displayText']?.toString() ?? '',
      );
}

class IntakeFlowResponse {
  final String status;
  final String intakeSessionId;
  final String stage;
  final Map<String, dynamic> mergedIntake;
  final String? assistantMessage;
  final List<IntakeQuestion> questions;
  final int round;
  final TriageResult? triageResult;
  final String? journeyId;
  final String? originDashboard;
  final String? originReferenceId;
  final String? continuationToken;
  final DateTime? continuationExpiresAt;

  const IntakeFlowResponse({
    required this.status,
    required this.intakeSessionId,
    this.stage = 'INFANT',
    required this.mergedIntake,
    this.assistantMessage,
    this.questions = const [],
    required this.round,
    this.triageResult,
    this.journeyId,
    this.originDashboard,
    this.originReferenceId,
    this.continuationToken,
    this.continuationExpiresAt,
  });

  factory IntakeFlowResponse.fromJson(Map<String, dynamic> json) {
    final resultJson = json['triageResult'];
    final sessionId =
        json['intakeSessionId']?.toString() ??
        (resultJson is Map<String, dynamic>
            ? resultJson['sessionId']?.toString()
            : null) ??
        '';
    final stage =
        json['stage']?.toString() ??
        (resultJson is Map<String, dynamic>
            ? resultJson['stage']?.toString()
            : null) ??
        'INFANT';
    if (!TriageResult.supportedStages.contains(stage)) {
      throw const FormatException('Invalid intake flow stage');
    }
    final mergedIntake = json['mergedIntake'] is Map<String, dynamic>
        ? Map<String, dynamic>.from(
            json['mergedIntake'] as Map<String, dynamic>,
          )
        : <String, dynamic>{};
    final mergedStage = mergedIntake['stage']?.toString();
    if (mergedStage != null && mergedStage != stage) {
      throw const FormatException('Merged intake stage mismatch');
    }
    TriageResult? result;
    if (resultJson is Map<String, dynamic>) {
      final isLegacyNestedResult =
          resultJson.containsKey('status') ||
          resultJson.containsKey('triageStatus');
      if (!isLegacyNestedResult) {
        final nestedStage = resultJson['stage']?.toString();
        if (nestedStage != null && nestedStage != stage) {
          throw const FormatException('Triage result stage mismatch');
        }
        _rejectNestedMismatch(
          resultJson,
          key: 'sessionId',
          authoritativeValue: sessionId,
          message: 'Triage result session mismatch',
        );
      }
      for (final binding in const {
        'journeyId': 'Triage result journey mismatch',
        'originDashboard': 'Triage result origin dashboard mismatch',
        'originReferenceId': 'Triage result origin reference mismatch',
      }.entries) {
        _rejectNestedMismatch(
          resultJson,
          key: binding.key,
          authoritativeValue: json[binding.key]?.toString(),
          message: binding.value,
        );
      }
      final envelopeStatus = json['status']?.toString();
      final resultStatus = envelopeStatus == 'TRIAGE_COMPLETE'
          ? 'COMPLETED'
          : envelopeStatus ?? resultJson['status']?.toString();
      final patched = <String, dynamic>{
        ...resultJson,
        'sessionId': sessionId,
        'stage': stage,
        'status': resultStatus,
        'triageStatus':
            envelopeStatus ??
            resultJson['triageStatus']?.toString() ??
            resultJson['status']?.toString(),
        'journeyId': json['journeyId'],
        'originDashboard': json['originDashboard'],
        'originReferenceId': json['originReferenceId'],
        'continuationToken': json['continuationToken'],
        'continuationExpiresAt': json['continuationExpiresAt'],
      };
      result = TriageResult.fromJson(patched);
    }
    return IntakeFlowResponse(
      status: json['status']?.toString() ?? '',
      intakeSessionId: sessionId,
      stage: stage,
      mergedIntake: mergedIntake,
      assistantMessage: json['assistantMessage']?.toString(),
      questions: (json['questions'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(IntakeQuestion.fromJson)
          .toList(),
      round: int.tryParse(json['round']?.toString() ?? '') ?? 1,
      triageResult: result,
      journeyId: json['journeyId']?.toString(),
      originDashboard: json['originDashboard']?.toString(),
      originReferenceId: json['originReferenceId']?.toString(),
      continuationToken: json['continuationToken']?.toString(),
      continuationExpiresAt: DateTime.tryParse(
        json['continuationExpiresAt']?.toString() ?? '',
      ),
    );
  }
}

void _rejectNestedMismatch(
  Map<String, dynamic> nested, {
  required String key,
  required String? authoritativeValue,
  required String message,
}) {
  final nestedValue = nested[key]?.toString();
  if (nestedValue != null &&
      authoritativeValue != null &&
      nestedValue != authoritativeValue) {
    throw FormatException(message);
  }
}
