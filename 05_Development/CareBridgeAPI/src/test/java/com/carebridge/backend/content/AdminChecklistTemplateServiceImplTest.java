package com.carebridge.backend.content;

import static com.carebridge.backend.content.ChecklistTemplateTestFactory.ADMIN_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.TEMPLATE_ID;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeItem;
import static com.carebridge.backend.content.ChecklistTemplateTestFactory.makeTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.checklist.model.ChecklistAnchorType;
import com.carebridge.backend.checklist.model.ChecklistRangeUnit;
import com.carebridge.backend.checklist.model.ChecklistRecipientRole;
import com.carebridge.backend.checklist.model.ChecklistCareContextType;
import com.carebridge.backend.checklist.model.ChecklistMaterializationPolicy;
import com.carebridge.backend.checklist.model.ChecklistScheduleEndMode;
import com.carebridge.backend.checklist.model.ChecklistScheduleType;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.checklist.model.ChecklistWeekBoundaryRule;
import com.carebridge.backend.content.dto.request.ChecklistItemRequest;
import com.carebridge.backend.content.dto.request.ChecklistSubstageRequest;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.HideChecklistTemplateRequest;
import com.carebridge.backend.content.dto.request.UpdateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.HideChecklistTemplateResponse;
import com.carebridge.backend.content.entity.ChecklistItem;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;
import com.carebridge.backend.content.entity.ChecklistTemplateType;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.mapper.ContentMapper;
import com.carebridge.backend.content.repository.ChecklistItemRepository;
import com.carebridge.backend.content.repository.ChecklistTemplateRepository;
import com.carebridge.backend.content.service.AdminChecklistTemplateServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AdminChecklistTemplateServiceImplTest {

        @Mock
        private ChecklistTemplateRepository checklistTemplateRepository;

        @Mock
        private ChecklistItemRepository checklistItemRepository;

        @Spy
        private ContentMapper contentMapper = new ContentMapper();

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @Mock
        private AuditService auditService;

        @InjectMocks
        private AdminChecklistTemplateServiceImpl service;

        private CreateChecklistTemplateRequest makeCreateRequest(List<ChecklistItemRequest> items) {
                return new CreateChecklistTemplateRequest("Checklist khám thai tháng 3", "Mô tả",
                                Set.of(ChecklistRecipientRole.MOTHER), ContentStage.PREGNANCY, pregnancyEligibility(),
                                items);
        }

        private UpdateChecklistTemplateRequest makeUpdateRequest(
                        String name, String description, ChecklistTemplateStatus status,
                        List<ChecklistItemRequest> items) {
                return new UpdateChecklistTemplateRequest(name, description, Set.of(ChecklistRecipientRole.MOTHER),
                                ContentStage.PREGNANCY, pregnancyEligibility(), status, items);
        }

        private ChecklistSubstageRequest pregnancyEligibility() {
                return new ChecklistSubstageRequest(
                                "PREGNANCY_WEEKS_0_12", ChecklistAnchorType.LMP, 0, 12, ChecklistRangeUnit.WEEK);
        }

        // CHKTPL-TC-001
        @Test
        void create_validWithItems_returnsDraftWithTwoItems() {
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> {
                                        ChecklistTemplate t = inv.getArgument(0);
                                        t.setId(TEMPLATE_ID);
                                        return t;
                                });
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

                CreateChecklistTemplateRequest request = makeCreateRequest(List.of(
                                new ChecklistItemRequest("Siêu âm đo độ mờ da gáy", 1, true),
                                new ChecklistItemRequest("Xét nghiệm Double test", 2, true)));

                AdminChecklistTemplateDetailResponse response = service.create(request, ADMIN_ID);

                assertEquals(ChecklistTemplateStatus.DRAFT, response.getStatus());
                assertEquals(2, response.getItems().size());
                verify(checklistItemRepository).saveAll(anyList());
                verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_CREATED), eq(ADMIN_ID),
                                eq("ChecklistTemplate"), any(), any());
        }

        @Test
        void create_withItemSourceUrl_persistsAndReturnsSourceUrl() {
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> {
                                        ChecklistTemplate t = inv.getArgument(0);
                                        t.setId(TEMPLATE_ID);
                                        return t;
                                });
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

                String sourceUrl = "https://www.who.int/health-topics/maternal-health";
                CreateChecklistTemplateRequest request = makeCreateRequest(List.of(
                                new ChecklistItemRequest(
                                                null,
                                                "Đọc hướng dẫn chăm sóc",
                                                1,
                                                true,
                                                ChecklistTargetSubject.MOTHER,
                                                null,
                                                null,
                                                false,
                                                false,
                                                sourceUrl)));

                AdminChecklistTemplateDetailResponse response = service.create(request, ADMIN_ID);

                ArgumentCaptor<List<ChecklistItem>> captor = ArgumentCaptor.forClass(List.class);
                verify(checklistItemRepository).saveAll(captor.capture());
                assertTrue(captor.getValue().getFirst().getConfigurationJson().contains("\"sourceUrl\""));
                assertEquals(sourceUrl, response.getItems().getFirst().getSourceUrl());
                assertEquals(Boolean.FALSE, response.getItems().getFirst().getRepeatWeekly());
                assertEquals(Boolean.FALSE, response.getItems().getFirst().getRepeatDaily());
        }

        @Test
        void create_withInvalidItemSourceUrl_rejectsBeforePersistence() {
                for (String invalidSourceUrl : List.of(
                                "ftp://example.org/document",
                                "https://?",
                                "https://user:password@example.org/document")) {
                        CreateChecklistTemplateRequest request = makeCreateRequest(List.of(
                                        new ChecklistItemRequest(
                                                        null,
                                                        "Đọc tài liệu",
                                                        1,
                                                        true,
                                                        ChecklistTargetSubject.MOTHER,
                                                        null,
                                                        null,
                                                        false,
                                                        false,
                                                        invalidSourceUrl)));

                        ContentException error = assertThrows(ContentException.class,
                                        () -> service.create(request, ADMIN_ID));
                        assertEquals("CNT-001", error.getCode());
                }
                verify(checklistTemplateRepository, never()).save(any(ChecklistTemplate.class));
        }

        // CHKTPL-TC-002
        @Test
        void create_emptyItems_returnsDraftWithNoItems() {
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> {
                                        ChecklistTemplate t = inv.getArgument(0);
                                        t.setId(TEMPLATE_ID);
                                        return t;
                                });

                AdminChecklistTemplateDetailResponse response = service.create(makeCreateRequest(List.of()), ADMIN_ID);

                assertEquals(ChecklistTemplateStatus.DRAFT, response.getStatus());
                assertTrue(response.getItems().isEmpty());
                verify(checklistItemRepository, never()).saveAll(anyList());
        }

        // CHKTPL-TC-002b — null items behaves the same as empty (§11.2)
        @Test
        void create_nullItems_returnsDraftWithNoItems() {
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> {
                                        ChecklistTemplate t = inv.getArgument(0);
                                        t.setId(TEMPLATE_ID);
                                        return t;
                                });

                AdminChecklistTemplateDetailResponse response = service.create(makeCreateRequest(null), ADMIN_ID);

                assertTrue(response.getItems().isEmpty());
        }

        // CHKTPL-TC-004 — regression guard for the immutable-list bug (Logic Issue L3)
        @Test
        void update_reconcilesExistingIdsAndSoftDeactivatesOmittedItems() {
                ChecklistTemplate template = makeTemplate();
                ChecklistItem old1 = makeItem(template, 1);
                old1.setIsActive(false);
                old1.setConfigurationJson("{\"sourceUrl\":\"https://example.org/original\"}");
                ChecklistItem old2 = makeItem(template, 2);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                                .thenReturn(List.of(old1, old2));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

                UpdateChecklistTemplateRequest request = makeUpdateRequest(
                                "Tên mới", "Mô tả mới", ChecklistTemplateStatus.DRAFT,
                                List.of(new ChecklistItemRequest(old1.getId(), "Mục 1", 1, true,
                                                ChecklistTargetSubject.MOTHER, null, null, false, false,
                                                "https://example.org/updated"),
                                                new ChecklistItemRequest("Mục 2", 2, false),
                                                new ChecklistItemRequest("Mục 3", 3, true)));

                AdminChecklistTemplateDetailResponse response = service.update(TEMPLATE_ID, request, ADMIN_ID);

                verify(checklistItemRepository, never()).deleteAll(anyList());
                ArgumentCaptor<List<ChecklistItem>> captor = ArgumentCaptor.forClass(List.class);
                verify(checklistItemRepository).saveAll(captor.capture());
                assertEquals(4, captor.getValue().size());
                assertEquals(old1.getId(), response.getItems().getFirst().getId());
                assertEquals("https://example.org/updated", response.getItems().getFirst().getSourceUrl());
                assertTrue(old1.getIsActive());
                assertEquals(false, old2.getIsActive());
                assertEquals(3, response.getItems().size());
                verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_UPDATED), eq(ADMIN_ID),
                                eq("ChecklistTemplate"), any(), any());
        }

        @Test
        void update_omittedItemSourceUrl_clearsExistingMetadata() {
                ChecklistTemplate template = makeTemplate();
                ChecklistItem existing = makeItem(template, 1);
                existing.setConfigurationJson("{\"sourceUrl\":\"https://example.org/original\"}");
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                                .thenReturn(List.of(existing));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

                UpdateChecklistTemplateRequest request = makeUpdateRequest(
                                template.getName(), template.getDescription(), ChecklistTemplateStatus.DRAFT,
                                List.of(new ChecklistItemRequest(existing.getId(), "Mục 1", 1, true)));

                AdminChecklistTemplateDetailResponse response = service.update(TEMPLATE_ID, request, ADMIN_ID);

                assertEquals("{}", existing.getConfigurationJson());
                assertEquals(null, response.getItems().getFirst().getSourceUrl());
        }

        @Test
        void update_duplicateExistingItemId_rejectsRequest() {
                ChecklistTemplate template = makeTemplate();
                ChecklistItem existing = makeItem(template, 1);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                                .thenReturn(List.of(existing));
                UpdateChecklistTemplateRequest request = makeUpdateRequest(
                                "Updated", "Updated", ChecklistTemplateStatus.DRAFT,
                                List.of(
                                                new ChecklistItemRequest(existing.getId(), "First", 1, true),
                                                new ChecklistItemRequest(existing.getId(), "Duplicate", 2, true)));

                ContentException error = assertThrows(
                                ContentException.class, () -> service.update(TEMPLATE_ID, request, ADMIN_ID));

                assertEquals("CHKTPL-009", error.getCode());
                verify(checklistItemRepository, never()).saveAll(anyList());
        }

        @Test
        void update_itemIdFromAnotherTemplate_rejectsRequest() {
                ChecklistTemplate template = makeTemplate();
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                                .thenReturn(List.of());
                UpdateChecklistTemplateRequest request = makeUpdateRequest(
                                "Updated", "Updated", ChecklistTemplateStatus.DRAFT,
                                List.of(new ChecklistItemRequest(UUID.randomUUID(), "Foreign", 1, true)));

                ContentException error = assertThrows(
                                ContentException.class, () -> service.update(TEMPLATE_ID, request, ADMIN_ID));

                assertEquals("CHKTPL-009", error.getCode());
                verify(checklistItemRepository, never()).saveAll(anyList());
        }

        // CHKTPL-TC-005
        @Test
        void update_nullItems_leavesExistingItemsUntouched() {
                ChecklistTemplate template = makeTemplate();
                ChecklistItem existing = makeItem(template, 1);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID)).thenReturn(List.of(existing));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                UpdateChecklistTemplateRequest request = makeUpdateRequest(
                                "Tên mới", "Mô tả mới", ChecklistTemplateStatus.DRAFT, null);

                AdminChecklistTemplateDetailResponse response = service.update(TEMPLATE_ID, request, ADMIN_ID);

                verify(checklistItemRepository, never()).deleteAll(anyList());
                verify(checklistItemRepository, never()).saveAll(anyList());
                assertEquals(1, response.getItems().size());
        }

        // update: current or target status outside DRAFT/PENDING_REVIEW -> CHKTPL-004
        @Test
        void update_currentStatusApproved_throwsVersionImmutable() {
                ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.APPROVED));
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

                UpdateChecklistTemplateRequest request = makeUpdateRequest(
                                "Tên mới", "Mô tả", ChecklistTemplateStatus.DRAFT, null);

                ContentException ex = assertThrows(ContentException.class,
                                () -> service.update(TEMPLATE_ID, request, ADMIN_ID));
                assertEquals("VERSION_IMMUTABLE", ex.getCode());
                verify(checklistTemplateRepository, never()).save(any());
        }

        // CHKTPL-TC-006
        @Test
        void archive_validReason_transitionsToArchivedWithoutTouchingItems() {
                ChecklistTemplate template = makeTemplate();
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                HideChecklistTemplateResponse response = service.archive(
                                TEMPLATE_ID, new HideChecklistTemplateRequest("Nội dung lỗi thời"), ADMIN_ID);

                assertEquals(ChecklistTemplateStatus.ARCHIVED, response.newStatus());
                verify(checklistItemRepository, never()).deleteAll(anyList());
                verify(auditService).log(eq(AuditAction.CHECKLIST_TEMPLATE_ARCHIVED), eq(ADMIN_ID),
                                eq("ChecklistTemplate"), any(), any());
        }

        // CHKTPL-TC-007
        @Test
        void archive_blankReason_throwsChktpl005() {
                ChecklistTemplate template = makeTemplate();
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

                ContentException ex = assertThrows(ContentException.class, () -> service.archive(
                                TEMPLATE_ID, new HideChecklistTemplateRequest(""), ADMIN_ID));

                assertEquals("CHKTPL-005", ex.getCode());
                verify(checklistTemplateRepository, never()).save(any());
        }

        // CHKTPL-TC-008
        @Test
        void archive_alreadyArchived_throwsChktpl006() {
                ChecklistTemplate template = makeTemplate(t -> t.setStatus(ChecklistTemplateStatus.ARCHIVED));
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

                ContentException ex = assertThrows(ContentException.class, () -> service.archive(
                                TEMPLATE_ID, new HideChecklistTemplateRequest("Lý do"), ADMIN_ID));

                assertEquals("CHKTPL-006", ex.getCode());
                verify(checklistTemplateRepository, never()).save(any());
        }

        // CHKTPL-003 — not found guard (referenced by TDS §9, applies to
        // getById/update/archive)
        @Test
        void getById_notFound_throwsChktpl003() {
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());

                ContentException ex = assertThrows(ContentException.class, () -> service.getById(TEMPLATE_ID));
                assertEquals("CHKTPL-003", ex.getCode());
        }

        @Test
        void update_resubmitReturnedDraft_clearsLatestReviewFeedback() {
                ChecklistTemplate template = makeTemplate();
                template.setRevisionReason("Bổ sung mục bắt buộc");
                template.setRevisionRequestedAt(Instant.now());
                template.setRevisionRequestedBy(ADMIN_ID);
                template.setRevisionRequestedVersion(1);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID)).thenReturn(List.of());

                service.update(TEMPLATE_ID, makeUpdateRequest(
                                template.getName(), template.getDescription(), ChecklistTemplateStatus.PENDING_REVIEW,
                                null), ADMIN_ID);

                assertEquals(ChecklistTemplateStatus.PENDING_REVIEW, template.getStatus());
                assertEquals(null, template.getRevisionReason());
                assertEquals(null, template.getRevisionRequestedAt());
                assertEquals(null, template.getRevisionRequestedBy());
                assertEquals(null, template.getRevisionRequestedVersion());
        }

        @Test
        void update_saveReturnedDraft_retainsLatestReviewFeedback() {
                ChecklistTemplate template = makeTemplate();
                Instant requestedAt = Instant.now();
                template.setRevisionReason("Bổ sung mục bắt buộc");
                template.setRevisionRequestedAt(requestedAt);
                template.setRevisionRequestedBy(ADMIN_ID);
                template.setRevisionRequestedVersion(1);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID)).thenReturn(List.of());

                service.update(TEMPLATE_ID, makeUpdateRequest(
                                template.getName(), template.getDescription(), ChecklistTemplateStatus.DRAFT, null),
                                ADMIN_ID);

                assertEquals("Bổ sung mục bắt buộc", template.getRevisionReason());
                assertEquals(requestedAt, template.getRevisionRequestedAt());
                assertEquals(ADMIN_ID, template.getRevisionRequestedBy());
                assertEquals(1, template.getRevisionRequestedVersion());
        }

        @Test
        void create_v2StampsTargetlessLeafContract() {
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> {
                                        ChecklistTemplate t = inv.getArgument(0);
                                        t.setId(TEMPLATE_ID);
                                        return t;
                                });
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

                CreateChecklistTemplateRequest request = new CreateChecklistTemplateRequest(
                                "Recommendation", "Advisory", ChecklistTemplateType.MANDATORY, (short) 2,
                                Set.of(ChecklistRecipientRole.MOTHER), ContentStage.PREGNANCY,
                                pregnancyEligibility(),
                                List.of(new ChecklistItemRequest(null, "Uống đủ nước", 1, true, null, null, null)), 0);

                AdminChecklistTemplateDetailResponse response = service.create(request, ADMIN_ID);

                ArgumentCaptor<List<ChecklistItem>> captor = ArgumentCaptor.forClass(List.class);
                verify(checklistItemRepository).saveAll(captor.capture());
                ChecklistItem item = captor.getValue().getFirst();
                assertEquals((short) 2, item.getChecklistContractVersion());
                assertEquals(null, item.getTargetSubject());
                assertEquals(Boolean.TRUE, item.getIsRequired());
                assertEquals((short) 2, response.getChecklistContractVersion());
        }

        @Test
        void create_mixedMarkedAndUnmarkedItems_rejectsAmbiguousCadence() {
                CreateChecklistTemplateRequest request = new CreateChecklistTemplateRequest(
                                "Checklist khám thai tháng 3", "Mô tả", ChecklistTemplateType.MANDATORY, (short) 2,
                                Set.of(ChecklistRecipientRole.MOTHER), ContentStage.PREGNANCY, pregnancyEligibility(),
                                List.of(
                                                new ChecklistItemRequest(null, "Theo dõi huyết áp", 1, true, null, null,
                                                                null, true, false),
                                                new ChecklistItemRequest(null, "Khám thai lần đầu", 2, true, null, null,
                                                                null, false, false)),
                                0,
                                ChecklistScheduleType.WEEKLY, ChecklistMaterializationPolicy.EACH_WEEK, null, null,
                                null, null);

                ContentException error = assertThrows(ContentException.class, () -> service.create(request, ADMIN_ID));

                assertEquals("CNT-001", error.getCode());
                verify(checklistTemplateRepository, never()).save(any(ChecklistTemplate.class));
        }

        @Test
        void update_v1ToV2WithoutReplacementItems_rejectsContractMismatch() {
                ChecklistTemplate template = makeTemplate();
                ChecklistItem existing = makeItem(template, 1);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

                UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                                "Recommendation", "Advisory", ChecklistTemplateType.MANDATORY, (short) 2,
                                Set.of(ChecklistRecipientRole.MOTHER), ContentStage.PREGNANCY,
                                pregnancyEligibility(), ChecklistTemplateStatus.DRAFT, null, 0);

                ContentException error = assertThrows(ContentException.class,
                                () -> service.update(TEMPLATE_ID, request, ADMIN_ID));

                assertEquals("CNT-001", error.getCode());
                verify(checklistTemplateRepository, never()).save(any(ChecklistTemplate.class));
                verify(checklistItemRepository, never()).findByTemplate_IdOrderByOrder(TEMPLATE_ID);
        }

        @Test
        void update_v1ToV2WithReplacementItems_stampsEveryLeaf() {
                ChecklistTemplate template = makeTemplate();
                ChecklistItem existing = makeItem(template, 1);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));
                when(checklistItemRepository.findAllByTemplateIdOrderByOrder(TEMPLATE_ID))
                                .thenReturn(List.of(existing));
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

                UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                                "Recommendation", "Advisory", ChecklistTemplateType.MANDATORY, (short) 2,
                                Set.of(ChecklistRecipientRole.MOTHER), ContentStage.PREGNANCY,
                                pregnancyEligibility(), ChecklistTemplateStatus.DRAFT,
                                List.of(new ChecklistItemRequest(existing.getId(), "Uống đủ nước", 1, true, null, null,
                                                null)),
                                0);

                service.update(TEMPLATE_ID, request, ADMIN_ID);

                assertEquals((short) 2, existing.getChecklistContractVersion());
                assertEquals(null, existing.getTargetSubject());
                assertEquals(Boolean.TRUE, existing.getIsRequired());
        }

        @Test
        void cloneApprovedPregnancyV2_preservesCadenceAndProvenanceForLaterActivation() {
                ChecklistTemplate source = makeTemplate();
                source.setStatus(ChecklistTemplateStatus.APPROVED);
                source.setChecklistContractVersion((short) 2);
                source.setScheduleType(ChecklistScheduleType.WEEKLY);
                source.setMaterializationPolicy(ChecklistMaterializationPolicy.EACH_WEEK);
                source.setScheduleGroupKey("PREGNANCY_WHO");
                source.setScheduleContextType(ChecklistCareContextType.JOURNEY);
                source.setScheduleEndMode(ChecklistScheduleEndMode.STAGE_EXIT);
                source.setWeekBoundaryRule(ChecklistWeekBoundaryRule.ANCHOR_RELATIVE_7D);
                source.setChecklistMetadataJson(
                                "{\"schema\":\"CHECKLIST_METADATA_V1\",\"provenanceStatus\":\"SIGNED_OFF\"}");
                source.setChecklistMetadataHash("sha256:source");
                ChecklistItem sourceItem = makeItem(source, 1);
                sourceItem.setChecklistContractVersion((short) 2);
                sourceItem.setTargetSubject(null);
                sourceItem.setIsRequired(true);

                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(source));
                when(checklistTemplateRepository.findMaxVersionNoForLineage(any())).thenReturn(3);
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(invocation -> {
                                        ChecklistTemplate value = invocation.getArgument(0);
                                        value.setId(UUID.randomUUID());
                                        return value;
                                });
                when(checklistItemRepository.findByTemplate_IdOrderByOrder(TEMPLATE_ID))
                                .thenReturn(List.of(sourceItem));
                when(checklistItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

                AdminChecklistTemplateDetailResponse response = service.cloneVersion(TEMPLATE_ID, ADMIN_ID);

                ArgumentCaptor<ChecklistTemplate> captor = ArgumentCaptor.forClass(ChecklistTemplate.class);
                verify(checklistTemplateRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
                ChecklistTemplate clone = captor.getAllValues().getLast();
                assertEquals(ChecklistTemplateStatus.DRAFT, clone.getStatus());
                assertEquals((short) 2, clone.getChecklistContractVersion());
                assertEquals(ChecklistScheduleType.WEEKLY, clone.getScheduleType());
                assertEquals(ChecklistMaterializationPolicy.EACH_WEEK, clone.getMaterializationPolicy());
                assertEquals("PREGNANCY_WHO", clone.getScheduleGroupKey());
                assertEquals(ChecklistCareContextType.JOURNEY, clone.getScheduleContextType());
                assertEquals(ChecklistScheduleEndMode.STAGE_EXIT, clone.getScheduleEndMode());
                assertEquals(ChecklistWeekBoundaryRule.ANCHOR_RELATIVE_7D, clone.getWeekBoundaryRule());
                assertEquals(source.getChecklistMetadataJson(), clone.getChecklistMetadataJson());
                assertEquals(source.getChecklistMetadataHash(), clone.getChecklistMetadataHash());
                assertEquals((short) 2, response.getChecklistContractVersion());
                ArgumentCaptor<List<ChecklistItem>> itemCaptor = ArgumentCaptor.forClass(List.class);
                verify(checklistItemRepository).saveAll(itemCaptor.capture());
                assertEquals(Boolean.TRUE, itemCaptor.getValue().getFirst().getIsRequired());
        }

        @Test
        void list_trimsKeywordAndPassesItToAdminRepository() {
                when(checklistTemplateRepository.findAdminByOptionalStageAndStatus(
                                any(), any(), eq("thai"), any()))
                                .thenReturn(new PageImpl<>(List.of()));

                service.list(null, ContentStage.PREGNANCY, "  thai  ", PageRequest.of(0, 20));

                verify(checklistTemplateRepository).findAdminByOptionalStageAndStatus(
                                eq(ContentStage.PREGNANCY), eq(null), eq("thai"), any());
        }

        @Test
        void update_prePregnancySubstage_acceptsNullOrNoneAnchor() {
                ChecklistTemplate template = makeTemplate();
                template.setStage(ContentStage.PRE_PREGNANCY);
                template.setEligibilityAnchorType(ChecklistAnchorType.NONE);
                when(checklistTemplateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
                when(checklistTemplateRepository.save(any(ChecklistTemplate.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                ChecklistSubstageRequest noneSubstage = new ChecklistSubstageRequest(
                                "PRE_PREGNANCY_NONE_DAY_0_0", ChecklistAnchorType.NONE, 0, 0, ChecklistRangeUnit.DAY);

                UpdateChecklistTemplateRequest request = new UpdateChecklistTemplateRequest(
                                "Chuẩn bị mang thai", "Mô tả", ChecklistTemplateType.MANDATORY, (short) 1,
                                Set.of(ChecklistRecipientRole.MOTHER), ContentStage.PRE_PREGNANCY,
                                noneSubstage, ChecklistTemplateStatus.PENDING_REVIEW, null, 1);

                AdminChecklistTemplateDetailResponse response = service.update(TEMPLATE_ID, request, ADMIN_ID);

                assertEquals(ChecklistTemplateStatus.PENDING_REVIEW, response.getStatus());
                assertEquals(ContentStage.PRE_PREGNANCY, response.getStage());
        }
}
