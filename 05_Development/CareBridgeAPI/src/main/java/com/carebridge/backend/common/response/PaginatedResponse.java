package com.carebridge.backend.common.response;

import java.time.Instant;
import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class PaginatedResponse<T> extends ApiResponse<List<T>> {

    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private PaginatedResponse(List<T> data, int page, int size, long totalElements, int totalPages) {
        super(true, data, null, Instant.now());
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> PaginatedResponse<T> of(Page<T> page) {
        return new PaginatedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
