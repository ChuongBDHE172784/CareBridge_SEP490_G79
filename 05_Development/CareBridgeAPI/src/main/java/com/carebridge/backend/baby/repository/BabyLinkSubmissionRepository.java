package com.carebridge.backend.baby.repository;

import com.carebridge.backend.baby.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface BabyLinkSubmissionRepository extends JpaRepository<BabyLinkSubmission, UUID> {
    @Override
    @Query(value = "SELECT count(*) FROM mother_journey_events "
            + "WHERE legacy_source = 'BABY_LINK'", nativeQuery = true)
    long count();

    @Query(value="select 1 from pg_advisory_xact_lock(hashtextextended(:key, 65))", nativeQuery=true)
    Integer acquireTransactionLock(@Param("key") String key);
    @Query(value = """
            SELECT * FROM audit_events
             WHERE actor_user_id=:owner
               AND event_category=('BABY_LINK_' || :#{#operation.name()})
               AND payload->>'submissionId'=CAST(:submission AS text)
             ORDER BY occurred_at DESC LIMIT 1
            """, nativeQuery = true)
    Optional<BabyLinkSubmission> findForUpdate(@Param("owner") UUID owner, @Param("operation") BabyLinkOperation operation, @Param("submission") UUID submission);
}
