package com.carebridge.backend.file;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.common.exception.AccessDeniedBusinessException;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.file.controller.FileController;
import com.carebridge.backend.file.dto.ViewFileResponse;
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

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class FileControllerViewTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IFileService fileService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID FILE_ID   = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OWNER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID STRANGER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    // FILE-VIEW-TC-010: No JWT → 401
    @Test
    void viewFile_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isUnauthorized());
    }

    // FILE-VIEW-TC-SEC-001: IDOR — stranger tries to view owner's file → 403, no metadata leaked
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = "MOTHER")
    void viewFile_strangerAccessesOwnerFile_returns403() throws Exception {
        when(fileService.viewFile(any(), any()))
                .thenThrow(new AccessDeniedBusinessException("Access denied to file"));

        mockMvc.perform(get("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isForbidden());
    }

    // Positive: owner can get file details → 200
    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void viewFile_ownerAuthenticated_returns200() throws Exception {
        ViewFileResponse resp = ViewFileResponse.builder()
                .fileId(FILE_ID)
                .originalName("ultrasound.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(2048L)
                .presignedUrl("https://storage.example.com/presigned")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
        when(fileService.viewFile(any(), any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/files/{id}", FILE_ID))
                .andExpect(status().isOk());
    }
}
