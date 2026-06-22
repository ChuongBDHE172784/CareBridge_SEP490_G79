package com.carebridge.backend.common.response;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    @Builder.Default
    private final boolean success = false;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<ErrorDetail> details;
    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
