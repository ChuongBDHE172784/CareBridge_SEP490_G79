package com.carebridge.backend.expertavailability.repository;

import com.carebridge.backend.expertavailability.availabilitystatus.AvailabilityStatus;
import com.carebridge.backend.expertavailability.entity.ExpertAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface ExpertAvailabilityRepository extends JpaRepository<ExpertAvailability, UUID> {
    List<ExpertAvailability> findByExpertProfileId(UUID expertProfileId);
    List<ExpertAvailability> findByExpertProfileIdAndStartAtGreaterThanEqualAndStartAtLessThan(
            UUID expertProfileId, Instant startAt, Instant endAt);
    List<ExpertAvailability> findByExpertProfileIdAndEndAtAfterOrderByStartAtAsc(
            UUID expertProfileId, Instant after);
    boolean existsByExpertProfileIdAndStartAtAndEndAtAndStatus(
            UUID expertProfileId,
            Instant startAt,
            Instant endAt,
            AvailabilityStatus status);
    @Query(value = "SELECT EXISTS (SELECT 1 FROM consultation_bookings WHERE availability_id = :availabilityId)", nativeQuery = true)
    boolean isReferencedByBooking(@Param("availabilityId") UUID availabilityId);
    Optional<ExpertAvailability> findTopByExpertProfileIdOrderByCreatedAtDesc(UUID expertProfileId);

    /**
     * Ai trong nhóm này còn ít nhất một ca trống sắp tới. Danh sách chuyên gia dùng nó
     * để gắn nhãn rảnh/bận cho cả trang bằng một truy vấn, thay vì hỏi từng người.
     *
     * <p>Cùng định nghĩa "còn trống" mà searchDirectory dùng để xếp thứ tự, nên nhãn
     * hiển thị không bao giờ mâu thuẫn với thứ tự sắp xếp.
     */
    @Query(value = """
            SELECT DISTINCT a.user_id FROM expert_availability a
            WHERE a.user_id IN (:expertProfileIds)
              AND a.status = 'AVAILABLE'
              AND a.start_at > CURRENT_TIMESTAMP
              AND NOT EXISTS (
                  SELECT 1 FROM expert_consultation_requests r
                  WHERE r.expert_profile_id = a.user_id
                    AND r.status IN ('PENDING', 'ACCEPTED')
                    AND r.preferred_window_start = a.start_at
                    AND r.preferred_window_end = a.end_at)
            """, nativeQuery = true)
    List<UUID> findExpertProfileIdsWithOpenSlot(
            @Param("expertProfileIds") java.util.Collection<UUID> expertProfileIds);

    /**
     * Cac ca mot me thuc su dat duoc: con AVAILABLE, chua toi gio, va chua co yeu cau
     * PENDING/ACCEPTED nao giu cho. O trang thai cua slot chi doi khi chuyen gia xu ly
     * yeu cau, nen neu chi loc theo status thi hai me van chon trung mot khung gio.
     */
    @Query(value = """
            SELECT a.* FROM expert_availability a
            WHERE a.user_id = :expertProfileId
              AND a.status = 'AVAILABLE'
              AND a.start_at > CURRENT_TIMESTAMP
              AND NOT EXISTS (
                  SELECT 1 FROM expert_consultation_requests r
                  WHERE r.expert_profile_id = a.user_id
                    AND r.status IN ('PENDING', 'ACCEPTED')
                    AND r.preferred_window_start = a.start_at
                    AND r.preferred_window_end = a.end_at)
            ORDER BY a.start_at
            """, nativeQuery = true)
    List<ExpertAvailability> findBookableSlotsForExpert(
            @Param("expertProfileId") UUID expertProfileId);

    /**
     * Quét ngang toàn bộ ca CÓ THỂ ĐẶT của mọi chuyên gia đủ điều kiện, phục vụ hàm vét điều phối.
     *
     * <p>Bốn điều kiện tài khoản lấy nguyên từ truy vấn danh bạ — thiếu một cái là lọt chuyên gia
     * đang bị đình chỉ vào danh sách gợi ý tự động.
     *
     * <p>{@code start_at > :now} chứ không phải {@code end_at > :now}: ca đang chạy dở không đặt
     * được, vì {@code validatePreferredAvailability} bắt buộc {@code start.isAfter(now)}.
     *
     * <p>NOT EXISTS loại ca đã có yêu cầu còn sống (PENDING/ACCEPTED). Đây là cách tránh trùng lịch
     * mà không cần đụng vào luồng accept: ca đã có người hỏi thì không gợi ý cho người thứ hai nữa.
     */
    @Query(value = """
            SELECT a.* FROM expert_availability a
            JOIN users u ON u.user_id = a.user_id
            WHERE u.role = 'EXPERT'
              AND u.verification_status = 'APPROVED'
              AND u.trust_status = 'ACTIVE'
              AND u.enabled = true AND u.locked = false
              AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
                   OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
              AND a.status = 'AVAILABLE'
              AND a.start_at > :notBefore
              AND a.start_at < :notAfter
              AND (CAST(:specialty AS text) IS NULL
                   OR LOWER(u.specialty) = LOWER(CAST(:specialty AS text))
                   OR EXISTS (
                       SELECT 1 FROM professional_specialties ps
                       JOIN specialties s ON s.specialty_id = ps.specialty_id
                       WHERE ps.professional_profile_id = u.user_id
                         AND s.is_active = true
                         AND (LOWER(s.code) = LOWER(CAST(:specialty AS text))
                              OR LOWER(s.name) = LOWER(CAST(:specialty AS text)))))
              AND NOT EXISTS (
                   SELECT 1 FROM expert_consultation_requests r
                   WHERE r.expert_profile_id = a.user_id
                     AND r.status IN ('PENDING', 'ACCEPTED')
                     AND r.preferred_window_start = a.start_at
                     AND r.preferred_window_end = a.end_at)
            ORDER BY a.user_id, a.start_at
            """, nativeQuery = true)
    List<ExpertAvailability> findBookableSlots(
            @Param("notBefore") Instant notBefore,
            @Param("notAfter") Instant notAfter,
            @Param("specialty") String specialty);
}
