package com.carebridge.backend;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CheckDb2 {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://aws-1-ap-northeast-1.pooler.supabase.com:5432/postgres?sslmode=require";
        String user = "postgres.ixsiyxtcujlywvybwwan";
        String pass = "N8kZ2H#Lw@pT7xM";
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            String sql = "SELECT email, password_hash FROM users WHERE email = 'admin@carebridge.dev'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("USER EXISTS: " + rs.getString(1) + " | HASH: " + rs.getString(2));
                    } else {
                        System.out.println("USER NOT FOUND");
                    }
                }
            }
        }
    }
}