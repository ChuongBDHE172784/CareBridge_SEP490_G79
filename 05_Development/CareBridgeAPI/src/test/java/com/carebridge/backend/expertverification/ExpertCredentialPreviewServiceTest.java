package com.carebridge.backend.expertverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.carebridge.backend.audit.service.AuditService;
import com.carebridge.backend.expert.exception.ExpertException;
import com.carebridge.backend.expert.repository.ExpertProfileRepository;
import com.carebridge.backend.expertverification.entity.ExpertCredential;
import com.carebridge.backend.expertverification.mapper.ExpertCredentialMapper;
import com.carebridge.backend.expertverification.repository.ExpertCredentialRepository;
import com.carebridge.backend.expertverification.service.impl.ExpertCredentialServiceImpl;
import com.carebridge.backend.file.dto.AuthorizedFileContent;
import com.carebridge.backend.file.service.IFileService;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpertCredentialPreviewServiceTest {

    @Mock private ExpertCredentialRepository credentialRepository;
    @Mock private ExpertProfileRepository expertProfileRepository;
    @Mock private ExpertCredentialMapper credentialMapper;
    @Mock private IFileService fileService;
    @Mock private AuditService auditService;

    @InjectMocks private ExpertCredentialServiceImpl service;

    @Test
    void extractsReadableTextFromPrivateDocxStoredInR2() throws Exception {
        UUID credentialId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        ExpertCredential credential = ExpertCredential.builder()
                .credentialId(credentialId)
                .credentialType("MEDICAL_LICENSE")
                .fileId(fileId)
                .build();
        byte[] bytes = docxBytes("Giấy phép hành nghề CareBridge");

        when(credentialRepository.findByCredentialId(credentialId))
                .thenReturn(Optional.of(credential));
        when(fileService.readAuthorizedFile(fileId, reviewerId, 10L * 1024 * 1024))
                .thenReturn(new AuthorizedFileContent(
                        fileId,
                        "giay-phep.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        bytes.length,
                        bytes));

        var response = service.previewCredential(credentialId, reviewerId);

        assertThat(response.getFileName()).isEqualTo("giay-phep.docx");
        assertThat(response.getContent()).contains("Giấy phép hành nghề CareBridge");
        assertThat(response.isTruncated()).isFalse();
    }

    @Test
    void rejectsDocumentsLargerThanPreviewLimitBeforeParsing() {
        UUID credentialId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        ExpertCredential credential = ExpertCredential.builder()
                .credentialId(credentialId)
                .credentialType("DEGREE")
                .fileId(fileId)
                .build();

        when(credentialRepository.findByCredentialId(credentialId))
                .thenReturn(Optional.of(credential));
        when(fileService.readAuthorizedFile(fileId, reviewerId, 10L * 1024 * 1024))
                .thenReturn(new AuthorizedFileContent(
                        fileId,
                        "large.pdf",
                        "application/pdf",
                        10L * 1024 * 1024 + 1,
                        new byte[] {1}));

        assertThatThrownBy(() -> service.previewCredential(credentialId, reviewerId))
                .isInstanceOf(ExpertException.class)
                .hasMessageContaining("too large");
    }

    private static byte[] docxBytes(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
