package com.carebridge.backend.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.baby.entity.BabyProfile;
import com.carebridge.backend.baby.entity.BabyProfileStatus;
import com.carebridge.backend.checklist.dto.ImportFromTemplateRequest;
import com.carebridge.backend.checklist.entity.UserChecklistItem;
import com.carebridge.backend.checklist.repository.UserChecklistItemRepository;
import com.carebridge.backend.checklist.service.impl.UserChecklistItemServiceImpl;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.policy.LifecycleContentStageResolver;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.policy.ResolvedLifecycleContext;
import com.carebridge.backend.baby.repository.BabyProfileRepository;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.content.support.Story69TestFactory;
import com.carebridge.backend.common.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** RED contracts for UC82-69-TC-009..013/015/023 and SEC-003. */
@ExtendWith(MockitoExtension.class)
class ChecklistImportBoundaryTest {

    @Mock private UserChecklistItemRepository checklistRepository;
    @Mock private ChecklistItemRepository templateItemRepository;
    @Mock private AuditService auditService;
    @Mock private LifecycleContentStageResolver lifecycleContentStageResolver;
    @Mock private BabyProfileRepository babyProfileRepository;
    @InjectMocks private UserChecklistItemServiceImpl service;

    @Test
    void uc82_69_tc_009_templateIdsHaveRequiredOneToFiftyBoundaryAndNonNullElements() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        List<UUID> fifty = new ArrayList<>();
        for (int index = 0; index < 50; index++) fifty.add(UUID.randomUUID());
        List<UUID> fiftyOne = new ArrayList<>(fifty);
        fiftyOne.add(UUID.randomUUID());

