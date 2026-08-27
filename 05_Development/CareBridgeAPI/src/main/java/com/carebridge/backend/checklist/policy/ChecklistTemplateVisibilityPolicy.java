package com.carebridge.backend.checklist.policy;

import com.carebridge.backend.checklist.entity.ChecklistInstance;
import com.carebridge.backend.checklist.model.ChecklistOrigin;
import com.carebridge.backend.content.entity.ChecklistTemplate;
import com.carebridge.backend.content.entity.ChecklistTemplateStatus;

/**
 * Shared read/mutation visibility rule for checklist templates.
 *
 * <p>Archived templates remain persisted for audit and task evidence, but they
 * are not part of any user-facing checklist view. Template-less legacy or
 * user-created rows remain visible through the template-only overload; callers
 * handling a persisted instance should use the instance-aware overload, which
 * fails closed when a system-template version cannot be resolved.</p>
 */
public final class ChecklistTemplateVisibilityPolicy {

    private ChecklistTemplateVisibilityPolicy() {
    }

    public static boolean isVisible(ChecklistTemplate template) {
        return template == null || template.getStatus() != ChecklistTemplateStatus.ARCHIVED;
    }

    /**
     * Fail closed for a system-template instance whose version cannot be resolved,
     * while preserving template-less user-created aggregates.
     */
    public static boolean isVisible(ChecklistInstance instance, ChecklistTemplate template) {
        if (isArchived(template)) {
            return false;
        }
        if (template != null || instance == null) {
            return true;
        }
        return instance.getOrigin() != ChecklistOrigin.SYSTEM_TEMPLATE;
    }

    public static boolean isArchived(ChecklistTemplate template) {
        return template != null && template.getStatus() == ChecklistTemplateStatus.ARCHIVED;
    }
}
