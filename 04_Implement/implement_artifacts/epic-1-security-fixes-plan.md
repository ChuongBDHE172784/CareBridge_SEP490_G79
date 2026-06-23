# Plan Xử Lý Các Lỗi Bảo Mật Nghiêm Trọng Trong Epic 1

Tài liệu này phác thảo kế hoạch kỹ thuật chi tiết để khắc phục 5 lỗi bảo mật nghiêm trọng đã được phát hiện trong quá trình kiểm toán Epic 1. Kế hoạch này được thiết kế dưới dạng đặc tả kỹ thuật để lập trình viên (hoặc Agent phát triển code) có thể làm theo và thực thi chính xác.

---

## 1. Lỗi 1: Session Endpoint dùng sai Principal (Wrong Principal)

### Mô tả vấn đề
Trong `SessionController.java`, các endpoint truy vấn và thu hồi session sử dụng annotation `@AuthenticationPrincipal User user` (mong đợi nhận được thực thể `com.carebridge.backend.security.entity.User`).
Tuy nhiên, trong `JwtAuthenticationFilter.java`, đối tượng Principal được gán vào Security Context thực chất chỉ là một `String` (User ID):
```java
UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
        jwtTokenProvider.getSubject(token), // Trả về String (User ID)
        null,
        jwtTokenProvider.getAuthorities(token));
```
Sự lệch pha này khiến Spring Security không thể map `String` sang thực thể `User`, dẫn đến lỗi `ClassCastException` hoặc truyền vào `null` (gây lỗi `NullPointerException` tại runtime khi gọi `user.getId()`).

### Giải pháp kỹ thuật
Để khắc phục đơn giản và tối ưu hiệu năng (tránh việc phải truy vấn CSDL để nạp thực thể `User` cho mỗi HTTP request filter), chúng ta sẽ cập nhật Controller sử dụng `java.security.Principal` chuẩn và lấy User ID thông qua lớp tiện ích `SecurityUtils` đã có sẵn.

#### Các bước thực hiện:
*   Mở file `SessionController.java`.
*   Thay thế `@AuthenticationPrincipal User user` bằng `java.security.Principal principal`.
*   Lấy `userId` bằng cách gọi `SecurityUtils.requireCurrentUserId(principal)`.

**Chi tiết thay đổi (Diff):**
```diff
     @GetMapping
-    public ApiResponse<List<SessionInfo>> getSessions(@AuthenticationPrincipal User user) {
-        List<SessionInfo> sessions = sessionService.getActiveSessions(user.getId());
+    public ApiResponse<List<SessionInfo>> getSessions(java.security.Principal principal) {
+        UUID userId = SecurityUtils.requireCurrentUserId(principal);
+        List<SessionInfo> sessions = sessionService.getActiveSessions(userId);
         return ApiResponse.success(sessions);
     }

     @DeleteMapping("/{sessionId}")
     public ApiResponse<Void> revokeSession(
             @PathVariable UUID sessionId,
-            @AuthenticationPrincipal User user,
+            java.security.Principal principal,
             HttpServletRequest request) {
         String ip = request.getRemoteAddr();
+        UUID userId = SecurityUtils.requireCurrentUserId(principal);
         try {
-            sessionService.revokeSession(sessionId, user.getId(), ip);
+            sessionService.revokeSession(sessionId, userId, ip);
             return ApiResponse.success(null);
```

### Tiêu chí nghiệm thu (Acceptance Criteria)
*   Gọi endpoint `/api/v1/sessions` và `/api/v1/sessions/{sessionId}` thành công với JWT token hợp lệ, trả về mã trạng thái `200 OK`.
*   Không xảy ra lỗi `ClassCastException` hoặc NPE.

---

## 2. Lỗi 2: Logout/Revoke Không Vô Hiệu Hóa Refresh Token

### Mô tả vấn đề
Khi người dùng đăng xuất hoặc thu hồi phiên (revoke session), hệ thống cập nhật trạng thái `revoked = true` trong bảng `user_sessions` hoặc lưu token bị hủy vào bảng `token_blacklist`. 
Tuy nhiên, logic tại endpoint làm mới token `/api/v1/auth/refresh` (`AuthServiceImpl.refresh`) không hề kiểm tra các bảng này, cho phép refresh token đã bị hủy vẫn tiếp tục tạo ra các access token mới.

