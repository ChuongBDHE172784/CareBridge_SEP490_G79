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

@Transactional
class ExpertProfileRepositorySearchIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ExpertProfileRepository expertProfileRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean private IZegoCloudService zegoCloudService;

    private static final UUID EXPERT_USER_ID = UUID.randomUUID();

    @BeforeEach
    void seed() {
        jdbcTemplate.update(
                "INSERT INTO users (user_id, person_id, full_name, display_name, phone, role, "
                        + "specialty, professional_title, workplace, verification_status, "
                        + "trust_status, enabled, locked, created_at, updated_at) "
                        + "VALUES (?, ?, 'Nguyen Van A', 'Nguyen Van A', ?, 'EXPERT', "
                        + "'San khoa', 'Bac si CKI', 'BV Tu Du', 'APPROVED', 'ACTIVE', "
                        + "true, false, now(), now())",
                EXPERT_USER_ID, EXPERT_USER_ID,
                "09" + String.valueOf(System.nanoTime()).substring(0, 8));
    }

    @Test
    void searchDirectory_matchesFullNameCaseInsensitive() {
        Page<ExpertProfile> result =
                expertProfileRepository.searchDirectory(null, "nguyen", PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(ExpertProfile::getUserId)
                .contains(EXPERT_USER_ID);
    }

    @Test
    void searchDirectory_matchesWorkplaceCaseInsensitive() {
        Page<ExpertProfile> result =
                expertProfileRepository.searchDirectory(null, "TU DU", PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(ExpertProfile::getUserId)
                .contains(EXPERT_USER_ID);
    }

    @Test
    void searchDirectory_noMatch_returnsEmpty() {
        Page<ExpertProfile> result = expertProfileRepository.searchDirectory(
                null, "khong-ton-tai-xyz", PageRequest.of(0, 10));
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void searchDirectory_sqlInjectionAttempt_handledSafely() {
        String malicious = "'; DROP TABLE users; --";

        Page<ExpertProfile> result =
                expertProfileRepository.searchDirectory(null, malicious, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        Long stillExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id = ? AND specialty IS NOT NULL",
                Long.class, EXPERT_USER_ID);
        assertThat(stillExists).isEqualTo(1L);
    }
}
