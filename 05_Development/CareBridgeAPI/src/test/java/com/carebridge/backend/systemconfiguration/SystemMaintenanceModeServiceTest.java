package com.carebridge.backend.systemconfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.carebridge.backend.systemconfiguration.entity.SystemConfiguration;
import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import com.carebridge.backend.systemconfiguration.service.SystemMaintenanceModeService;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

class SystemMaintenanceModeServiceTest {

    private final SystemConfigurationRepository repository = mock(SystemConfigurationRepository.class);
    private final TaskExecutor sameThreadExecutor = Runnable::run;
    private final SystemMaintenanceModeService service =
            new SystemMaintenanceModeService(repository, sameThreadExecutor);

    @Test
    void isMaintenanceEnabled_cachesRepositoryValueWithinRefreshWindow() {
        SystemConfiguration configuration = new SystemConfiguration();
        configuration.setMaintenanceModeEnabled(true);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(configuration));

        assertThat(service.isMaintenanceEnabled()).isTrue();
        assertThat(service.isMaintenanceEnabled()).isTrue();

        verify(repository).findFirstByOrderByCreatedAtAsc();
        verifyNoMoreInteractions(repository);
    }

    @Test
    void isMaintenanceEnabled_retainsLastKnownStateWhenRefreshFails() {
        service.updateFrom(true);
        ReflectionTestUtils.setField(service, "refreshAfterNanos", 0L);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenThrow(new IllegalStateException("database unavailable"));

        assertThat(service.isMaintenanceEnabled()).isTrue();
        verify(repository).findFirstByOrderByCreatedAtAsc();
    }

    @Test
    void isMaintenanceEnabled_doesNotQueueConcurrentRequestsBehindRefresh() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch allowRefreshToFinish = new CountDownLatch(1);
        SystemConfiguration configuration = new SystemConfiguration();
        configuration.setMaintenanceModeEnabled(true);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenAnswer(invocation -> {
            refreshStarted.countDown();
            assertThat(allowRefreshToFinish.await(2, TimeUnit.SECONDS)).isTrue();
            return Optional.of(configuration);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> refresh = executor.submit(service::isMaintenanceEnabled);
            assertThat(refreshStarted.await(2, TimeUnit.SECONDS)).isTrue();

            assertThat(service.isMaintenanceEnabled()).isFalse();
            allowRefreshToFinish.countDown();
            assertThat(refresh.get(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            allowRefreshToFinish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void isMaintenanceEnabled_defaultsToDisabledWhenConfigurationDoesNotExist() {
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        assertThat(service.isMaintenanceEnabled()).isFalse();
    }

    @Test
    void isMaintenanceEnabled_afterInitialization_returnsStaleValueWhileRefreshingInBackground() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch allowRefreshToFinish = new CountDownLatch(1);
        CountDownLatch refreshFinished = new CountDownLatch(1);
        SystemConfiguration configuration = new SystemConfiguration();
        configuration.setMaintenanceModeEnabled(true);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenAnswer(invocation -> {
            refreshStarted.countDown();
            try {
                assertThat(allowRefreshToFinish.await(2, TimeUnit.SECONDS)).isTrue();
                return Optional.of(configuration);
            } finally {
                refreshFinished.countDown();
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Future<?>> refreshTask = new AtomicReference<>();
        SystemMaintenanceModeService asyncService = new SystemMaintenanceModeService(
                repository, command -> refreshTask.set(executor.submit(command)));
        asyncService.updateFrom(false);
        ReflectionTestUtils.setField(asyncService, "refreshAfterNanos", 0L);
        try {
            assertThat(asyncService.isMaintenanceEnabled()).isFalse();
            assertThat(refreshStarted.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(asyncService.isMaintenanceEnabled()).isFalse();

            allowRefreshToFinish.countDown();
            assertThat(refreshFinished.await(2, TimeUnit.SECONDS)).isTrue();
            refreshTask.get().get(2, TimeUnit.SECONDS);
            assertThat(asyncService.isMaintenanceEnabled()).isTrue();
        } finally {
            allowRefreshToFinish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void updateFrom_winsOverRefreshThatStartedWithOlderGeneration() throws Exception {
        CountDownLatch refreshStarted = new CountDownLatch(1);
        CountDownLatch allowRefreshToFinish = new CountDownLatch(1);
        CountDownLatch refreshFinished = new CountDownLatch(1);
        SystemConfiguration staleConfiguration = new SystemConfiguration();
        staleConfiguration.setMaintenanceModeEnabled(false);
        when(repository.findFirstByOrderByCreatedAtAsc()).thenAnswer(invocation -> {
            refreshStarted.countDown();
            try {
                assertThat(allowRefreshToFinish.await(2, TimeUnit.SECONDS)).isTrue();
                return Optional.of(staleConfiguration);
            } finally {
                refreshFinished.countDown();
            }
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Future<?>> refreshTask = new AtomicReference<>();
        SystemMaintenanceModeService asyncService = new SystemMaintenanceModeService(
                repository, command -> refreshTask.set(executor.submit(command)));
        asyncService.updateFrom(false);
        ReflectionTestUtils.setField(asyncService, "refreshAfterNanos", 0L);
        try {
            assertThat(asyncService.isMaintenanceEnabled()).isFalse();
            assertThat(refreshStarted.await(2, TimeUnit.SECONDS)).isTrue();

            asyncService.updateFrom(true);
            allowRefreshToFinish.countDown();

            assertThat(refreshFinished.await(2, TimeUnit.SECONDS)).isTrue();
            refreshTask.get().get(2, TimeUnit.SECONDS);
            assertThat(asyncService.isMaintenanceEnabled()).isTrue();
        } finally {
            allowRefreshToFinish.countDown();
            executor.shutdownNow();
        }
    }
}
