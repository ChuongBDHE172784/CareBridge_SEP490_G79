package com.carebridge.backend.triage.engine;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TriageGraphService {

    public static final String DISCLAIMER = "CareBridge không chẩn đoán bệnh, không kê thuốc và không thay thế bác sĩ. "
            + "Nếu trẻ có dấu hiệu nặng, hãy liên hệ cơ sở y tế/cấp cứu.";

    private final SymptomNormalizer symptomNormalizer;
    private final SourceRetriever sourceRetriever;
    private final PediatricRiskRules riskRules;

    public ChildTriageResult run(RunIntakeRequest request) {
        List<String> questions = collectIntake(request);
        List<String> symptoms = normalizeSymptoms(request);
        List<MedicalSource> sources = retrieveSources(symptoms);
        PediatricRiskRules.RuleOutcome outcome = applyRules(request, symptoms);
        List<TriageCitation> citations = attachCitations(sources);

        if (symptoms.isEmpty() && !"RED".equals(outcome.riskLevel())) {
            questions = new ArrayList<>(questions);
            questions.add("Vui lòng mô tả lại bằng dấu hiệu cụ thể mà bạn quan sát được ở trẻ.");
            questions = questions.stream().distinct().limit(3).toList();
        }

        // RED precedence: missing demographic/context fields never downgrade an
        // immediately dangerous structured symptom.
        if (!questions.isEmpty() && !"RED".equals(outcome.riskLevel())) {
            return ChildTriageResult.builder()
                    .status("NEED_MORE_INFO")
                    .summary("CareBridge cần thêm thông tin quan trọng trước khi phân loại rủi ro chắc chắn.")
                    .possibleConcern("Thiếu dữ liệu nền như tuổi, hô hấp, ý thức hoặc bú/uống.")
                    .recommendedAction("Vui lòng trả lời các câu hỏi bổ sung. Nếu trẻ có dấu hiệu nặng, hãy liên hệ cơ sở y tế/cấp cứu ngay.")
                    .emergencyActionRequired(false)
                    .redFlags(List.of())
                    .matchedRules(List.of())
                    .normalizedSymptoms(symptoms)
                    .citations(citations)
                    .disclaimer(DISCLAIMER)
                    .questions(questions)
                    .warning(citations.isEmpty() ? "Không tìm thấy nguồn phù hợp trong knowledge base" : null)
                    .build();
        }

        String riskLevel = scoreRisk(outcome);
        return ChildTriageResult.builder()
                .status("COMPLETED")
                .riskLevel(riskLevel)
                .riskColor(colorFor(riskLevel))
                .summary(summary(riskLevel, symptoms, outcome.redFlags()))
                .possibleConcern(possibleConcern(riskLevel, outcome.redFlags()))
                .recommendedAction(recommendedAction(riskLevel))
                .emergencyActionRequired("RED".equals(riskLevel))
                .redFlags(outcome.redFlags())
                .matchedRules(outcome.matchedRules())
                .normalizedSymptoms(symptoms)
                .citations(citations)
                .disclaimer(DISCLAIMER)
                .questions(List.of())
                .warning(citations.isEmpty() ? "Không tìm thấy nguồn phù hợp trong knowledge base" : null)
                .build();
    }

    private List<String> collectIntake(RunIntakeRequest request) {
        return riskRules.questions(request);
    }

    private List<String> normalizeSymptoms(RunIntakeRequest request) {
        return symptomNormalizer.normalize(request);
    }

    private List<MedicalSource> retrieveSources(List<String> symptoms) {
        return sourceRetriever.retrieve(symptoms);
    }

    private PediatricRiskRules.RuleOutcome applyRules(RunIntakeRequest request, List<String> symptoms) {
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

    private String summary(String riskLevel, List<String> symptoms, List<String> redFlags) {
        return switch (riskLevel) {
            case "RED" -> "Thông tin nhập vào có dấu hiệu cảnh báo cần xử trí khẩn cấp.";
            case "YELLOW" -> "Trẻ có triệu chứng cần theo dõi sát hoặc hỏi ý kiến nhân viên y tế: "
                    + (symptoms.isEmpty() ? "triệu chứng đã nhập" : String.join(", ", symptoms)) + ".";
            default -> "Chưa ghi nhận red flag từ thông tin hiện có.";
        };
    }

    private String possibleConcern(String riskLevel, List<String> redFlags) {
        return switch (riskLevel) {
            case "RED" -> String.join("; ", redFlags);
            case "YELLOW" -> "Triệu chứng chưa có red flag rõ, nhưng cần theo dõi diễn tiến.";
            default -> "Triệu chứng nhẹ, trẻ vẫn tỉnh táo, bú/uống tốt và thở bình thường.";
        };
    }

    private String recommendedAction(String riskLevel) {
        return switch (riskLevel) {
            case "RED" -> "Liên hệ cấp cứu hoặc đưa trẻ đến cơ sở y tế gần nhất ngay. Không chờ tư vấn AI nếu tình trạng đang xấu đi.";
            case "YELLOW" -> "Theo dõi nhiệt độ, nhịp thở, bú/uống và tình trạng tỉnh táo. Liên hệ bác sĩ nếu kéo dài, nặng hơn hoặc xuất hiện red flag.";
            default -> "Theo dõi tại nhà, cho trẻ bú/uống đủ, ghi nhận triệu chứng và quay lại kiểm tra nếu có thay đổi.";
        };
    }
}
