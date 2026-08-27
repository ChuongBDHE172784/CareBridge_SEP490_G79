package com.carebridge.backend.community.dto.request;

import com.carebridge.backend.community.entity.PregnancyStage;
import com.carebridge.backend.community.entity.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;
import java.util.List;

/**
 * DTO tiếp nhận dữ liệu yêu cầu tạo câu hỏi cộng đồng mới từ người dùng (Role: MOTHER, FAMILY).
 */
@Data
public class CreateCommunityQuestionRequest {

    // ID của chủ đề (Topic) mà câu hỏi thuộc về (Bắt buộc; phải là TOPIC con, không phải CATEGORY cha và không bị ẩn)
    @NotNull(message = "topicId is required")
    private UUID topicId;

    // Tiêu đề ngắn gọn của câu hỏi (Bắt buộc, độ dài từ 5 đến 255 ký tự)
    @NotBlank(message = "title is required")
    @Size(min = 5, max = 255, message = "title must be between 5 and 255 characters")
    private String title;

    // Nội dung chi tiết của câu hỏi/triệu chứng cần tư vấn (Bắt buộc, độ dài từ 10 đến 5000 ký tự)
    @NotBlank(message = "body is required")
    @Size(min = 10, max = 5000, message = "body must be between 10 and 5000 characters")
    private String body;

    // Danh sách URL hình ảnh đính kèm (Tối đa 3 ảnh, bắt buộc phải lưu trên Cloudinary và do chính user upload)
    @Size(max = 3, message = "imageUrls must contain at most 3 images")
    private List<@NotBlank @Pattern(
            regexp = "^https://res\\.cloudinary\\.com/.+",
            message = "image URL must be hosted by Cloudinary") String> imageUrls;

    // Giai đoạn thai kỳ hoặc nuôi con (PLANNING, PREGNANT, POSTPARTUM, PARENTING)
    @NotNull(message = "stage is required")
    private PregnancyStage stage;

    // Tuần thai hiện tại của mẹ bầu (Áp dụng khi đang mang thai, từ tuần 1 đến tuần 42)
    @Min(value = 1, message = "pregnancyWeek must be >= 1")
    @Max(value = 42, message = "pregnancyWeek must be <= 42")
    private Integer pregnancyWeek;

    // Tuổi của bé tính theo tháng (Áp dụng giai đoạn sau sinh/nuôi con, từ 0 đến 72 tháng = 6 tuổi)
    @Min(value = 0, message = "babyAgeMonths must be >= 0")
    @Max(value = 72, message = "babyAgeMonths must be <= 72")
    private Integer babyAgeMonths;

    // Mức độ khẩn cấp của câu hỏi (LOW, NORMAL, HIGH, URGENT)
    @NotNull(message = "urgency is required")
    private UrgencyLevel urgency;

    // Tùy chọn đăng ẩn danh: Nếu true, danh tính tác giả sẽ bị che khi hiển thị cho cộng đồng (ADR-COM-002)
    @NotNull(message = "isAnonymous is required")
    private Boolean isAnonymous;
}
