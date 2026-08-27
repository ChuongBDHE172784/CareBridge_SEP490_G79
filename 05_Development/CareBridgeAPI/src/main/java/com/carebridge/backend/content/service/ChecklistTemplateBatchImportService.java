package com.carebridge.backend.content.service;

import com.carebridge.backend.content.dto.request.ChecklistTemplateBatchImportRequest;
import com.carebridge.backend.content.dto.response.ChecklistTemplateBatchImportResponse;
import java.util.UUID;

public interface ChecklistTemplateBatchImportService {

    ChecklistTemplateBatchImportResponse importBatch(
            ChecklistTemplateBatchImportRequest request, UUID adminUserId);
}
