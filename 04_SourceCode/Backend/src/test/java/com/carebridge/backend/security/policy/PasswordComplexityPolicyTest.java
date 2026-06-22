package com.carebridge.backend.security.policy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordComplexityPolicyTest {

    private final PasswordComplexityPolicy policy = new PasswordComplexityPolicy();

    @Test
    void isComplexEnough_ShouldReturnFalse_WhenPasswordIsNull() {
        assertFalse(policy.isComplexEnough(null));
    }

    @Test
    void isComplexEnough_ShouldReturnFalse_WhenPasswordIsTooShort() {
        assertFalse(policy.isComplexEnough("Abc1!"));
    }

    @Test
    void isComplexEnough_ShouldReturnFalse_WhenMissingUppercase() {
        assertFalse(policy.isComplexEnough("abcdef1!"));
    }

    @Test
    void isComplexEnough_ShouldReturnFalse_WhenMissingLowercase() {
        assertFalse(policy.isComplexEnough("ABCDEF1!"));
    }

    @Test
    void isComplexEnough_ShouldReturnFalse_WhenMissingDigit() {
        assertFalse(policy.isComplexEnough("ABCDabcd!"));
    }

    @Test
    void isComplexEnough_ShouldReturnFalse_WhenMissingSpecialChar() {
        assertFalse(policy.isComplexEnough("ABCDabcd1"));
    }

    @Test
    void isComplexEnough_ShouldReturnTrue_WhenPasswordMeetsAllRequirements() {
        assertTrue(policy.isComplexEnough("MyP@ssw0rd123"));
    }

    @Test
    void isComplexEnough_ShouldReturnTrue_WithMinimumLength() {
        assertTrue(policy.isComplexEnough("Abcdef1!"));
    }

    @Test
    void getRequirements_ShouldReturnExpectedMessage() {
        String requirements = policy.getRequirements();
        assertNotNull(requirements);
        assertTrue(requirements.contains("at least 8 characters"));
        assertTrue(requirements.contains("uppercase"));
        assertTrue(requirements.contains("lowercase"));
        assertTrue(requirements.contains("digit"));
        assertTrue(requirements.contains("special character"));
    }
}
