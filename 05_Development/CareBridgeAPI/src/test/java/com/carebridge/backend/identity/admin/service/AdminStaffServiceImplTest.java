package com.carebridge.backend.identity.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.identity.admin.dto.response.StaffAccountResponse;
import com.carebridge.backend.identity.admin.service.impl.AdminStaffServiceImpl;
import com.carebridge.backend.identity.admin.testsupport.AdminGovernanceTestFactory;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.policy.PasswordComplexityPolicy;
import com.carebridge.backend.security.rbac.Role;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.EmailService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * UC115 Create Staff Account — unit tests (UC115_CreateStaffAccount_Test-Spec.md §4).
 * Covers TC-001, TC-002, TC-003, TC-004, TC-005, TC-006, TC-007, TC-008, TC-009, TC-012.
 */
@ExtendWith(MockitoExtension.class)
class AdminStaffServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;

    private AdminStaffServiceImpl newService() {
        return new AdminStaffServiceImpl(
                userRepository, passwordEncoder, new PasswordComplexityPolicy(), emailService, auditService);
    }

    // UC115-TC-001
    @Test
    void createStaffAccount_moderatorHappyPath_savesEmailsAndAudits() {
        AdminStaffServiceImpl service = newService();
        UUID adminId = UUID.randomUUID();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        StaffAccountResponse response = service.createStaffAccount(adminId,
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setRole(Role.MODERATOR)));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getRole()).isEqualTo(Role.MODERATOR);
        assertThat(savedCaptor.getValue().isEnabled()).isTrue();
        assertThat(savedCaptor.getValue().isMustChangePassword()).isTrue();

        verify(emailService, times(1)).sendStaffAccountCredentialsEmail(anyString(), anyString(), anyString());
        verify(auditService, times(1)).log(
                eq(AuditAction.STAFF_ACCOUNT_CREATED), eq(adminId), eq("USER"), anyString(), any());

        assertThat(response.getRole()).isEqualTo(Role.MODERATOR);
        assertThat(response.isMustChangePassword()).isTrue();
    }

    @Test
    void createStaffAccount_formattedPhone_checksAndPersistsCanonicalPhone() {
        AdminStaffServiceImpl service = newService();
        String formattedPhone = "(+84) 90-111.2222";
        String canonicalPhone = "+84901112222";
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(canonicalPhone)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        service.createStaffAccount(UUID.randomUUID(),
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setPhone(formattedPhone)));

        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByPhone(canonicalPhone);
        verify(userRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue().getPhone()).isEqualTo(canonicalPhone);
    }

    // UC115-TC-002
    @Test
    void createStaffAccount_contentAdmin_accepted() {
        AdminStaffServiceImpl service = newService();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        StaffAccountResponse response = service.createStaffAccount(UUID.randomUUID(),
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setRole(Role.CONTENT_ADMIN)));

        assertThat(response.getRole()).isEqualTo(Role.CONTENT_ADMIN);
    }

    // UC115-TC-003
    @Test
    void createStaffAccount_systemAdminRole_acceptedAndAudited() {
        AdminStaffServiceImpl service = newService();
        UUID adminId = UUID.randomUUID();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        StaffAccountResponse response = service.createStaffAccount(adminId,
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setRole(Role.SYSTEM_ADMIN)));

        assertThat(response.getRole()).isEqualTo(Role.SYSTEM_ADMIN);
        verify(auditService).log(eq(AuditAction.STAFF_ACCOUNT_CREATED), eq(adminId), eq("USER"), anyString(), any());
    }

    // UC115-TC-004
    @Test
    void createStaffAccount_generatesDistinctPasswordsPerCall_neverInResponseDto() {
        AdminStaffServiceImpl service = newService();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash1", "$2a$10$hash2");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        service.createStaffAccount(UUID.randomUUID(), AdminGovernanceTestFactory.makeCreateStaffRequest(r -> {}));
        service.createStaffAccount(UUID.randomUUID(), AdminGovernanceTestFactory.makeCreateStaffRequest(r -> {}));

        ArgumentCaptor<String> plaintextCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder, times(2)).encode(plaintextCaptor.capture());
        assertThat(plaintextCaptor.getAllValues()).doesNotHaveDuplicates();

        // Reflection: response DTO has no field that could carry a plaintext/hashed
        // password value (mustChangePassword is a boolean status flag, not a credential).
        boolean hasPasswordValueField = java.util.Arrays.stream(StaffAccountResponse.class.getDeclaredFields())
                .anyMatch(f -> f.getType() == String.class && f.getName().toLowerCase().contains("password"));
        assertThat(hasPasswordValueField).isFalse();
    }

    // UC115-TC-005
    @Test
    void createStaffAccount_mustChangePasswordAlwaysTrue() {
        AdminStaffServiceImpl service = newService();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        StaffAccountResponse response = service.createStaffAccount(
                UUID.randomUUID(), AdminGovernanceTestFactory.makeCreateStaffRequest(r -> {}));

        assertThat(response.isMustChangePassword()).isTrue();
    }

    // UC115-TC-006
    @Test
    void createStaffAccount_motherRole_rejectedBeforeAnyPersistence() {
        AdminStaffServiceImpl service = newService();

        assertThatThrownBy(() -> service.createStaffAccount(UUID.randomUUID(),
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setRole(Role.MOTHER))))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(emailService);
        verifyNoInteractions(auditService);
    }

    // UC115-TC-007
    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"FAMILY", "EXPERT", "MOTHER"})
    void createStaffAccount_nonStaffRoles_allRejected(Role role) {
        AdminStaffServiceImpl service = newService();

        assertThatThrownBy(() -> service.createStaffAccount(UUID.randomUUID(),
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setRole(role))))
                .isInstanceOf(ValidationException.class);
    }

    // UC115-TC-008
    @Test
    void createStaffAccount_duplicateEmail_rejectedBeforeAnyPersistence() {
        AdminStaffServiceImpl service = newService();
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.createStaffAccount(UUID.randomUUID(),
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setEmail("existing@carebridge.dev"))))
                .isInstanceOf(com.carebridge.backend.common.exception.BusinessException.class);

        verifyNoInteractions(emailService);
        verifyNoInteractions(auditService);
    }

    // UC115-TC-009
    @Test
    void createStaffAccount_duplicatePhone_rejected() {
        AdminStaffServiceImpl service = newService();
        String canonicalPhone = "+84901112222";
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(canonicalPhone)).thenReturn(true);

        assertThatThrownBy(() -> service.createStaffAccount(UUID.randomUUID(),
                AdminGovernanceTestFactory.makeCreateStaffRequest(r -> r.setPhone("84 90 111 2222"))))
                .isInstanceOf(com.carebridge.backend.common.exception.BusinessException.class);

        verify(userRepository).existsByPhone(canonicalPhone);
        verify(userRepository, never()).save(any());
    }

    // UC115-TC-012
    @Test
    void generatedTempPassword_satisfiesExistingComplexityPolicy() {
        AdminStaffServiceImpl service = newService();
        PasswordComplexityPolicy policy = new PasswordComplexityPolicy();
        for (int i = 0; i < 50; i++) {
            String pwd = service.generateTempPassword();
            assertThat(policy.isComplexEnough(pwd)).isTrue();
        }
    }
}
