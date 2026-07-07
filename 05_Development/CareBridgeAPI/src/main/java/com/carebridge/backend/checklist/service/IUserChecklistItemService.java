package com.carebridge.backend.checklist.service;

import com.carebridge.backend.checklist.dto.*;

import java.util.List;
import java.util.UUID;

public interface IUserChecklistItemService {

    ChecklistItemResponse addItem(AddChecklistItemRequest request, UUID userId);

    List<ChecklistItemResponse> importFromTemplate(ImportFromTemplateRequest request, UUID userId);

    List<ChecklistItemResponse> listItems(UUID userId, UUID journeyId, UUID babyId);

    ChecklistItemResponse toggleComplete(UUID itemId, UUID userId);

    ChecklistItemResponse updateItem(UUID itemId, UpdateChecklistItemRequest request, UUID userId);

    void deleteItem(UUID itemId, UUID userId);
}
