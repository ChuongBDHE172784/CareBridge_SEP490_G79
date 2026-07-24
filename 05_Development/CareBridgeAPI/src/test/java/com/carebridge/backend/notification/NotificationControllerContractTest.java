package com.carebridge.backend.notification;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.notification.controller.NotificationController;
import com.carebridge.backend.notification.dto.NotificationRecordResponse;
import com.carebridge.backend.notification.service.NotificationService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = NotificationController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class NotificationControllerContractTest {

    private static final String USER_ID = "00000000-0000-0000-0000-000000000042";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = USER_ID, roles = "MOTHER")
    void getMyNotificationsReturnsCanonicalReadAndDeliveryFields() throws Exception {
        Instant now = Instant.parse("2026-07-22T00:00:00Z");
        NotificationRecordResponse response = new NotificationRecordResponse(
                UUID.randomUUID(), UUID.fromString(USER_ID), "GROUP_INVITE", "Invitation", "Join the care group",
                null, null, "DELIVERED", now, now, true, now, "PUSH", "fcm-42", 1, null, now,
                Map.of("careGroupId", "group-42"));
        when(notificationService.getMyNotifications(eq(UUID.fromString(USER_ID)), eq(null), any(), any(Principal.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].isRead").value(true))
                .andExpect(jsonPath("$.data.content[0].channel").value("PUSH"))
                .andExpect(jsonPath("$.data.content[0].fcmMessageId").value("fcm-42"))
                .andExpect(jsonPath("$.data.content[0].attemptCount").value(1));
    }
}
