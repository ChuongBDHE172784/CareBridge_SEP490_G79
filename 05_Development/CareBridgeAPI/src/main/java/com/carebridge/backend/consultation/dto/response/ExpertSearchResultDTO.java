package com.carebridge.backend.consultation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Paginated expert search result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertSearchResultDTO {

    private List<ExpertListItem> experts;
    private long total;
    private Integer page;
    private Integer size;
    private Integer totalPages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpertListItem {
        private Long expertId;
        private String specialty;
        private String professionalTitle;
        private String workplace;
        private Integer experienceYears;
        private Double averageRating;
        private Integer reviewCount;
        private Boolean isAvailable;
    }
}
