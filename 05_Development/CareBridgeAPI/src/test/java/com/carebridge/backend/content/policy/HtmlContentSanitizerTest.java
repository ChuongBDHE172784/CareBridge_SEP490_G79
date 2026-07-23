package com.carebridge.backend.content.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// RTE-TC-001..006 — see ContentRichTextEditor_Test-Spec.md §4
class HtmlContentSanitizerTest {

    private final HtmlContentSanitizer sanitizer = new HtmlContentSanitizer();

    // RTE-TC-001: HTML hợp lệ giữ nguyên sau sanitize
    @Test
    void sanitize_validFormattingHtml_keepsContent() {
        String input = "<p>Nội dung <strong>an toàn</strong></p>";

        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("<p>"));
        assertTrue(output.contains("<strong>"));
        assertTrue(output.contains("Nội dung"));
        assertTrue(output.contains("an toàn"));
    }

    // RTE-TC-002: <script> bị loại bỏ hoàn toàn
    @Test
    void sanitize_scriptTag_strippedCompletely() {
        String input = "<p>Xin chào</p><script>alert(document.cookie)</script>";

        String output = sanitizer.sanitize(input);

        assertFalse(output.toLowerCase().contains("<script"));
        assertFalse(output.contains("alert(document.cookie)"));
        assertTrue(output.contains("Xin chào"));
    }

    // RTE-TC-003: event handler attribute (onerror) bị loại bỏ
    @Test
    void sanitize_eventHandlerAttribute_stripped() {
        String input = "<img src=\"https://res.cloudinary.com/demo/x.jpg\" onerror=\"alert(1)\">";

        String output = sanitizer.sanitize(input);

        assertFalse(output.toLowerCase().contains("onerror"));
    }

    // RTE-TC-004: src="javascript:..." bị loại bỏ
    @Test
    void sanitize_javascriptUrlInImgSrc_stripped() {
        String input = "<img src=\"javascript:alert(1)\">";

        String output = sanitizer.sanitize(input);

        assertFalse(output.toLowerCase().contains("javascript:"));
    }

    // RTE-TC-004 (data: URL biến thể khác của cùng lớp tấn công)
    @Test
    void sanitize_dataUrlInImgSrc_stripped() {
        String input = "<img src=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==\">";

        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("data:text/html"));
    }

    // RTE-TC-005: <img src="https://res.cloudinary.com/..."> hợp lệ giữ nguyên
    @Test
    void sanitize_cloudinaryImageSrc_keptIntact() {
        String input = "<img src=\"https://res.cloudinary.com/demo/image/upload/v1/carebridge/abc.jpg\" alt=\"minh hoạ\">";

        String output = sanitizer.sanitize(input);

        assertTrue(output.contains("res.cloudinary.com"));
        assertTrue(output.contains("carebridge/abc.jpg"));
        assertTrue(output.contains("minh hoạ"));
    }

    // Non-Cloudinary host bị loại — theo TDS §11.4 (đề xuất mặc định: chỉ cho phép host Cloudinary)
    @Test
    void sanitize_nonCloudinaryImageSrc_stripped() {
        String input = "<img src=\"https://evil.example.com/tracker.png\" alt=\"x\">";

        String output = sanitizer.sanitize(input);

        assertFalse(output.contains("evil.example.com"));
    }

    // RTE-TC-006: CSS property allowlist — giữ color/font-size, loại position
    @Test
    void sanitize_styleAttribute_keepsAllowedPropertiesStripsPosition() {
        String input = "<span style=\"font-size:18px;color:#ff0000;position:fixed;top:0\">Test</span>";

        String output = sanitizer.sanitize(input);
        String style = extractStyleAttribute(output);

        assertTrue(style.toLowerCase().contains("font-size"));
        assertTrue(style.contains("18px"));
        assertTrue(style.toLowerCase().contains("color"));
        assertFalse(style.toLowerCase().contains("position"));
    }

    // font-family cũng phải được giữ (yêu cầu người dùng: "font")
    @Test
    void sanitize_fontFamilyStyle_kept() {
        String input = "<span style=\"font-family:Georgia, serif\">Test</span>";

        String output = sanitizer.sanitize(input);
        String style = extractStyleAttribute(output);

        assertTrue(style.toLowerCase().contains("font-family"));
    }

    // null an toàn — body có thể null khi tạo content mà không kèm nội dung (vd CHECKLIST)
    @Test
    void sanitize_nullInput_returnsNull() {
        assertNull(sanitizer.sanitize(null));
    }

    // Không so khớp chuỗi cứng cho style — parse attribute trước khi assert (per Test-Spec §4 RTE-TC-006 note)
    private static String extractStyleAttribute(String html) {
        int start = html.indexOf("style=\"");
        assertTrue(start >= 0, "expected a style attribute in: " + html);
        int valueStart = start + "style=\"".length();
        int end = html.indexOf('"', valueStart);
        return html.substring(valueStart, end);
    }
}
