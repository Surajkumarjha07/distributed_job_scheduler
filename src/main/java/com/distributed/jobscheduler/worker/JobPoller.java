package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.dto.JobTask;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.services.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobPoller {
    private final JobService jobService;
    private final JobRunner jobRunner;
    private final ThreadPoolExecutor jobExecutor;
    private final String workerId = "worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${scheduler.poll-delay-ms:5000}")
    public void poll() {
        if (jobRunner.isShuttingDown()) return;

        int capacity = jobExecutor.getMaximumPoolSize() - jobExecutor.getActiveCount()
                        + jobExecutor.getQueue().remainingCapacity();

        if (capacity <= 0) return;

        List<JobEntity> jobs = jobService.claimJob(workerId, Math.min(capacity, 10));
        jobs.forEach(j -> log.info("claimed {} lockedBy={} lockedAt={}", j.getJobId(), j.getLockedBy(), j.getLockedAt()));
        for (JobEntity job: jobs) {
            try {
                jobExecutor.execute(new JobTask(job, jobRunner));
            } catch (RejectedExecutionException e) {
                log.warn("Executor rejected job {}, releasing", job.getJobId());
                jobService.releaseJob(job.getJobId(), workerId);
            }
        }
    }
}
