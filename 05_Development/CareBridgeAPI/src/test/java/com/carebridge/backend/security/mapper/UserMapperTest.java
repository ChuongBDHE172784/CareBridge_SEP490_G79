package com.carebridge.backend.security.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    private static final UUID USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID_10 = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private User createTestUser(UUID id, String phone, String email, String name,
                                String avatarUrl, Role role) {
        return User.builder()
                .id(id)
                .phone(phone)
                .email(email)
                .name(name)
                .avatarUrl(avatarUrl)
                .role(role)
                .enabled(true)
                .locked(false)
                .passwordHash("$2a$10$TestHashDoNotUseInProduction")
                .build();
    }

    // PRF-TC-005: Response excludes passwordHash and internal fields
    @Test
    void toProfileResponse_shouldNotContainSensitiveFields() {
        Set<String> responseFields = Arrays.stream(UserProfileResponse.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertFalse(responseFields.contains("passwordHash"), "Response must not contain passwordHash");
        assertFalse(responseFields.contains("password"), "Response must not contain password");
        assertFalse(responseFields.contains("enabled"), "Response must not contain enabled");
        assertFalse(responseFields.contains("locked"), "Response must not contain locked");
    }

    // PRF-TC-001 (mapper part): Happy path mapping
    @Test
    void toProfileResponse_happyPath_shouldMapAllFields() {
        User user = createTestUser(USER_ID_1, "0900000001", "mother@test.com",
                "Test Mother", "https://test.carebridge.vn/avatar.jpg", Role.MOTHER);

        UserProfileResponse response = mapper.toProfileResponse(user);

        assertNotNull(response);
        assertEquals(USER_ID_1, response.getId());
        assertEquals("0900000001", response.getPhone());
        assertEquals("mother@test.com", response.getEmail());
        assertEquals("Test Mother", response.getName());
        assertEquals("https://test.carebridge.vn/avatar.jpg", response.getAvatarUrl());
        assertEquals(Role.MOTHER, response.getRole());
    }

    // PRF-TC-001 (null handling): Handles null optional fields
    @Test
    void toProfileResponse_nullOptionalFields_shouldNotThrow() {
        User user = createTestUser(USER_ID_2, "0900000002", null, null, null, Role.FAMILY);

        UserProfileResponse response = mapper.toProfileResponse(user);

        assertNotNull(response);
        assertEquals(USER_ID_2, response.getId());
        assertNull(response.getEmail());
        assertNull(response.getName());
        assertNull(response.getAvatarUrl());
    }

    @Test
    void toProfileResponse_nullUser_shouldReturnNull() {
        assertNull(mapper.toProfileResponse(null));
    }

    // PRF-TC-006: Response includes correct role for each role type
    @ParameterizedTest
    @EnumSource(Role.class)
    void toProfileResponse_shouldMapCorrectRole(Role role) {
        User user = createTestUser(USER_ID_10, "0900000010", "test@test.com", "Test", null, role);

        UserProfileResponse response = mapper.toProfileResponse(user);

        assertEquals(role, response.getRole(), "Role must match exactly");
    }
}
