package com.carebridge.backend.family.repository;

import com.carebridge.backend.family.entity.CareTask;
import com.carebridge.backend.family.entity.CareTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("familyCareTaskRepository")
public interface CareTaskRepository extends JpaRepository<CareTask, UUID> {

    /** UC-73: list all tasks in a care group. */
    List<CareTask> findByCareGroupId(UUID careGroupId);

    /** Delete all tasks in a care group (used when group is hard-deleted). */
    void deleteByCareGroupId(UUID careGroupId);

    /** UC-73: look up a specific task scoped to a group (prevents cross-group access). */
    Optional<CareTask> findByIdAndCareGroupId(UUID id, UUID careGroupId);

    /** UC-74: calendar aggregation — tasks scheduled in [rangeStart, rangeEnd]. */
    List<CareTask> findByCareGroupIdAndDueAtBetween(UUID careGroupId, Instant rangeStart, Instant rangeEnd);
    @Modifying
    @Query("""
            UPDATE FamilyCareTask t
               SET t.assignedTo = :toUserId,
                   t.updatedAt = :now
             WHERE t.careGroupId = :groupId
               AND t.assignedTo = :fromUserId
               AND t.status IN :statuses
            """)
    int reassignIncompleteTasks(@Param("groupId") UUID groupId,
                                @Param("fromUserId") UUID fromUserId,
                                @Param("toUserId") UUID toUserId,
                                @Param("statuses") List<CareTaskStatus> statuses,
                                @Param("now") Instant now);

    default int reassignIncompleteTasks(UUID groupId, UUID fromUserId, UUID toUserId) {
        return reassignIncompleteTasks(groupId, fromUserId, toUserId,
                List.of(CareTaskStatus.OPEN, CareTaskStatus.IN_PROGRESS), Instant.now());
    }
}
