package com.carebridge.backend.family.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.family.dto.CareGroupMembersResponse;
import com.carebridge.backend.family.dto.CreateCareGroupRequest;
import com.carebridge.backend.family.dto.CreateCareGroupResponse;
import com.carebridge.backend.family.service.ICareGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/care-groups")
@RequiredArgsConstructor
public class CareGroupController {

    private final ICareGroupService careGroupService;

    // UC70: Create care group
    @PostMapping
    @PreAuthorize("hasRole('MOTHER')")
    public ResponseEntity<ApiResponse<CreateCareGroupResponse>> createCareGroup(
            @Valid @RequestBody CreateCareGroupRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.createCareGroup(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Care group created successfully"));
    }

    // UC216: View care group members
    @GetMapping("/{groupId}/members")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareGroupMembersResponse>> listMembers(
            @PathVariable UUID groupId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = careGroupService.listMembers(groupId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
