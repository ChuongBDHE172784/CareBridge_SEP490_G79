package com.carebridge.backend.checklist.today.controller;

import com.carebridge.backend.checklist.today.dto.TaskActionRequest;
import com.carebridge.backend.checklist.today.dto.TaskActionResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistActionResponse;
import com.carebridge.backend.checklist.today.dto.CurrentChecklistResponse;
import com.carebridge.backend.checklist.today.model.TaskAction;
import com.carebridge.backend.checklist.today.service.CurrentChecklistService;
import com.carebridge.backend.checklist.today.service.UnifiedTaskActionFacade;
import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.common.util.SecurityUtils;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp REST API cho luồng Việc cần làm hôm nay (Today Checklist / To-dos).
 * Tiếp nhận request đọc danh sách task của giai đoạn hiện tại và thực hiện thao tác Complete/Reopen task.
 */
@RestController
@RequestMapping("/api/v1/checklists")
public class CurrentChecklistController {
    private final CurrentChecklistService checklistService;
    private final UnifiedTaskActionFacade actionFacade;
    private final com.carebridge.backend.journey.repository.MotherJourneyRepository motherJourneyRepository;

    public CurrentChecklistController(
            CurrentChecklistService checklistService,
            UnifiedTaskActionFacade actionFacade,
            com.carebridge.backend.journey.repository.MotherJourneyRepository motherJourneyRepository) {
        this.checklistService = checklistService;
        this.actionFacade = actionFacade;
        this.motherJourneyRepository = motherJourneyRepository;
    }

    /**
     * [BƯỚC 1: TIẾP NHẬN REQUEST LẤY DANH SÁCH VIỆC CẦN LÀM HIỆN TẠI]
     * Endpoint: GET /api/v1/checklists/current/tasks
     * Quyền hạn: MOTHER (Mẹ), FAMILY (Người thân trong Care Group)
     *
     * @param date Ngày cần lấy danh sách (tùy chọn, mặc định lấy ngày hiện tại)
     * @param timezone Múi giờ của thiết bị người dùng (Request Header: X-User-Timezone)
     * @param principal Thông tin xác thực của user đang đăng nhập
     * @return CurrentChecklistResponse Danh sách task chia theo 4 time bucket (Overdue, Today, Upcoming, Unscheduled)
     */
    @GetMapping("/current/tasks")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public CurrentChecklistResponse getCurrentTasks(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-User-Timezone", required = false) String timezone,
            Principal principal) {
        // Trích xuất User ID của người gọi từ Authentication Principal
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);

        // Chuyển tiếp tới Service để tự động đối soát vòng đời (reconcile) và nạp danh sách việc cần làm
        return checklistService.getCurrentTasks(actorId, date, timezone);
    }

    /**
     * [LẤY DANH SÁCH VIỆC CẦN LÀM THEO HÀNH TRÌNH PHỤC VỤ REALTIME SYNC CHO CHUYÊN GIA]
     * Endpoint: GET /api/v1/checklists/journeys/{journeyId}/tasks
     * Quyền hạn: MOTHER, FAMILY, EXPERT, ADMIN
     */
    @GetMapping("/journeys/{journeyId}/tasks")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'ADMIN')")
    public CurrentChecklistResponse getJourneyTasks(
            @PathVariable UUID journeyId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-User-Timezone", required = false) String timezone) {
        var journey = motherJourneyRepository.findById(journeyId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "JOURNEY_NOT_FOUND", "Journey not found"));
        return checklistService.getCurrentTasks(journey.getOwnerUserId(), date, timezone);
    }

    /**
     * [LẤY DANH SÁCH VIỆC CẦN LÀM THEO USER ID PHỤC VỤ CHUYÊN GIA / ADMIN]
     * Endpoint: GET /api/v1/checklists/users/{userId}/tasks
     * Quyền hạn: EXPERT, ADMIN
     */
    @GetMapping("/users/{userId}/tasks")
    @PreAuthorize("hasAnyRole('EXPERT', 'ADMIN')")
    public CurrentChecklistResponse getUserTasks(
            @PathVariable UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader(value = "X-User-Timezone", required = false) String timezone) {
        return checklistService.getCurrentTasks(userId, date, timezone);
    }

    /**
     * [THỰC HIỆN THAO TÁC TRÊN ĐẦU VIỆC (COMPLETE / REOPEN)]
     * Endpoint: POST /api/v1/checklists/tasks/{taskId}/actions
     * Quyền hạn: MOTHER, FAMILY (Chỉ hỗ trợ 2 hành động: COMPLETE - Hoàn thành, REOPEN - Mở lại)
     *
     * @param taskId ID của việc cần làm cụ thể (ChecklistTaskInstance ID)
     * @param request Payload chứa loại hành động (action) và clientRequestId để chống trùng lặp
     * @param principal Thông tin xác thực của user
     * @return CurrentChecklistActionResponse Kết quả cập nhật trạng thái kèm mã kiểm toán truy vết
     */
    @PostMapping("/tasks/{taskId}/actions")
    @PreAuthorize("hasAnyRole('MOTHER', 'FAMILY')")
    public CurrentChecklistActionResponse applyAction(
            @PathVariable UUID taskId,
            @Valid @RequestBody TaskActionRequest request,
            Principal principal) {
        // [Validate]: Checklist Task chỉ cho phép chuyển đổi giữa COMPLETE và REOPEN
        if (request == null || (request.action() != TaskAction.COMPLETE
                && request.action() != TaskAction.REOPEN)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "CHECKLIST_ACTION_INVALID",
                    "Only COMPLETE and REOPEN are supported for checklist tasks");
        }

        // Lấy User ID của người thực hiện hành động
        UUID actorId = SecurityUtils.requireCurrentUserId(principal);

        // Gọi UnifiedTaskActionFacade để cập nhật trạng thái FSM, kiểm tra idempotency và ghi Audit Log
        TaskActionResponse response = actionFacade.apply(actorId,
                com.carebridge.backend.checklist.today.model.TaskKind.CHECKLIST,
                taskId, request);

        // Đóng gói phản hồi trạng thái mới về cho Frontend
        return new CurrentChecklistActionResponse(response.taskId(), response.instanceId(),
                response.action(), response.previousStatus(), response.status(),
                response.appliedAt(), response.idempotentReplay(), response.correlationId());
    }
}
