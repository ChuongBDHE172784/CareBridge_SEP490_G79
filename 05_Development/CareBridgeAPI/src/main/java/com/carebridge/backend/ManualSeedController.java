package com.carebridge.backend;
import com.carebridge.backend.common.dev.DevDataSeeder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ManualSeedController {
    private final DevDataSeeder seeder;
    @GetMapping("/api/manual-seed")
    public String seed() throws Exception {
        seeder.run(null);
        return "Seeded!";
    }
}
