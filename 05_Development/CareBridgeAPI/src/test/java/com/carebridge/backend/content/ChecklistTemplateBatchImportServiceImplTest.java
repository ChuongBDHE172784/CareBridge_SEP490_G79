package com.carebridge.backend.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.content.dto.request.ChecklistTemplateBatchImportRequest;
import com.carebridge.backend.content.dto.request.ChecklistTemplateBatchImportRowRequest;
import com.carebridge.backend.content.dto.request.CreateChecklistTemplateRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateBatchImportResponse;
import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.exception.ContentException;
import com.carebridge.backend.content.service.AdminChecklistTemplateService;
import com.carebridge.backend.content.service.ChecklistTemplateBatchImportServiceImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChecklistTemplateBatchImportServiceImplTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIRST_ID = UUID.fromString("69000000-0000-0000-0000-000000000801");
    private static final UUID SECOND_ID = UUID.fromString("69000000-0000-0000-0000-000000000802");

    @Mock
    private AdminChecklistTemplateService adminChecklistTemplateService;

    @InjectMocks
    private ChecklistTemplateBatchImportServiceImpl service;

    @Test
    void importBatch_allRowsValid_returnsCreatedIds() {
        when(adminChecklistTemplateService.create(any(), eq(ADMIN_ID)))
                .thenReturn(response(FIRST_ID), response(SECOND_ID));

        ChecklistTemplateBatchImportResponse result = service.importBatch(
                request(row(2, "CHK-001"), row(3, "CHK-002")), ADMIN_ID);

        assertEquals(2, result.totalRows());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failedCount());
        assertTrue(result.errors().isEmpty());
        assertEquals(List.of(FIRST_ID, SECOND_ID), result.createdIds());
        verify(adminChecklistTemplateService).create(row(2, "CHK-001").template(), ADMIN_ID);
        verify(adminChecklistTemplateService).create(row(3, "CHK-002").template(), ADMIN_ID);
    }

    @Test
    void importBatch_oneCreateFails_keepsSuccessfulRowsAndReportsError() {
        when(adminChecklistTemplateService.create(any(), eq(ADMIN_ID)))
                .thenReturn(response(FIRST_ID))
                .thenThrow(ContentException.validationFailed("items", "invalid cadence"));

        ChecklistTemplateBatchImportResponse result = service.importBatch(
                request(row(5, "CHK-VALID"), row(8, "CHK-INVALID")), ADMIN_ID);

        assertEquals(2, result.totalRows());
        assertEquals(1, result.successCount());
        assertEquals(1, result.failedCount());
        assertEquals(List.of(FIRST_ID), result.createdIds());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().getFirst().contains("Row 8 (CHK-INVALID)"));
        assertTrue(result.errors().getFirst().contains("invalid cadence"));
    }

    @Test
    void importBatch_unexpectedFailure_doesNotExposeInfrastructureDetails() {
        when(adminChecklistTemplateService.create(any(), eq(ADMIN_ID)))
                .thenThrow(new IllegalStateException("jdbc:postgresql://secret-host/internal"));

        ChecklistTemplateBatchImportResponse result = service.importBatch(
                request(row(9, "CHK-FAILED")), ADMIN_ID);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.errors().getFirst().contains("Checklist template could not be created"));
        assertTrue(!result.errors().getFirst().contains("secret-host"));
    }

    @Test
    void importBatch_duplicateChecklistCode_rejectsWholeBatchBeforeCreate() {
        ChecklistTemplateBatchImportRequest request = request(
                row(2, "CHK-001"), row(6, " chk-001 "));

        ContentException error = assertThrows(
                ContentException.class, () -> service.importBatch(request, ADMIN_ID));

        assertEquals("CNT-001", error.getCode());
        assertTrue(error.getMessage().contains("duplicate checklistCode"));
        verify(adminChecklistTemplateService, never()).create(any(), any());
    }

    private ChecklistTemplateBatchImportRequest request(ChecklistTemplateBatchImportRowRequest... rows) {
        return new ChecklistTemplateBatchImportRequest(List.of(rows));
    }

    private ChecklistTemplateBatchImportRowRequest row(int rowIndex, String checklistCode) {
        CreateChecklistTemplateRequest template = new CreateChecklistTemplateRequest(
                "Checklist " + checklistCode.trim(), "Imported from Excel", ContentStage.PRE_PREGNANCY, List.of());
        return new ChecklistTemplateBatchImportRowRequest(rowIndex, checklistCode, template);
    }

    private AdminChecklistTemplateDetailResponse response(UUID id) {
        return AdminChecklistTemplateDetailResponse.builder().id(id).items(List.of()).build();
    }
}
