package com.carebridge.backend.checklist.today.provider;

import com.carebridge.backend.checklist.today.dto.TodayTaskCandidate;
import com.carebridge.backend.checklist.today.model.TaskKind;
import java.util.List;
import java.util.UUID;

public interface TodayTaskProvider {
    TaskKind taskKind();

    List<TodayTaskCandidate> findAuthorizedTasks(UUID actorUserId);
}
