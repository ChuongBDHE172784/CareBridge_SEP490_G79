package com.carebridge.backend.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.carebridge.backend.file.enums.FileAccessMode;
import com.carebridge.backend.file.service.impl.CloudinaryStorageService;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.junit.jupiter.api.Test;

// RTE-TC-008..010 — see ContentRichTextEditor_Test-Spec.md §4.
// generateSignedUrl() builds the URL locally via the Cloudinary SDK (no network call), so it
// is safe to unit test with fake credentials.
class CloudinaryStorageServiceTest {

    private final CloudinaryStorageService service =
            new CloudinaryStorageService("demo", "fake-key", "fake-secret");

    private static final String PUBLIC_ID = "carebridge/abc123";

    // RTE-TC-008: accessMode=PUBLIC trả URL không có expires_at/signature — ảnh nhúng vĩnh viễn
    // trong content_items.body không được phép hết hạn (ADR-RTE-004).
    @Test
    void generateSignedUrl_publicAccessMode_returnsPermanentUnsignedUrl() {
        String url = service.generateSignedUrl(PUBLIC_ID, 15, FileAccessMode.PUBLIC, "image");

        assertFalse(url.contains("expires_at"), "PUBLIC URL must not carry an expiry: " + url);
        assertFalse(url.contains("s--"), "PUBLIC URL must not be Cloudinary-signed: " + url);
        // Must resolve to the real asset — the old bug encoded "?expires_at=..." into the
        // public_id path segment itself, producing a URL that never matched the stored asset.
        assertTrue(url.contains("/" + PUBLIC_ID), "PUBLIC URL must reference the plain public_id: " + url);
    }

    // RTE-TC-009: accessMode=PRIVATE trả URL đã ký (signed), KHÔNG còn nhét expires_at vào
    // public_id nữa — đó chính là bug khiến Cloudinary trả 400 cho mọi request PRIVATE/
    // AUTHENTICATED (ảnh hưởng cả tài liệu định danh chuyên gia). Verify live bằng curl thật
    // (200 sau khi sửa, 400 trước khi sửa) trước khi đổi assertion này.
    @Test
    void generateSignedUrl_privateAccessMode_returnsValidSignedUrlWithoutBrokenExpiresAt() {
        String url = service.generateSignedUrl(PUBLIC_ID, 15, FileAccessMode.PRIVATE, "image");

        assertFalse(url.contains("expires_at"), "must not reintroduce the broken expires_at-in-public_id bug: " + url);
        assertTrue(url.contains("/" + PUBLIC_ID), "PRIVATE URL must reference the plain public_id: " + url);
        assertTrue(url.contains("s--"), "PRIVATE URL must be Cloudinary-signed: " + url);
    }

    // RTE-TC-010: accessMode=AUTHENTICATED — same fix, same guard.
    @Test
    void generateSignedUrl_authenticatedAccessMode_returnsValidSignedUrlWithoutBrokenExpiresAt() {
        String url = service.generateSignedUrl(PUBLIC_ID, 15, FileAccessMode.AUTHENTICATED, "image");

        assertFalse(url.contains("expires_at"), "must not reintroduce the broken expires_at-in-public_id bug: " + url);
        assertTrue(url.contains("/" + PUBLIC_ID), "AUTHENTICATED URL must reference the plain public_id: " + url);
        assertTrue(url.contains("s--"), "AUTHENTICATED URL must be Cloudinary-signed: " + url);
    }

    // --- storePublic(): dedicated path for public content images (ADR-RTE-007, decoupled design).
    // Deliberately NOT testing store() here — store() is untouched/shared with expert-identity and
    // other existing callers, and is out of scope for this feature by design.

    private CloudinaryStorageService serviceWithMockClient(Cloudinary mockCloudinary) throws Exception {
        Constructor<CloudinaryStorageService> ctor =
                CloudinaryStorageService.class.getDeclaredConstructor(Cloudinary.class);
        ctor.setAccessible(true);
        return ctor.newInstance(mockCloudinary);
    }

    // The bug this feature addresses: content images must reach Cloudinary as type=upload
    // (permanent, public), regardless of MIME type — via a path that cannot affect store().
    @Test
    void storePublic_alwaysUploadsAsCloudinaryTypeUpload() throws Exception {
        Cloudinary mockCloudinary = mock(Cloudinary.class);
        Uploader mockUploader = mock(Uploader.class);
        when(mockCloudinary.uploader()).thenReturn(mockUploader);
        when(mockUploader.upload(any(), any())).thenReturn(Map.of("public_id", PUBLIC_ID));

        CloudinaryStorageService svc = serviceWithMockClient(mockCloudinary);
        svc.storePublic("files/x.jpg", new byte[]{1, 2, 3}, "image/jpeg");

        verify(mockUploader).upload(any(), argThat((Map<?, ?> opts) -> "upload".equals(opts.get("type"))));
    }
}
