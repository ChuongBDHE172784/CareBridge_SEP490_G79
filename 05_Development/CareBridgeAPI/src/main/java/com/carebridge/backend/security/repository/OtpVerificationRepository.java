package com.carebridge.backend.security.repository;

import com.carebridge.backend.security.entity.OtpVerification;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM OtpVerification o WHERE o.subjectIdentifier=:phone AND o.purpose IN (com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.REGISTER,com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.LOGIN) AND o.usedAt IS NULL ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findTopByPhoneAndUsedAtIsNullOrderByCreatedAtDesc(String phone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM OtpVerification o WHERE o.subjectIdentifier=:email AND o.purpose IN (com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.REGISTER,com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.LOGIN) AND o.usedAt IS NULL ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT * FROM auth_challenges
            WHERE user_id = :userId
              AND challenge_type IN ('REGISTER', 'LOGIN')
              AND used_at IS NULL
            ORDER BY created_at DESC, challenge_id DESC
            LIMIT 1
            FOR UPDATE
            """, nativeQuery = true)
    Optional<OtpVerification> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM OtpVerification o WHERE o.subjectIdentifier=:phone AND o.purpose IN (com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.REGISTER,com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.LOGIN) AND o.status='PENDING' ORDER BY o.createdAt DESC LIMIT 1")
    Optional<OtpVerification> findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(String phone);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM OtpVerification o WHERE o.expiresAt<:expiresAt AND o.purpose IN (com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.REGISTER,com.carebridge.backend.security.entity.OtpVerification.OtpPurpose.LOGIN)")
    long deleteByExpiresAtBefore(Instant expiresAt);
}
