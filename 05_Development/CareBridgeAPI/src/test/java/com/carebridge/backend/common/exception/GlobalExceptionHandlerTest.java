package com.carebridge.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.common.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_preservesDomainErrorCode() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/change-password");

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new ValidationException("AUTH-071", "Current password is incorrect"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("AUTH-071");
        assertThat(response.getBody().getMessage()).isEqualTo("Current password is incorrect");
    }
}
