package com.carebridge.backend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import com.carebridge.backend.security.repository.UserRepository;
import com.carebridge.backend.security.entity.User;
import com.carebridge.backend.security.rbac.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public class ManualSeedRunner {
    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    record SeedAccount(String email, String fullName, Role role) {}

    @Test
    public void seedUsers() {
        List<SeedAccount> accounts = List.of(
            new SeedAccount("admin@carebridge.dev", "Admin Test", Role.SYSTEM_ADMIN),
            new SeedAccount("moderator@carebridge.dev", "Moderator Test", Role.MODERATOR),
            new SeedAccount("content@carebridge.dev", "Content Test", Role.CONTENT_ADMIN),
            new SeedAccount("expert@carebridge.dev", "Expert Test", Role.EXPERT),
            new SeedAccount("mother@carebridge.dev", "Mother Test", Role.MOTHER),
            new SeedAccount("family@carebridge.dev", "Family Test", Role.FAMILY)
        );
        String defaultPassword = "Test@1234";
        String passwordHash = passwordEncoder.encode(defaultPassword);
        
        for (SeedAccount seed : accounts) {
            userRepository.findByEmail(seed.email()).ifPresent(u -> {
                userRepository.delete(u);
            });
            
            User user = User.builder()
                    .email(seed.email())
                    .name(seed.fullName())
                    .passwordHash(passwordHash)
                    .role(seed.role())
                    .enabled(true)
                    .locked(false)
                    .accountStatus("ACTIVE")
                    .emailVerified(true)
                    .phoneVerified(true)
                    .build();
            userRepository.save(user);
            System.out.println("Inserted: " + seed.email());
        }
    }
}
