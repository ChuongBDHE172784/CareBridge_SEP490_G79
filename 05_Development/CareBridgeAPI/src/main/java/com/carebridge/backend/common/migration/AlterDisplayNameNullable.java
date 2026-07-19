package com.carebridge.backend.common.migration;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@Profile("supabase")
public class AlterDisplayNameNullable implements CommandLineRunner {

    private final DataSource dataSource;

    public AlterDisplayNameNullable(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='public' AND table_name='expert_profiles' AND column_name='display_name' AND is_nullable='NO'");
            rs.next();
            boolean stillNotNull = rs.getInt(1) > 0;
            rs.close();
            if (stillNotNull) {
                stmt.execute("ALTER TABLE public.expert_profiles ALTER COLUMN display_name DROP NOT NULL");
                System.out.println("[MIGRATION] expert_profiles.display_name is now nullable");
            } else {
                System.out.println("[MIGRATION] expert_profiles.display_name already nullable, skipping");
            }
        } catch (Exception e) {
            System.out.println("[MIGRATION] display_name already nullable or column missing: " + e.getMessage());
        }
    }
}
