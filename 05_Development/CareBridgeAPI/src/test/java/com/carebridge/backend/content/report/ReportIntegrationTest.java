package com.carebridge.backend.content.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.TopicType;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * RPT-TC-014-INT-001 — full stack: real controller → real service → real repositories →
 * Testcontainers PostgreSQL. Verifies the report is persisted with status=PENDING.
 */
@Transactional
class ReportIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.report.uc14@test.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityTopicRepository topicRepository;
    @Autowired private CommunityQuestionRepository questionRepository;
    @Autowired private ContentReportRepository contentReportRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void createReport_fullStack_persistsPendingReportToDb() throws Exception {
        User reporter = userRepository.save(User.builder()
                .email(EMAIL)
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
        String token = jwtTokenProvider.generateAccessToken(reporter);

        UUID categorySuffix = UUID.randomUUID();
        CommunityTopic category = topicRepository.save(CommunityTopic.builder()
                .name("Report target category - " + categorySuffix)
                .slug("report-target-category-" + categorySuffix)
                .type(TopicType.CATEGORY)
                .build());
        UUID topicSuffix = UUID.randomUUID();
        CommunityTopic topic = topicRepository.save(CommunityTopic.builder()
                .name("Report target topic - " + topicSuffix)
                .slug("report-target-topic-" + topicSuffix)
                .type(TopicType.TOPIC)
                .parentId(category.getId())
                .build());
        CommunityQuestion targetQuestion = questionRepository.save(CommunityQuestion.builder()
                .topicId(topic.getId())
                .authorId(UUID.randomUUID())
                .title("Câu hỏi bị báo cáo")
                .body("Nội dung")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED)
                .build());

        String body = """
                {
                  "targetType": "QUESTION",
                  "targetId": "%s",
                  "category": "HARASSMENT",
                  "description": "Nội dung xúc phạm"
                }
                """.formatted(targetQuestion.getId());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ContentReport saved = contentReportRepository.findAll().stream()
                .filter(r -> targetQuestion.getId().equals(r.getTargetId()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(saved.getReporterUserId()).isEqualTo(reporter.getId());
        assertThat(saved.getCategory()).isEqualTo("HARASSMENT");
    }
}
