package com.carebridge.backend.expert.repository;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.verificationstatus.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {

 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.expertProfileId = :userId")
 Optional<ExpertProfile> findByUserId(@Param("userId") UUID userId);

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.expertProfileId = :id")
 Optional<ExpertProfile> findByIdForUpdate(@Param("id") UUID id);

 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.expertProfileId = :userId")
 Optional<ExpertProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

 @Query("SELECT (COUNT(ep) > 0) FROM ExpertProfile ep WHERE ep.expertProfileId = :userId")
 boolean existsByUserId(@Param("userId") UUID userId);

 List<ExpertProfile> findByVerificationStatus(VerificationStatus status);

 // Native: User.suspendedUntil is @Transient (canonically stored in users.settings_jsonb),
 // so the not-currently-suspended predicate follows the UserRepository settings_jsonb pattern.
 @Query(value = """
     SELECT u.* FROM users u
     WHERE u.role = 'EXPERT'
       AND u.verification_status = 'APPROVED'
       AND u.trust_status = 'ACTIVE'
       AND u.enabled = true AND u.locked = false
       AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
            OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
     ORDER BY u.rating_avg DESC NULLS LAST
     """, nativeQuery = true)
 List<ExpertProfile> findVerifiedPublic();

 @Query("SELECT ep FROM ExpertProfile ep JOIN FETCH ep.user u " +
        "WHERE (:search IS NULL OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
        "OR LOWER(ep.specialty) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
        "AND (COALESCE(:statuses, NULL) IS NULL OR ep.verificationStatus IN :statuses)")
 Page<ExpertProfile> findForReview(@Param("search") String search, @Param("statuses") java.util.List<VerificationStatus> statuses, Pageable pageable);

 @Query(value = """
     SELECT u.* FROM users u
     WHERE u.role = 'EXPERT'
       AND u.verification_status = 'APPROVED'
       AND u.trust_status = 'ACTIVE'
       AND u.enabled = true AND u.locked = false
       AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
            OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
       AND (CAST(:specialty AS text) IS NULL OR EXISTS (
            SELECT 1 FROM professional_specialties ps
            JOIN specialties s ON s.specialty_id = ps.specialty_id
            WHERE ps.professional_profile_id = u.user_id
              AND s.is_active = true
              AND (LOWER(s.code) = LOWER(CAST(:specialty AS text))
                   OR LOWER(s.name) = LOWER(CAST(:specialty AS text)))))
     """, nativeQuery = true)
 List<ExpertProfile> findVerifiedBySpecialty(@Param("specialty") String specialty);

 @Query("SELECT ep FROM ExpertProfile ep WHERE ep.expertProfileId IN :userIds")
 List<ExpertProfile> findByUserIdIn(@Param("userIds") Set<UUID> userIds);

 // ADR-MEDI-001 mục 1-2 — real pagination (Pageable actually applied, unlike
 // findVerifiedPublic/findVerifiedBySpecialty above) + optional case-insensitive text search
 // across canonical users.full_name / professional_title / workplace.
  @Query(value = """
      SELECT u.* FROM users u
      WHERE u.role = 'EXPERT'
        AND u.verification_status = 'APPROVED'
        AND u.trust_status = 'ACTIVE'
        AND u.enabled = true AND u.locked = false
        AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
             OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
        AND (CAST(:specialty AS text) IS NULL 
             OR LOWER(u.specialty) = LOWER(CAST(:specialty AS text))
             OR EXISTS (
                 SELECT 1 FROM professional_specialties ps
                 JOIN specialties s ON s.specialty_id = ps.specialty_id
                 WHERE ps.professional_profile_id = u.user_id
                   AND s.is_active = true
                   AND (LOWER(s.code) = LOWER(CAST(:specialty AS text))
                        OR LOWER(s.name) = LOWER(CAST(:specialty AS text)))))
        AND (CAST(:q AS text) IS NULL 
             OR LOWER(COALESCE(u.full_name, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.display_name, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.professional_title, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.workplace, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.specialty, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR EXISTS (
                 SELECT 1 FROM care_facilities cf
                 WHERE cf.facility_id = u.facility_id
                   AND LOWER(cf.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
             OR EXISTS (
                 SELECT 1 FROM professional_specialties ps2
                 JOIN specialties s2 ON s2.specialty_id = ps2.specialty_id
                 WHERE ps2.professional_profile_id = u.user_id
                   AND s2.is_active = true
                   AND (LOWER(s2.code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(s2.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))))
      ORDER BY CASE WHEN u.expert_type = 'CONTRACTED' THEN 0 ELSE 1 END,
               u.rating_avg DESC NULLS LAST, u.user_id ASC
      """,
      countQuery = """
      SELECT COUNT(*) FROM users u
      WHERE u.role = 'EXPERT'
        AND u.verification_status = 'APPROVED'
        AND u.trust_status = 'ACTIVE'
        AND u.enabled = true AND u.locked = false
        AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
             OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
        AND (CAST(:specialty AS text) IS NULL 
             OR LOWER(u.specialty) = LOWER(CAST(:specialty AS text))
             OR EXISTS (
                 SELECT 1 FROM professional_specialties ps
                 JOIN specialties s ON s.specialty_id = ps.specialty_id
                 WHERE ps.professional_profile_id = u.user_id
                   AND s.is_active = true
                   AND (LOWER(s.code) = LOWER(CAST(:specialty AS text))
                        OR LOWER(s.name) = LOWER(CAST(:specialty AS text)))))
        AND (CAST(:q AS text) IS NULL 
             OR LOWER(COALESCE(u.full_name, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.display_name, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.professional_title, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.workplace, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR LOWER(COALESCE(u.specialty, '')) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
             OR EXISTS (
                 SELECT 1 FROM care_facilities cf
                 WHERE cf.facility_id = u.facility_id
                   AND LOWER(cf.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))
             OR EXISTS (
                 SELECT 1 FROM professional_specialties ps2
                 JOIN specialties s2 ON s2.specialty_id = ps2.specialty_id
                 WHERE ps2.professional_profile_id = u.user_id
                   AND s2.is_active = true
                   AND (LOWER(s2.code) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%'))
                        OR LOWER(s2.name) LIKE LOWER(CONCAT('%', CAST(:q AS text), '%')))))
      """,
      nativeQuery = true)

 Page<ExpertProfile> searchDirectory(@Param("specialty") String specialty, @Param("q") String q, Pageable pageable);

 /**
  * Chuyên gia đủ điều kiện mà KHÔNG còn ca AVAILABLE nào trong tương lai — ứng viên "yêu cầu mở"
  * của hàm vét điều phối (docs/expert-matching-sweep.md §4).
  *
  * <p>Luật: đã set lịch nghĩa là đang tuyên bố khi nào mình rảnh, hệ thống tôn trọng tuyên bố đó và
  * chỉ cho đặt theo ca. Chưa set ca nào thì chưa tuyên bố gì, nên nhận yêu cầu mở bất cứ lúc nào.
  * Nhờ luật này, chuyên gia hợp đồng kín lịch vẫn rơi xuống được tuyến 2 đúng như đặc tả.
  */
 @Query(value = """
   SELECT u.* FROM users u
   WHERE u.role = 'EXPERT'
     AND u.verification_status = 'APPROVED'
     AND u.trust_status = 'ACTIVE'
     AND u.enabled = true AND u.locked = false
     AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
          OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
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
          SELECT 1 FROM expert_availability a
          WHERE a.user_id = u.user_id
            AND a.status = 'AVAILABLE'
            AND a.start_at > :now)
   """, nativeQuery = true)
 List<ExpertProfile> findOpenRequestExperts(
   @Param("specialty") String specialty, @Param("now") java.time.Instant now);

 @Query(value = """
     SELECT DISTINCT s.name
     FROM users u
     JOIN professional_specialties ps
       ON ps.professional_profile_id = u.user_id
     JOIN specialties s ON s.specialty_id = ps.specialty_id
     WHERE u.verification_status = 'APPROVED'
       AND u.trust_status = 'ACTIVE'
       AND u.enabled = true AND u.locked = false
       AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
       AND s.is_active = true
     ORDER BY s.name
     """, nativeQuery = true)
 List<String> findApprovedSpecialties();

 @Query(value = """
     SELECT after_payload_jsonb ->> 'reason'
     FROM audit_events
     WHERE event_origin = 'AUDIT_LOG'
       AND event_category = 'EXPERT_VERIFICATION'
       AND resource_type = 'ExpertProfile'
       AND resource_id = :expertProfileId
       AND after_payload_jsonb ->> 'decision' = 'REJECTED'
     ORDER BY occurred_at DESC, audit_event_id DESC
     LIMIT 1
     """, nativeQuery = true)
 Optional<String> findLatestProfileRejectionReason(@Param("expertProfileId") UUID expertProfileId);

 @Query(value = """
     SELECT u.* FROM users u
     WHERE u.role = 'EXPERT'
       AND u.expert_type = 'CONTRACTED'
       AND u.verification_status = 'APPROVED'
       AND u.trust_status = 'ACTIVE'
       AND u.enabled = true AND u.locked = false
       AND (nullif(u.settings_jsonb ->> 'suspendedUntil', '') IS NULL
            OR CAST(nullif(u.settings_jsonb ->> 'suspendedUntil', '') AS timestamptz) <= CURRENT_TIMESTAMP)
     ORDER BY u.rating_avg DESC NULLS LAST, u.user_id ASC
     """, nativeQuery = true)
 List<ExpertProfile> findActiveContractedExperts();
}
