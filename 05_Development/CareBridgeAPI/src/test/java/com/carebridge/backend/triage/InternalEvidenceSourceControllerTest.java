package com.carebridge.backend.triage;

import com.carebridge.backend.triage.controller.InternalEvidenceSourceController;
import com.carebridge.backend.triage.service.EvidenceSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalEvidenceSourceControllerTest {
    @Test
    void canonicalStageAliasesAreAdaptedAtTheLegacyEvidenceBoundary() {
        EvidenceSourceService service = mock(EvidenceSourceService.class);
        when(service.approvedForStage(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of());
        InternalEvidenceSourceController controller = new InternalEvidenceSourceController(service);
        ReflectionTestUtils.setField(controller, "internalApiKey", "secret");

        controller.approved("POSTPARTUM_MOTHER", "secret");
        controller.approved("INFANT_0_12M", "secret");
        controller.approved("TODDLER_12_24M", "secret");

        verify(service).approvedForStage("POSTPARTUM");
        verify(service).approvedForStage("INFANT");
        verify(service).approvedForStage("TODDLER");
    }
}
