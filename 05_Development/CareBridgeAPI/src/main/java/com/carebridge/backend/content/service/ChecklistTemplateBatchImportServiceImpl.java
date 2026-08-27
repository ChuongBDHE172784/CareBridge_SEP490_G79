package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ChecklistTemplateBatchImportRequest;
import com.carebridge.backend.content.dto.request.ChecklistTemplateBatchImportRowRequest;
import com.carebridge.backend.content.dto.response.AdminChecklistTemplateDetailResponse;
import com.carebridge.backend.content.dto.response.ChecklistTemplateBatchImportResponse;
import com.carebridge.backend.content.exception.ContentException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistTemplateBatchImportServiceImpl implements ChecklistTemplateBatchImportService {

    private final AdminChecklistTemplateService adminChecklistTemplateService;

    /**
     * Deliberately has no outer transaction. The injected admin service is a Spring proxy, so every
     * create call keeps its existing independent transaction and audit boundary.
     */
    @Override
    public ChecklistTemplateBatchImportResponse importBatch(
            ChecklistTemplateBatchImportRequest request, UUID adminUserId) {
        rejectDuplicateChecklistCodes(request.templates());

        List<UUID> createdIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (ChecklistTemplateBatchImportRowRequest row : request.templates()) {
            try {
                AdminChecklistTemplateDetailResponse created =
                        adminChecklistTemplateService.create(row.template(), adminUserId);
                createdIds.add(created.getId());
            } catch (ContentException exception) {
                errors.add(formatError(row, exception.getMessage()));
            } catch (RuntimeException exception) {
                log.warn("checklist_template_batch_import_row_failed rowIndex={} checklistCode={}",
                        row.rowIndex(), row.checklistCode().trim(), exception);
                errors.add(formatError(row, "Checklist template could not be created"));
            }
        }

        int totalRows = request.templates().size();
        int successCount = createdIds.size();
        return new ChecklistTemplateBatchImportResponse(
                totalRows, successCount, totalRows - successCount, errors, createdIds);
    }

    private void rejectDuplicateChecklistCodes(List<ChecklistTemplateBatchImportRowRequest> rows) {
        Set<String> seenCodes = new HashSet<>();
        for (ChecklistTemplateBatchImportRowRequest row : rows) {
            String normalizedCode = row.checklistCode().trim().toUpperCase(Locale.ROOT);
            if (!seenCodes.add(normalizedCode)) {
                throw ContentException.validationFailed(
                        "templates", "duplicate checklistCode: " + row.checklistCode().trim());
            }
        }
    }

    private String formatError(ChecklistTemplateBatchImportRowRequest row, String rawMessage) {
        String message = rawMessage;
        if (message == null || message.isBlank()) {
            message = "Checklist template could not be created";
        }
        return "Row " + row.rowIndex() + " (" + row.checklistCode().trim() + "): " + message;
    }
}
