package com.carebridge.backend;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class JdbcSeeder {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres";
        String user = "postgres.yeylrlfdfpuytggdptem";
        String password = "Test12345678";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Test@1234");

        String[][] accounts = {
            {"admin@carebridge.dev", "Admin Test", "SYSTEM_ADMIN"},
            {"moderator@carebridge.dev", "Moderator Test", "MODERATOR"},
            {"content@carebridge.dev", "Content Test", "CONTENT_ADMIN"},
            {"expert@carebridge.dev", "Expert Test", "EXPERT"},
            {"mother@carebridge.dev", "Mother Test", "MOTHER"},
            {"family@carebridge.dev", "Family Test", "FAMILY"}
        };

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected to db!");
            String checkSql = "SELECT id FROM users WHERE email = ?";
            String insertSql = "INSERT INTO users (id, email, name, password_hash, role, enabled, locked, account_status, email_verified, phone_verified, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())";
            
            for (String[] acc : accounts) {
                try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                    check.setString(1, acc[0]);
                    ResultSet rs = check.executeQuery();
                    if (rs.next()) {
                        System.out.println("Exists: " + acc[0]);
                    } else {
                        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                            insert.setObject(1, UUID.randomUUID());
                            insert.setString(2, acc[0]);
                            insert.setString(3, acc[1]);
                            insert.setString(4, hash);
                            insert.setString(5, acc[2]);
                            insert.setBoolean(6, true);
                            insert.setBoolean(7, false);
                            insert.setString(8, "ACTIVE");
                            insert.setBoolean(9, true);
                            insert.setBoolean(10, true);
                            insert.executeUpdate();
                            System.out.println("Inserted: " + acc[0]);
                        }
                    }
                }
            }
        }
    }
}
