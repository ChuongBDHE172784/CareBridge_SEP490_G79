package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.IUserChecklistItemService;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.journey.entity.JourneyStatus;
import com.carebridge.backend.journey.entity.JourneyType;
import com.carebridge.backend.journey.entity.MotherJourney;
import com.carebridge.backend.journey.repository.MotherJourneyRepository;
import com.carebridge.backend.testsupport.AbstractPostgresIntegrationTest;
import com.carebridge.backend.testsupport.CanonicalUserFixture;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

/** Real PostgreSQL/Flyway transaction evidence for Story 6.9 INT-001/002. */
class ChecklistImportPostgresIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private IUserChecklistItemService checklistService;
    @Autowired private UserChecklistItemRepository userChecklistItemRepository;
    @Autowired private ChecklistTemplateRepository templateRepository;
    @Autowired private ChecklistItemRepository templateItemRepository;
    @Autowired private MotherJourneyRepository journeyRepository;
    @Autowired private BabyProfileRepository babyProfileRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;
    @Autowired private MockMvc mockMvc;
    @MockitoSpyBean private AuditEligibilityPolicy auditEligibilityPolicy;

    private UUID motherId;
    private UUID journeyId;

    @BeforeEach
    void setUp() {
        wipeStoryFixtures();
        motherId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, motherId, "Story 69 Mother", uniquePhone(), "MOTHER");
        journeyId = seedJourney(motherId, LocalDate.of(2026, 1, 1));
    }

    @AfterEach
    void cleanUp() {
        reset(auditEligibilityPolicy);
        wipeStoryFixtures();
    }

    @Test
    void r69_007_fullContextValidationFreezesChecklist001AgainstGlobalAdvice() throws Exception {
        mockMvc.perform(post("/api/v1/user-checklist-items/import")
                        .with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateItemIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("CHECKLIST-001"))
                .andExpect(jsonPath("$.message").value(
                        "templateItemIds must contain 1 to 50 entries"));

        mockMvc.perform(post("/api/v1/user-checklist-items/import")
                        .with(csrf())
                        .with(user(motherId.toString()).roles("MOTHER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateItemIds\":[not-json]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("CHECKLIST-001"))
                .andExpect(jsonPath("$.message").value("Invalid checklist request"));
    }

    @Test
    void uc82_69_int_001_mixedApprovedAndDeniedBatchRollsBackRowsAndAudits() {
        UUID approved = seedItem(ChecklistTemplateStatus.APPROVED, ContentStage.PREGNANCY, "Approved");
        UUID rejected = seedItem(ChecklistTemplateStatus.REJECTED, ContentStage.PREGNANCY, "Rejected");
        long rowsBefore = userChecklistItemRepository.count();
        long auditsBefore = checklistAuditCount();

        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(journeyId, null, List.of(approved, rejected)), motherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("CHECKLIST-007"));

        entityManager.clear();
        assertThat(userChecklistItemRepository.count()).isEqualTo(rowsBefore);
        assertThat(checklistAuditCount()).isEqualTo(auditsBefore);
    }

    @Test
    void uc82_69_int_002_secondAuditFailureRollsBackEveryImportedRowAndAudit() {
        UUID first = seedItem(ChecklistTemplateStatus.APPROVED, ContentStage.PREGNANCY, "First");
        UUID second = seedItem(ChecklistTemplateStatus.APPROVED, ContentStage.PREGNANCY, "Second");
        AtomicInteger eligibleChecklistAudits = new AtomicInteger();
        doAnswer(invocation -> {
            AuditAction action = invocation.getArgument(0);
            if (action == AuditAction.CHECKLIST_ITEM_ADDED
                    && eligibleChecklistAudits.incrementAndGet() == 2) {
                throw new IllegalStateException("synthetic second audit persistence failure");
            }
            return invocation.callRealMethod();
        }).when(auditEligibilityPolicy).shouldAudit(any(AuditAction.class));

        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(journeyId, null, List.of(first, second)), motherId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic second audit persistence failure");

        entityManager.clear();
        assertThat(userChecklistItemRepository.count()).isZero();
        assertThat(checklistAuditCount()).isZero();
    }

    @Test
    void uc82_69_tc_015_existingSnapshotSurvivesArchiveWhileNewImportIsDenied() {
        UUID itemId = seedItem(
                ChecklistTemplateStatus.APPROVED, ContentStage.PREGNANCY, "Snapshot text");

        var imported = checklistService.importFromTemplate(
                new ImportFromTemplateRequest(journeyId, null, List.of(itemId)), motherId);
        assertThat(imported).singleElement().satisfies(row -> {
            assertThat(row.templateItemId()).isEqualTo(itemId);
            assertThat(row.itemText()).isEqualTo("Snapshot text");
            assertThat(row.itemOrder()).isEqualTo(1);
        });
        UUID templateId = jdbcTemplate.queryForObject(
                "select parent_template_id from care_item_templates "
                        + "where template_id=? and entry_type='CHECKLIST_ENTRY'",
                UUID.class, itemId);
        assertThat(jdbcTemplate.update(
                "update care_item_templates set content_status='ARCHIVED', updated_at=now() "
                        + "where template_id=? and entry_type='TEMPLATE_ROOT'",
                templateId)).isOne();
        entityManager.clear();

        assertThat(checklistService.listItems(motherId, journeyId, null))
                .singleElement().satisfies(snapshot -> {
                    assertThat(snapshot.templateItemId()).isEqualTo(itemId);
                    assertThat(snapshot.itemText()).isEqualTo("Snapshot text");
                    assertThat(snapshot.itemOrder()).isEqualTo(1);
                });
        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(journeyId, null, List.of(itemId)), motherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> assertThat(((BusinessException) error).getCode())
                        .isEqualTo("CHECKLIST-007"));

        assertThat(userChecklistItemRepository.count()).isOne();
        assertThat(checklistAuditCount()).isOne();
    }

    @Test
    void uc82_69_sec_003_foreignJourneyAndBabyDenyWithZeroRowsAndAudits() {
        UUID secondMotherId = UUID.randomUUID();
        CanonicalUserFixture.insertUser(
                jdbcTemplate, secondMotherId, "Story 69 Second Mother", uniquePhone(), "MOTHER");
        seedJourney(secondMotherId, LocalDate.of(2026, 1, 2));
        UUID pregnancyItem = seedItem(
                ChecklistTemplateStatus.APPROVED, ContentStage.PREGNANCY, "Foreign journey item");
        UUID babyId = babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(motherId)
                .nickname("Synthetic foreign baby")
                .birthDate(LocalDate.of(2026, 1, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build()).getId();
        UUID babyItem = seedItem(
                ChecklistTemplateStatus.APPROVED, ContentStage.BABY_CARE, "Foreign baby item");

        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(journeyId, null, List.of(pregnancyItem)),
                        secondMotherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    assertThat(((BusinessException) error).getCode()).isEqualTo("CHECKLIST-007");
                    assertThat(error.getMessage()).isEqualTo("Template item not found or unavailable");
                });
        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(null, babyId, List.of(babyItem)), secondMotherId))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    assertThat(((BusinessException) error).getCode()).isEqualTo("CHECKLIST-007");
                    assertThat(error.getMessage()).isEqualTo("Template item not found or unavailable");
                });

        entityManager.clear();
        assertThat(userChecklistItemRepository.count()).isZero();
        assertThat(checklistAuditCount(secondMotherId)).isZero();
    }

    @Test
    void uc82_69_tc_010_trulyMissingItemDeniesWithZeroPostgresRowsAndAudits() {
        UUID missingItemId = UUID.fromString("69000000-0000-0000-0000-000000000909");

        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(
                                journeyId, null, List.of(missingItemId)), motherId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("CHECKLIST-007");
                    assertThat(exception.getMessage())
                            .isEqualTo("Template item not found or unavailable");
                });

        entityManager.clear();
        assertThat(userChecklistItemRepository.count()).isZero();
        assertThat(checklistAuditCount()).isZero();
    }

    @Test
    void r69_017_explicitJourneyWithoutCanonicalContextDeniesNeutrallyWithZeroRowsAndAudits() {
        UUID approvedItemId = seedItem(
                ChecklistTemplateStatus.APPROVED, ContentStage.PREGNANCY,
                "Explicit journey without canonical context");
        MotherJourney formerCanonical = journeyRepository.findById(journeyId).orElseThrow();
        formerCanonical.setStatus(JourneyStatus.COMPLETED);
        journeyRepository.saveAndFlush(formerCanonical);
        entityManager.clear();

        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(
                                journeyId, null, List.of(approvedItemId)), motherId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("CHECKLIST-007");
                    assertThat(exception.getMessage())
                            .isEqualTo("Template item not found or unavailable");
                });
        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(
                                null, null, List.of(approvedItemId)), motherId))
                .isInstanceOfSatisfying(
                        com.carebridge.backend.content.exception.ContentException.class,
                        exception -> {
                            assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                            assertThat(exception.getCode()).isEqualTo("CNT-013");
                        });

        entityManager.clear();
        assertThat(userChecklistItemRepository.count()).isZero();
        assertThat(checklistAuditCount()).isZero();
    }

    @Test
    void uc82_69_tc_013_canonicalStatusAcceptsOwnedActiveBabyAndDeniesArchivedBaby() {
        UUID archivedBabyId = babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(motherId)
                .nickname("Story 69 archived baby")
                .birthDate(LocalDate.of(2025, 1, 1))
                .status(BabyProfileStatus.ARCHIVED)
                .active(true)
                .build()).getId();
        UUID canonicalActiveBabyId = babyProfileRepository.saveAndFlush(BabyProfile.builder()
                .ownerUserId(motherId)
                .nickname("Story 69 canonical active baby")
                .birthDate(LocalDate.of(2025, 2, 1))
                .status(BabyProfileStatus.ACTIVE)
                .active(false)
                .build()).getId();
        UUID babyCareItemId = seedItem(
                ChecklistTemplateStatus.APPROVED, ContentStage.BABY_CARE,
                "Approved baby care item");

        assertThatThrownBy(() -> checklistService.importFromTemplate(
                        new ImportFromTemplateRequest(
                                null, archivedBabyId, List.of(babyCareItemId)), motherId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("CHECKLIST-007"));

        setActiveBaby(canonicalActiveBabyId);
        entityManager.clear();
        BabyProfile reloadedActiveBaby = babyProfileRepository.findById(canonicalActiveBabyId)
                .orElseThrow();
        assertThat(reloadedActiveBaby.getStatus()).isEqualTo(BabyProfileStatus.ACTIVE);
        assertThat(reloadedActiveBaby.getActive()).isTrue();
        var imported = checklistService.importFromTemplate(
                new ImportFromTemplateRequest(
                        null, canonicalActiveBabyId, List.of(babyCareItemId)), motherId);
        assertThat(imported).singleElement().satisfies(row -> {
            assertThat(row.babyId()).isEqualTo(canonicalActiveBabyId);
            assertThat(row.journeyId()).isNull();
            assertThat(row.templateItemId()).isEqualTo(babyCareItemId);
        });

        entityManager.clear();
        assertThat(userChecklistItemRepository.count()).isOne();
        assertThat(checklistAuditCount()).isOne();
    }

    private UUID seedItem(ChecklistTemplateStatus status, ContentStage stage, String text) {
        ChecklistTemplate template = templateRepository.saveAndFlush(ChecklistTemplate.builder()
                .name(text + " template")
                .description("Story 6.9 integration fixture")
                .stage(stage)
                .status(status)
                .versionNo(1)
                .build());
        return templateItemRepository.saveAndFlush(ChecklistItem.builder()
                .template(template)
                .itemText(text)
                .order(1)
                .isRequired(true)
                .build()).getId();
    }

    private UUID seedJourney(UUID ownerId, LocalDate startDate) {
        UUID careSubjectId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into care_subjects (
                    care_subject_id, person_id, owner_user_id, subject_type,
                    nickname, status, created_at, updated_at)
                select ?, u.person_id, u.user_id, 'MOTHER', u.display_name,
                       'ACTIVE', now(), now()
                  from users u
                 where u.user_id = ?
                """, careSubjectId, ownerId);
        MotherJourney journey = journeyRepository.saveAndFlush(MotherJourney.builder()
                .ownerUserId(ownerId)
                .careSubjectId(careSubjectId)
                .journeyType(JourneyType.PREGNANCY)
                .status(JourneyStatus.ACTIVE)
                .startDate(startDate)
                .build());
        jdbcTemplate.update(
                "update care_subjects set mother_journey_id=? where care_subject_id=?",
                journey.getId(), careSubjectId);
        return journey.getId();
    }

    private long checklistAuditCount() {
        return checklistAuditCount(motherId);
    }

    private long checklistAuditCount(UUID actorId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from audit_events where actor_user_id=? "
                        + "and event_category='CHECKLIST_ITEM_ADDED' and event_origin='AUDIT_LOG'",
                Long.class, actorId);
        return count == null ? 0L : count;
    }

    private String uniquePhone() {
        return "09" + String.format("%08d", Math.floorMod(System.nanoTime(), 100_000_000L));
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
                        + "care_subjects, mother_journeys, "
                        + "audit_events, users cascade");
    }
}
