package com.carebridge.backend.file;

import com.carebridge.backend.common.config.JpaAuditingConfig;
import com.carebridge.backend.config.MockMvcSecurityBuilderConfig;
import com.carebridge.backend.file.controller.FileController;
import com.carebridge.backend.file.dto.UploadFileResponse;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FileController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
)
@Import({SecurityConfig.class, MockMvcSecurityBuilderConfig.class})
class FileControllerUploadTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private IFileService fileService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserRepository userRepository;

    private static final UUID FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = "MOTHER")
    void uploadHealthRecordFile_motherRole_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "ultrasound.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});
        UploadFileResponse response = UploadFileResponse.builder()
                .fileId(FILE_ID)
                .originalName("ultrasound.jpg")
                .mimeType("image/jpeg")
                .fileSizeBytes(2L)
                .presignedUrl("https://storage.example.com/presigned")
                .createdAt(Instant.now())
                .build();
        when(fileService.uploadHealthRecordFile(any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/files/health-records").file(file))
                .andExpect(status().isCreated());

        verify(fileService).uploadHealthRecordFile(any(), any());
    }

    @Test
    @WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = "FAMILY")
    void uploadHealthRecordFile_familyRole_returns403ServiceNotCalled() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "ultrasound.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8});

        mockMvc.perform(multipart("/api/v1/files/health-records").file(file))
                .andExpect(status().isForbidden());

        verify(fileService, never()).uploadHealthRecordFile(any(), any());
    }
}
