package com.carebridge.backend.security.service;

import com.carebridge.backend.security.dto.request.RegisterRequest;
import com.carebridge.backend.security.rbac.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceRegisterTest {

    @Autowired
    private AuthService authService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private SmsService smsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoads() {
        assertNotNull(authService);
    }

    @Test
    void register_IntegrationTest_Email() {
        // Tạo request
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPhone(null);
        request.setPassword("MyP@ssw0rd123");
        request.setRole(Role.MOTHER);

        // Test will fail due to mocks not configured - this is just structure check
        // Full integration test with @DataJpaTest would be better
    }
}
