package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.triage.policy.TriageRedFlagPolicy;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TriageRedFlagPolicyTest {

    private final TriageRedFlagPolicy policy = new TriageRedFlagPolicy();

    @ParameterizedTest
    @MethodSource("safetyFloorPhrases")
    void isRedFlag_matchesEveryNonConfigurableSafetyPhrase(String phrase) {
        assertThat(policy.isRedFlag("Tôi đang " + phrase.toUpperCase() + ", phải làm sao?"))
                .isTrue();
    }

    @Test
    void isRedFlag_rejectsBlankAndOrdinaryQueries() {
        assertThat(policy.isRedFlag(null)).isFalse();
        assertThat(policy.isRedFlag("  ")).isFalse();
        assertThat(policy.isRedFlag("Tôi muốn hỏi lịch khám thai định kỳ")).isFalse();
    }

    private static Stream<String> safetyFloorPhrases() {
        return Stream.of(
                "chảy máu nhiều", "ngất xỉu", "khó thở", "co giật", "tim ngừng đập",
                "xuất huyết", "hôn mê", "đau ngực dữ dội", "sảy thai", "sinh non",
                "ngộ độc", "bất tỉnh", "đuối nước", "gãy xương hở", "bỏng nặng",
                "mất ý thức", "không thở", "đau bụng dữ dội", "chảy máu âm đạo nhiều",
                "băng huyết", "kết liễu", "không muốn sống", "không thiết sống",
                "mất máu nhiều", "máu ồ ạt", "thấm ướt băng", "ướt đẫm băng");
    }
}
