package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.HealthMemoryContextItem;
import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.TriageStage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
@Deprecated
public class TriageGraphService {

    public static final String DISCLAIMER = "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế bác sĩ. "
            + "Nếu trẻ có dấu hiệu nặng, hãy liên hệ cơ sở y tế/cấp cứu.";

    private final SymptomNormalizer symptomNormalizer;
    private final SourceRetriever sourceRetriever;
    private final RiskRuleFactory riskRuleFactory;

    @Autowired
    public TriageGraphService(
            SymptomNormalizer symptomNormalizer,
            SourceRetriever sourceRetriever,
            RiskRuleFactory riskRuleFactory) {
        this.symptomNormalizer = symptomNormalizer;
        this.sourceRetriever = sourceRetriever;
        this.riskRuleFactory = riskRuleFactory;
    }

    public TriageGraphService(
            SymptomNormalizer symptomNormalizer,
            SourceRetriever sourceRetriever,
            PediatricRiskRules pediatricRiskRules) {
        this(
                symptomNormalizer,
                sourceRetriever,
                new RiskRuleFactory(
                        new PreconceptionRiskRules(),
                        new MaternalPregnancyRiskRules(),
                        new PostpartumRiskRules(),
                        new PediatricInfantRiskRules(),
                        new PediatricToddlerRiskRules()));
    }

    /**
     * CB-TRIAGE-THMC-IMP-001 §8.1 (@version 2.0): same deterministic rule evaluation as
     * {@link #run(RunIntakeRequest)}; healthContext is used ONLY for narrative fields
     * (summary phrasing). It never changes rule matching, therefore never lowers
     * riskLevel (BR-THMC-004 / ADR-THMC-003).
     */
    public ChildTriageResult run(RunIntakeRequest request, List<HealthMemoryContextItem> healthContext) {
        ChildTriageResult result = run(request);
        if (healthContext == null || healthContext.isEmpty()) {
            return result;
        }
        // Narrative-only augmentation: risk level, emergency flag, red flags, matched rules,
        // recommendations and questions are copied UNCHANGED from the deterministic run
        // (BR-THMC-004: context can never lower — or otherwise alter — the computed risk).
        String advisoryNote = " (Đã tham chiếu " + healthContext.size()
                + " ghi nhớ sức khỏe trước đó — chỉ mang tính bổ trợ, không thay đổi phân loại rủi ro.)";
        return ChildTriageResult.builder()
                .status(result.getStatus())
                .riskLevel(result.getRiskLevel())
                .riskColor(result.getRiskColor())
                .summary(result.getSummary() == null ? null : result.getSummary() + advisoryNote)
                .possibleConcern(result.getPossibleConcern())
                .recommendedAction(result.getRecommendedAction())
                .emergencyActionRequired(result.isEmergencyActionRequired())
                .redFlags(result.getRedFlags())
                .matchedRules(result.getMatchedRules())
                .normalizedSymptoms(result.getNormalizedSymptoms())
                .citations(result.getCitations())
                .disclaimer(result.getDisclaimer())
                .questions(result.getQuestions())
                .warning(result.getWarning())
                .build();
    }

    public ChildTriageResult run(RunIntakeRequest request) {
        TriageStage stage = request.getStage() == null ? TriageStage.INFANT : request.getStage();
        StageRiskRules riskRules = riskRuleFactory.forStage(stage);
        List<String> questions = collectIntake(request, riskRules);
        List<String> symptoms = normalizeSymptoms(request);
        List<MedicalSource> sources = stage.isPediatric()
                ? retrieveSources(symptoms, request.getChildAgeMonths())
                : List.of();
        PediatricRiskRules.RuleOutcome outcome = applyRules(request, symptoms, riskRules);
        List<TriageCitation> citations = attachCitations(sources);

        if (symptoms.isEmpty() && !"RED".equals(outcome.riskLevel())) {
            questions = new ArrayList<>(questions);
            questions.add(stage.isMaternal()
                    ? "Vui lòng mô tả cụ thể dấu hiệu sức khỏe bạn đang gặp."
                    : "Vui lòng mô tả lại bằng dấu hiệu cụ thể mà bạn quan sát được ở trẻ.");
            questions = questions.stream().distinct().limit(3).toList();
        }

        // RED precedence: missing demographic/context fields never downgrade an
        // immediately dangerous structured symptom.
        if (("NEED_MORE_INFO".equals(outcome.riskLevel()) || !questions.isEmpty()) && !"RED".equals(outcome.riskLevel())) {
            return ChildTriageResult.builder()
                    .status("NEED_MORE_INFO")
                    .summary("CareBridge cần thêm thông tin quan trọng trước khi phân loại rủi ro chắc chắn.")
                    .possibleConcern(stage.isMaternal()
                            ? "Thông tin sức khỏe hiện chưa đủ để phân loại an toàn."
                            : "Thiếu dữ liệu nền như tuổi, hô hấp, ý thức hoặc bú/uống.")
                    .recommendedAction(stage.isMaternal()
                            ? "Vui lòng trả lời các câu hỏi bổ sung. Nếu bạn thấy không an toàn hoặc có dấu hiệu nặng, hãy liên hệ cơ sở y tế/cấp cứu ngay."
                            : "Vui lòng trả lời các câu hỏi bổ sung. Nếu trẻ có dấu hiệu nặng, hãy liên hệ cơ sở y tế/cấp cứu ngay.")
                    .emergencyActionRequired(false)
                    .redFlags(List.of())
                    .matchedRules(outcome.matchedRules())
                    .normalizedSymptoms(symptoms)
                    .citations(citations)
                    .disclaimer(disclaimer(stage))
                    .questions(questions)
                    .warning(citations.isEmpty() ? "Không tìm thấy nguồn phù hợp trong knowledge base" : null)
                    .build();
        }

        String riskLevel = scoreRisk(outcome);
        return ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel(riskLevel)
                .riskColor(colorFor(riskLevel))
                .summary(summary(stage, riskLevel, symptoms, outcome.redFlags()))
                .possibleConcern(possibleConcern(stage, riskLevel, outcome.redFlags()))
                .recommendedAction(recommendedAction(stage, riskLevel))
                .emergencyActionRequired("RED".equals(riskLevel))
                .redFlags(outcome.redFlags())
                .matchedRules(outcome.matchedRules())
                .normalizedSymptoms(symptoms)
                .citations(citations)
                .disclaimer(disclaimer(stage))
                .questions(List.of())
                .warning(citations.isEmpty() ? "Không tìm thấy nguồn phù hợp trong knowledge base" : null)
                .build();
    }

