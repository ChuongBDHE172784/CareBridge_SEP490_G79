package com.carebridge.backend.security.service.impl;

import com.carebridge.backend.security.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class GmailEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(GmailEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${carebridge.mail.from-address}")
    private String fromAddress;

    @Value("${carebridge.mail.from-name}")
    private String fromName;

    public GmailEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpVerificationEmail(String to, String otp, int expiryMinutes) {
        String subject = "Mã xác thực CareBridge của bạn";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#FFF8F6;border-radius:16px;">
                  <h2 style="color:#5A463F;margin-bottom:8px;">CareBridge</h2>
                  <p style="color:#524440;font-size:15px;">Mã xác thực của bạn là:</p>
                  <div style="background:#C98C7B;border-radius:12px;padding:24px;text-align:center;margin:24px 0;">
                    <span style="font-size:36px;font-weight:700;letter-spacing:12px;color:#fff;">%s</span>
                  </div>
                  <p style="color:#524440;font-size:13px;">Mã này có hiệu lực trong <strong>%d phút</strong>. Không chia sẻ mã này với bất kỳ ai.</p>
                  <hr style="border:none;border-top:1px solid #F2EAE4;margin:24px 0;">
                  <p style="color:#9C857C;font-size:12px;">Nếu bạn không yêu cầu mã này, hãy bỏ qua email này.</p>
                </div>
                """.formatted(otp, expiryMinutes);
        send(to, subject, html);
    }

    @Override
    public void sendRegistrationSuccessEmail(String to, String name) {
        String subject = "Chào mừng bạn đến với CareBridge!";
        String displayName = (name != null && !name.isBlank()) ? name : "bạn";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#FFF8F6;border-radius:16px;">
                  <h2 style="color:#5A463F;">Chào mừng đến với CareBridge 🌸</h2>
                  <p style="color:#524440;font-size:15px;">Xin chào <strong>%s</strong>,</p>
                  <p style="color:#524440;font-size:15px;">Tài khoản của bạn đã được kích hoạt thành công. Chúng tôi rất vui khi được đồng hành cùng bạn trên hành trình chăm sóc sức khỏe mẹ và bé.</p>
                  <hr style="border:none;border-top:1px solid #F2EAE4;margin:24px 0;">
                  <p style="color:#9C857C;font-size:12px;">Đội ngũ CareBridge</p>
                </div>
                """.formatted(displayName);
        send(to, subject, html);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token, int expiryMinutes) {
        String subject = "Đặt lại mật khẩu CareBridge";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#FFF8F6;border-radius:16px;">
                  <h2 style="color:#5A463F;">Đặt lại mật khẩu</h2>
                  <p style="color:#524440;font-size:15px;">Mã đặt lại mật khẩu của bạn là:</p>
                  <div style="background:#845143;border-radius:12px;padding:24px;text-align:center;margin:24px 0;">
                    <span style="font-size:28px;font-weight:700;letter-spacing:8px;color:#fff;">%s</span>
                  </div>
                  <p style="color:#524440;font-size:13px;">Mã này hết hạn sau <strong>%d phút</strong>.</p>
                  <hr style="border:none;border-top:1px solid #F2EAE4;margin:24px 0;">
                  <p style="color:#9C857C;font-size:12px;">Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>
                </div>
                """.formatted(token, expiryMinutes);
        send(to, subject, html);
    }

    @Override
    public void sendStaffAccountCredentialsEmail(String to, String name, String tempPassword) {
        String subject = "Tài khoản nhân sự CareBridge của bạn";
        String displayName = (name != null && !name.isBlank()) ? name : "bạn";
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;background:#FFF8F6;border-radius:16px;">
                  <h2 style="color:#5A463F;">Tài khoản nhân sự CareBridge</h2>
                  <p style="color:#524440;font-size:15px;">Xin chào <strong>%s</strong>,</p>
                  <p style="color:#524440;font-size:15px;">Tài khoản nhân sự của bạn đã được tạo. Mật khẩu tạm thời của bạn là:</p>
                  <div style="background:#845143;border-radius:12px;padding:24px;text-align:center;margin:24px 0;">
                    <span style="font-size:22px;font-weight:700;letter-spacing:2px;color:#fff;">%s</span>
                  </div>
                  <p style="color:#524440;font-size:13px;">Bạn sẽ được yêu cầu đổi mật khẩu ngay khi đăng nhập lần đầu.</p>
                  <hr style="border:none;border-top:1px solid #F2EAE4;margin:24px 0;">
                  <p style="color:#9C857C;font-size:12px;">Đội ngũ CareBridge</p>
                </div>
                """.formatted(displayName, tempPassword);
        send(to, subject, html);
    }

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            logger.info("[EMAIL] Sent '{}' to {}", subject, to);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            logger.error("[EMAIL] Failed to send to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }
}
