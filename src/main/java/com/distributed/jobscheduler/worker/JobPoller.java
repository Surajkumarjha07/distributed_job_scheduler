package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.services.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

@Component
@RequiredArgsConstructor
public class JobPoller {
    private final JobService jobService;   // short @Transactional claim
//    private final JobRunner jobRunner;            // executes + updates status
    private final ThreadPoolExecutor jobExecutor;
    private final String workerId = "worker-" + UUID.randomUUID();

    @Scheduled(fixedDelayString = "${scheduler.poll-delay-ms:5000}")
    public void poll() {
        int capacity = jobExecutor.getMaximumPoolSize() - jobExecutor.getActiveCount()
                        + jobExecutor.getQueue().remainingCapacity();

        if (capacity <= 0) return;

        List<JobEntity> jobs = jobService.claimJob(workerId, Math.min(capacity, 10));
        for (JobEntity job: jobs) {
//            jobExecutor.execute(() -> jobRunner.run(job);
        }
    }
}
