package com.carebridge.backend.checklist.sequence;

import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checklists/sequences")
@RequiredArgsConstructor
public class ChecklistSequenceController {
    private final ChecklistSequenceAdvanceService advanceService;

    @PostMapping("/advance")
    @PreAuthorize("hasRole('MOTHER')")
    public ChecklistSequenceAdvanceResponse advance(
            @Valid @RequestBody ChecklistSequenceAdvanceRequest request,
            Principal principal) {
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);
        return advanceService.advance(actorId, request);
    }
}
