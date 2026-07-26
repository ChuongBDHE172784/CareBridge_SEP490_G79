package com.carebridge.backend.expertavailability.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.expert.entity.ExpertProfile;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expertavailability.dto.request.SetOnlineStatusRequest;
import com.carebridge.backend.expertavailability.dto.response.LocationShareResponse;
import com.carebridge.backend.expertavailability.service.IExpertAvailabilityService;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpertAvailabilityControllerOwnershipTest {

    @Mock private IExpertAvailabilityService availabilityService;
    @Mock private ExpertProfileRepository expertProfileRepository;

    @Test
    void onlineStatusUsesTheProfileOwnedByTheAuthenticatedPrincipal() {
        UUID userId = UUID.randomUUID();
        UUID ownedProfileId = UUID.randomUUID();
        Principal principal = userId::toString;
        ExpertProfile ownedProfile = ExpertProfile.builder()
                .expertProfileId(ownedProfileId)
                .userId(userId)
                .build();
        LocationShareResponse expected = LocationShareResponse.builder()
                .expertProfileId(ownedProfileId)
                .availabilityStatus("ONLINE")
                .build();
        when(expertProfileRepository.findByUserId(userId)).thenReturn(Optional.of(ownedProfile));
        when(availabilityService.setOnlineStatus(ownedProfileId, true)).thenReturn(expected);
        ExpertAvailabilityController controller =
                new ExpertAvailabilityController(availabilityService, expertProfileRepository);

        var response = controller.setOnlineStatus(
                principal, SetOnlineStatusRequest.builder().online(true).build());

        assertThat(response.getBody()).isNotNull();
        verify(expertProfileRepository).findByUserId(userId);
        verify(availabilityService).setOnlineStatus(ownedProfileId, true);
    }
}
