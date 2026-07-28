package com.carebridge.backend;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CheckDb {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.yeylrlfdfpuytggdptem";
        String password = "Test12345678";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected!");
            String checkSql = "SELECT email, password_hash, enabled, account_status FROM users WHERE email = ?";
            
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setString(1, "moderator@carebridge.dev");
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    System.out.println("Found user: " + rs.getString(1));
                    System.out.println("Hash: " + rs.getString(2));
                    System.out.println("Enabled: " + rs.getBoolean(3));
                    System.out.println("Status: " + rs.getString(4));
                } else {
                    System.out.println("User not found!");
                }
            }
        }
    }
}
