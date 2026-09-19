package com.carebridge.backend.expertverification.async;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.stereotype.Component;

/**
 * Runs the CompreFace detect -> crop -> verify pipeline off the request thread.
 *
 * <p>This is a dedicated type rather than a plain {@link Executor} bean on purpose: injecting
 * {@code Executor} by type would clash with Spring Boot's own {@code applicationTaskExecutor},
 * and a qualifier-by-name fallback is easy to break later by renaming a field.</p>
 *
 * <p>The pool is wrapped in {@link DelegatingSecurityContextExecutor} so the submitting user's
 * authentication travels with the task. {@code FileServiceImpl} reads the caller's role from
 * {@code SecurityContextHolder} when it stamps {@code uploaderRole} on the cropped face files;
 * without propagation those rows would silently be written as PATIENT instead of EXPERT.</p>
 *
 * <p>Saturation falls back to {@link ThreadPoolExecutor.CallerRunsPolicy}: under a burst the
 * submission simply becomes as slow as it used to be instead of dropping the pipeline and
 * leaving the attempt stuck at PROCESSING.</p>
 */
@Slf4j
@Component
public class IdentityPipelineExecutor {

    private final Executor delegate;
    private final ThreadPoolTaskExecutor pool;

    // Explicit: the class has a second, private constructor for tests, and with more than one
    // declared constructor Spring stops guessing and looks for a no-arg one instead.
    @Autowired
    public IdentityPipelineExecutor(
            @Value("${carebridge.compreface.pipeline-core-threads:2}") int coreThreads,
            @Value("${carebridge.compreface.pipeline-max-threads:4}") int maxThreads,
            @Value("${carebridge.compreface.pipeline-queue-capacity:50}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreThreads);
        executor.setMaxPoolSize(Math.max(coreThreads, maxThreads));
        executor.setQueueCapacity(Math.max(0, queueCapacity));
        executor.setThreadNamePrefix("identity-pipeline-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Let an in-flight pipeline finish on shutdown; otherwise the attempt row would stay
        // at PROCESSING with its originals already stored.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        this.pool = executor;
        this.delegate = new DelegatingSecurityContextExecutor(executor);
    }

    private IdentityPipelineExecutor(Executor delegate) {
        this.delegate = delegate;
        this.pool = null;
    }

    /** Runs every task on the calling thread. For tests that assert on pipeline side effects. */
    public static IdentityPipelineExecutor sameThread() {
        return new IdentityPipelineExecutor(Runnable::run);
    }

    public void run(Runnable task) {
        delegate.execute(task);
    }

    @PreDestroy
    void shutdown() {
        if (pool != null) {
            pool.shutdown();
        }
    }
}
