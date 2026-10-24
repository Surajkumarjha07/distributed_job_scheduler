package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobReaper {
    private final JobRepository jobRepository;

    @Value("${scheduler.lock-timeout-seconds:300}")
    private long lockTimeoutSeconds;

    @Value("${scheduler.max-retries:3}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${scheduler.reaper-delay-ms:30000}")
    public void reap() {
        int reclaimed = jobRepository.reapStale(lockTimeoutSeconds, maxRetries);
        if (reclaimed > 0) {
            log.warn("Reaper reclaimed {} stale job(s)", reclaimed);
        }
    }
}
