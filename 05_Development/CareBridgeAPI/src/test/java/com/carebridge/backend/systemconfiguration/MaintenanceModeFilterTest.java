package com.carebridge.backend.systemconfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.systemconfiguration.security.MaintenanceModeFilter;
import com.carebridge.backend.systemconfiguration.service.SystemMaintenanceModeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MaintenanceModeFilterTest {

    @Mock
    private SystemMaintenanceModeService maintenanceModeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void normalTraffic_whenMaintenanceEnabled_returns503WithoutCallingApplication() throws Exception {
        when(maintenanceModeService.isMaintenanceEnabled()).thenReturn(true);
        MaintenanceModeFilter filter = new MaintenanceModeFilter(maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/community/feed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SYSTEM_MAINTENANCE");
        assertThat(response.getHeader("Retry-After")).isEqualTo("2");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    }

    @Test
    void systemAdmin_whenMaintenanceEnabled_reachesApplication() throws Exception {
        authenticate("ROLE_SYSTEM_ADMIN");
        MaintenanceModeFilter filter = new MaintenanceModeFilter(maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin/audit-logs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(maintenanceModeService, never()).isMaintenanceEnabled();
    }

    @Test
    void configurationEndpoint_isAlwaysAvailableForRecovery() throws Exception {
        MaintenanceModeFilter filter = new MaintenanceModeFilter(maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/admin/system-configuration");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isTrue();
        verify(maintenanceModeService, never()).isMaintenanceEnabled();
    }

    @Test
    void loginAndReadiness_areAlwaysAvailable() throws Exception {
        MaintenanceModeFilter filter = new MaintenanceModeFilter(maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));

        for (MockHttpServletRequest request : List.of(
                new MockHttpServletRequest("POST", "/api/v1/auth/login"),
                new MockHttpServletRequest("GET", "/actuator/health/readiness"))) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            boolean[] invoked = {false};
            filter.doFilter(request, response, (req, res) -> invoked[0] = true);
            assertThat(invoked[0]).isTrue();
        }
        verify(maintenanceModeService, never()).isMaintenanceEnabled();
    }

    @Test
    void recoveryEndpoint_withContextPathAndTrailingSlash_isAvailable() throws Exception {
        MaintenanceModeFilter filter = new MaintenanceModeFilter(
                maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "PUT", "/carebridge/api/v1/admin/system-configuration/");
        request.setContextPath("/carebridge");
        request.setServletPath("/api/v1/admin/system-configuration/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isTrue();
        verify(maintenanceModeService, never()).isMaintenanceEnabled();
    }

    @Test
    void nearMatchOfRecoveryEndpoint_isStillBlocked() throws Exception {
        when(maintenanceModeService.isMaintenanceEnabled()).thenReturn(true);
        MaintenanceModeFilter filter = new MaintenanceModeFilter(
                maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/admin/system-configuration-export");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void recoveryPath_withWrongMethod_isStillBlocked() throws Exception {
        when(maintenanceModeService.isMaintenanceEnabled()).thenReturn(true);
        MaintenanceModeFilter filter = new MaintenanceModeFilter(
                maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    @Test
    void futureApiVersion_isBlockedByMaintenance() throws Exception {
        when(maintenanceModeService.isMaintenanceEnabled()).thenReturn(true);
        MaintenanceModeFilter filter = new MaintenanceModeFilter(
                maintenanceModeService, new ObjectMapper().registerModule(new JavaTimeModule()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/community/feed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        filter.doFilter(request, response, (req, res) -> invoked[0] = true);

        assertThat(invoked[0]).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
    }

    private void authenticate(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }
}
