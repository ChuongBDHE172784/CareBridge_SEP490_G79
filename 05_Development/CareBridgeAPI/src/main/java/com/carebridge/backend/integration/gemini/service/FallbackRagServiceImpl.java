package com.carebridge.backend.integration.gemini.service;

import com.carebridge.backend.integration.gemini.dto.RagAnswerRequest;
import com.carebridge.backend.integration.gemini.dto.RagAnswerResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Always-active fallback that satisfies the RagService dependency when no
 * profile-specific implementation (GeminiRagServiceImpl, MockRagServiceImpl) is
 * registered — e.g. when the application starts in the default profile or without
 * a Gemini API key.  GeminiRagServiceImpl is @Primary so it wins when both exist.
 */
@Service
public class FallbackRagServiceImpl implements RagService {

    static final String STANDARD_DISCLAIMER =
            "Đây là thông tin hỗ trợ AI — không phải chẩn đoán y tế. " +
            "Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế.";

    static final String CONSERVATIVE_FALLBACK =
            "Tôi hiện không thể trả lời câu hỏi này. " +
            "Vui lòng tham khảo bác sĩ hoặc chuyên gia y tế của bạn.";

    @Override
    public RagAnswerResponse generateAnswer(RagAnswerRequest request) {
        return RagAnswerResponse.builder()
                .answer(CONSERVATIVE_FALLBACK)
                .disclaimer(STANDARD_DISCLAIMER)
                .sources(List.of())
                .fallback(true)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
