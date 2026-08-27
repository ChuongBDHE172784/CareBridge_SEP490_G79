package com.carebridge.backend.moderation;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import java.time.Instant;
import java.util.UUID;

// CB-MOD-TEST-004 §4 Props Isolation Boilerplate
final class PendingContentTestFactory {

    private PendingContentTestFactory() {}

    static CommunityQuestion pendingQuestion() {
        return CommunityQuestion.builder()
                .id(UUID.randomUUID())
                .topicId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Em bi dau bung duoi o tuan 20")
                .body("Em bi dau bung duoi nhe 2 ngay nay, co dang lo khong a?")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.LOW)
                .status(QuestionStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }

    static CommunityAnswer pendingAnswer() {
        return CommunityAnswer.builder()
                .id(UUID.randomUUID())
                .questionId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .body("Ban nen di kham som de bac si kiem tra truc tiep nhe.")
                .status(AnswerStatus.PENDING)
                .createdAt(Instant.now())
                .build();
    }
}
