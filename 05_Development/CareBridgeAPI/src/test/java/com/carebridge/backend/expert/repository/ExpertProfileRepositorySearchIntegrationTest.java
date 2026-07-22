package com.carebridge.backend.expert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.integration.zegocloud.IZegoCloudService;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

// MEDI-TC-002, MEDI-TC-SEC-001 — real Postgres, native query with LIKE + bind params.
// IZegoCloudService mocked because the full @SpringBootTest context also boots
// ConversationCallServiceImpl (directchat), which needs carebridge.zego.app-id — same
// pattern as DirectChatIntegrationTest, unrelated to what this test actually exercises.
// @Transactional — each test method rolls back automatically (per AbstractPostgresIntegrationTest
// javadoc), so the fixed EXPERT_USER_ID seeded in @BeforeEach never collides across methods.
@Transactional
class ExpertProfileRepositorySearchIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;

    private static final UUID EXPERT_USER_ID = UUID.randomUUID();

    @BeforeEach
    void seed() {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, full_name, phone, role, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, 'Nguyễn Văn A', ?, 'EXPERT', true, false, now(), now())",
                EXPERT_USER_ID, "09" + String.valueOf(System.nanoTime()).substring(0, 8));
        jdbcTemplate.update(
                "INSERT INTO professional_profiles (professional_profile_id, user_id, specialty, professional_title, workplace, verification_status, created_at, updated_at) "
                        + "VALUES (?, ?, 'Sản khoa', 'Bác sĩ CKI', 'BV Từ Dũ', 'APPROVED', now(), now())",
                UUID.randomUUID(), EXPERT_USER_ID);
    }

    // MEDI-TC-002 step 1 — matches on full_name, case-insensitive
    @Test
    void searchDirectory_matchesFullNameCaseInsensitive() {
        Page<ExpertProfile> result = expertProfileRepository.searchDirectory(null, "nguyễn", PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(ExpertProfile::getUserId).contains(EXPERT_USER_ID);
    }

    // MEDI-TC-002 step 2 — matches on workplace, case-insensitive
    @Test
    void searchDirectory_matchesWorkplaceCaseInsensitive() {
        Page<ExpertProfile> result = expertProfileRepository.searchDirectory(null, "TỪ DŨ", PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(ExpertProfile::getUserId).contains(EXPERT_USER_ID);
    }

    // MEDI-TC-002 step 3 — no match returns empty, not an error
    @Test
    void searchDirectory_noMatch_returnsEmpty() {
        Page<ExpertProfile> result = expertProfileRepository.searchDirectory(null, "khong-ton-tai-xyz", PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }

    // MEDI-TC-SEC-001 — injection payload is a bind parameter, never concatenated into SQL
    @Test
    void searchDirectory_sqlInjectionAttempt_handledSafely() {
        String malicious = "'; DROP TABLE professional_profiles; --";

        Page<ExpertProfile> result = expertProfileRepository.searchDirectory(null, malicious, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        Long stillExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM professional_profiles WHERE user_id = ?", Long.class, EXPERT_USER_ID);
        assertThat(stillExists).isEqualTo(1L);
    }
}
