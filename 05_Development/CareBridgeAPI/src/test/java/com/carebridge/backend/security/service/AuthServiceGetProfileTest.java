package com.carebridge.backend.security.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.mapper.UserMapper;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceGetProfileTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserMapper userMapper = new UserMapper();

    @InjectMocks
    private AuthServiceImpl authService;

    private static final UUID USER_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID_999 = UUID.fromString("00000000-0000-0000-0000-000000000999");

    private User createTestUser(UUID id, Role role) {
        return User.builder()
                .id(id)
                .phone("0900000001")
                .email("user@test.com")
                .name("Test User")
                .avatarUrl("https://test.carebridge.vn/avatar.jpg")
                .role(role)
                .enabled(true)
                .locked(false)
                .passwordHash("$2a$10$TestHash")
                .build();
    }

    // PRF-TC-001: Happy path — authenticated user views own profile
    @Test
    void getProfile_existingUser_shouldReturnProfile() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(USER_ID_1);

        assertNotNull(response);
        assertEquals(USER_ID_1, response.getId());
        assertEquals("0900000001", response.getPhone());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals(Role.MOTHER, response.getRole());
        verify(userMapper).toProfileResponse(user);
    }

    // PRF-TC-004: User not found in DB returns exception
    @Test
    void getProfile_nonExistentUser_shouldThrowResourceNotFound() {
        when(userRepository.findById(USER_ID_999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.getProfile(USER_ID_999));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    // PRF-TC-005 (service level): Response must not leak passwordHash
    @Test
    void getProfile_shouldNotExposePasswordHash() throws Exception {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(USER_ID_1);

        for (Field field : response.getClass().getDeclaredFields()) {
            assertNotEquals("passwordHash", field.getName(),
                    "UserProfileResponse must never contain passwordHash field");
        }
    }

    // PRF-TC-007: RBAC — Mother role returns correct profile
    @Test
    void getProfile_motherRole_shouldReturnMotherProfile() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(USER_ID_1);

        assertEquals(Role.MOTHER, response.getRole());
    }

    // PRF-TC-008: RBAC — Admin role returns correct profile
    @Test
    void getProfile_adminRole_shouldReturnAdminProfile() {
        User user = createTestUser(USER_ID_2, Role.SYSTEM_ADMIN);
        when(userRepository.findById(USER_ID_2)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(USER_ID_2);

        assertEquals(Role.SYSTEM_ADMIN, response.getRole());
    }
}
