package com.carebridge.backend.expertcontract.service;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.experttype.ExpertType;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import com.carebridge.backend.expertcontract.entity.ExpertContractAcceptance;
import com.carebridge.backend.expertcontract.repository.ExpertContractAcceptanceRepository;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.reviewstatus.ReviewStatus;
import com.carebridge.backend.file.dto.UploadFileResponse;
import com.carebridge.backend.file.enums.FilePurpose;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.security.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Vòng đời Thoả thuận hợp tác chuyên gia — xem docs/expert-two-tier-flow.md §6.
 *
 * <p>Bất biến quan trọng nhất của cả phân hệ nằm ở đây: {@link ExpertType#CONTRACTED} chỉ được
 * ghi bởi {@link #accept}, tức chỉ đạt được qua hành vi ký của chính chuyên gia. Admin không có
 * đường nào đặt thẳng trạng thái đó.
 *
 * <p>DTO và bộ sinh PDF nằm lồng trong lớp này thay vì tách file riêng: cả ba DTO đều chỉ phục vụ
 * đúng hai endpoint của service, và {@link PdfRenderer} không có trạng thái nên không cần là bean.
 */
@Service
@RequiredArgsConstructor
public class ExpertContractService {

    public static final String TERMS_VERSION = "v1.0";
    private static final String TEMPLATE_PATH = "contracts/expert-contract-" + TERMS_VERSION + ".md";
    private static final int DEFAULT_TERM_MONTHS = 12;
    private static final int DEFAULT_MIN_SLOTS_PER_WEEK = 10;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ExpertProfileRepository expertProfileRepository;
    private final ExpertCredentialRepository credentialRepository;
    private final ExpertContractAcceptanceRepository acceptanceRepository;
    private final UserRepository userRepository;
    private final IFileService fileService;

    // ── DTO ────────────────────────────────────────────────────────────

    /** Bản đề nghị hợp tác gửi cho chuyên gia xem trước khi ký. */
    public record Offer(
            String termsVersion,
            /** SHA-256 của nội dung đề nghị; client phải gửi lại nguyên vẹn khi chấp nhận. */
            String termsHash,
            /** Toàn văn đã điền dữ liệu đã xác minh, để render trực tiếp trên trang ký. */
            String content,
            String contractNumber,
            String issuedDate,
            int termMonths,
            int minSlotsPerWeek,
            /** Họ tên trên hồ sơ đã duyệt — trang ký yêu cầu gõ lại đúng chuỗi này. */
            String expectedFullName,
            boolean alreadyAccepted) {}

    /**
     * IP và User-Agent KHÔNG nằm trong record này một cách có chủ ý — server tự đọc từ request.
     * Client gửi lên được thì bằng chứng đồng thuận mất giá trị.
     */
    public record AcceptRequest(
            @NotBlank @Size(max = 20) String termsVersion,
            @NotBlank @Size(max = 100) String termsHash,
            @NotBlank @Size(max = 200) String acceptedFullName) {}

    public record Acceptance(
            UUID acceptanceId,
            UUID contractFileId,
            String termsVersion,
            String termsHash,
            LocalDateTime acceptedAt,
            String expertType) {}

    // ── Đề nghị ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Offer getOffer(UUID expertUserId) {
        ExpertProfile profile = expertProfileRepository.findById(expertUserId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-003", "Expert profile not found"));
        assertOfferOpen(profile);
        Map<String, String> values = offerValues(profile);
        String content = fill(loadTemplate(), values);
        return new Offer(
                TERMS_VERSION,
                sha256(content),
                content,
                values.get("contractNumber"),
                values.get("issuedDate"),
                DEFAULT_TERM_MONTHS,
                DEFAULT_MIN_SLOTS_PER_WEEK,
                values.get("expertFullName"),
                profile.isContracted());
    }

    /**
     * Bản xem trước cho admin, dành cho lúc đang cân nhắc có phát hành đề nghị hay không.
     *
     * <p>Khác {@link #getOffer}: KHÔNG kiểm trạng thái hồ sơ, vì ở thời điểm admin đọc thì chuyên
     * gia còn đang PENDING và chưa được phát hành đề nghị nào. Chỉ đọc, không ghi gì.
     */
    @Transactional(readOnly = true)
    public Offer previewFor(UUID expertProfileId) {
        ExpertProfile profile = expertProfileRepository.findById(expertProfileId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-003", "Expert profile not found"));
        Map<String, String> values = offerValues(profile);
        String content = fill(loadTemplate(), values);
        return new Offer(
                TERMS_VERSION,
                sha256(content),
                content,
                values.get("contractNumber"),
                values.get("issuedDate"),
                DEFAULT_TERM_MONTHS,
                DEFAULT_MIN_SLOTS_PER_WEEK,
                values.get("expertFullName"),
                profile.isContracted());
    }

    // ── Chấp nhận ──────────────────────────────────────────────────────

    @Transactional
    public Acceptance accept(
            UUID expertUserId, AcceptRequest request, String ipAddress, String userAgent) {

        ExpertProfile profile = expertProfileRepository.findByIdForUpdate(expertUserId)
                .orElseThrow(() -> new ExpertException(
                        HttpStatus.NOT_FOUND, "EXPERT-003", "Expert profile not found"));
        assertOfferOpen(profile);

        if (!TERMS_VERSION.equals(request.termsVersion())) {
            throw new ExpertException(HttpStatus.CONFLICT, "EXPERT-CONTRACT-VERSION-STALE",
                    "Điều khoản đã được cập nhật, vui lòng tải lại bản mới nhất");
        }

        Map<String, String> values = offerValues(profile);
        String template = loadTemplate();
        String expectedHash = sha256(fill(template, values));
        // Chặn kịch bản: chuyên gia mở trang → admin sửa điều khoản → chuyên gia tick đồng ý,
        // và hệ thống ghi nhận họ đã đồng ý một bản họ chưa từng thấy.
        if (!expectedHash.equalsIgnoreCase(request.termsHash())) {
            throw new ExpertException(HttpStatus.CONFLICT, "EXPERT-CONTRACT-CONTENT-CHANGED",
                    "Nội dung thoả thuận đã thay đổi, vui lòng đọc lại trước khi chấp nhận");
        }

        if (!namesMatch(values.get("expertFullName"), request.acceptedFullName())) {
            throw new ExpertException(HttpStatus.BAD_REQUEST, "EXPERT-CONTRACT-NAME-MISMATCH",
                    "Họ tên xác nhận phải trùng với họ tên trên hồ sơ đã được duyệt");
        }

        LocalDateTime acceptedAt = LocalDateTime.now();
        Map<String, String> signed = new LinkedHashMap<>(values);
        signed.put("acceptedFullName", request.acceptedFullName().trim());
        signed.put("acceptedAt", acceptedAt.format(TIMESTAMP));
        signed.put("ipAddress", blankToDash(ipAddress));
        signed.put("userAgent", truncate(blankToDash(userAgent), 200));
        signed.put("termsHash", expectedHash);

        byte[] pdf = PdfRenderer.render(fill(template, signed));
        UploadFileResponse stored = fileService.uploadPrivateBytes(
                pdf, expertUserId, "application/pdf",
                "thoa-thuan-hop-tac-" + TERMS_VERSION + ".pdf", FilePurpose.EXPERT_CONTRACT);

        Map<String, Object> payload = new HashMap<>();
        payload.put("termsVersion", TERMS_VERSION);
        payload.put("termsHash", expectedHash);
        payload.put("acceptedFullName", signed.get("acceptedFullName"));
        payload.put("contractNumber", values.get("contractNumber"));
        payload.put("termMonths", DEFAULT_TERM_MONTHS);
        payload.put("minSlotsPerWeek", DEFAULT_MIN_SLOTS_PER_WEEK);

        ExpertContractAcceptance acceptance = acceptanceRepository.save(
                ExpertContractAcceptance.builder()
                        .expertUserId(expertUserId)
                        .decision(ExpertContractAcceptance.DECISION_ACCEPTED)
                        .contractFileId(stored.getFileId())
                        .ipAddress(ipAddress)
                        .userAgent(truncate(userAgent, 500))
                        .payload(payload)
                        .build());

        profile.setExpertType(ExpertType.CONTRACTED);
        expertProfileRepository.save(profile);

        return new Acceptance(
                acceptance.getId(),
                stored.getFileId(),
                TERMS_VERSION,
                expectedHash,
                acceptance.getOccurredAt() != null ? acceptance.getOccurredAt() : acceptedAt,
                ExpertType.CONTRACTED.name());
    }

    // ── Nội bộ ─────────────────────────────────────────────────────────

    private void assertOfferOpen(ExpertProfile profile) {
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new ExpertException(HttpStatus.CONFLICT, "EXPERT-CONTRACT-NOT-APPROVED",
                    "Hồ sơ chuyên môn phải được duyệt trước khi ký Thoả thuận");
        }
        if (profile.isContracted()) {
            throw new ExpertException(HttpStatus.CONFLICT, "EXPERT-CONTRACT-ALREADY-SIGNED",
                    "Chuyên gia đã ký Thoả thuận hợp tác");
        }
        if (!profile.isAwaitingContractSignature()) {
            throw new ExpertException(HttpStatus.CONFLICT, "EXPERT-CONTRACT-NOT-OFFERED",
                    "Chưa có đề nghị hợp tác nào dành cho tài khoản này");
        }
    }

    private Map<String, String> offerValues(ExpertProfile profile) {
        String fullName = userRepository.findById(profile.getUserId())
                .map(user -> user.getName())
                .orElse(null);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("contractNumber", contractNumber(profile.getExpertProfileId()));
        values.put("termsVersion", TERMS_VERSION);
        values.put("issuedDate", LocalDate.now().format(DATE));
        values.put("expertFullName", blankToDash(fullName));
        values.put("professionalTitle", blankToDash(profile.getProfessionalTitle()));
        values.put("specialty", blankToDash(profile.getSpecialty()));
        values.put("licenseNumber", blankToDash(licenseNumber(profile.getExpertProfileId())));
        values.put("workplace", blankToDash(profile.getWorkplace()));
        values.put("termMonths", String.valueOf(DEFAULT_TERM_MONTHS));
        values.put("minSlotsPerWeek", String.valueOf(DEFAULT_MIN_SLOTS_PER_WEEK));
        // Khối xác nhận để trống ở bản đề nghị; hash tính trên chính bản này.
        values.put("acceptedFullName", "................................");
        values.put("acceptedAt", "................................");
        values.put("ipAddress", "................................");
        values.put("userAgent", "................................");
        values.put("termsHash", "................................");
        return values;
    }

    /** Số giấy phép lấy từ credential nghề nghiệp đã được duyệt, không lấy từ lời tự khai. */
    private String licenseNumber(UUID expertProfileId) {
        return credentialRepository
                .findByExpertProfileIdAndReviewStatus(expertProfileId, ReviewStatus.APPROVED)
                .stream()
                .filter(credential -> credential.getCredentialType() != null
                        && !credential.getCredentialType().startsWith("IDENTITY_"))
                .map(ExpertCredential::getCredentialNumber)
                .filter(number -> number != null && !number.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String contractNumber(UUID expertProfileId) {
        return "CB-%d-%s".formatted(
                LocalDate.now().getYear(),
                expertProfileId.toString().substring(0, 8).toUpperCase(Locale.ROOT));
    }

    private static String loadTemplate() {
        try (InputStream in = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được mẫu thoả thuận " + TEMPLATE_PATH, e);
        }
    }

    private static String fill(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                  .append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    /** So khớp bỏ qua dấu, hoa thường và khoảng trắng thừa — gõ thiếu dấu vẫn là chủ ý hợp lệ. */
    private static boolean namesMatch(String expected, String provided) {
        return expected != null && provided != null
                && normalize(expected).equals(normalize(provided));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String blankToDash(String value) {
        return (value == null || value.isBlank()) ? "..............." : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    // ── Sinh PDF ───────────────────────────────────────────────────────

    /**
     * Dùng Noto Sans nhúng (SIL OFL) thay vì 14 font chuẩn của PDF: WinAnsiEncoding của bộ font
     * chuẩn không có dấu tiếng Việt, ký tự sẽ rơi hết thành ô vuông hoặc ném lỗi encode.
     *
     * <p>Không trạng thái nên để static, không cần đăng ký thành bean.
     */
    static final class PdfRenderer {

        private static final String FONT_REGULAR = "fonts/NotoSans-Regular.ttf";
        private static final String FONT_BOLD = "fonts/NotoSans-Bold.ttf";

        private static final float MARGIN = 56f;          // ~2cm
        private static final float FONT_SIZE = 10.5f;
        private static final float TITLE_SIZE = 14f;
        private static final float LEADING = 15f;

        private PdfRenderer() {}

        static byte[] render(String documentText) {
            try (PDDocument document = new PDDocument();
                    ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                PDType0Font regular = loadFont(document, FONT_REGULAR);
                PDType0Font bold = loadFont(document, FONT_BOLD);

                float contentWidth = PDRectangle.A4.getWidth() - (2 * MARGIN);
                Cursor cursor = new Cursor(document);
                try {
                    boolean firstLine = true;
                    for (String rawLine : documentText.replace("\r\n", "\n").split("\n", -1)) {
                        String line = rawLine.stripTrailing();
                        if (line.isEmpty()) {
                            cursor.advance(LEADING * 0.6f);
                            continue;
                        }
                        boolean heading = isHeading(line);
                        PDType0Font font = heading ? bold : regular;
                        float size = firstLine ? TITLE_SIZE : FONT_SIZE;
                        for (String piece : wrap(sanitize(line), font, size, contentWidth)) {
                            cursor.write(piece, font, size);
                        }
                        if (heading) {
                            cursor.advance(LEADING * 0.3f);
                        }
                        firstLine = false;
                    }
                } finally {
                    cursor.close();
                }

                document.save(out);
                return out.toByteArray();
            } catch (IOException e) {
                throw new IllegalStateException("Không sinh được PDF thoả thuận: " + e.getMessage(), e);
            }
        }

        private static boolean isHeading(String line) {
            return line.startsWith("ĐIỀU ")
                    || line.startsWith("THOẢ THUẬN")
                    || line.startsWith("BÊN A")
                    || line.startsWith("BÊN B")
                    || line.startsWith("XÁC NHẬN");
        }

        private static PDType0Font loadFont(PDDocument document, String path) throws IOException {
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                return PDType0Font.load(document, in);
            }
        }

        /** Bỏ ký tự font không encode được để showText() không ném giữa chừng. */
        private static String sanitize(String text) {
            StringBuilder sb = new StringBuilder(text.length());
            text.codePoints().forEach(cp -> {
                if (cp == '\t') {
                    sb.append("    ");
                } else if (cp >= 32) {
                    sb.appendCodePoint(cp);
                }
            });
            return sb.toString();
        }

        private static List<String> wrap(String line, PDType0Font font, float size, float maxWidth)
                throws IOException {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (String word : line.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(font, size, candidate) <= maxWidth || current.isEmpty()) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            lines.add(current.toString());
            return lines;
        }

        private static float width(PDType0Font font, float size, String text) throws IOException {
            return font.getStringWidth(text) / 1000f * size;
        }

        /** Giữ trang hiện tại + vị trí con trỏ, tự sang trang mới khi chạm lề dưới. */
        private static final class Cursor {
            private final PDDocument document;
            private PDPage page;
            private PDPageContentStream stream;
            private float y;

            Cursor(PDDocument document) throws IOException {
                this.document = document;
                newPage();
            }

            private void newPage() throws IOException {
                if (stream != null) {
                    stream.close();
                }
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                stream = new PDPageContentStream(document, page);
                y = page.getMediaBox().getHeight() - MARGIN;
            }

            void write(String text, PDType0Font font, float size) throws IOException {
                if (y <= MARGIN) {
                    newPage();
                }
                stream.beginText();
                stream.setFont(font, size);
                stream.newLineAtOffset(MARGIN, y);
                stream.showText(text);
                stream.endText();
                y -= LEADING;
            }

            void advance(float amount) throws IOException {
                y -= amount;
                if (y <= MARGIN) {
                    newPage();
                }
            }

            void close() throws IOException {
                if (stream != null) {
                    stream.close();
                    stream = null;
                }
            }
        }
    }
}
