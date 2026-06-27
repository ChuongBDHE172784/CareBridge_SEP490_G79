package com.carebridge.backend.security.policy;

import org.springframework.stereotype.Component;

@Component
public class PasswordComplexityPolicy {

    /**
     * Kiểm tra độ phức tạp mật khẩu theo yêu tầu:
     * - Tối thiểu 8 ký tự
     * - Có ít nhất 1 chữ hoa
     * - Có ít nhất 1 chữ thường
     * - Có ít nhất 1 chữ số
     * - Có ít nhất 1 ký tự đặc biệt
     *
     * @param password mật khẩu cần kiểm tra
     * @return true nếu hợp lệ, false nếu không
     */
    public boolean isComplexEnough(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (Character.isLowerCase(c)) {
                hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }

            if (hasUpper && hasLower && hasDigit && hasSpecial) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tạo thông báo lỗi chi tiết về yêu cầu mật khẩu.
     *
     * @return thông báo yêu cầu mật khẩu
     */
    public String getRequirements() {
        return "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
    }
}
