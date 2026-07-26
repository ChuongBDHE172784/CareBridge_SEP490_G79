package com.carebridge.backend.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import com.carebridge.backend.expertavailability.entity.ExpertLocationShare;
import com.carebridge.backend.expertavailability.repository.ExpertAvailabilityRepository;
import com.carebridge.backend.expertavailability.repository.ExpertLocationShareRepository;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CanonicalExpertRuntimePersistencePostgresTest extends AbstractPostgresIntegrationTest {

    @Autowired private ExpertCredentialRepository credentialRepository;
    @Autowired private ExpertAvailabilityRepository availabilityRepository;
    @Autowired private ExpertLocationShareRepository locationShareRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void jpaPersistsAllThreeExpertRuntimeRecordsUsingOnlyCanonicalProfileId() {
        UUID userId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();
        insertExpert(userId, profileId);

        ExpertCredential credential = credentialRepository.saveAndFlush(ExpertCredential.builder()
                .expertProfileId(profileId)
                .credentialType("MEDICAL_LICENSE")
                .credentialNumber("CANONICAL-" + UUID.randomUUID())
                .reviewStatus(ReviewStatus.PENDING)
                .build());
        ExpertAvailability availability = availabilityRepository.saveAndFlush(ExpertAvailability.builder()
                .expertProfileId(profileId)
                .startAt(Instant.now().plusSeconds(3600))
                .endAt(Instant.now().plusSeconds(7200))
                .channelType("VIDEO")
                .status(AvailabilityStatus.AVAILABLE)
                .build());
        ExpertLocationShare locationShare = locationShareRepository.saveAndFlush(
                ExpertLocationShare.builder()
                        .expertProfileId(profileId)
                        .latitude(new BigDecimal("10.7769"))
                        .longitude(new BigDecimal("106.7009"))
                        .availabilityStatus("OFFLINE")
                        .expiresAt(LocalDateTime.now().plusMinutes(30))
                        .build());

        assertThat(credential.getCredentialId()).isNotNull();
        assertThat(availability.getAvailabilityId()).isNotNull();
        assertThat(locationShare.getLocationShareId()).isNotNull();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM expert_credentials
                 WHERE credential_id = ? AND professional_profile_id = ?
                """, Long.class, credential.getCredentialId(), profileId)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM expert_availability
                 WHERE availability_id = ? AND professional_profile_id = ?
                """, Long.class, availability.getAvailabilityId(), profileId)).isOne();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*)
                  FROM expert_location_shares
                 WHERE location_share_id = ? AND professional_profile_id = ?
                """, Long.class, locationShare.getLocationShareId(), profileId)).isOne();
    }

    private void insertExpert(UUID userId, UUID profileId) {
        jdbcTemplate.update("""
                INSERT INTO persons (person_id, display_name, created_at, updated_at)
                VALUES (?, 'Canonical persistence expert', now(), now())
                """, userId);
        jdbcTemplate.update("""
                INSERT INTO users (
                    user_id, person_id, email, role, enabled, locked, created_at, updated_at)
                VALUES (?, ?, ?, 'EXPERT', true, false, now(), now())
                """, userId, userId, "canonical-persistence-" + userId + "@test.invalid");
        jdbcTemplate.update("""
                INSERT INTO professional_profiles (
                    professional_profile_id, user_id, specialty, verification_status,
                    trust_status, created_at, updated_at)
                VALUES (?, ?, 'OBSTETRICS', 'APPROVED', 'ACTIVE', now(), now())
                """, profileId, userId);
    }
}
