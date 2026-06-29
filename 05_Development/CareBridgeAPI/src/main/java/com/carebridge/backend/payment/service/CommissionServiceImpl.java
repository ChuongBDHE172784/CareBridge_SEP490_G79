package com.carebridge.backend.payment.service;

import com.carebridge.backend.common.exception.ResourceNotFoundException;
import com.carebridge.backend.payment.dto.response.CommissionRecordDTO;
import com.carebridge.backend.payment.dto.response.SettlementRecordDTO;
import com.carebridge.backend.payment.entity.CommissionRecord;
import com.carebridge.backend.payment.entity.SettlementRecord;
import com.carebridge.backend.payment.mapper.PaymentMapper;
import com.carebridge.backend.payment.repository.CommissionRecordRepository;
import com.carebridge.backend.payment.repository.SettlementRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Commission Service Implementation.
 * Handles commission calculations and settlements.
 *
 * Implements ICommissionService for commission management.
 *
 * TV4 Use Cases: UC-125 (View Commission), UC-130 (Create Settlement)
 */
@Service("commissionService")
@RequiredArgsConstructor
@Slf4j
public class CommissionServiceImpl implements ICommissionService {

    private final CommissionRecordRepository commissionRepository;
    private final SettlementRecordRepository settlementRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRecordDTO> getExpertCommissions(Long expertId) {
        log.debug("Getting commissions for expertId: {}", expertId);

        List<CommissionRecord> commissions = commissionRepository.findByExpertId(expertId);

        return commissions.stream()
                .map(paymentMapper::toCommissionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommissionRecord> getEligibleCommissions(Long expertId) {
        log.debug("Getting eligible commissions for expertId: {}", expertId);
        // Get all pending commissions that are eligible for settlement
        return commissionRepository.findEligible("PENDING", Instant.now());
    }

    @Override
    @Transactional
    public SettlementRecord createSettlement(Long expertId, CreateSettlementRequest request) {
        log.info("Creating settlement for expertId: {}, period: {} - {}",
                expertId, request.getPeriodStart(), request.getPeriodEnd());

        // Find eligible commissions for the expert
        List<CommissionRecord> allEligible = commissionRepository.findEligible("PENDING", Instant.now());
        List<CommissionRecord> expertCommissions = allEligible.stream()
                .filter(c -> c.getExpertId().equals(expertId))
                .toList();

        if (expertCommissions.isEmpty()) {
            throw new IllegalStateException("No eligible commissions for settlement");
        }

        // Calculate totals
        int totalCommission = expertCommissions.stream()
                .mapToInt(CommissionRecord::getCommissionAmount)
                .sum();

        int totalGatewayFee = expertCommissions.stream()
                .mapToInt(CommissionRecord::getGatewayFee)
                .sum();

        int totalRefund = expertCommissions.stream()
                .mapToInt(CommissionRecord::getRefundAmount)
                .sum();

        // Create settlement record
        SettlementRecord settlement = SettlementRecord.builder()
                .expertId(expertId)
                .settlementPeriodStart(request.getPeriodStart())
                .settlementPeriodEnd(request.getPeriodEnd())
                .grossAmount(totalCommission + totalGatewayFee)
                .commissionAmount(totalCommission)
                .gatewayFee(totalGatewayFee)
                .refundAmount(totalRefund)
                .expertNetAmount(totalCommission - totalGatewayFee - totalRefund)
                .status(com.carebridge.backend.expert.enums.SettlementStatus.PENDING)
                .referenceCode("STL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .build();

        settlement = settlementRepository.save(settlement);

        log.info("Settlement created: settlementId={}, netAmount={}",
                settlement.getSettlementId(), settlement.getExpertNetAmount());

        return settlement;
    }

    @Override
    @Transactional
    public SettlementRecord completeSettlement(Long settlementId) {
        log.info("Completing settlement: {}", settlementId);

        SettlementRecord settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));

        settlement.setStatus(com.carebridge.backend.expert.enums.SettlementStatus.COMPLETED);
        settlement.setSettledAt(Instant.now());

        SettlementRecord saved = settlementRepository.save(settlement);
        log.info("Settlement completed: {}", settlementId);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SettlementRecordDTO> getSettlements(Long expertId, Pageable pageable) {
        log.debug("Getting settlements for expertId: {}", expertId);

        List<SettlementRecord> settlements = settlementRepository.findByExpertId(expertId);

        // Simple pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), settlements.size());
        List<SettlementRecordDTO> pageContent = start >= settlements.size() ? List.of() :
                settlements.subList(start, end).stream()
                        .map(paymentMapper::toSettlementDTO)
                        .toList();

        return new PageImpl<>(pageContent, pageable, settlements.size());
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementRecordDTO getSettlement(Long settlementId) {
        log.debug("Getting settlement: {}", settlementId);

        SettlementRecord settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found"));

        return paymentMapper.toSettlementDTO(settlement);
    }
}
