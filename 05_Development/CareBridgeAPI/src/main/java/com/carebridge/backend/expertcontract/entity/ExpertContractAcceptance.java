package com.carebridge.backend.expertcontract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Bằng chứng vòng đời Thoả thuận hợp tác chuyên gia, lưu trên {@code audit_events} theo đúng
 * pattern của {@code ContributionPoint} — không tạo bảng mới.
 *
 * <p>Bảng audit_events đã có sẵn đúng bộ field mà một bằng chứng đồng thuận cần:
 * {@code actor_user_id}, {@code ip_address}, {@code user_agent}, {@code occurred_at},
 * {@code decision}, {@code payload}. Cột {@code expert_type} trên users giữ TRẠNG THÁI HIỆN TẠI;
 * bảng này giữ LỊCH SỬ.
 *
 * <p>Ba loại row, phân biệt bằng {@link #decision}:
 * <ul>
 *   <li>{@code REQUESTED} — chuyên gia chọn hình thức hợp tác ở bước 2 onboarding</li>
 *   <li>{@code ACCEPTED} — chuyên gia bấm đồng ý ở trang ký; đây là bản có giá trị pháp lý</li>
 *   <li>{@code TERMINATED} — admin hạ khỏi nhóm hợp tác; bản ACCEPTED cũ giữ nguyên</li>
 * </ul>
 */
@Entity
@Table(name = "audit_events")
@SQLRestriction("event_category = 'EXPERT_CONTRACT'")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertContractAcceptance {

    public static final String DECISION_REQUESTED = "REQUESTED";
    public static final String DECISION_ACCEPTED = "ACCEPTED";
    public static final String DECISION_TERMINATED = "TERMINATED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_event_id", nullable = false, updatable = false)
    private UUID id;

    /** Chuyên gia là chủ thể của hành vi ký. */
    @Column(name = "actor_user_id", nullable = false)
    private UUID expertUserId;

    /** REQUESTED | ACCEPTED | TERMINATED */
    @Column(name = "decision", length = 50)
    private String decision;

    /** attachments.attachment_id của bản PDF đã ký (chỉ có ở row ACCEPTED). */
    @Column(name = "resource_id")
    private UUID contractFileId;

    @Builder.Default
    @Column(name = "resource_type", length = 100)
    private String resourceType = "ExpertContract";

    // Server tự đọc từ request — không bao giờ nhận từ client, nếu không bằng chứng vô giá trị.
    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Builder.Default
    @Column(name = "event_category", nullable = false, length = 80)
    private String eventCategory = "EXPERT_CONTRACT";

    @Builder.Default
    @Column(name = "event_origin", length = 255)
    private String eventOrigin = "EXPERT_CONTRACT";

    /** { termsVersion, termsHash, acceptedFullName, termMonths, reason… } */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @PrePersist
    void applyCanonicalCategory() {
        eventCategory = "EXPERT_CONTRACT";
        eventOrigin = "EXPERT_CONTRACT";
        if (resourceType == null || resourceType.isBlank()) {
            resourceType = "ExpertContract";
        }
        if (payload == null) {
            payload = new HashMap<>();
        }
    }

    public String payloadText(String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? null : value.toString();
    }
}
