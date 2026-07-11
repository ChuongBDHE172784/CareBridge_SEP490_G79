package com.carebridge.backend.family.service;

import com.carebridge.backend.family.dto.AssignFamilyTaskRequest;
import com.carebridge.backend.family.dto.AssignFamilyTaskResponse;
import com.carebridge.backend.family.dto.CancelFamilyTaskResponse;
import com.carebridge.backend.family.dto.CareTaskDetailResponse;
import com.carebridge.backend.family.dto.CareTasksResponse;
import com.carebridge.backend.family.dto.UpdateFamilyTaskRequest;
import com.carebridge.backend.family.dto.UpdateFamilyTaskResponse;
import com.carebridge.backend.family.dto.UpdateTaskStatusRequest;
import com.carebridge.backend.family.dto.UpdateTaskStatusResponse;

import java.util.UUID;

public interface ICareTaskService {

    /**
     * Assigns a new care task within a care group.
     * Caller must be the ACCEPTED OWNER (ADR-FAM-032).
     * Assignee must be an ACCEPTED member of the same group.
     * Due date must be strictly in the future (ADR-FAM-033).
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-031/403) caller not Owner
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-030/409) assignee not ACCEPTED member
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-032/400) dueAt not in future
     */
    AssignFamilyTaskResponse assignFamilyTask(UUID groupId, AssignFamilyTaskRequest request, UUID callerId);

    /**
     * Lists all care tasks for a care group.
     * Caller must be an ACCEPTED member (any role) — read is broader than write (ADR-FAM-032).
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-003/403) caller not ACCEPTED member
     */
    CareTasksResponse listTasks(UUID groupId, UUID callerId);

    CareTaskDetailResponse getTaskDetail(UUID groupId, UUID taskId, UUID callerId);

    UpdateFamilyTaskResponse updateFamilyTask(UUID groupId, UUID taskId,
                                              UpdateFamilyTaskRequest request, UUID callerId);

    CancelFamilyTaskResponse cancelFamilyTask(UUID groupId, UUID taskId, UUID callerId);

    /**
     * UC-85: Allows the assignee to update the status of their assigned task following the FSM.
     * Only the task's assignee may call this (ADR-FAM-006).
     *
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-005/404) care group not found
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-033/404) task not found in this group
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-034/403) caller is not the assignee
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-035/400) invalid status value
     * @throws com.carebridge.backend.common.exception.BusinessException (FAM-023/409) FSM transition not allowed
     */
    UpdateTaskStatusResponse updateTaskStatus(UUID groupId, UUID taskId,
                                              UpdateTaskStatusRequest request, UUID callerId);
}
