package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import com.carebridge.backend.integration.gemini.dto.RagSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test-profile implementation of RagService.
 * Returns canned responses — never calls Gemini API.
 * Used in all unit/integration tests via @Profile("test").
 */
@Service
@Primary
@Profile("test")
public class MockRagServiceImpl implements RagService {

    static final String STANDARD_DISCLAIMER =
            "Đây là thông tin hỗ trợ AI — không phải chẩn đoán y tế. " +
            "Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế.";

    private static final String CANNED_ANSWER =
            "Phù chân nhẹ trong thai kỳ là hiện tượng phổ biến do áp lực tử cung tăng. " +
            "Tuy nhiên, nên theo dõi và tham khảo ý kiến bác sĩ nếu có triệu chứng bất thường.";

    private static final UUID MOCK_CONTENT_ID =
            UUID.fromString("11111111-0000-0000-0000-000000000001");

    @Override
    public RagAnswerResponse generateAnswer(RagAnswerRequest request) {
        String query = request.getQuery();

        // Simulate red-flag
        if (query != null && (query.contains("chảy máu") || query.contains("ngất xỉu"))) {
            return RagAnswerResponse.builder()
                    .answer("Đây có thể là tình huống khẩn cấp. Hãy gọi 115 ngay lập tức.")
                    .disclaimer(STANDARD_DISCLAIMER)
                    .sources(List.of())
                    .fallback(true)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // Simulate Gemini unavailable / timeout
        if ("timeout_test".equals(query)) {
            return RagAnswerResponse.builder()
                    .answer("Tôi hiện không thể trả lời câu hỏi này. " +
                            "Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế.")
                    .disclaimer(STANDARD_DISCLAIMER)
                    .sources(List.of())
                    .fallback(true)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        // Happy path — fixed canned response (NOT echoing query to prevent injection test failures)
        return RagAnswerResponse.builder()
                .answer(CANNED_ANSWER)
                .disclaimer(STANDARD_DISCLAIMER)
                .sources(List.of(RagSource.builder()
                        .contentId(MOCK_CONTENT_ID)
                        .title("Bài viết test 1 — Dinh dưỡng thai kỳ")
                        .build()))
                .fallback(false)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
