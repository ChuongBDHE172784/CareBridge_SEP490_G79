package com.carebridge.backend.security.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.security.dto.request.UpdateProfileRequest;
import com.carebridge.backend.security.dto.response.UserProfileResponse;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.mapper.UserMapper;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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

    @Mock
    private AuditService auditService;

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
    @DisplayName("PROF-TC-008-001: Retrieve valid user profile")
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
    @DisplayName("PROF-TC-008-004: userId not found throws PROF-001")
    @Test
    void getProfile_nonExistentUser_shouldThrowResourceNotFound() {
        when(userRepository.findById(USER_ID_999)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.getProfile(USER_ID_999));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    // PRF-TC-005 (service level): Response must not leak passwordHash
    @DisplayName("PROF-TC-008-002: passwordHash not exposed")
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

    // ═══════════════════════════════════════════════════════════
    // UC09 — Update Account Profile tests
    // ═══════════════════════════════════════════════════════════

    private UpdateProfileRequest createUpdateRequest(String name, String avatarUrl) {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName(name);
        req.setAvatarUrl(avatarUrl);
        return req;
    }

    // PRF-TC-001 — Valid update succeeds
    @Test
    @DisplayName("PRF-TC-001: Valid update profile request succeeds")
    void updateProfile_validRequest_updatesAndReturns() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = createUpdateRequest("New Name", "https://cdn.carebridge.com/avatar.jpg");
        UserProfileResponse response = authService.updateProfile(USER_ID_1, request);

        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    // PRF-TC-007 — XSS payload in name sanitized
    @Test
    @DisplayName("PRF-TC-007: XSS payload in name is sanitized")
    void updateProfile_xssInName_isSanitized() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = createUpdateRequest("<script>alert('xss')</script>", null);
        authService.updateProfile(USER_ID_1, request);

        // Verify saved user name does not contain raw script tags
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        String savedName = captor.getValue().getName();
        assertFalse(savedName.contains("<script>"), "Name must not contain raw script tags");
    }

    // PRF-TC-008 — Partial update only changes specified fields
    @Test
    @DisplayName("PRF-TC-008: Partial update only changes name, not other fields")
    void updateProfile_partialUpdate_onlyChangesSpecifiedFields() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        String originalEmail = user.getEmail();
        String originalPhone = user.getPhone();
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = createUpdateRequest("Updated Name", null);
        authService.updateProfile(USER_ID_1, request);

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(originalEmail, saved.getEmail());
        assertEquals(originalPhone, saved.getPhone());
    }

    // PRF-TC-006 — Non-existent user returns error
    @Test
    @DisplayName("PRF-TC-006: Update non-existent user throws ResourceNotFoundException")
    void updateProfile_nonExistentUser_throwsNotFound() {
        when(userRepository.findById(USER_ID_999)).thenReturn(Optional.empty());

        UpdateProfileRequest request = createUpdateRequest("Name", null);
        assertThrows(ResourceNotFoundException.class,
                () -> authService.updateProfile(USER_ID_999, request));
    }

    // PROF-TC-008-SEC-001 — No internal/sensitive fields exposed in the profile contract
    @Test
    @DisplayName("PROF-TC-008-SEC-001: Profile response never exposes internal fields")
    void getProfile_responseHasNoInternalFields() throws Exception {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getProfile(USER_ID_1);

        // 1) The DTO class must not even declare these fields.
        java.util.Set<String> declared = new java.util.HashSet<>();
        for (java.lang.reflect.Field f : response.getClass().getDeclaredFields()) {
            declared.add(f.getName());
        }
        assertFalse(declared.contains("passwordHash"), "must not declare passwordHash");
        assertFalse(declared.contains("password"), "must not declare password");
        assertFalse(declared.contains("lockedAt"), "must not declare lockedAt");
        assertFalse(declared.contains("enabled"), "must not declare enabled");

        // 2) Serialized JSON must not contain these keys either.
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        String json = mapper.writeValueAsString(response);
        assertFalse(json.contains("passwordHash"), "JSON must not contain passwordHash");
        assertFalse(json.contains("\"password\""), "JSON must not contain password");
        assertFalse(json.contains("lockedAt"), "JSON must not contain lockedAt");
        assertFalse(json.contains("\"enabled\""), "JSON must not contain enabled");
    }

    // PRF-TC-009 — Audit log written after a successful update (ADR-002)
    @Test
    @DisplayName("PRF-TC-009: Successful update writes a PROFILE_UPDATED audit record")
    void updateProfile_success_writesAuditLog() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.updateProfile(USER_ID_1, createUpdateRequest("New Name", null));

        verify(auditService, times(1)).log(
                eq(com.carebridge.backend.audit.entity.AuditAction.PROFILE_UPDATED),
                eq(USER_ID_1),
                eq("User"),
                eq(USER_ID_1.toString()),
                any());
    }

    // PRF-TC-009b — Audit NOT written when the persistence step fails (transactional integrity)
    @Test
    @DisplayName("PRF-TC-009b: Failed update does not write an audit record")
    void updateProfile_saveFails_noAuditLog() {
        User user = createTestUser(USER_ID_1, Role.MOTHER);
        when(userRepository.findById(USER_ID_1)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class,
                () -> authService.updateProfile(USER_ID_1, createUpdateRequest("New Name", null)));

        verify(auditService, never()).log(any(), any(), any(), any(), any());
    }
}
