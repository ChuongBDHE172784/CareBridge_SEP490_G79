package com.carebridge.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.community.entity.CommunityQuestion;
import com.carebridge.backend.community.entity.CommunityTopic;
import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.QuestionStatus;
import com.carebridge.backend.community.entity.UrgencyLevel;
import com.carebridge.backend.community.repository.CommunityQuestionRepository;
import com.carebridge.backend.community.repository.CommunityTopicRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * SEARCH-TC-013-010 (pagination) and SEARCH-TC-013-009 (SQL injection safety) — full stack:
 * real controller → real service → real provider → real repository → Testcontainers
 * PostgreSQL. Not mocked, unlike the unit/controller tests above.
 */
@Transactional
class SearchIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String EMAIL = "int.search.uc13@test.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CommunityTopicRepository topicRepository;
    @Autowired private CommunityQuestionRepository questionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String seedUserAndGetToken() {
        User user = userRepository.save(User.builder()
                .email(EMAIL)
                .role(Role.MOTHER)
                .passwordHash(passwordEncoder.encode("SecureP@ss1"))
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
        return jwtTokenProvider.generateAccessToken(user);
    }

    // SEARCH-TC-013-010: page=0 and page=1 return distinct, correctly-paginated results
    @Test
    void search_pagination_returnsDistinctPagesWithCorrectTotals() throws Exception {
        String token = seedUserAndGetToken();
        CommunityTopic topic = topicRepository.save(CommunityTopic.builder()
                .name("Dinh dưỡng thai kỳ - " + UUID.randomUUID())
                .build());

        for (int i = 0; i < 25; i++) {
            questionRepository.save(CommunityQuestion.builder()
                    .topicId(topic.getId())
                    .authorId(UUID.randomUUID())
                    .title("dinh dưỡng thai kỳ " + i)
                    .body("Nội dung câu hỏi số " + i)
                    .stage(PregnancyStage.PREGNANCY)
                    .urgency(UrgencyLevel.NORMAL)
                    .status(QuestionStatus.APPROVED)
                    .build());
        }

        String page0Body = mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "dinh dưỡng")
                        .param("type", "QUESTION")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(10))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(25))
                .andExpect(jsonPath("$.data.pagination.totalPages").value(3))
                .andReturn().getResponse().getContentAsString();

        String page1Body = mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "dinh dưỡng")
                        .param("type", "QUESTION")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(10))
                .andReturn().getResponse().getContentAsString();

        Set<String> page0Ids = extractIds(page0Body);
        Set<String> page1Ids = extractIds(page1Body);
        assertThat(page0Ids).hasSize(10);
        assertThat(page1Ids).hasSize(10);
        assertThat(page0Ids).doesNotContainAnyElementsOf(page1Ids);

        long count = questionRepository.searchApproved("dinh dưỡng", null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 100)).getTotalElements();
        assertThat(count).isEqualTo(25);
    }

    // SEARCH-TC-013-009: SQL injection attempt in q — must not error or drop data
    @Test
    void search_sqlInjectionAttempt_isHandledSafely() throws Exception {
        String token = seedUserAndGetToken();
        CommunityTopic topic = topicRepository.save(CommunityTopic.builder()
                .name("Safety topic - " + UUID.randomUUID())
                .build());
        questionRepository.save(CommunityQuestion.builder()
                .topicId(topic.getId())
                .authorId(UUID.randomUUID())
                .title("Câu hỏi an toàn")
                .body("Nội dung")
                .stage(PregnancyStage.PREGNANCY)
                .urgency(UrgencyLevel.NORMAL)
                .status(QuestionStatus.APPROVED)
                .build());

        String maliciousQuery = "'; DROP TABLE community_content; --";

        mockMvc.perform(get("/api/v1/search")
                        .header("Authorization", "Bearer " + token)
                        .param("q", maliciousQuery)
                        .param("type", "QUESTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));

        // Table must still exist and still contain the seeded row.
        assertThat(questionRepository.count()).isGreaterThanOrEqualTo(1);
    }

    private Set<String> extractIds(String responseBody) {
        Set<String> ids = new HashSet<>();
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("\"id\":\"([0-9a-fA-F-]{36})\"").matcher(responseBody);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }
}
