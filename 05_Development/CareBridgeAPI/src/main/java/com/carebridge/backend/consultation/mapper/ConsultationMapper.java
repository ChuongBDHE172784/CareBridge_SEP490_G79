package com.carebridge.backend.consultation.mapper;

import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationDetailDTO;
import com.carebridge.backend.consultation.dto.response.ConsultationSessionDTO;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import com.carebridge.backend.consultation.dto.response.ExpertSearchResultDTO;
import com.carebridge.backend.consultation.entity.AvailabilitySlot;
import com.carebridge.backend.consultation.entity.Consultation;
import com.carebridge.backend.consultation.entity.ConsultationSession;
import com.carebridge.backend.expert.entity.Expert;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for Consultation module entities to DTOs.
 */
@Component
public class ConsultationMapper {

    /**
     * Map Consultation to summary DTO.
     */
    public ConsultationSummaryDTO toSummaryDTO(Consultation consultation, Expert expert) {
        return ConsultationSummaryDTO.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .expertId(consultation.getExpertId())
                .expertSpecialty(expert != null ? expert.getSpecialty() : null)
                .expertName(expert != null ? expert.getProfessionalTitle() : null)
                .modality(consultation.getChannelType() != null ?
                        com.carebridge.backend.expert.enums.ConsultationModality.valueOf(consultation.getChannelType()) : null)
                .scheduledStart(consultation.getScheduledStart())
                .scheduledEnd(consultation.getScheduledEnd())
                .status(consultation.getStatus() != null ?
                        com.carebridge.backend.expert.enums.ConsultationStatus.valueOf(consultation.getStatus().name()) : null)
                .priceAmount(consultation.getPriceSnapshotAmount())
                .currency(consultation.getCurrency())
                .sessionToken(consultation.getSessionToken())
                .build();
    }

    /**
     * Map Consultation to detail DTO.
     */
    public ConsultationDetailDTO toDetailDTO(Consultation consultation,
                                             Expert expert,
                                             ConsultationSession session,
                                             PaymentInfo paymentInfo) {
        ConsultationDetailDTO.ExpertInfo.Builder expertInfoBuilder = null;
        if (expert != null) {
            expertInfoBuilder = ConsultationDetailDTO.ExpertInfo.builder()
                    .expertId(expert.getExpertId())
                    .specialty(expert.getSpecialty())
                    .professionalTitle(expert.getProfessionalTitle())
                    .workplace(expert.getWorkplace())
                    .averageRating(expert.getRatingAvg())
                    .reviewCount(expert.getReviewCount());
        }

        ConsultationDetailDTO.RealtimeSessionInfo sessionInfo = null;
        if (session != null) {
            sessionInfo = ConsultationDetailDTO.RealtimeSessionInfo.builder()
                    .sessionId(session.getSessionId())
                    .roomId(session.getCommunicationRoomId())
                    .providerType(session.getProviderType())
                    .sessionStatus(session.getSessionStatus() != null ?
                            com.carebridge.backend.expert.enums.SessionStatus.valueOf(session.getSessionStatus().name()) : null)
                    .startedAt(session.getStartedAt())
                    .endedAt(session.getEndedAt())
                    .build();
        }

        return ConsultationDetailDTO.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .expertId(consultation.getExpertId())
                .expert(expertInfoBuilder != null ? expertInfoBuilder.build() : null)
                .requesterUserId(consultation.getRequesterUserId())
                .modality(consultation.getChannelType() != null ?
                        com.carebridge.backend.expert.enums.ConsultationModality.valueOf(consultation.getChannelType().name()) : null)
                .durationMinutes(consultation.getDurationMinutes())
                .scheduledStart(consultation.getScheduledStart())
                .scheduledEnd(consultation.getScheduledEnd())
                .status(consultation.getStatus() != null ?
                        com.carebridge.backend.expert.enums.ConsultationStatus.valueOf(consultation.getStatus().name()) : null)
                .priceSnapshotAmount(consultation.getPriceSnapshotAmount())
                .currency(consultation.getCurrency())
                .sessionToken(consultation.getSessionToken())
                .expertSummary(consultation.getExpertSummary())
                .disputeStatus(consultation.getDisputeStatus() != null ?
                        consultation.getDisputeStatus().name() : null)
                .realtimeSession(sessionInfo)
                .payment(paymentInfo)
                .createdAt(consultation.getCreatedAt())
                .updatedAt(consultation.getUpdatedAt())
                .build();
    }

