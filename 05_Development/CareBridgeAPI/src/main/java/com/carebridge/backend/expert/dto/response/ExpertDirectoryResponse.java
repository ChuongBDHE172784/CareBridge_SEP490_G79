package com.carebridge.backend.expert.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertDirectoryResponse {

    private List<ExpertProfileResponse> experts;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private List<String> specialties;
}
