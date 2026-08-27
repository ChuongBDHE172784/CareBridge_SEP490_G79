package com.carebridge.backend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class JdbcSeeder {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("SUPABASE_DB_URL");
        if (url == null || url.isBlank()) {
            url = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        }
        String user = System.getenv("SUPABASE_DB_USERNAME");
        if (user == null || user.isBlank()) {
            user = "postgres.wqsunmakzdaxwyknegkq";
        }
        String password = System.getenv("SUPABASE_DB_PASSWORD");
        if (password == null || password.isBlank()) {
            password = "sdSjTLr8eWTgA7WI";
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Test@1234");

        String[][] accounts = {
            {"admin@carebridge.dev", "Admin System", "SYSTEM_ADMIN"},
            {"moderator@carebridge.dev", "Moderator Test", "MODERATOR"},
            {"content@carebridge.dev", "Content Manager", "CONTENT_ADMIN"},
            {"expert@carebridge.dev", "Bác sĩ Nguyễn Văn A", "EXPERT"},
            {"mother@carebridge.dev", "Mẹ Trần Thị B", "MOTHER"},
            {"family@carebridge.dev", "Người nhà Lê Văn C", "FAMILY"}
        };

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to database: " + url);
            String checkSql = "SELECT user_id FROM users WHERE email = ?";
            String updateSql = "UPDATE users SET password_hash = ?, enabled = true, locked = false, "
                    + "account_status = 'ACTIVE', email_verified = true, phone_verified = true, "
                    + "verification_status = 'VERIFIED', trust_status = 'ACTIVE', updated_at = now() WHERE email = ?";
            String insertSql = "INSERT INTO public.users ("
                    + "user_id, person_id, email, full_name, display_name, "
                    + "password_hash, role, enabled, locked, account_status, "
                    + "email_verified, phone_verified, verification_status, trust_status, "
                    + "settings_jsonb, social_identities, created_at, updated_at"
                    + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '{}'::jsonb, '[]'::jsonb, now(), now())";

            for (String[] acc : accounts) {
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, acc[0]);
                    ResultSet rs = check.executeQuery();
                    if (rs.next()) {
                        try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                            update.setString(1, hash);
                            update.setString(2, acc[0]);
                            update.executeUpdate();
                            System.out.println("Updated existing account: " + acc[0] + " (" + acc[2] + ")");
                        }
                    } else {
                        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                            UUID userId = UUID.randomUUID();
                            UUID personId = UUID.randomUUID();
                            insert.setObject(1, userId);
                            insert.setObject(2, personId);
                            insert.setString(3, acc[0]);
                            insert.setString(4, acc[1]);
                            insert.setString(5, acc[1]);
                            insert.setString(6, hash);
                            insert.setString(7, acc[2]);
                            insert.setBoolean(8, true);
                            insert.setBoolean(9, false);
                            insert.setString(10, "ACTIVE");
                            insert.setBoolean(11, true);
                            insert.setBoolean(12, true);
                            insert.setString(13, "VERIFIED");
                            insert.setString(14, "ACTIVE");
                            insert.executeUpdate();
                            System.out.println("Created new account: " + acc[0] + " (" + acc[2] + ")");
                        }
                    }
                }
            }
            System.out.println("Successfully seeded all dev test accounts!");
        }
    }
}
