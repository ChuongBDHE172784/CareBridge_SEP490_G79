package com.carebridge.backend.consultation.repository;

import com.carebridge.backend.consultation.entity.ConsultationBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ConsultationBookingRepository extends JpaRepository<ConsultationBooking, UUID> {
}