### Giải pháp kỹ thuật
Cập nhật phương thức `refresh` trong `AuthServiceImpl.java` để thực hiện kiểm tra kiểm soát trạng thái của refresh token trước khi cấp access token mới.

#### Các bước thực hiện:
1.  Băm SHA-256 mã refresh token thô nhận được từ client.
2.  Kiểm tra xem băm của refresh token đó có tồn tại trong bảng `token_blacklist` không. Nếu có, ném ra ngoại lệ `AuthenticationException("Token has been blacklisted")`.
3.  Tìm kiếm session tương ứng với refresh token băm trong bảng `user_sessions`. Nếu không tìm thấy hoặc session đó có thuộc tính `isRevoked() == true` hay trạng thái là `REVOKED`, ném ra ngoại lệ `AuthenticationException("Session has been revoked")`.

**Logic xử lý đề xuất trong `AuthServiceImpl.refresh`:**
```java
String tokenHash = TokenUtils.hashSha256(request.getRefreshToken());

// 1. Kiểm tra trong Blacklist
if (tokenBlacklistRepository.existsByTokenHash(tokenHash)) {
    throw new AuthenticationException("Token has been blacklisted");
}

// 2. Kiểm tra trạng thái Session tương ứng
UserSession session = userSessionRepository.findByRefreshTokenHash(tokenHash)
    .orElseThrow(() -> new AuthenticationException("Session not found"));
if (session.isRevoked() || "REVOKED".equals(session.getStatus())) {
    throw new AuthenticationException("Session has been revoked");
}
```

### Tiêu chí nghiệm thu (Acceptance Criteria)
*   Gửi request làm mới token bằng một refresh token đã bị đăng xuất hoặc thu hồi sẽ bị từ chối thẳng thừng với HTTP status `401 Unauthorized` hoặc `400 Bad Request`.

---

## 3. Lỗi 3: Lệch Hash Token (Raw-Token/Hash Mismatch)

### Mô tả vấn đề
Có sự không nhất quán giữa việc lưu băm SHA-256 của token vào cơ sở dữ liệu và cách truy vấn so sánh. Một số nơi truy vấn bằng token thô trực tiếp hoặc thực hiện băm 2 lần (double hashing), dẫn đến việc đối chiếu dữ liệu bị lỗi, hệ thống không tìm thấy token để khóa hoặc xác thực.

### Giải pháp kỹ thuật
Thống nhất logic băm token SHA-256 vào một lớp tiện ích (Utility Class) và áp dụng đồng nhất ở tất cả các dịch vụ.

#### Các bước thực hiện:
1.  Tạo hoặc sử dụng lớp tiện ích `TokenUtils.java` trong gói bảo mật/common:
    ```java
    package com.carebridge.backend.security.util;
    
    import java.nio.charset.StandardCharsets;
    import java.security.MessageDigest;
    import java.security.NoSuchAlgorithmException;
    import org.bouncycastle.util.encoders.Hex;

    public final class TokenUtils {
        private TokenUtils() {}

        public static String hashSha256(String input) {
            if (input == null) return null;
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
                return Hex.toHexString(hash); // Trả về chuỗi Hex viết thường thống nhất
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 algorithm not available", e);
            }
        }
    }
    ```
2.  Refactor lại `AuthServiceImpl.java`, `SessionServiceImpl.java` và `TokenBlacklistService` để luôn gọi `TokenUtils.hashSha256()` trước khi lưu hoặc tìm kiếm token băm trong database.

### Tiêu chí nghiệm thu (Acceptance Criteria)
*   Mọi thao tác lưu trữ và tìm kiếm trên trường `token_hash` / `refresh_token_hash` trong CSDL đều sử dụng chuỗi Hex-encoded SHA-256 chính xác và khớp nhau.

---

## 4. Lỗi 4: OTP Có Thể Replay Đồng Thời (Race Condition)

