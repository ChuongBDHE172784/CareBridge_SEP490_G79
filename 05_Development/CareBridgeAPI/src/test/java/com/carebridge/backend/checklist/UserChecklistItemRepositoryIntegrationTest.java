package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class UserChecklistItemRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private UserChecklistItemRepository checklistRepository;
    @Autowired private ChecklistTemplateRepository templateRepository;
    @Autowired private ChecklistItemRepository templateItemRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    void concurrentImportsResolveToOnePersistedRow() throws Exception {
        Scope scope = transactionTemplate.execute(status -> seedScope());
        assertThat(scope).isNotNull();

        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> importOnce(scope, ready, start));
                var second = executor.submit(() -> importOnce(scope, ready, start));

                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();

                UserChecklistItem firstResult = first.get(15, TimeUnit.SECONDS);
                UserChecklistItem secondResult = second.get(15, TimeUnit.SECONDS);
                assertThat(firstResult.getId()).isEqualTo(secondResult.getId());
            }

            Integer persistedCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM preparation_checklist_items
                    WHERE owner_user_id = ?
                      AND mother_journey_id = ?
                      AND baby_id IS NULL
                      AND template_entry_id = ?
                    """, Integer.class, scope.ownerId(), scope.journeyId(), scope.templateItemId());
            assertThat(persistedCount).isEqualTo(1);
        } finally {
            transactionTemplate.executeWithoutResult(status -> cleanupScope(scope));
        }
    }

    private UserChecklistItem importOnce(
            Scope scope, CountDownLatch ready, CountDownLatch start) throws Exception {
        return transactionTemplate.execute(status -> {
            ready.countDown();
            try {
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while synchronizing concurrent import", exception);
            }
            checklistRepository.insertImportedIfAbsent(
                    UUID.randomUUID(),
                    scope.ownerId(),
                    scope.journeyId(),
                    null,
                    scope.templateItemId(),
                    "Prepare discharge documents",
                    1);
            return checklistRepository.findImportedByExactScope(
                            scope.ownerId(), scope.journeyId(), null, scope.templateItemId())
                    .orElseThrow();
        });
    }

    private Scope seedScope() {
        User owner = userRepository.saveAndFlush(User.builder()
                .email("checklist-" + UUID.randomUUID() + "@test.local")
                .role(Role.MOTHER)
                .passwordHash("not-used")
                .enabled(true)
                .locked(false)
                .emailVerified(true)
                .phoneVerified(false)
                .accountStatus("ACTIVE")
                .build());
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                SELECT ?, u.person_id, u.user_id, 'MOTHER', p.display_name,
                       'ACTIVE', now(), now()
                  FROM users u
                  JOIN persons p ON p.person_id = u.person_id
                 WHERE u.user_id = ?
                """, careSubjectId, owner.getId());
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(owner.getId())
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .build());
        ChecklistTemplate template = templateRepository.saveAndFlush(ChecklistTemplate.builder()
                .name("Approved concurrent import")
                .stage(ContentStage.PREGNANCY)
                .status(ChecklistTemplateStatus.APPROVED)
                .build());
        ChecklistItem templateItem = templateItemRepository.saveAndFlush(ChecklistItem.builder()
                .template(template)
                .itemText("Prepare discharge documents")
                .order(1)
                .isRequired(false)
                .build());
        return new Scope(
                owner.getId(),
                owner.getPerson().getId(),
                careSubjectId,
                journey.getId(),
                template.getId(),
                templateItem.getId());
    }

    private void cleanupScope(Scope scope) {
        jdbcTemplate.update("DELETE FROM preparation_checklist_items WHERE owner_user_id = ?", scope.ownerId());
        jdbcTemplate.update("DELETE FROM mother_journey_events WHERE mother_journey_id = ?", scope.journeyId());
        jdbcTemplate.update("DELETE FROM mother_journeys WHERE journey_id = ?", scope.journeyId());
        jdbcTemplate.update("DELETE FROM care_subjects WHERE care_subject_id = ?", scope.careSubjectId());
        jdbcTemplate.update("DELETE FROM care_item_templates WHERE template_id = ?", scope.templateItemId());
        jdbcTemplate.update("DELETE FROM care_item_templates WHERE template_id = ?", scope.templateId());
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", scope.ownerId());
        jdbcTemplate.update("DELETE FROM persons WHERE person_id = ?", scope.personId());
    }

    private record Scope(
            UUID ownerId,
            UUID personId,
            UUID careSubjectId,
            UUID journeyId,
            UUID templateId,
            UUID templateItemId) {
    }
}
