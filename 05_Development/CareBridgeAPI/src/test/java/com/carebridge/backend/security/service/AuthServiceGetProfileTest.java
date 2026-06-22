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

    private User createTestUser(Long id, Role role) {
        return User.builder()
                .id(id)
                .phone("090000000" + id)
                .email("user" + id + "@test.com")
                .name("Test User " + id)
                .avatarUrl("https://test.carebridge.vn/avatar-" + id + ".jpg")
                .role(role)
                .enabled(true)
                .locked(false)
                .passwordHash("$2a$10$TestHash")
                .build();
    }

    // PRF-TC-001: Happy path — authenticated user views own profile
    @Test
    void getProfile_existingUser_shouldReturnProfile() {
        User user = createTestUser(1L, Role.MOTHER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("0900000001", response.getPhone());
        assertEquals("user1@test.com", response.getEmail());
        assertEquals("Test User 1", response.getName());
        assertEquals(Role.MOTHER, response.getRole());
        verify(userMapper).toProfileResponse(user);
    }

    // PRF-TC-004: User not found in DB returns exception
    @Test
    void getProfile_nonExistentUser_shouldThrowResourceNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.getProfile(999L));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    // PRF-TC-005 (service level): Response must not leak passwordHash
    @Test
    void getProfile_shouldNotExposePasswordHash() throws Exception {
        User user = createTestUser(1L, Role.MOTHER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(1L);

        for (Field field : response.getClass().getDeclaredFields()) {
            assertNotEquals("passwordHash", field.getName(),
                    "UserProfileResponse must never contain passwordHash field");
        }
    }

    // PRF-TC-007: RBAC — Mother role returns correct profile
    @Test
    void getProfile_motherRole_shouldReturnMotherProfile() {
        User user = createTestUser(1L, Role.MOTHER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(1L);

        assertEquals(Role.MOTHER, response.getRole());
    }

    // PRF-TC-008: RBAC — Admin role returns correct profile
    @Test
    void getProfile_adminRole_shouldReturnAdminProfile() {
        User user = createTestUser(2L, Role.SYSTEM_ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(2L);

        assertEquals(Role.SYSTEM_ADMIN, response.getRole());
    }
}
