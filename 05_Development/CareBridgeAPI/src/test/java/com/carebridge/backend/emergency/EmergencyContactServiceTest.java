package com.carebridge.backend.emergency;

import com.carebridge.backend.emergency.dto.request.EmergencyContactRequest;
import com.carebridge.backend.emergency.dto.response.EmergencyContactResponse;
import com.carebridge.backend.emergency.entity.EmergencyContact;
import com.carebridge.backend.emergency.repository.IEmergencyContactRepository;
import com.carebridge.backend.emergency.service.impl.EmergencyContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock
    private IEmergencyContactRepository emergencyContactRepository;

    @InjectMocks
    private EmergencyContactService emergencyContactService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    void upsertContact_shouldCreateWhenMissing() {
        when(emergencyContactRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(emergencyContactRepository.save(any())).thenAnswer(invocation -> {
            EmergencyContact contact = invocation.getArgument(0);
            contact.setId(UUID.fromString("00000000-0000-0000-0000-000000000088"));
            return contact;
        });

        EmergencyContactResponse result = emergencyContactService.upsertContact(
                USER_ID,
                new EmergencyContactRequest("Lan Nguyen", "+84901234567", "Sister", true));

        assertThat(result.getName()).isEqualTo("Lan Nguyen");
        assertThat(result.getPhone()).isEqualTo("+84901234567");
        assertThat(result.isPrimaryContact()).isTrue();
    }
}