### Mô tả vấn đề
API xác thực OTP `/api/v1/auth/verify-otp` không sử dụng cơ chế khóa dữ liệu (data locking) khi truy vấn. Nếu attacker gửi đồng thời nhiều request xác thực cùng 1 mã OTP tại cùng một mili giây, các thread sẽ xử lý song song và cùng thấy OTP chưa được sử dụng (`verified = false`), cho phép xác thực thành công nhiều lần hoặc vượt qua giới hạn số lần thử (`attempts`).

### Giải pháp kỹ thuật
Sử dụng cơ chế khóa ghi bi quan (Pessimistic Write Lock) trên câu lệnh truy vấn OTP để tuần tự hóa các yêu cầu xác thực.

#### Các bước thực hiện:
*   Mở `OtpVerificationRepository.java`.
*   Thêm annotation `@Lock(LockModeType.PESSIMISTIC_WRITE)` cho các phương thức truy vấn xác thực OTP:
    ```java
    package com.carebridge.backend.security.repository;

    import com.carebridge.backend.security.entity.OtpVerification;
    import jakarta.persistence.LockModeType;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Lock;
    import java.util.Optional;

    public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<OtpVerification> findByEmailAndPurposeAndVerifiedFalse(String email, OtpPurpose purpose);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<OtpVerification> findByPhoneAndPurposeAndVerifiedFalse(String phone, OtpPurpose purpose);
    }
    ```
*   Đảm bảo logic xác thực OTP trong `AuthServiceImpl` chạy trong một `@Transactional` để khóa được giữ cho đến khi giao dịch kết thúc (khi OTP đã được đánh dấu là đã sử dụng).

### Tiêu chí nghiệm thu (Acceptance Criteria)
*   Khi có nhiều request đồng thời gửi lên cùng 1 OTP, chỉ có request đầu tiên được xử lý thành công (hoặc ghi nhận lượt thử), các request tiếp theo lập tức bị chặn do OTP đã được cập nhật trạng thái ngay sau đó.

---

## 5. Lỗi 5: JWT Secret Mặc Định Dễ Bị Giả Mạo

### Mô tả vấn đề
Trong tệp cấu hình `application.yaml`, JWT Secret được gán một giá trị mặc định là `"carebridge-local-dev-secret-key-must-be-at-least-32-characters"`. Nếu deploy lên môi trường Production mà quên không cấu hình biến môi trường `JWT_SECRET`, ứng dụng vẫn chạy bình thường với key mặc định này, cho phép hacker tự ký JWT token có quyền admin để tấn công hệ thống.

### Giải pháp kỹ thuật
Bỏ giá trị mặc định (fallback default value) của JWT Secret trong tệp cấu hình `application.yaml`. Điều này ép buộc Spring Boot phải nạp biến môi trường `JWT_SECRET`. Nếu không có biến này khi khởi chạy, Spring Boot sẽ ném ra ngoại lệ cấu hình (`IllegalArgumentException`/`PlaceholderResolutionException`) và dừng khởi chạy ngay lập tức (Fail-Fast).

#### Các bước thực hiện:
*   Mở tệp `src/main/resources/application.yaml`.
*   Sửa cấu hình `secret` dưới phần `carebridge.security.jwt` bằng cách bỏ fallback string:
    ```yaml
    carebridge:
      security:
        jwt:
          secret: ${JWT_SECRET}
    ```
*   Đảm bảo rằng biến môi trường `JWT_SECRET` đã được cấu hình trong tệp `.env` của thư mục `CareBridgeAPI`:
    ```env
    JWT_SECRET=d223dab3-a116-4539-b280-d42483b293ad
    ```

### Tiêu chí nghiệm thu (Acceptance Criteria)
*   Khi khởi chạy ứng dụng Spring Boot local dev (sử dụng tệp `.env` chứa `JWT_SECRET`), ứng dụng khởi động bình thường.
*   Nếu tạm thời xóa hoặc đổi tên biến `JWT_SECRET` trong môi trường, ứng dụng sẽ lập tức crash khi khởi động và báo lỗi không thể phân giải thuộc tính `${JWT_SECRET}`.

