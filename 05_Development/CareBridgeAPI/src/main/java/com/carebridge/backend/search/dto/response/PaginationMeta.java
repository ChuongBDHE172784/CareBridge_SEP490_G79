package com.carebridge.backend.search.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
public class PaginationMeta {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public static PaginationMeta of(Page<?> page) {
        return new PaginationMeta(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
