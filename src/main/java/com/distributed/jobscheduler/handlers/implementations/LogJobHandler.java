package com.distributed.jobscheduler.handlers.implementations;

import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.handlers.interfaces.JobStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogJobHandler implements JobStrategy {
    @Override public String type() { return "LOG"; }

    @Override
    public void handle(JobEntity job) {
        log.info("Executing job {} for user {}: {}", job.getJobId(), job.getUserId(), job.getMessage());
    }
}
