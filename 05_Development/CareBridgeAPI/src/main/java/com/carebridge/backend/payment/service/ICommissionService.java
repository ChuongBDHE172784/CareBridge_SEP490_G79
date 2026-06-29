package com.carebridge.backend.payment.service;

import com.carebridge.backend.payment.dto.request.CreateSettlementRequest;
import com.carebridge.backend.payment.dto.response.CommissionRecordDTO;
import com.carebridge.backend.payment.dto.response.SettlementRecordDTO;
import com.carebridge.backend.payment.entity.CommissionRecord;
import com.carebridge.backend.payment.entity.SettlementRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Commission Service Interface.
 * Defines contract for expert commission management.
 *
 * ISP: Interface segregation - only commission-specific methods.
 */
public interface ICommissionService {

    /**
     * Get commission records for an expert.
     *
     * @param expertId the expert ID
     * @return list of commission records
     */
    List<CommissionRecordDTO> getExpertCommissions(Long expertId);

    /**
     * Get eligible commissions for settlement.
     *
     * @param expertId the expert ID
     * @return list of eligible commission records
     */
    List<CommissionRecord> getEligibleCommissions(Long expertId);

    /**
     * Create a settlement for an expert.
     *
     * @param expertId the expert ID
     * @param request the settlement request
     * @return the created settlement record
     */
    SettlementRecord createSettlement(Long expertId, CreateSettlementRequest request);

    /**
     * Complete a settlement (mark as paid).
     *
     * @param settlementId the settlement ID
     * @return the updated settlement record
     */
    SettlementRecord completeSettlement(Long settlementId);

    /**
     * Get settlement records for an expert with pagination.
     *
     * @param expertId the expert ID
     * @param pageable pagination parameters
     * @return paged settlement results
     */
    Page<SettlementRecordDTO> getSettlements(Long expertId, Pageable pageable);

    /**
     * Get settlement by ID.
     *
     * @param settlementId the settlement ID
     * @return settlement record
     */
    SettlementRecordDTO getSettlement(Long settlementId);
}
