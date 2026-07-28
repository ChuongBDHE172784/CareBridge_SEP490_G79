package com.carebridge.backend.content.dto.request;

import com.carebridge.backend.content.entity.ContentStage;
import com.carebridge.backend.content.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateContentRequest {

    @NotNull(message = "Content type is required")
    private ContentType type;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    @Size(max = 50000, message = "Body must not exceed 50000 characters")
    private String body;

    @Size(max = 150, message = "Summary must not exceed 150 characters")
    private String summary;

    @NotNull(message = "Stage is required")
    private ContentStage stage;

    private UUID topicId;

    private List<UUID> tagIds;

    private List<@jakarta.validation.Valid ContentSourceRequest> sources;
}
