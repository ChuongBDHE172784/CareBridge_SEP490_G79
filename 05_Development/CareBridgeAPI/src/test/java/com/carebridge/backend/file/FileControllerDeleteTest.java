package com.carebridge.backend.file;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.file.controller.FileController;
import com.carebridge.backend.file.service.IFileService;
import com.carebridge.backend.security.config.SecurityConfig;
import com.carebridge.backend.security.jwt.JwtTokenProvider;
import com.carebridge.backend.security.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class FileControllerDeleteTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IFileService fileService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    // FILE-DEL-TC-012: No JWT → 401
    @Test
    void deleteFile_noJwt_returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isUnauthorized());
    }

    // FILE-DEL-TC-013: Non-MOTHER role → 403 at controller role gate (service never invoked)
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "FAMILY")
    void deleteFile_familyRole_returns403ServiceNotCalled() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isForbidden());

        verify(fileService, never()).deleteFile(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000004", roles = "SYSTEM_ADMIN")
    void deleteFile_adminRole_returns403ServiceNotCalled() throws Exception {
        mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isForbidden());

        verify(fileService, never()).deleteFile(any(), any());
    }

    // FILE-DEL-TC-SEC-001: IDOR — Mother deletes another Mother's file → 403
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000005", roles = "MOTHER")
    void deleteFile_motherDeletesAnotherMothersFile_returns403() throws Exception {
        doThrow(new AccessDeniedBusinessException("Not the owner"))
                .when(fileService).deleteFile(any(), any());

        mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isForbidden());
    }

    // Positive: owner (MOTHER) deletes own file → 204
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void deleteFile_ownerMotherRole_returns204() throws Exception {
        doNothing().when(fileService).deleteFile(any(), any());

        mockMvc.perform(delete("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isNoContent());
    }
}
