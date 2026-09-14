package com.carebridge.backend.security.service;

import com.carebridge.backend.common.exception.ValidationException;
import com.carebridge.backend.security.dto.request.CheckRegistrationRequest;
import com.carebridge.backend.security.exception.AccountAlreadyExistsException;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceCheckRegistrationTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("checkRegistrationAvailability: available contacts pass without exception")
    void checkRegistrationAvailability_available_passes() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+84912345678")).thenReturn(false);

        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email("new@example.com")
                .phone("0912345678")
                .build();

        authService.checkRegistrationAvailability(req);

        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).existsByPhone("+84912345678");
    }

    @Test
    @DisplayName("checkRegistrationAvailability: duplicate email throws exception with specific message")
    void checkRegistrationAvailability_duplicateEmail_throws() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email("existing@example.com")
                .phone("0912345678")
                .build();

        assertThatThrownBy(() -> authService.checkRegistrationAvailability(req))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasMessage("Email này đã được đăng ký tài khoản.");
    }

    @Test
    @DisplayName("checkRegistrationAvailability: duplicate phone throws exception with specific message")
    void checkRegistrationAvailability_duplicatePhone_throws() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+84912345678")).thenReturn(true);

        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email("new@example.com")
                .phone("0912345678")
                .build();

        assertThatThrownBy(() -> authService.checkRegistrationAvailability(req))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasMessage("Số điện thoại này đã được đăng ký tài khoản.");
    }

    @Test
    @DisplayName("checkRegistrationAvailability: duplicate email and phone throws composite message")
    void checkRegistrationAvailability_duplicateBoth_throws() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        when(userRepository.existsByPhone("+84912345678")).thenReturn(true);

        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email("existing@example.com")
                .phone("0912345678")
                .build();

        assertThatThrownBy(() -> authService.checkRegistrationAvailability(req))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasMessage("Email và số điện thoại này đã được đăng ký tài khoản.");
    }

    @Test
    @DisplayName("checkRegistrationAvailability: null email and phone throws ValidationException")
    void checkRegistrationAvailability_nullBoth_throwsValidationException() {
        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email(null)
                .phone(null)
                .build();

        assertThatThrownBy(() -> authService.checkRegistrationAvailability(req))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Email or phone is required");

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).existsByPhone(anyString());
    }

    @Test
    @DisplayName("checkRegistrationAvailability: only email checks email and skips phone")
    void checkRegistrationAvailability_onlyEmail_checksEmailOnly() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email("new@example.com")
                .phone(null)
                .build();

        authService.checkRegistrationAvailability(req);

        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository, never()).existsByPhone(anyString());
    }

    @Test
    @DisplayName("checkRegistrationAvailability: only phone checks phone and skips email")
    void checkRegistrationAvailability_onlyPhone_checksPhoneOnly() {
        when(userRepository.existsByPhone("+84912345678")).thenReturn(false);

        CheckRegistrationRequest req = CheckRegistrationRequest.builder()
                .email(null)
                .phone("0912345678")
                .build();

        authService.checkRegistrationAvailability(req);

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository).existsByPhone("+84912345678");
    }
}
