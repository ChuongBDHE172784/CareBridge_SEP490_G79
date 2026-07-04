package com.carebridge.backend.profile.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * UC-09 Update Account Profile — bean-validation tests for {@link UpdateProfileRequest}.
 * These exercise the {@code @Valid} constraints that the controller applies before the
 * request reaches the service (phone / displayName length / avatar URL). A rejected field
 * surfaces at the HTTP layer as the standard VALIDATION_ERROR envelope (the codebase's
 * established bean-validation convention); the field-level rejection — the actual control —
 * is asserted directly here.
 */
class UpdateProfileRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    private boolean hasViolationOn(Set<ConstraintViolation<UpdateProfileRequest>> violations, String field) {
        return violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    // PRF-TC-002 — an invalid Vietnamese phone number is rejected (Oracle: BR-PRF-PHONE).
    @Test
    @DisplayName("PRF-TC-002: Invalid phone number is rejected on the phoneNumber field")
    void invalidPhone_isRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, "123abc", null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "phoneNumber"),
                "Expected a constraint violation on phoneNumber");
    }

    @Test
    @DisplayName("PRF-TC-002b: A valid Vietnamese phone number passes validation")
    void validPhone_passes() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, null, "0912345678", null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertFalse(hasViolationOn(violations, "phoneNumber"));
    }

    // PRF-TC-003 — displayName shorter than 2 characters is rejected (Oracle: BR-PRF-NAME).
    @Test
    @DisplayName("PRF-TC-003: displayName under 2 characters is rejected")
    void displayNameTooShort_isRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "A", null, null, null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "displayName"),
                "Expected a constraint violation on displayName");
    }

    @Test
    @DisplayName("PRF-TC-003b: A 2-character displayName passes the min-length boundary")
    void displayNameAtMinBoundary_passes() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Al", null, null, null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertFalse(hasViolationOn(violations, "displayName"));
    }

    // PRF-TC-005 — a non-URL avatarUrl is rejected (Oracle: BR-PRF-AVATAR / PRF-006).
    @Test
    @DisplayName("PRF-TC-005: Invalid avatarUrl is rejected on the avatarUrl field")
    void invalidAvatarUrl_isRejected() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, "not-a-url", null, null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(hasViolationOn(violations, "avatarUrl"),
                "Expected a constraint violation on avatarUrl");
    }

    @Test
    @DisplayName("PRF-TC-005b: A valid https avatarUrl passes validation")
    void validAvatarUrl_passes() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null, "https://cdn.carebridge.vn/avatars/test.jpg", null, null, null);

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertFalse(hasViolationOn(violations, "avatarUrl"));
    }

    // Sanity — a fully valid request produces no violations at all.
    @Test
    @DisplayName("PRF-TC-001: A fully valid request produces no constraint violations")
    void fullyValidRequest_passes() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Nguyen Test",
                "https://cdn.carebridge.vn/avatars/test.jpg",
                "0912345678",
                LocalDate.of(1995, 6, 15),
                "Ha Noi");

        Set<ConstraintViolation<UpdateProfileRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Expected no violations for a fully valid request");
    }
}
