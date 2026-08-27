package com.carebridge.backend.content.dto.response;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkImportResponse {

    private int totalRows;

    private int successCount;

    private int failedCount;

    private List<String> errors;

    private List<UUID> createdIds;
}
