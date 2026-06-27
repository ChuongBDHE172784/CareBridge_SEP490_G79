package com.carebridge.backend.audit.controller;

import com.carebridge.backend.audit.dto.request.AddSecurityNoteRequest;
import com.carebridge.backend.audit.dto.request.ReviewSecurityEventRequest;
import com.carebridge.backend.audit.dto.response.SecurityEventNoteResponse;
import com.carebridge.backend.audit.dto.response.SecurityEventResponse;
import com.carebridge.backend.audit.service.SecurityIncidentService;
import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/security-events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityIncidentController {

    private final SecurityIncidentService securityIncidentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SecurityEventResponse>>> searchEvents(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        Page<SecurityEventResponse> result = securityIncidentService.searchEvents(
                userId, eventType, severity, status, ipAddress, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/timeline")
    public ResponseEntity<ApiResponse<List<SecurityEventResponse>>> getTimeline(
            @RequestParam UUID correlationId) {
        List<SecurityEventResponse> timeline = securityIncidentService.getTimeline(correlationId);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }

    @PutMapping("/{eventId}/review")
    public ResponseEntity<ApiResponse<SecurityEventResponse>> reviewEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody ReviewSecurityEventRequest request,
            Principal principal) {
        UUID reviewerId = SecurityUtils.requireCurrentUserId(principal);
        SecurityEventResponse response = securityIncidentService.reviewEvent(eventId, request, reviewerId);
        return ResponseEntity.ok(ApiResponse.success(response, "Security event reviewed"));
    }

    @PostMapping("/{eventId}/notes")
    public ResponseEntity<ApiResponse<SecurityEventNoteResponse>> addNote(
            @PathVariable Long eventId,
            @Valid @RequestBody AddSecurityNoteRequest request,
            Principal principal) {
        UUID authorId = SecurityUtils.requireCurrentUserId(principal);
        SecurityEventNoteResponse response = securityIncidentService.addNote(eventId, request, authorId);
        return ResponseEntity.ok(ApiResponse.success(response, "Note added"));
    }

    @GetMapping("/{eventId}/notes")
    public ResponseEntity<ApiResponse<List<SecurityEventNoteResponse>>> getNotes(
            @PathVariable Long eventId) {
        List<SecurityEventNoteResponse> notes = securityIncidentService.getNotes(eventId);
        return ResponseEntity.ok(ApiResponse.success(notes));
    }
}
