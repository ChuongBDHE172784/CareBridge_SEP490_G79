package com.carebridge.backend.checklist.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.audit.entity.AuditAction;
import com.carebridge.backend.audit.policy.AuditEligibilityPolicy;
import com.carebridge.backend.checklist.audit.ChecklistAuditWriter;
import com.carebridge.backend.content.service.ChecklistTemplateApprovalServiceImpl;
import com.carebridge.backend.family.service.impl.CareGroupServiceImpl;
import org.junit.jupiter.api.Test;

class ChecklistApprovalDistributionAuditContractTest {

    @Test
    void chk037_distributionApprovalActionIsExplicitlyAllowlisted() {
        assertThat(new AuditEligibilityPolicy().shouldAudit(AuditAction.CHECKLIST_TEMPLATE_DECIDED))
                .as("approval activates distribution and therefore cannot be silently omitted from audit")
                .isTrue();
    }

    @Test
    void chk038_approvalUsesStrictAuditBoundarySoMissingAuditAbortsMutation() throws Exception {
        assertThat(ChecklistTemplateApprovalServiceImpl.class.getDeclaredField("auditService").getType())
                .as("distribution approval must use the strict writer that throws on eligibility/serialization/persistence failure")
                .isEqualTo(ChecklistAuditWriter.class);
    }

    @Test
    void chk038_approvalHasNoLegacyAuditFallbackForStrictAction() {
        assertThat(ChecklistTemplateApprovalServiceImpl.class.getDeclaredFields())
                .as("strict template decisions must never fall back to the generic audit boundary")
                .noneMatch(field -> field.getName().equals("legacyAuditService"));
    }

    @Test
    void chk038_requestMaterializationRemovesDurableDistributionCandidateBoundary() {
        assertThat(java.util.Arrays.stream(CareGroupServiceImpl.class.getDeclaredFields())
                .filter(field -> field.getName().equals("distributionCandidatePublisher"))
                .toList())
                .isEmpty();
    }
}
