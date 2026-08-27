package com.carebridge.backend.integration.gemini.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RagSource {

    private UUID contentId;
    private String title;
    /** Direct URL declared by the approved ContentItem source. */
    private String url;
    private String publisher;
    private String excerpt;
    private String sourceVersion;
    private String lastReviewed;
}
