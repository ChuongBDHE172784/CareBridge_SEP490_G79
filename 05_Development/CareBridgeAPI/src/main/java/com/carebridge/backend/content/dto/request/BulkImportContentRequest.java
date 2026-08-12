package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class BulkImportContentRequest {

    @NotNull(message = "Content type is required")
    private ContentType type;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<BulkImportItemRequest> items;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkImportItemRequest {

        private int rowIndex;

        private String title;

        private String body;

        private String summary;

        private String stage;

        private String categoryName;

        private UUID topicId;

        private String sourceLabel;

        private String sourceUrl;

        private String sourcePublisher;
    }
}
