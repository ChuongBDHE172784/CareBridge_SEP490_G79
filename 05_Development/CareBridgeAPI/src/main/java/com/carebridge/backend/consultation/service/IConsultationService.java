package com.carebridge.backend.consultation.service;

import com.carebridge.backend.consultation.dto.request.CreateBookingRequest;
import com.carebridge.backend.consultation.dto.request.UpdateConsultationStatusRequest;
import com.carebridge.backend.consultation.dto.response.BookingResponse;
import com.carebridge.backend.consultation.dto.response.ConsultationDetailDTO;
import com.carebridge.backend.consultation.dto.response.ConsultationSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Consultation Service Interface.
 * Defines contract for consultation management operations.
 *
 * ISP: Interface segregation - only consultation-specific methods.
 */
public interface IConsultationService {

    /**
     * Get consultation detail by ID.
     */
    ConsultationDetailDTO getConsultation(Long bookingId, Long requestingUserId, String role);

    /**
     * Get consultations for a user with pagination.
     */
    Page<ConsultationSummaryDTO> getUserConsultations(Long userId, String role, Pageable pageable);

    /**
     * Cancel a consultation booking.
     */
    void cancelConsultation(Long bookingId, Long userId, String role, String reason);

    /**
     * Reschedule a consultation to a new time.
     */
    ConsultationDetailDTO rescheduleConsultation(Long bookingId,
                                                  Long userId,
                                                  String role,
                                                  UpdateConsultationStatusRequest request);
}
