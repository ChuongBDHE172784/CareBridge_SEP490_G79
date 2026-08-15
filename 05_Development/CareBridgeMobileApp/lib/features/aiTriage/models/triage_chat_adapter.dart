import 'triage_intake_flow_model.dart';
import 'triage_result_model.dart';
import 'triage_session.dart';

/// Presents a canonical session in the shape the existing chat already renders.
///
/// The canonical engine replaces the retired engine, not the app's chat. The chat keeps its
/// bubbles, history and emergency action while this adapter projects deterministic session state.
///
/// The translation is deliberately lossy in one direction only: everything the chat shows is
/// derived from canonical state, and nothing here can invent an outcome. Where the engine says
/// less than the old response did, the adapter says less rather than guessing.
class TriageChatAdapter {
  const TriageChatAdapter._();

  /// V1 stage vocabulary, which the chat and its guards key off.
  static const _canonicalToDisplayStage = <String, String>{
    'PRECONCEPTION': 'PRECONCEPTION',
    'POSSIBLE_PREGNANCY': 'PRECONCEPTION',
    'PREGNANCY': 'PREGNANCY',
    'POSTPARTUM_MOTHER': 'POSTPARTUM',
    'INFANT_0_12M': 'INFANT',
    'TODDLER_12_24M': 'TODDLER',
  };

  /// Canonical outcomes carry no GREEN: the release gate rewrites it to NEEDS_MORE_INFO,
  /// so the chat can never show a reassuring result the engine did not actually reach.
  static const _outcomeToRisk = <String, String>{
    'RED': 'RED',
    'YELLOW': 'YELLOW',
    'NEEDS_MORE_INFO': 'NEEDS_MORE_INFO',
    'OUT_OF_SCOPE': 'OUT_OF_SCOPE',
  };

  static String stageForChat(
    TriageSession session, {
    required String fallback,
  }) => _canonicalToDisplayStage[session.stage] ?? fallback;

  /// Turns the planned question ids into the chat's question objects, using the canonical mobile
  /// catalogue for text and options. A question the catalogue does not know becomes free text
  /// rather than an empty choice list the user could not answer.
  static List<IntakeQuestion> questions(TriageSession session) {
    return session.questionDetails.map((question) {
      return IntakeQuestion(
        questionKey: question.id,
        text: question.text,
        answerType: question.answerType,
        options: question.options
            .map(
              (option) => IntakeQuestionOption(
                code: option.optionCode,
                label: option.displayText,
              ),
            )
            .toList(growable: false),
      );
    }).toList();
  }

