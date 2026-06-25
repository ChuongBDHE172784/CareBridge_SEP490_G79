package com.carebridge.backend.integration.gemini.builder;

import com.carebridge.backend.content.entity.ContentItem;
import com.carebridge.backend.integration.gemini.dto.UserStage;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeminiPromptBuilder {

    static final String SAFETY_SYSTEM_INSTRUCTION =
            "Bạn là trợ lý thông tin sức khỏe. " +
            "Không chẩn đoán bệnh. Không kê đơn thuốc cụ thể. Không nêu tên thuốc. " +
            "Luôn khuyến nghị tham khảo bác sĩ. " +
            "Chỉ trả lời dựa trên context được cung cấp. " +
            "Nếu câu hỏi vượt quá phạm vi hỗ trợ, hướng dẫn người dùng gặp chuyên gia y tế.";

    private static final int MAX_BODY_CHARS = 500;

    public String buildSafetyConstrainedPrompt(String query, List<ContentItem> contextChunks, UserStage userStage) {
        StringBuilder sb = new StringBuilder();
        sb.append("[SYSTEM INSTRUCTION]\n").append(SAFETY_SYSTEM_INSTRUCTION).append("\n\n");

        if (userStage != null) {
            sb.append("[USER STAGE]\n").append(userStage.name()).append("\n\n");
        }

        if (contextChunks != null && !contextChunks.isEmpty()) {
            sb.append("[CONTEXT FROM APPROVED SOURCES]\n");
            for (int i = 0; i < contextChunks.size(); i++) {
                ContentItem item = contextChunks.get(i);
                sb.append("Source ").append(i + 1).append(": ").append(item.getTitle()).append("\n");
                if (item.getBody() != null) {
                    String body = item.getBody().length() > MAX_BODY_CHARS
                            ? item.getBody().substring(0, MAX_BODY_CHARS) + "..."
                            : item.getBody();
                    sb.append(body).append("\n\n");
                }
            }
        }

        sb.append("[USER QUESTION]\n").append(query);
        return sb.toString();
    }

    public String getSafetySystemInstruction() {
        return SAFETY_SYSTEM_INSTRUCTION;
    }
}
