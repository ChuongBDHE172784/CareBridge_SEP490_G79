package com.carebridge.backend.reminder.notification;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.reminder.notification.service.AppointmentNotificationRuleValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentNotificationRuleValidatorTest {

    private final AppointmentNotificationRuleValidator validator =
            new AppointmentNotificationRuleValidator();

    @Test
    void normalize_deduplicatesAndSortsSignedOffsets() {
        assertThat(validator.normalize(List.of(15, -30, 0, -1440, -30)))
                .containsExactly(-1440, -30, 0, 15);
    }

    @Test
    void normalize_emptyListDisablesAppointmentNotifications() {
        assertThat(validator.normalize(List.of())).isEmpty();
    }

    @Test
    void normalize_rejectsOutOfRangeOffsetsWithStableCode() {
        assertThatThrownBy(() -> validator.normalize(List.of(-43201)))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("REM-017");
    }

    @Test
    void normalizeTimeZone_acceptsIanaZoneAndRejectsInvalidValue() {
        assertThat(validator.normalizeTimeZone("Asia/Ho_Chi_Minh"))
                .isEqualTo("Asia/Ho_Chi_Minh");
        assertThatThrownBy(() -> validator.normalizeTimeZone("Not/A_Zone"))
                .isInstanceOf(BusinessException.class);
    }
}
