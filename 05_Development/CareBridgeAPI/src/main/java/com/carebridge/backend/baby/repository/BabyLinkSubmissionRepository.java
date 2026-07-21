package com.carebridge.backend.baby.repository;

import com.carebridge.backend.baby.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface BabyLinkSubmissionRepository extends JpaRepository<BabyLinkSubmission, UUID> {
    @Query(value="select 1 from pg_advisory_xact_lock(hashtextextended(:key, 65))", nativeQuery=true)
    Integer acquireTransactionLock(@Param("key") String key);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BabyLinkSubmission s where s.ownerUserId=:owner and s.operationType=:operation and s.submissionId=:submission")
    Optional<BabyLinkSubmission> findForUpdate(@Param("owner") UUID owner, @Param("operation") BabyLinkOperation operation, @Param("submission") UUID submission);
}
