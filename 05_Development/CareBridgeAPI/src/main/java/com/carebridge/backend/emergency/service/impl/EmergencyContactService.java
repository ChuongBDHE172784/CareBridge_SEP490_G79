package com.carebridge.backend.emergency.service.impl;

import com.carebridge.backend.emergency.dto.request.EmergencyContactRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyContactResponse;
import com.carebridge.backend.emergency.entity.EmergencyContact;
import com.carebridge.backend.emergency.exception.EmergencyException;
import com.carebridge.backend.emergency.repository.IEmergencyContactRepository;
import com.carebridge.backend.emergency.service.IEmergencyContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmergencyContactService implements IEmergencyContactService {

    private final IEmergencyContactRepository emergencyContactRepository;

    @Override
    @Transactional(readOnly = true)
    public EmergencyContactResponse getContact(UUID userId) {
        return emergencyContactRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new EmergencyException(HttpStatus.NOT_FOUND, "EMERG-005",
                        "Emergency contact not configured"));
    }

    @Override
    public EmergencyContactResponse upsertContact(UUID userId, EmergencyContactRequest request) {
        EmergencyContact contact = emergencyContactRepository.findByUserId(userId)
                .orElseGet(() -> EmergencyContact.builder()
                        .userId(userId)
                        .build());
        contact.setName(request.getName());
        contact.setPhone(request.getPhone());
        contact.setRelationship(request.getRelationship());
        contact.setPrimaryContact(request.isPrimaryContact());
        contact.setUpdatedBy(userId);
        return toResponse(emergencyContactRepository.save(contact));
    }

    private EmergencyContactResponse toResponse(EmergencyContact contact) {
        return EmergencyContactResponse.builder()
                .id(contact.getId())
                .userId(contact.getUserId())
                .name(contact.getName())
                .phone(contact.getPhone())
                .relationship(contact.getRelationship())
                .primaryContact(contact.isPrimaryContact())
                .updatedAt(contact.getUpdatedAt())
                .build();
    }
}
