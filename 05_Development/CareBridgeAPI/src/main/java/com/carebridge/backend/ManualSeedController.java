package com.carebridge.backend;
import com.carebridge.backend.common.dev.DevDataSeeder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Profile("dev & !prod")
@ConditionalOnProperty(
        prefix = "carebridge.dev-seed",
        name = "enabled",
        havingValue = "true")
public class ManualSeedController {
    private final DevDataSeeder seeder;
    @GetMapping("/api/manual-seed")
    public String seed() throws Exception {
        seeder.run(null);
        return "Seeded!";
    }
}
