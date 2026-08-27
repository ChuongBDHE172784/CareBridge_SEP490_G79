package com.carebridge.backend.vaccination.controller;

import com.carebridge.backend.common.response.ApiResponse;
import com.carebridge.backend.common.util.SecurityUtils;
import com.carebridge.backend.vaccination.dto.AddVaccinationRecordRequest;
import com.carebridge.backend.vaccination.dto.AddVaccinationRecordResponse;
import com.carebridge.backend.vaccination.dto.MarkVaccinationCompletedRequest;
import com.carebridge.backend.vaccination.dto.PostponeVaccinationRequest;
import com.carebridge.backend.vaccination.dto.PostponeVaccinationResponse;
import com.carebridge.backend.vaccination.dto.UpdateVaccinationRecordRequest;
import com.carebridge.backend.vaccination.dto.VaccinationCompletionResponse;
import com.carebridge.backend.vaccination.dto.VaccinationRecordResponse;
import com.carebridge.backend.vaccination.dto.VaccinationScheduleResponse;
import com.carebridge.backend.vaccination.service.IVaccinationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vaccination")
@RequiredArgsConstructor
public class VaccinationController {

    private final IVaccinationService vaccinationService;

    @GetMapping("/babies/{babyId}/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<VaccinationRecordResponse>>> listVaccinationRecords(
            @PathVariable UUID babyId, Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        return ResponseEntity.ok(ApiResponse.success(
                vaccinationService.listVaccinationRecords(babyId, callerId)));
    }

    // UC228: View vaccination schedule for baby
    @GetMapping("/babies/{babyId}/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VaccinationScheduleResponse>> getVaccinationSchedule(
            @PathVariable UUID babyId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = vaccinationService.getVaccinationSchedule(babyId, callerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/babies/{babyId}/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AddVaccinationRecordResponse>> addVaccinationRecord(
            @PathVariable UUID babyId,
            @Valid @RequestBody AddVaccinationRecordRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = vaccinationService.addVaccinationRecord(babyId, callerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/babies/{babyId}/records/{recordId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VaccinationRecordResponse>> updateVaccinationRecord(
            @PathVariable UUID babyId,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateVaccinationRecordRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = vaccinationService.updateVaccinationRecord(babyId, recordId, callerId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/babies/{babyId}/records/{recordId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteVaccinationRecord(
            @PathVariable UUID babyId,
            @PathVariable UUID recordId,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        vaccinationService.deleteVaccinationRecord(babyId, recordId, callerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/babies/{babyId}/completions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VaccinationCompletionResponse>> markVaccinationCompleted(
            @PathVariable UUID babyId,
            @Valid @RequestBody MarkVaccinationCompletedRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = vaccinationService.markVaccinationCompleted(babyId, callerId, request);
        var status = response.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }

    @PostMapping("/babies/{babyId}/postponements")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PostponeVaccinationResponse>> postponeVaccination(
            @PathVariable UUID babyId,
            @Valid @RequestBody PostponeVaccinationRequest request,
            Principal principal) {
        var callerId = SecurityUtils.requireCurrentUserId(principal);
        var response = vaccinationService.postponeVaccination(babyId, callerId, request);
        var status = response.isCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.success(response));
    }
}
