package com.carebridge.backend.systemconfiguration.service;

import com.carebridge.backend.systemconfiguration.repository.SystemConfigurationRepository;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/** Provides a low-cost, last-known view of the singleton maintenance flag for request filtering. */
@Service
public class SystemMaintenanceModeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemMaintenanceModeService.class);
    private static final long CACHE_NANOS = Duration.ofSeconds(2).toNanos();
    private static final long WARNING_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();

    private final SystemConfigurationRepository repository;
    private final TaskExecutor taskExecutor;
    private final AtomicBoolean refreshInProgress = new AtomicBoolean();
    private final Object stateLock = new Object();
    private final AtomicLong nextWarningNanos = new AtomicLong(Long.MIN_VALUE);

    private volatile boolean maintenanceEnabled;
    private volatile long refreshAfterNanos;
    private volatile boolean initialized;
    private long publicationGeneration;

    public SystemMaintenanceModeService(
            SystemConfigurationRepository repository,
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.repository = repository;
        this.taskExecutor = taskExecutor;
    }

    public boolean isMaintenanceEnabled() {
        long now = System.nanoTime();
        if (now < refreshAfterNanos) {
            return maintenanceEnabled;
        }
        if (!refreshInProgress.compareAndSet(false, true)) {
            return maintenanceEnabled;
        }

        if (!initialized) {
            refreshAndRelease();
            return maintenanceEnabled;
        }

        try {
            taskExecutor.execute(this::refreshAndRelease);
        } catch (RuntimeException exception) {
            refreshInProgress.set(false);
            deferRefreshAfterFailure(currentGeneration());
            warnRefreshFailure(exception);
        }
        return maintenanceEnabled;
    }

    public void updateFrom(boolean enabled) {
        synchronized (stateLock) {
            publicationGeneration++;
            maintenanceEnabled = enabled;
            initialized = true;
            refreshAfterNanos = System.nanoTime() + CACHE_NANOS;
        }
    }

    private void refreshAndRelease() {
        try {
            long expectedGeneration = currentGeneration();
            try {
                boolean refreshedValue = repository.findFirstByOrderByCreatedAtAsc()
                        .map(configuration -> configuration.isMaintenanceModeEnabled())
                        .orElse(false);
                publishRefresh(expectedGeneration, refreshedValue);
            } catch (RuntimeException exception) {
                deferRefreshAfterFailure(expectedGeneration);
                warnRefreshFailure(exception);
            }
        } finally {
            refreshInProgress.set(false);
        }
    }

    private long currentGeneration() {
        synchronized (stateLock) {
            return publicationGeneration;
        }
    }

    private void publishRefresh(long expectedGeneration, boolean enabled) {
        synchronized (stateLock) {
            if (publicationGeneration != expectedGeneration) {
                return;
            }
            maintenanceEnabled = enabled;
            initialized = true;
            refreshAfterNanos = System.nanoTime() + CACHE_NANOS;
        }
    }

    private void deferRefreshAfterFailure(long expectedGeneration) {
        synchronized (stateLock) {
            if (publicationGeneration != expectedGeneration) {
                return;
            }
            initialized = true;
            refreshAfterNanos = System.nanoTime() + CACHE_NANOS;
        }
    }

    private void warnRefreshFailure(RuntimeException exception) {
        long now = System.nanoTime();
        long nextWarning = nextWarningNanos.get();
        if (now >= nextWarning
                && nextWarningNanos.compareAndSet(nextWarning, now + WARNING_INTERVAL_NANOS)) {
            LOGGER.warn("Unable to refresh maintenance mode; retaining the last known state", exception);
        } else {
            LOGGER.debug("Unable to refresh maintenance mode; retaining the last known state", exception);
        }
    }
}
