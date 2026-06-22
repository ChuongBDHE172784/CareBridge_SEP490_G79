package com.carebridge.backend.expert.service;

import com.carebridge.backend.expert.dto.request.AvailabilitySlotRequest;
import com.carebridge.backend.expert.dto.response.AvailabilitySlotResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface AvailabilityService {

    List<AvailabilitySlotResponse> getMyAvailability(UUID userId);

    AvailabilitySlotResponse createSlot(UUID userId, AvailabilitySlotRequest request);

    AvailabilitySlotResponse updateSlot(UUID userId, UUID slotId, AvailabilitySlotRequest request);

    void deleteSlot(UUID userId, UUID slotId);
}
