package com.carebridge.backend.content.dto.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class ChecklistItemRequestValidationTest {

    @Test
    void sourceUrl_acceptsHttpAndHttpsButRejectsOtherSchemesAndOversizedValues() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(request(null)).isEmpty());
            assertTrue(validator.validate(request("https://example.org/guidance")).isEmpty());
            assertTrue(validator.validate(request("http://example.org/source")).isEmpty());
            assertFalse(validator.validate(request("javascript:alert(1)")).isEmpty());
            assertFalse(validator.validate(request("https://?")).isEmpty());
            assertFalse(validator.validate(request("https://user:password@example.org/document")).isEmpty());
            assertFalse(validator.validate(request("https://" + "a".repeat(2041))).isEmpty());
        }
    }

    private ChecklistItemRequest request(String sourceUrl) {
        return new ChecklistItemRequest(
                null,
                "Theo dõi sức khỏe",
                1,
                true,
                ChecklistTargetSubject.MOTHER,
                null,
                null,
                false,
                false,
                sourceUrl);
    }
}
