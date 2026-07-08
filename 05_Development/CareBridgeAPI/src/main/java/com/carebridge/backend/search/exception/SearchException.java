package com.carebridge.backend.search.exception;

import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * UC-13 Search and Filter error codes (TDS §10 — prefix SEARCH-).
 * Caught by the generic {@code BusinessException} handler in {@code GlobalExceptionHandler}.
 */
public class SearchException extends BusinessException {

    public SearchException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }

    public static SearchException blankQuery() {
        return new SearchException("SEARCH-001", "Search query 'q' must not be blank");
    }

    public static SearchException queryTooLong() {
        return new SearchException("SEARCH-001", "Search query 'q' must not exceed 200 characters");
    }

    public static SearchException invalidType(String type) {
        return new SearchException("SEARCH-002", "Invalid search type: " + type);
    }

    public static SearchException invalidPage(int page) {
        return new SearchException("SEARCH-003", "page must not be negative: " + page);
    }

    public static SearchException invalidSize(int size) {
        return new SearchException("SEARCH-003", "size must be between 1 and 50: " + size);
    }
}
