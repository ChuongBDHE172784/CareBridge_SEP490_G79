package com.carebridge.backend.triage.policy;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TriageRedFlagPolicy {

    private static final List<String> RED_FLAG_KEYWORDS = List.of(
            "chảy máu nhiều", "ngất xỉu", "khó thở", "co giật", "tim ngừng đập",
            "xuất huyết", "hôn mê", "đau ngực dữ dội", "sảy thai", "sinh non",
            "ngộ độc", "bất tỉnh", "đuối nước", "gãy xương hở", "bỏng nặng",
            "mất ý thức", "không thở", "đau bụng dữ dội", "chảy máu âm đạo nhiều");

    private static final String EMERGENCY_GUIDANCE =
            "Đây có thể là tình huống khẩn cấp y tế. " +
            "Hãy gọi 115 hoặc đến cơ sở y tế gần nhất ngay lập tức. Đừng chờ đợi.";

    public boolean isRedFlag(String query) {
        if (query == null || query.isBlank()) return false;
        String lowerQuery = query.toLowerCase();
        return RED_FLAG_KEYWORDS.stream()
                .anyMatch(keyword -> lowerQuery.contains(keyword.toLowerCase()));
    }

    public String getEmergencyGuidance() {
        return EMERGENCY_GUIDANCE;
    }
}
