package com.carebridge.backend.checklist.today.policy;

import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.checklist.model.ChecklistTargetSubject;
import com.carebridge.backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UnifiedTaskMutationPolicy {
    public void requireUserCreatedTarget(ChecklistOrigin origin, ChecklistTargetSubject targetSubject) {
        if (origin == ChecklistOrigin.USER_CREATED && targetSubject == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ITEM_TARGET_REQUIRED",
                    "User-created tasks require an explicit target subject");
        }
    }

    public void requireMutable(ChecklistOrigin origin) {
        if (origin == ChecklistOrigin.SYSTEM_TEMPLATE) {
            throw new BusinessException(HttpStatus.CONFLICT, "SYSTEM_TASK_IMMUTABLE",
                    "System tasks cannot be edited or deleted");
        }
    }
}