    /**
     * Map Consultation to BookingResponse.
     */
    public BookingResponse toBookingResponse(Consultation consultation, Expert expert, PaymentInfo paymentInfo) {
        return BookingResponse.builder()
                .bookingId(consultation.getBookingId())
                .bookingRef(consultation.getBookingRef())
                .expert(BookingResponse.ExpertSummary.builder()
                        .expertId(expert.getExpertId())
                        .specialty(expert.getSpecialty())
                        .professionalTitle(expert.getProfessionalTitle())
                        .build())
                .scheduledTime(consultation.getScheduledStart())
                .modality(consultation.getChannelType() != null ?
                        com.carebridge.backend.expert.enums.ConsultationModality.valueOf(consultation.getChannelType().name()) : null)
                .price(consultation.getPriceSnapshotAmount())
                .status(consultation.getStatus() != null ?
                        com.carebridge.backend.expert.enums.ConsultationStatus.valueOf(consultation.getStatus().name()) : null)
                .payment(paymentInfo)
                .build();
    }

    /**
     * Map ConsultationSession to DTO.
     */
    public ConsultationSessionDTO toSessionDTO(ConsultationSession session) {
        return ConsultationSessionDTO.builder()
                .sessionId(session.getSessionId())
                .bookingId(session.getBookingId())
                .communicationRoomId(session.getCommunicationRoomId())
                .sessionToken(session.getSessionToken())
                .providerType(session.getProviderType())
                .sessionStatus(session.getSessionStatus() != null ?
                        com.carebridge.backend.expert.enums.SessionStatus.valueOf(session.getSessionStatus().name()) : null)
                .expertSummary(session.getExpertSummary())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    /**
     * Map AvailabilitySlot to DTO.
     */
    public com.carebridge.backend.expert.dto.response.AvailabilitySlotDTO toSlotDTO(AvailabilitySlot slot) {
        return com.carebridge.backend.expert.dto.response.AvailabilitySlotDTO.builder()
                .availabilityId(slot.getAvailabilityId())
                .expertId(slot.getExpertId())
                .slotStart(slot.getSlotStart())
                .slotEnd(slot.getSlotEnd())
                .channelType(slot.getChannelType())
                .status(slot.getStatus() != null ?
                        com.carebridge.backend.expert.enums.SlotStatus.valueOf(slot.getStatus().name()) : null)
                .bookingId(slot.getBookingId())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .build();
    }

    /**
     * Map list of experts to search result.
     */
    public ExpertSearchResultDTO toSearchResult(List<Expert> experts, long total, int page, int size) {
        List<ExpertSearchResultDTO.ExpertListItem> items = experts.stream()
                .map(expert -> ExpertSearchResultDTO.ExpertListItem.builder()
                        .expertId(expert.getExpertId())
                        .specialty(expert.getSpecialty())
                        .professionalTitle(expert.getProfessionalTitle())
                        .workplace(expert.getWorkplace())
                        .experienceYears(expert.getExperienceYears())
                        .averageRating(expert.getRatingAvg())
                        .reviewCount(expert.getReviewCount())
                        .isAvailable(false) // TODO: check availability
                        .build())
                .collect(Collectors.toList());

        return ExpertSearchResultDTO.builder()
                .experts(items)
                .total(total)
                .page(page)
                .size(size)
                .totalPages((int) Math.ceil((double) total / size))
                .build();
    }

    /**
     * Payment info builder for BookingResponse.
     */
    public static class PaymentInfoBuilder {
        public BookingResponse.PaymentInfo build(String qrCodeUrl, String paymentUrl, Instant expiresAt) {
            return BookingResponse.PaymentInfo.builder()
                    .qrCodeUrl(qrCodeUrl)
                    .paymentUrl(paymentUrl)
                    .expiresAt(expiresAt)
                    .build();
        }
    }
}
