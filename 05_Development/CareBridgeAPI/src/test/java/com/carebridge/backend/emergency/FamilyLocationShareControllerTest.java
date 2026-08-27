package com.carebridge.backend.emergency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.emergency.controller.FamilyLocationShareController;
import com.carebridge.backend.emergency.dto.response.LocationShareResponse;
import com.carebridge.backend.emergency.service.FamilyLocationShareService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = FamilyLocationShareController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class FamilyLocationShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private FamilyLocationShareService locationShareService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    @Test
    void shareWithoutJwtReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/emergency/location-shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":10.762622,\"longitude\":106.660172}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "00000000-0000-4000-8000-000000000030", roles = "FAMILY")
    void familyCannotShareMotherLocation() throws Exception {
        mockMvc.perform(post("/api/v1/emergency/location-shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":10.762622,\"longitude\":106.660172}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "00000000-0000-4000-8000-000000000010", roles = "MOTHER")
    void motherCanShareValidatedLocation() throws Exception {
        UUID motherId = UUID.fromString("00000000-0000-4000-8000-000000000010");
        org.mockito.Mockito.when(locationShareService.share(eq(motherId), any()))
                .thenReturn(new LocationShareResponse(UUID.randomUUID(), 1, 1, Instant.now()));

        mockMvc.perform(post("/api/v1/emergency/location-shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":10.762622,\"longitude\":106.660172}"))
                .andExpect(status().isOk());

        verify(locationShareService).share(eq(motherId), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-4000-8000-000000000010", roles = "MOTHER")
    void highPrecisionGpsCoordinatesAreAccepted() throws Exception {
        UUID motherId = UUID.fromString("00000000-0000-4000-8000-000000000010");
        org.mockito.Mockito.when(locationShareService.share(eq(motherId), any()))
                .thenReturn(new LocationShareResponse(UUID.randomUUID(), 1, 1, Instant.now()));

        mockMvc.perform(post("/api/v1/emergency/location-shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":0.02410812345678901,"
                                + "\"longitude\":105.51990012345678901}"))
                .andExpect(status().isOk());

        verify(locationShareService).share(eq(motherId), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-4000-8000-000000000010", roles = "MOTHER")
    void invalidCoordinatesReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/emergency/location-shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":91,\"longitude\":106.660172}"))
                .andExpect(status().isBadRequest());
    }
}
