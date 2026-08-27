package com.carebridge.backend.content.controller;

import com.carebridge.backend.content.dto.response.EscalationItemResponse;
import com.carebridge.backend.content.entity.ModerationActionType;
import com.carebridge.backend.content.repository.ModerationActionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** System Admin work queue for moderator recommendations; intentionally exposes no health/profile data. */
@RestController @RequestMapping("/api/v1/admin/moderation/escalations") @RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemAdminEscalationController {
    private final ModerationActionRepository actions;
    @GetMapping
    public ResponseEntity<List<EscalationItemResponse>> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 50) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(actions.findByActionTypeOrderByActionAtDesc(ModerationActionType.ESCALATE,
                PageRequest.of(page, size)).map(a -> new EscalationItemResponse(a.getId(), a.getReportId(),
                a.getTargetId(), a.getTargetType(), a.getModeratorUserId(), a.getReason(), a.getActionAt())).getContent());
    }
}
