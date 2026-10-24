package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.repositories.JobRepository;
import com.distributed.jobscheduler.handlers.interfaces.JobStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JobRunner {
    private final JobRepository jobRepository;
    private final Map<String, JobStrategy> handlers;
    private final int maxRetries;
    private final long retryBaseSeconds;
    private final ScheduledExecutorService heartbeatScheduler;

    @Value("${scheduler.heartbeat-seconds:10}")
    private long heartbeatSeconds;

    public JobRunner(
            JobRepository jobRepository,
            List<JobStrategy> handlerList,
            @Value("${scheduler.max-retries:3}") int maxRetries,
            @Value("${scheduler.retry-base-seconds:5}") long retryBaseSeconds,
            ScheduledExecutorService heartbeatScheduler
    ) {
        this.jobRepository = jobRepository;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(JobStrategy::type, Function.identity()));
        this.maxRetries = maxRetries;
        this.retryBaseSeconds = retryBaseSeconds;
        this.heartbeatScheduler = heartbeatScheduler;
    }

    public void run(JobEntity job) {
        String jobId = job.getJobId();
        String workerId = job.getLockedBy();

        JobStrategy handler = handlers.get(job.getType());

        if (handler == null) {
            log.error("No handler for type '{}', job {} -> DEAD", job.getType(), jobId);
            jobRepository.markDead(jobId, workerId);
            return;
        }

        ScheduledFuture<?> hb = startHeartbeat(jobId, workerId, Thread.currentThread());

        try {
            handler.handle(job);
            if (jobRepository.markCompleted(jobId, workerId) == 0) {
                log.warn("Job {} finished but was no longer ours (reclaimed?)", jobId);
            }
        } catch (Exception e) {
            handleFailure(job, workerId, e);
        } finally {
            hb.cancel(false);
            Thread.interrupted();
        }
    }

    public void handleFailure(JobEntity job, String workerId, Exception e) {
        int attempts = job.getRetryCount();
        if (attempts >= maxRetries) {
            log.error("Job {} failed permanently after {} retries", job.getJobId(), attempts, e);
            jobRepository.markDead(job.getJobId(), workerId);
        } else {
            long delay = backoffSeconds(attempts);
            log.warn("Job {} failed (attempt {}), retrying in {}s: {}",
                    job.getJobId(), attempts + 1, delay, e.toString());
            jobRepository.markRetry(job.getJobId(), workerId, delay);
        }
    }

    private long backoffSeconds(int attempt) {
        long exp = Math.min(retryBaseSeconds * (1L << Math.min(attempt, 3)), 80);
        return exp + ThreadLocalRandom.current().nextLong(exp / 5 + 1);
    }

    private ScheduledFuture<?> startHeartbeat(String jobId, String token, Thread runnerThread) {
        AtomicInteger failures = new AtomicInteger();

        return heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (jobRepository.heartbeat(jobId, token) == 0) {
                    log.warn("Lost lease on job {}, interrupting", jobId);
                    runnerThread.interrupt();
                    throw new CancellationException();
                }

                failures.set(0);
            } catch (CancellationException e) {
                throw e;
            } catch (Exception e) {
                if (failures.incrementAndGet() >= 3) {
                    log.error("Heartbeat failed 3 times for job {}, interrupting", jobId, e);
                    runnerThread.interrupt();
                    throw new CancellationException();
                }
                log.warn("Heartbeat failed for job {}", jobId, e);
            }
        }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
    }
}
