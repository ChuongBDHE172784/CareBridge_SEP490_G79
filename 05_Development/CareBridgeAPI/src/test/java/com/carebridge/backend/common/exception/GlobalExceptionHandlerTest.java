package com.carebridge.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.common.response.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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

    @Test
    void handleNoResourceFound_returnsNeutral404WithoutFrameworkResourceMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/unmatched-story-69-route");
        NoResourceFoundException exception = new NoResourceFoundException(
                HttpMethod.GET,
                "api/v1/unmatched-story-69-route",
                "PRIVATE-FRAMEWORK-RESOURCE-SENTINEL");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("Resource not found");
        assertThat(response.getBody().getMessage())
                .doesNotContain(exception.getResourcePath(), "PRIVATE-FRAMEWORK-RESOURCE-SENTINEL");
        assertThat(response.getBody().getDetails()).isNull();
    }

    @Test
    void handleMethodNotSupported_returnsNeutral405WithoutAllowedMethodMetadata() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/user-checklist-items/unmapped-route");
        HttpRequestMethodNotSupportedException exception =
                new HttpRequestMethodNotSupportedException(
                        "GET", java.util.List.of("PUT", "PATCH", "DELETE"));

        ResponseEntity<ErrorResponse> response =
                handler.handleMethodNotSupported(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(405);
        assertThat(response.getBody().getError()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(response.getBody().getMessage()).isEqualTo("Request method not supported");
        assertThat(response.getBody().getMessage())
                .doesNotContain("GET", "PUT", "PATCH", "DELETE");
        assertThat(response.getBody().getDetails()).isNull();
    }
}
