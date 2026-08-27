package com.carebridge.backend.file;

import com.carebridge.backend.common.exception.BusinessException;
import com.carebridge.backend.file.service.StorageServiceResolver;
import com.carebridge.backend.file.service.impl.CloudinaryStorageService;
import com.carebridge.backend.file.service.impl.R2StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceResolverTest {

    @Mock private CloudinaryStorageService cloudinaryStorageService;
    @Mock private R2StorageService r2StorageService;
    @Mock private ObjectProvider<R2StorageService> r2StorageServiceProvider;

    private StorageServiceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new StorageServiceResolver(cloudinaryStorageService, r2StorageServiceProvider);
    }

    @Test
    void resolveCloudinary_returnsCloudinaryAdapter() {
        assertThat(resolver.resolve("cloudinary")).isSameAs(cloudinaryStorageService);
    }

    @Test
    void resolveR2_whenConfigured_returnsR2Adapter() {
        when(r2StorageServiceProvider.getIfAvailable()).thenReturn(r2StorageService);

        assertThat(resolver.resolve("r2")).isSameAs(r2StorageService);
    }

    @Test
    void resolveR2_whenMissing_throwsFile005() {
        when(r2StorageServiceProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> resolver.resolve("r2"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo("FILE-005");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    @Test
    void resolveUnknownProvider_throwsFile006() {
        assertThatThrownBy(() -> resolver.resolve("unknown"))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getCode()).isEqualTo("FILE-006");
                    assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}
