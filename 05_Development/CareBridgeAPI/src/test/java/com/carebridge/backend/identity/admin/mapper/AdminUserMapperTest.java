package com.carebridge.backend.identity.admin.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.identity.admin.dto.response.AdminUserSummaryResponse;
import com.carebridge.backend.identity.admin.testsupport.AdminGovernanceTestFactory;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * UC114-TC-002 — search response DTO never exposes passwordHash (CWE-200).
 */
class AdminUserMapperTest {

    private final AdminUserMapper mapper = new AdminUserMapper();

    @Test
    void toSummary_neverExposesPasswordHash() {
        User user = AdminGovernanceTestFactory.makeUser(Role.MOTHER, u -> u.setPasswordHash("$2a$10$realSecretHash"));

        AdminUserSummaryResponse dto = mapper.toSummary(user);

        Field[] fields = AdminUserSummaryResponse.class.getDeclaredFields();
        boolean hasPasswordField = Arrays.stream(fields)
                .anyMatch(f -> f.getName().toLowerCase().contains("password"));
        assertThat(hasPasswordField).isFalse();

        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getEmail()).isEqualTo(user.getEmail());
    }
}
