package com.carebridge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.community.entity.AnswerStatus;
import com.carebridge.backend.community.entity.CommunityAnswer;
import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.repository.CommunityAnswerRepository;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.content.dto.request.DashboardFilter;
import com.carebridge.backend.content.dto.response.CommunityDashboardResponse;
import com.carebridge.backend.content.entity.ContentReport;
import com.carebridge.backend.content.entity.ReportStatus;
import com.carebridge.backend.content.entity.ReportTargetType;
import com.carebridge.backend.content.repository.ContentReportRepository;
import com.carebridge.backend.content.service.CommunityDashboardService;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import com.carebridge.backend.security.service.SmsService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// DASH-TC-INT-001, DASH-TC-INT-002
// Note: no Testcontainers/real-Postgres harness exists in this codebase (verified project-wide, same
// finding as UC-110's RedFlagRuleIntegrationTest / ModerateContentIntegrationTest). Hosted as
// @SpringBootTest + H2 (real Spring-managed beans end-to-end) instead — seeded rows here ARE the oracle
// (equivalent to a direct-SQL cross-check since the expected values are defined by the seed itself).
@SpringBootTest
@Transactional
class CommunityDashboardIntegrationTest {

    @Autowired
    private CommunityDashboardService communityDashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityQuestionRepository communityQuestionRepository;

    @Autowired
    private CommunityAnswerRepository communityAnswerRepository;

    @Autowired
    private CommunityTopicRepository communityTopicRepository;

    @Autowired
    private ContentReportRepository contentReportRepository;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    private User makeUser(Role role, boolean enabled, boolean locked, Instant suspendedUntil, String phoneSuffix) {
        return userRepository.save(User.builder()
                .phone("09" + phoneSuffix)
                .role(role)
                .enabled(enabled)
                .locked(locked)
                .suspendedUntil(suspendedUntil)
                .build());
    }

    private CommunityTopic makeTopic(String name, boolean hidden) {
        CommunityTopic topic = new CommunityTopic();
        topic.setName(name);
        topic.setHidden(hidden);
        return communityTopicRepository.save(topic);
    }

    private CommunityQuestion makeQuestion(UUID topicId, QuestionStatus status, Instant createdAt) {
        CommunityQuestion q = new CommunityQuestion();
        q.setTopicId(topicId);
        q.setAuthorId(UUID.randomUUID());
        q.setTitle("Test question");
        q.setBody("Test body");
        q.setStage(PregnancyStage.PREGNANCY);
        q.setUrgency(UrgencyLevel.NORMAL);
        q.setStatus(status);
        q.setCreatedAt(createdAt);
        return communityQuestionRepository.save(q);
    }

    private CommunityAnswer makeAnswer(UUID questionId, AnswerStatus status) {
        CommunityAnswer a = new CommunityAnswer();
        a.setQuestionId(questionId);
        a.setAuthorId(UUID.randomUUID());
        a.setBody("Test answer");
        a.setStatus(status);
        a.setCreatedAt(Instant.now());
        return communityAnswerRepository.save(a);
    }

    private ContentReport makeReport(ReportStatus status, Instant createdAt, Instant resolvedAt) {
        ContentReport r = new ContentReport();
        r.setTargetId(UUID.randomUUID());
        r.setTargetType(ReportTargetType.QUESTION);
        r.setStatus(status);
        r.setCreatedAt(createdAt);
        r.setResolvedAt(resolvedAt);
        return contentReportRepository.save(r);
    }

    // DASH-TC-INT-001
    @Test
    void getDashboard_seededData_countsMatchKnownSeed() {
        makeUser(Role.MOTHER, true, false, null, "11111111");
        makeUser(Role.MOTHER, true, false, Instant.now().plusSeconds(3600), "22222222"); // suspended future
        makeUser(Role.SYSTEM_ADMIN, true, false, null, "33333333");

        CommunityTopic visible = makeTopic("Dinh dưỡng thai kỳ", false);
        CommunityTopic hidden = makeTopic("Hidden topic", true);

        // CommunityQuestion.createdAt is @CreationTimestamp — Hibernate overwrites any explicit value with
        // the actual insert instant, so the reporting window below must bracket "now", not a fixed date.
        makeQuestion(visible.getId(), QuestionStatus.APPROVED, null);
        makeQuestion(visible.getId(), QuestionStatus.APPROVED, null);
        makeQuestion(hidden.getId(), QuestionStatus.APPROVED, null);
        makeQuestion(hidden.getId(), QuestionStatus.APPROVED, null);
        makeQuestion(hidden.getId(), QuestionStatus.APPROVED, null); // hidden topic has MORE questions

        CommunityQuestion q1 = makeQuestion(visible.getId(), QuestionStatus.PENDING, null);
        makeAnswer(q1.getId(), AnswerStatus.APPROVED);

        // ContentReport.createdAt/resolvedAt have no @CreationTimestamp — explicit values persist as-is.
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        makeReport(ReportStatus.RESOLVED, base, base.plusSeconds(3600));
        makeReport(ReportStatus.RESOLVED, base, base.plusSeconds(10800));
        makeReport(ReportStatus.PENDING, base, null);

        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        CommunityDashboardResponse response = communityDashboardService.getDashboard(
                new DashboardFilter(today.minusDays(1), today.plusDays(1)));

        assertThat(response.userMetrics().total()).isEqualTo(3);
        assertThat(response.userMetrics().active()).isEqualTo(2); // 1 suspended-future excluded

        assertThat(response.questionMetrics().total()).isEqualTo(6);
        assertThat(response.questionMetrics().newInPeriod()).isEqualTo(6);

        assertThat(response.answerMetrics().total()).isEqualTo(1);

        assertThat(response.reportMetrics().byStatus().get("RESOLVED")).isEqualTo(2L);
        assertThat(response.reportMetrics().avgHandlingTimeSeconds()).isEqualTo(7200.0, org.assertj.core.data.Offset.offset(0.01));

        // Hidden topic has the most questions (3) but must NOT appear in trending
        assertThat(response.trendingTopics())
                .extracting("topicName")
                .containsExactly("Dinh dưỡng thai kỳ")
                .doesNotContain("Hidden topic");
    }

    // DASH-TC-INT-002
    @Test
    void getDashboard_readOnly_doesNotMutateAnyTable() {
        makeUser(Role.MOTHER, true, false, null, "44444444");
        CommunityTopic topic = makeTopic("Read-only check", false);
        makeQuestion(topic.getId(), QuestionStatus.APPROVED, Instant.now());

        long usersBefore = userRepository.count();
        long questionsBefore = communityQuestionRepository.count();
        long answersBefore = communityAnswerRepository.count();
        long reportsBefore = contentReportRepository.count();

        communityDashboardService.getDashboard(new DashboardFilter(null, null));

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(communityQuestionRepository.count()).isEqualTo(questionsBefore);
        assertThat(communityAnswerRepository.count()).isEqualTo(answersBefore);
        assertThat(contentReportRepository.count()).isEqualTo(reportsBefore);
    }
}
