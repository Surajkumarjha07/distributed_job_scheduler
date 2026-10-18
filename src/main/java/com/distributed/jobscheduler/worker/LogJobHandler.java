package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.entities.JobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogJobHandler implements JobHandler {
    @Override public String type() { return "LOG"; }

    @Override
    public void handle(JobEntity job) {
        log.info("Executing job {} for user {}: {}", job.getJobId(), job.getUserId(), job.getMessage());
    }
}
