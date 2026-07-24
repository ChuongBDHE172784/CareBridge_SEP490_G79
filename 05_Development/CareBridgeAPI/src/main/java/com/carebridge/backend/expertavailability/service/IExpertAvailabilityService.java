package com.carebridge.backend.expertavailability.service;

import com.carebridge.backend.expertavailability.dto.request.CreateAvailabilityRequest;
import com.carebridge.backend.expertavailability.dto.request.SetOnlineStatusRequest;
import com.carebridge.backend.expertavailability.dto.request.ShareLocationRequest;
import com.carebridge.backend.expertavailability.dto.response.AvailabilityResponse;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import java.util.List;
import java.util.UUID;

public interface IExpertAvailabilityService {

    AvailabilityResponse createAvailability(UUID expertProfileId, CreateAvailabilityRequest request);

    List<AvailabilityResponse> getMyAvailability(UUID expertProfileId);

    void deleteAvailability(UUID availabilityId, UUID expertProfileId);

    LocationShareResponse shareLocation(UUID expertProfileId, ShareLocationRequest request);

    void stopLocationShare(UUID expertProfileId);

    LocationShareResponse setOnlineStatus(UUID expertProfileId, Boolean online);
}