        assertThat(validator.validate(new ImportFromTemplateRequest(null, null, null))).isNotEmpty();
        assertThat(validator.validate(new ImportFromTemplateRequest(null, null, List.of()))).isNotEmpty();
        assertThat(validator.validate(new ImportFromTemplateRequest(null, null, fiftyOne))).isNotEmpty();
        assertThat(validator.validate(new ImportFromTemplateRequest(
                null, null, Arrays.asList((UUID) null)))).isNotEmpty();
        assertThat(validator.validate(new ImportFromTemplateRequest(
                null, null, List.of(UUID.randomUUID())))).isEmpty();
        assertThat(validator.validate(new ImportFromTemplateRequest(null, null, fifty))).isEmpty();
    }

    @Test
    void uc82_69_tc_009_bothContextIdsFailBeforeAnyContextLookupOrWrite() {
        UUID userId = UUID.randomUUID();
        var request = new ImportFromTemplateRequest(
                UUID.randomUUID(), UUID.randomUUID(), List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> service.importFromTemplate(request, userId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(400);
                    assertThat(exception.getCode()).isEqualTo("CHECKLIST-001");
                    assertThat(exception.getMessage()).isEqualTo("Invalid checklist import request");
                });

        verifyNoInteractions(lifecycleContentStageResolver, babyProfileRepository,
                templateItemRepository, checklistRepository, auditService);
    }

    @Test
    void uc82_69_tc_010_missingTemplateItemFailsNeutrallyBeforeWriteOrAudit() {
        UUID userId = UUID.randomUUID();
        UUID journeyId = UUID.randomUUID();
        UUID missingItemId = UUID.randomUUID();
        when(lifecycleContentStageResolver.resolveForUpdate(userId))
                .thenReturn(new ResolvedLifecycleContext(journeyId, ContentStage.PREGNANCY));
        when(templateItemRepository.findAllAvailableByIdInForUpdate(
                List.of(missingItemId), ChecklistTemplateStatus.APPROVED,
                ContentStage.PREGNANCY)).thenReturn(List.of());

        assertThatThrownBy(() -> service.importFromTemplate(
                new ImportFromTemplateRequest(journeyId, null, List.of(missingItemId)), userId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("CHECKLIST-007");
                    assertThat(exception.getMessage())
                            .isEqualTo("Template item not found or unavailable");
                });

        verifyNoInteractions(checklistRepository, auditService, babyProfileRepository);
    }

    @Test
    void uc82_69_tc_013_foreignArchivedAndInactiveBabyPartitionsFailBeforeItemLookup() {
        UUID userId = UUID.randomUUID();
        for (UUID unavailableBabyId : List.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())) {
            when(babyProfileRepository.findOwnedActiveByIdForUpdate(unavailableBabyId, userId))
                    .thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.importFromTemplate(
                    new ImportFromTemplateRequest(
                            null, unavailableBabyId, List.of(UUID.randomUUID())), userId))
                    .isInstanceOfSatisfying(BusinessException.class,
                            exception -> assertThat(exception.getCode()).isEqualTo("CHECKLIST-007"));
        }

        verifyNoInteractions(lifecycleContentStageResolver, templateItemRepository,
                checklistRepository, auditService);
    }

    @Test
    void uc82_69_tc_013_ownedActiveBabyImportsOnlyApprovedBabyCareItem() {
        UUID userId = UUID.randomUUID();
        UUID babyId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        BabyProfile baby = BabyProfile.builder()
                .id(babyId)
                .ownerUserId(userId)
                .nickname("Story 69 active baby")
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build();
        ChecklistTemplate approved = ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .stage(ContentStage.POSTPARTUM)
                .status(ChecklistTemplateStatus.APPROVED)
                .build();
        ChecklistItem item = ChecklistItem.builder()
                .id(itemId)
                .template(approved)
                .itemText("Baby care database text")
                .order(6)
                .build();
        when(babyProfileRepository.findOwnedActiveByIdForUpdate(babyId, userId))
                .thenReturn(Optional.of(baby));
        when(templateItemRepository.findAllAvailableByIdInForUpdate(
                List.of(itemId), ChecklistTemplateStatus.APPROVED, ContentStage.POSTPARTUM))
                .thenReturn(List.of(item));
        UUID persistedId = UUID.randomUUID();
        when(checklistRepository.insertBabyImportedIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(babyId),
                org.mockito.ArgumentMatchers.eq(itemId),
                org.mockito.ArgumentMatchers.eq("Baby care database text"),
                org.mockito.ArgumentMatchers.eq(6)))
                .thenReturn(1);
        when(checklistRepository.findBabyImportedByExactScope(userId, babyId, itemId))
                .thenReturn(Optional.of(UserChecklistItem.builder()
                        .id(persistedId)
                        .ownerUserId(userId)
                        .babyId(babyId)
                        .templateItemId(itemId)
                        .itemText("Baby care database text")
                        .itemOrder(6)
                        .build()));

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(null, babyId, List.of(itemId)), userId);

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.babyId()).isEqualTo(babyId);
            assertThat(row.journeyId()).isNull();
            assertThat(row.templateItemId()).isEqualTo(itemId);
            assertThat(row.itemText()).isEqualTo("Baby care database text");
            assertThat(row.itemOrder()).isEqualTo(6);
        });
        verify(lifecycleContentStageResolver, never()).resolveForUpdate(userId);
        verify(auditService).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.CHECKLIST_ITEM_ADDED),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq("UserChecklistItem"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("imported"));
    }

    @Test
    void babyImportReusesCanonicalBabyOnlySnapshot() {
        UUID userId = UUID.randomUUID();
        UUID babyId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        BabyProfile baby = BabyProfile.builder()
                .id(babyId)
                .ownerUserId(userId)
                .nickname("Legacy scoped baby")
                .status(BabyProfileStatus.ACTIVE)
                .active(true)
                .build();
        ChecklistItem item = ChecklistItem.builder()
                .id(itemId)
                .template(ChecklistTemplate.builder()
                        .id(UUID.randomUUID())
                        .stage(ContentStage.POSTPARTUM)
                        .status(ChecklistTemplateStatus.APPROVED)
                        .build())
                .itemText("Legacy database text")
                .order(4)
                .build();
        UserChecklistItem legacySnapshot = UserChecklistItem.builder()
                .id(UUID.randomUUID())
                .ownerUserId(userId)
                .babyId(babyId)
                .templateItemId(itemId)
                .itemText("Legacy database text")
                .itemOrder(4)
                .build();

        when(babyProfileRepository.findOwnedActiveByIdForUpdate(babyId, userId))
                .thenReturn(Optional.of(baby));
        when(templateItemRepository.findAllAvailableByIdInForUpdate(
                List.of(itemId), ChecklistTemplateStatus.APPROVED, ContentStage.POSTPARTUM))
                .thenReturn(List.of(item));
        when(checklistRepository.insertBabyImportedIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(babyId),
                org.mockito.ArgumentMatchers.eq(itemId),
                org.mockito.ArgumentMatchers.eq("Legacy database text"),
                org.mockito.ArgumentMatchers.eq(4)))
                .thenReturn(0);
        when(checklistRepository.findBabyImportedByExactScope(userId, babyId, itemId))
                .thenReturn(Optional.of(legacySnapshot));

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(null, babyId, List.of(itemId)), userId);

        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.itemId()).isEqualTo(legacySnapshot.getId());
            assertThat(row.journeyId()).isNull();
            assertThat(row.babyId()).isEqualTo(babyId);
        });
        assertThat(legacySnapshot.getJourneyId()).isNull();
        verifyNoInteractions(auditService);
    }

    @Test
    void uc82_69_tc_023_missingCanonicalContextStopsBeforeItemLookupOrWrite() {
        UUID userId = UUID.randomUUID();
        when(lifecycleContentStageResolver.resolveForUpdate(userId))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        assertThatThrownBy(() -> service.importFromTemplate(
                new ImportFromTemplateRequest(null, null, List.of(UUID.randomUUID())), userId))
                .isInstanceOfSatisfying(ContentException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("CNT-013");
                    assertThat(exception.getHttpStatus().value()).isEqualTo(409);
                });

        verifyNoInteractions(templateItemRepository, checklistRepository, auditService, babyProfileRepository);
    }

    @Test
    void r69_017_explicitJourneyWithoutCanonicalContextUsesNeutralChecklist007() {
        UUID userId = UUID.randomUUID();
        UUID assertedJourneyId = UUID.randomUUID();
        when(lifecycleContentStageResolver.resolveForUpdate(userId))
                .thenThrow(ContentException.lifecycleContextUnavailable());

        assertThatThrownBy(() -> service.importFromTemplate(
                new ImportFromTemplateRequest(
                        assertedJourneyId, null, List.of(UUID.randomUUID())), userId))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getHttpStatus().value()).isEqualTo(404);
                    assertThat(exception.getCode()).isEqualTo("CHECKLIST-007");
                    assertThat(exception.getMessage())
                            .isEqualTo("Template item not found or unavailable");
                });

        verifyNoInteractions(templateItemRepository, checklistRepository, auditService, babyProfileRepository);
    }

    @Test
    void uc82_69_tc_010_012_013_importPrevalidatesContextAndCompleteItemSetBeforeSave() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/checklist/service/impl/UserChecklistItemServiceImpl.java");
        source = source.substring(source.indexOf("importFromTemplate"), source.indexOf("listItems"));

        assertThat(source.contains("resolveForUpdate") && source.contains("findOwnedActiveByIdForUpdate"))
                .as("TC-012/013: import locks canonical journey or owned active baby first")
                .isTrue();
        assertThat(source.contains("findAllAvailableByIdInForUpdate")
                        && source.contains("ChecklistTemplateStatus.APPROVED")
                        && source.contains("CHECKLIST-007"))
                .as("TC-010: import resolves the complete approved/stage-compatible set")
                .isTrue();
        assertThat(source.contains("templateItemRepository.findById(templateId)"))
                .as("TC-010: direct item lookup cannot bypass parent status/stage")
                .isFalse();

        int validation = source.indexOf("findAllAvailableByIdInForUpdate");
        int firstSave = source.indexOf("checklistRepository.insertBabyImportedIfAbsent");
        assertThat(validation)
                .as("TC-010/INT-001: complete validation must precede the first write")
                .isGreaterThanOrEqualTo(0);
        assertThat(firstSave).isGreaterThan(validation);
        String contentException = Story69TestFactory.productionSource(
                "com/carebridge/backend/content/exception/ContentException.java");
        assertThat(source.indexOf("resolveForUpdate"))
                .as("TC-023: no-context import delegates to the fail-closed resolver before item lookup")
                .isGreaterThanOrEqualTo(0)
                .isLessThan(validation);
        assertThat(contentException.contains("CNT-013")
                        && contentException.contains("Lifecycle content context unavailable"))
                .isTrue();
    }

    @Test
    void uc82_69_tc_011_importDeduplicatesBeforePersistenceAndKeepsSuccessAuditTransactional() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/checklist/service/impl/UserChecklistItemServiceImpl.java");

        assertThat(source.contains("LinkedHashSet") || source.contains("distinctTemplateItemIds"))
                .as("TC-011: duplicate IDs use one deterministic first-occurrence snapshot")
                .isTrue();
        assertThat(source.contains("CHECKLIST_ITEM_ADDED"))
                .as("TC-011: one success audit is emitted per saved distinct row")
                .isTrue();
        assertThat(source.contains("request.templateItemIds().stream()\n                .map"))
                .as("TC-011/INT-002: audit happens only after a validated save")
                .isFalse();
    }

    @Test
    void uc82_69_tc_011_importsBThenAOnceUsingDatabaseTextOrderAndTwoSuccessAudits() {
        UUID userId = UUID.fromString("69000000-0000-0000-0000-000000000101");
        UUID journeyId = UUID.fromString("69000000-0000-0000-0000-000000000102");
        UUID itemAId = UUID.fromString("69000000-0000-0000-0000-000000000103");
        UUID itemBId = UUID.fromString("69000000-0000-0000-0000-000000000104");
        ChecklistTemplate approved = ChecklistTemplate.builder()
                .id(UUID.randomUUID())
                .stage(ContentStage.PREGNANCY)
                .status(ChecklistTemplateStatus.APPROVED)
                .build();
        ChecklistItem itemA = ChecklistItem.builder().id(itemAId).template(approved)
                .itemText("Database A").order(17).build();
        ChecklistItem itemB = ChecklistItem.builder().id(itemBId).template(approved)
                .itemText("Database B").order(4).build();
        when(lifecycleContentStageResolver.resolveForUpdate(userId))
                .thenReturn(new ResolvedLifecycleContext(journeyId, ContentStage.PREGNANCY));
        when(templateItemRepository.findAllAvailableByIdInForUpdate(
                List.of(itemAId, itemBId), ChecklistTemplateStatus.APPROVED,
                ContentStage.PREGNANCY)).thenReturn(List.of(itemA, itemB));
        UUID savedBId = UUID.fromString("69000000-0000-0000-0000-000000000105");
        UUID savedAId = UUID.fromString("69000000-0000-0000-0000-000000000106");
        when(checklistRepository.insertJourneyImportedIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(journeyId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(1);
        when(checklistRepository.findJourneyImportedByExactScope(userId, journeyId, itemBId))
                .thenReturn(Optional.of(UserChecklistItem.builder()
                        .id(savedBId).ownerUserId(userId).journeyId(journeyId)
                        .templateItemId(itemBId).itemText("Database B").itemOrder(4).build()));
        when(checklistRepository.findJourneyImportedByExactScope(userId, journeyId, itemAId))
                .thenReturn(Optional.of(UserChecklistItem.builder()
                        .id(savedAId).ownerUserId(userId).journeyId(journeyId)
                        .templateItemId(itemAId).itemText("Database A").itemOrder(17).build()));

        var result = service.importFromTemplate(
                new ImportFromTemplateRequest(null, null, List.of(itemBId, itemAId, itemBId)), userId);

        var persistenceOrder = org.mockito.Mockito.inOrder(checklistRepository);
        persistenceOrder.verify(checklistRepository).insertJourneyImportedIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(journeyId),
                org.mockito.ArgumentMatchers.eq(itemBId),
                org.mockito.ArgumentMatchers.eq("Database B"),
                org.mockito.ArgumentMatchers.eq(4));
        persistenceOrder.verify(checklistRepository).insertJourneyImportedIfAbsent(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq(journeyId),
                org.mockito.ArgumentMatchers.eq(itemAId),
                org.mockito.ArgumentMatchers.eq("Database A"),
                org.mockito.ArgumentMatchers.eq(17));
        assertThat(result).extracting(response -> response.templateItemId())
                .containsExactly(itemBId, itemAId);
        verify(checklistRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        verify(auditService, times(2)).log(
                org.mockito.ArgumentMatchers.eq(AuditAction.CHECKLIST_ITEM_ADDED),
                org.mockito.ArgumentMatchers.eq(userId),
                org.mockito.ArgumentMatchers.eq("UserChecklistItem"),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("imported"));
    }

    @Test
    void uc82_69_tc_015_existingPersonalSnapshotsAreNotRevokedByImportPolicy() {
        String source = Story69TestFactory.productionSource(
                "com/carebridge/backend/checklist/service/impl/UserChecklistItemServiceImpl.java");

        assertThat(source.contains("deleteByTemplateItemId")
                        || source.contains("updateImportedSnapshotStatus"))
                .as("TC-015: source status changes must not delete/update prior snapshots")
                .isFalse();
    }
}
