import 'triage_result_model.dart';

class IntakeQuestion {
  final String questionKey;
  final String text;
  final String answerType;
  final List<String> options;

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
          .map((item) => item.toString())
          .toList(),
    );
  }
}

class IntakeFlowResponse {
  final String status;
  final String intakeSessionId;
  final Map<String, dynamic> mergedIntake;
  final String? assistantMessage;
  final List<IntakeQuestion> questions;
  final int round;
  final TriageResult? triageResult;

  const IntakeFlowResponse({
    required this.status,
    required this.intakeSessionId,
    required this.mergedIntake,
    this.assistantMessage,
    this.questions = const [],
    required this.round,
    this.triageResult,
  });

  factory IntakeFlowResponse.fromJson(Map<String, dynamic> json) {
    final resultJson = json['triageResult'];
    final sessionId = json['intakeSessionId']?.toString() ?? '';
    TriageResult? result;
    if (resultJson is Map<String, dynamic>) {
      final patched = <String, dynamic>{
        'sessionId': sessionId,
        'status': json['status'] == 'TRIAGE_COMPLETE' ? 'COMPLETED' : json['status'],
        'triageStatus': json['status'],
        ...resultJson,
      };
      result = TriageResult.fromJson(patched);
    }
    return IntakeFlowResponse(
      status: json['status']?.toString() ?? '',
      intakeSessionId: sessionId,
      mergedIntake: json['mergedIntake'] is Map<String, dynamic>
          ? Map<String, dynamic>.from(json['mergedIntake'] as Map<String, dynamic>)
          : <String, dynamic>{},
      assistantMessage: json['assistantMessage']?.toString(),
      questions: (json['questions'] as List<dynamic>? ?? const [])
          .whereType<Map<String, dynamic>>()
          .map(IntakeQuestion.fromJson)
          .toList(),
      round: int.tryParse(json['round']?.toString() ?? '') ?? 1,
      triageResult: result,
    );
  }
}
