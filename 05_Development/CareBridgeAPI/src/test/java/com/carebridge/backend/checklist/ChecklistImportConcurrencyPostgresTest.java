package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Deterministic two-transaction PostgreSQL lock evidence for Story 6.9 INT-003. */
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
class ChecklistImportConcurrencyPostgresTest extends AbstractPostgresIntegrationTest {

    @Autowired private IUserChecklistItemService checklistService;
    @Autowired private ChecklistTemplateRepository templateRepository;
    @Autowired private ChecklistItemRepository templateItemRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private BabyProfileRepository babyProfileRepository;

    private UUID motherId;

    @BeforeEach
    void setUp() {
        wipeStoryFixtures();
        motherId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, motherId, "Story 69 Race Mother", uniquePhone(), "MOTHER");
    }

    @AfterEach
    void cleanUp() {
        wipeStoryFixtures();
    }

    @Test
    void uc82_69_int_003_journeyTransitionSerializesBeforeImportAndLeavesNoPartialEffects()
            throws Exception {
        UUID journeyId = seedJourney(JourneyType.PREGNANCY);
        UUID itemId = seedItem(ContentStage.PREGNANCY, "Pregnancy item");
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch importStarted = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var transition = executor.submit(() -> transaction().executeWithoutResult(status -> {
                jdbcTemplate.queryForObject(
                        "select journey_id from mother_journeys where journey_id=? for update",
                        UUID.class, journeyId);
                lockHeld.countDown();
                await(releaseLock);
                jdbcTemplate.update(
                        "update mother_journeys set status='COMPLETED', updated_at=now() where journey_id=?",
                        journeyId);
            }));
            assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();

            var importAttempt = executor.submit(() -> {
                importStarted.countDown();
                return importOutcome(new ImportFromTemplateRequest(journeyId, null, List.of(itemId)));
            });
            try {
                assertThat(importStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(awaitBlockedQuery("mother_journeys"))
                        .as("PostgreSQL must report the canonical SELECT FOR UPDATE waiting on a row lock")
                        .isTrue();
            } finally {
                releaseLock.countDown();
            }
            transition.get(10, TimeUnit.SECONDS);
            assertThat(importAttempt.get(10, TimeUnit.SECONDS)).isEqualTo("CHECKLIST-007");
        }

        assertNoImportSideEffects();
    }

    @Test
    void uc82_69_int_003_babyInvalidationSerializesBeforeImportAndLeavesNoPartialEffects()
            throws Exception {
        UUID babyId = babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(motherId)
                .nickname("Story 69 baby")
                .birthDate(LocalDate.of(2026, 1, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build()).getId();
        setActiveBaby(babyId);
        UUID itemId = seedItem(ContentStage.BABY_CARE, "Baby care item");
        CountDownLatch lockHeld = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        CountDownLatch importStarted = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var invalidation = executor.submit(() -> transaction().executeWithoutResult(status -> {
                jdbcTemplate.queryForObject(
                        "select care_subject_id from care_subjects "
                                + "where care_subject_id=? and subject_type='BABY' for update",
                        UUID.class, babyId);
                lockHeld.countDown();
                await(releaseLock);
                jdbcTemplate.update(
                        "update care_subjects set status='ARCHIVED', updated_at=now() "
                                + "where care_subject_id=? and subject_type='BABY'",
                        babyId);
            }));
            assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();

            var importAttempt = executor.submit(() -> {
                importStarted.countDown();
                return importOutcome(new ImportFromTemplateRequest(null, babyId, List.of(itemId)));
            });
            try {
                assertThat(importStarted.await(10, TimeUnit.SECONDS)).isTrue();
                assertThat(awaitBlockedQuery("care_subjects"))
                        .as("PostgreSQL must report the owned ACTIVE baby SELECT FOR UPDATE waiting on a row lock")
                        .isTrue();
            } finally {
                releaseLock.countDown();
            }
            invalidation.get(10, TimeUnit.SECONDS);
            assertThat(importAttempt.get(10, TimeUnit.SECONDS)).isEqualTo("CHECKLIST-007");
        }

        assertNoImportSideEffects();
    }

    @Test
    void legacyBabyImportSerializesConcurrentReimportWithoutCreatingNormalizedDuplicate()
            throws Exception {
        UUID journeyId = seedJourney(JourneyType.POSTPARTUM);
        UUID babyId = babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(motherId)
                .relatedJourneyId(journeyId)
                .nickname("Legacy scoped baby")
                .birthDate(LocalDate.of(2026, 1, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build()).getId();
        setActiveBaby(babyId);
        UUID itemId = seedItem(ContentStage.BABY_CARE, "Legacy baby care item");
        UUID legacyRowId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into preparation_checklist_items (
                    checklist_item_id,
                    owner_user_id,
                    mother_journey_id,
                    baby_id,
                    template_entry_id,
                    title,
                    category,
                    status,
                    display_order,
                    created_at,
                    updated_at
                ) values (?, ?, ?, ?, ?, 'Legacy baby care item', 'GENERAL', 'OPEN', 1, now(), now())
                """, legacyRowId, motherId, journeyId, babyId, itemId);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ImportFromTemplateRequest request = new ImportFromTemplateRequest(null, babyId, List.of(itemId));

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                ready.countDown();
                await(start);
                return checklistService.importFromTemplate(request, motherId).getFirst();
            });
            var second = executor.submit(() -> {
                ready.countDown();
                await(start);
                return checklistService.importFromTemplate(request, motherId).getFirst();
            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            assertThat(firstResult.itemId()).isEqualTo(legacyRowId);
            assertThat(secondResult.itemId()).isEqualTo(legacyRowId);
            assertThat(firstResult.journeyId()).isNull();
            assertThat(secondResult.journeyId()).isNull();
            assertThat(firstResult.babyId()).isEqualTo(babyId);
            assertThat(secondResult.babyId()).isEqualTo(babyId);
        }

        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from preparation_checklist_items
                where owner_user_id = ?
                  and baby_id = ?
                  and template_entry_id = ?
                """, Long.class, motherId, babyId, itemId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                select count(*)
                from preparation_checklist_items
                where checklist_item_id = ?
                  and mother_journey_id is null
                """, Long.class, legacyRowId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_events where actor_user_id=? "
                        + "and event_category='CHECKLIST_ITEM_ADDED' and event_origin='AUDIT_LOG'",
                Long.class, motherId)).isZero();
    }

    private String importOutcome(ImportFromTemplateRequest request) {
        try {
            checklistService.importFromTemplate(request, motherId);
            return "SUCCESS";
        } catch (BusinessException exception) {
            return exception.getCode();
        } catch (ContentException exception) {
            return exception.getCode();
        }
    }

    private boolean awaitBlockedQuery(String relation) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Boolean blocked = jdbcTemplate.queryForObject(
                    "select exists (select 1 from pg_stat_activity "
                            + "where pid <> pg_backend_pid() and wait_event_type='Lock' and query ilike ?)",
                    Boolean.class, "%" + relation + "%");
            if (Boolean.TRUE.equals(blocked)) {
                return true;
            }
            Thread.onSpinWait();
        }
        return false;
    }

    private UUID seedItem(ContentStage stage, String text) {
        ChecklistTemplate template = templateRepository.saveAndFlush(ChecklistTemplate.builder()
                .name(text + " template")
                .stage(stage)
                .status(ChecklistTemplateStatus.APPROVED)
                .versionNo(1)
                .build());
        return templateItemRepository.saveAndFlush(ChecklistItem.builder()
                .template(template)
                .itemText(text)
                .order(1)
                .isRequired(true)
                .build()).getId();
    }

    private UUID seedJourney(JourneyType journeyType) {
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, u.person_id, u.user_id, 'MOTHER', p.display_name,
                       'ACTIVE', now(), now()
                  from users u join persons p on p.person_id = u.person_id
                 where u.user_id = ?
                """, careSubjectId, motherId);
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(motherId)
                .careSubjectId(careSubjectId)
                .journeyType(journeyType)
                .status(JourneyStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1))
                .build());
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey.getId(), careSubjectId);
        return journey.getId();
    }

    private TransactionTemplate transaction() {
        return new TransactionTemplate(transactionManager);
    }

    private void assertNoImportSideEffects() {
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from preparation_checklist_items", Long.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "select count(*) from audit_events where actor_user_id=? "
                                + "and event_category='CHECKLIST_ITEM_ADDED' and event_origin='AUDIT_LOG'",
                        Long.class, motherId))
                .isZero();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for deterministic transaction barrier");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for deterministic transaction barrier", exception);
        }
    }

    private String uniquePhone() {
        return "08" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
    }

    private void setActiveBaby(UUID babyId) {
        assertThat(jdbcTemplate.update(
                "update users set settings_jsonb=jsonb_set(coalesce(settings_jsonb,'{}'::jsonb), "
                        + "'{activeBabyId}', to_jsonb(cast(? as text)), true) where user_id=?",
                babyId.toString(), motherId)).isOne();
    }

    private void wipeStoryFixtures() {
        jdbcTemplate.execute(
                "truncate table preparation_checklist_items, care_item_templates, "
                        + "mother_journey_events, care_subjects, mother_journeys, "
                        + "audit_events, users, persons cascade");
    }
}