    private List<String> collectIntake(RunIntakeRequest request, StageRiskRules riskRules) {
        return riskRules.questions(request);
    }

    private List<String> normalizeSymptoms(RunIntakeRequest request) {
        return symptomNormalizer.normalize(request);
    }

    private List<MedicalSource> retrieveSources(List<String> symptoms, Integer childAgeMonths) {
        return sourceRetriever.retrieve(symptoms, childAgeMonths);
    }

    private PediatricRiskRules.RuleOutcome applyRules(RunIntakeRequest request, List<String> symptoms, StageRiskRules riskRules) {
        return riskRules.apply(request, symptoms);
    }

    private String scoreRisk(PediatricRiskRules.RuleOutcome outcome) {
        return outcome.riskLevel();
    }

    private List<TriageCitation> attachCitations(List<MedicalSource> sources) {
        return sourceRetriever.citations(sources);
    }

    private String colorFor(String riskLevel) {
        return switch (riskLevel) {
            case "GREEN" -> "#22C55E";
            case "YELLOW" -> "#FACC15";
            case "RED" -> "#EF4444";
            default -> null;
        };
    }

    private String summary(TriageStage stage, String riskLevel, List<String> symptoms, List<String> redFlags) {
        if (stage == TriageStage.POSTPARTUM) {
            return switch (riskLevel) {
                case "RED" -> "Thông tin nhập vào có dấu hiệu cảnh báo cần được hỗ trợ khẩn cấp.";
                case "YELLOW" -> "Dấu hiệu hồi phục cần được nhân viên y tế đánh giá thêm.";
                default -> "Chưa ghi nhận dấu hiệu nguy hiểm từ thông tin hiện có.";
            };
        }
        return switch (riskLevel) {
            case "RED" -> "Thông tin nhập vào có dấu hiệu cảnh báo cần xử trí khẩn cấp.";
            case "YELLOW" -> "Trẻ có triệu chứng cần theo dõi sát hoặc hỏi ý kiến nhân viên y tế: "
                    + (symptoms.isEmpty() ? "triệu chứng đã nhập" : String.join(", ", symptoms)) + ".";
            default -> "Chưa ghi nhận red flag từ thông tin hiện có.";
        };
    }

    private String possibleConcern(TriageStage stage, String riskLevel, List<String> redFlags) {
        if (stage == TriageStage.POSTPARTUM) {
            return switch (riskLevel) {
                case "RED" -> String.join("; ", redFlags);
                case "YELLOW" -> "Dấu hiệu chưa đủ để kết luận và cần được theo dõi hoặc đánh giá trực tiếp.";
                default -> "Thông tin hiện có chưa cho thấy dấu hiệu nguy hiểm rõ ràng.";
            };
        }
        return switch (riskLevel) {
            case "RED" -> String.join("; ", redFlags);
            case "YELLOW" -> "Triệu chứng chưa có red flag rõ, nhưng cần theo dõi diễn tiến.";
            default -> "Triệu chứng nhẹ, trẻ vẫn tỉnh táo, bú/uống tốt và thở bình thường.";
        };
    }

    private String recommendedAction(TriageStage stage, String riskLevel) {
        if (stage.isMaternal()) {
            return switch (riskLevel) {
                case "RED" -> "Gọi cấp cứu 115 hoặc đến cơ sở y tế gần nhất ngay. Không chờ thêm kết quả trực tuyến nếu tình trạng đang xấu đi.";
                case "YELLOW" -> "Liên hệ nhân viên y tế để được đánh giá và theo dõi diễn tiến.";
                default -> "Tiếp tục theo dõi và liên hệ nhân viên y tế nếu dấu hiệu kéo dài hoặc nặng hơn.";
            };
        }
        return switch (riskLevel) {
            case "RED" -> "Liên hệ cấp cứu hoặc đưa trẻ đến cơ sở y tế gần nhất ngay. Không chờ tư vấn AI nếu tình trạng đang xấu đi.";
            case "YELLOW" -> "Theo dõi nhiệt độ, nhịp thở, bú/uống và tình trạng tỉnh táo. Liên hệ bác sĩ nếu kéo dài, nặng hơn hoặc xuất hiện red flag.";
            default -> "Theo dõi tại nhà, cho trẻ bú/uống đủ, ghi nhận triệu chứng và quay lại kiểm tra nếu có thay đổi.";
        };
    }

    private String disclaimer(TriageStage stage) {
        if (stage.isMaternal()) {
            return "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế nhân viên y tế. "
                    + "Nếu bạn có dấu hiệu nặng hoặc cảm thấy không an toàn, hãy liên hệ cơ sở y tế/cấp cứu.";
        }
        return DISCLAIMER;
    }
}
