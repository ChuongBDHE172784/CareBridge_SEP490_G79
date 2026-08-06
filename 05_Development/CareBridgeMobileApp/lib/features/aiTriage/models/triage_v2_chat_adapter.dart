import 'triage_intake_flow_model.dart';
import 'triage_result_model.dart';
import 'triage_v2_session.dart';

/// Presents a V2 session in the shape the existing chat already renders.
///
/// V2 replaces V1's *engine*, not the app's chat. V2's own screen is an internal test harness — a
/// target selector, a text box and a submit button — so routing users there would trade a working
/// conversation for a form. Instead the chat keeps its bubbles, history and emergency action, and
/// this adapter feeds it from the deterministic V2 workflow.
///
/// The translation is deliberately lossy in one direction only: everything the chat shows is
/// derived from V2 state, and nothing here can invent an outcome. Where V2 says less than V1 did
/// (no free-text summary, no per-question answer map), the adapter says less rather than guessing.
class TriageV2ChatAdapter {
  const TriageV2ChatAdapter._();

  /// V1 stage vocabulary, which the chat and its guards key off.
  static const _v2ToV1Stage = <String, String>{
    'PRECONCEPTION': 'PRECONCEPTION',
    'POSSIBLE_PREGNANCY': 'PRECONCEPTION',
    'PREGNANCY': 'PREGNANCY',
    'POSTPARTUM_MOTHER': 'POSTPARTUM',
    'INFANT_0_12M': 'INFANT',
    'TODDLER_12_24M': 'TODDLER',
  };

  /// V2 outcomes carry no GREEN: the release gate rewrites a would-be GREEN to NEEDS_MORE_INFO,
  /// so the chat can never show a reassuring result the engine did not actually reach.
  static const _outcomeToRisk = <String, String>{
    'RED': 'RED',
    'YELLOW': 'YELLOW',
    'NEEDS_MORE_INFO': 'NEEDS_MORE_INFO',
    'OUT_OF_SCOPE': 'OUT_OF_SCOPE',
  };

  static String stageForChat(TriageV2Session session, {required String fallback}) =>
      _v2ToV1Stage[session.stage] ?? fallback;

  /// Turns the planned question ids into the chat's question objects, using the canonical mobile
  /// catalogue for text and options. A question the catalogue does not know becomes free text
  /// rather than an empty choice list the user could not answer.
  static List<IntakeQuestion> questions(TriageV2Session session) {
    return session.questionIds.map((id) {
      final question = TriageV2Question.fromId(id);
      return IntakeQuestion(
        questionKey: id,
        text: question.text,
        answerType: question.optionCodes.isEmpty ? 'TEXT' : 'SINGLE_CHOICE',
        options: question.optionCodes,
      );
    }).toList();
  }

  /// A completed disposition, or null while the conversation is still gathering information.
  static TriageResult? result(TriageV2Session session, {required String chatStage}) {
    if (!session.stop) return null;
    final risk = _outcomeToRisk[session.outcome];
    if (risk == null) return null;
    return TriageResult(
      sessionId: session.sessionId,
      stage: chatStage,
      status: 'COMPLETED',
      triageStatus: 'COMPLETED',
      riskLevel: risk,
      summary: assistantMessage(session),
      recommendedAction: session.action,
      emergencyActionRequired: session.action == 'IMMEDIATE_EMERGENCY_ASSESSMENT'
          || session.action == 'IMMEDIATE_SAFETY_SUPPORT',
      redFlags: session.pendingRisks,
      disclaimer: session.disclaimer,
      citations: session.citations
          .map((citation) => TriageCitation(
                id: citation.sourceId,
                title: citation.title,
                source: citation.organization,
                organization: citation.organization,
                url: citation.url,
                domain: citation.domain,
                excerpt: '',
                retrievedAt: '',
                // Constants rather than copied fields: TriageV2Citation only exists after
                // verifiedFromJson has already rejected anything that is not a hash-pinned
                // SOURCE_VERIFIED document retrieved by local BM25, so these labels cannot
                // overstate what the citation is.
                sourceStatus: 'SOURCE_VERIFIED',
                retrievalMode: 'LOCAL_BM25',
                matchedRules: citation.ruleIds,
              ))
          .toList(),
    );
  }

  /// The assistant line for this turn. Never a diagnosis and never generated: it is chosen from
  /// the required action the deterministic engine returned.
  static String assistantMessage(TriageV2Session session) {
    if (session.questionIds.isNotEmpty) {
      return 'Vui lòng trả lời các câu hỏi bên dưới để hệ thống tiếp tục định hướng.';
    }
    return switch (session.action) {
      'IMMEDIATE_EMERGENCY_ASSESSMENT' =>
        'Dấu hiệu bạn mô tả cần được nhân viên y tế đánh giá ngay.',
      'IMMEDIATE_SAFETY_SUPPORT' =>
        'Bạn không nên ở một mình lúc này. Hãy tìm người thân hoặc hỗ trợ y tế ngay.',
      'EARLY_CLINICAL_ASSESSMENT' => 'Bạn nên được nhân viên y tế đánh giá sớm.',
      'ROUTE_TO_HEALTHCARE_WORKER' =>
        'Hiện chưa đủ thông tin để định hướng. Bạn nên trao đổi trực tiếp với nhân viên y tế.',
      'OUT_OF_SCOPE_REDIRECT' =>
        'Nội dung này nằm ngoài phạm vi sức khỏe sinh sản mẹ và bé của công cụ.',
      _ => 'Hệ thống đang tiếp tục xử lý thông tin bạn cung cấp.',
    };
  }

  /// The chat's status vocabulary.
  static String status(TriageV2Session session) =>
      session.stop ? 'TRIAGE_COMPLETE' : 'NEED_MORE_INFO';

  /// Full projection onto the response object the chat consumes.
  static IntakeFlowResponse toFlowResponse(
    TriageV2Session session, {
    required String fallbackStage,
    required int round,
    required Map<String, dynamic> mergedIntake,
  }) {
    final chatStage = stageForChat(session, fallback: fallbackStage);
    return IntakeFlowResponse(
      status: status(session),
      intakeSessionId: session.sessionId,
      stage: chatStage,
      mergedIntake: {...mergedIntake, 'stage': chatStage},
      assistantMessage: assistantMessage(session),
      questions: questions(session),
      round: round,
      triageResult: result(session, chatStage: chatStage),
    );
  }
}