  /// A completed disposition, or null while the conversation is still gathering information.
  static TriageResult? result(
    TriageSession session, {
    required String chatStage,
  }) {
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
      recommendedAction: actionLabel(session.action),
      emergencyActionRequired:
          risk == 'RED' ||
          session.action == 'IMMEDIATE_EMERGENCY_ASSESSMENT' ||
          session.action == 'IMMEDIATE_SAFETY_SUPPORT',
      redFlags: session.pendingRisks
          .map(pendingRiskLabel)
          .toList(growable: false),
      disclaimer: session.disclaimer,
      citations: session.citations
          .map(
            (citation) => TriageCitation(
              id: citation.sourceId,
              title: citation.title,
              source: citation.organization,
              organization: citation.organization,
              url: citation.url,
              domain: citation.domain,
              excerpt: '',
              retrievedAt: '',
              // Constants rather than copied fields: VerifiedTriageCitation only exists after
              // verifiedFromJson has already rejected anything that is not a hash-pinned
              // SOURCE_VERIFIED document retrieved by local BM25, so these labels cannot
              // overstate what the citation is.
              sourceStatus: 'SOURCE_VERIFIED',
              retrievalMode: 'LOCAL_BM25',
              matchedRules: citation.ruleIds,
            ),
          )
          .toList(),
    );
  }

  /// The assistant line for this turn. Never a diagnosis and never generated: it is chosen from
  /// the required action the deterministic engine returned.
  static String assistantMessage(TriageSession session) {
    if (session.questionIds.isNotEmpty) {
      return 'Vui lòng trả lời các câu hỏi bên dưới để hệ thống tiếp tục định hướng.';
    }
    final actionMessage = switch (session.action) {
      'IMMEDIATE_EMERGENCY_ASSESSMENT' =>
        'Dấu hiệu bạn mô tả cần được nhân viên y tế đánh giá ngay.',
      'IMMEDIATE_SAFETY_SUPPORT' =>
        'Bạn không nên ở một mình lúc này. Hãy tìm người thân hoặc hỗ trợ y tế ngay.',
      'EARLY_CLINICAL_ASSESSMENT' =>
        'Bạn nên được nhân viên y tế đánh giá sớm.',
      'ROUTE_TO_HEALTHCARE_WORKER' =>
        'Hiện chưa đủ thông tin để định hướng. Bạn nên trao đổi trực tiếp với nhân viên y tế.',
      'OUT_OF_SCOPE_REDIRECT' =>
        'Nội dung này nằm ngoài phạm vi sức khỏe sinh sản mẹ và bé của công cụ.',
      _ => 'Hệ thống đang tiếp tục xử lý thông tin bạn cung cấp.',
    };
    if (!session.stop) return actionMessage;
    final rationale = session.rationale.trim().isEmpty
        ? actionMessage
        : session.rationale.trim();
    return '$rationale ${evidenceStatusLabel(session.evidenceStatus)}';
  }

  static String evidenceStatusLabel(String status) => switch (status) {
    'AVAILABLE' => 'Đã có nguồn tham khảo được kiểm chứng cho kết quả này.',
    'PENDING' => 'Nguồn tham khảo đang chờ được kiểm chứng.',
    'REJECTED' => 'Nguồn tham khảo chưa đạt yêu cầu kiểm chứng.',
    _ => 'Hiện chưa có nguồn tham khảo phù hợp để hiển thị.',
  };

  /// The chat's status vocabulary.
  static String status(TriageSession session) =>
      session.stop ? 'TRIAGE_COMPLETE' : 'NEED_MORE_INFO';

  static String riskLabel(String? risk) => switch (risk) {
    'RED' => 'Đỏ — cần được đánh giá ngay',
    'YELLOW' => 'Vàng — cần được đánh giá sớm',
    'GREEN' => 'Xanh — chưa được phép phát hành công khai',
    'NEEDS_MORE_INFO' => 'Cần thêm thông tin',
    'OUT_OF_SCOPE' => 'Ngoài phạm vi hỗ trợ',
    _ => 'Chưa xác định',
  };

  static String actionLabel(String action) => switch (action) {
    'IMMEDIATE_EMERGENCY_ASSESSMENT' =>
      'Đến cơ sở y tế hoặc liên hệ cấp cứu ngay.',
    'IMMEDIATE_SAFETY_SUPPORT' =>
      'Tìm người hỗ trợ và liên hệ nhân viên y tế ngay.',
    'EARLY_CLINICAL_ASSESSMENT' => 'Sắp xếp để nhân viên y tế đánh giá sớm.',
    'ASK_CLARIFYING_QUESTIONS' =>
      'Cần bổ sung thông tin để tiếp tục phân loại.',
    'ROUTE_TO_HEALTHCARE_WORKER' => 'Trao đổi trực tiếp với nhân viên y tế.',
    'OUT_OF_SCOPE_REDIRECT' =>
      'Trao đổi với nhân viên y tế phù hợp với vấn đề này.',
    _ => 'Làm theo hướng dẫn của nhân viên y tế.',
  };

  static String pendingRiskLabel(String code) => switch (code) {
    'UNRESOLVED_RED_CONDITION' => 'Chưa loại trừ dấu hiệu nguy cơ cao',
    'UNRESOLVED_GLOBAL_SAFETY_SCREEN' =>
      'Chưa hoàn tất sàng lọc dấu hiệu nguy hiểm',
    'UNRESOLVED_SAFETY_BLOCKER' => 'Còn thông tin an toàn cần xác minh',
    'UNRESOLVED_CONTEXT' => 'Chưa xác định đủ bối cảnh sức khỏe',
    _ => 'Còn thông tin nguy cơ cần xác minh',
  };

  /// Full projection onto the response object the chat consumes.
  static IntakeFlowResponse toFlowResponse(
    TriageSession session, {
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
