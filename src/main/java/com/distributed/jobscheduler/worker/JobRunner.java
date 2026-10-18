package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.repositories.JobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JobRunner {
    private final JobRepository jobRepository;
    private final Map<String, JobHandler> handlers;
    private final int maxRetries;
    private final long retryBaseSeconds;

    public JobRunner(
            JobRepository jobRepository,
            List<JobHandler> handlerList,
            @Value("${scheduler.max-retries:3}") int maxRetries,
            @Value("${scheduler.retry-base-seconds:5}") long retryBaseSeconds
    ) {
        this.jobRepository = jobRepository;
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(JobHandler::type, Function.identity()));
        this.maxRetries = maxRetries;
        this.retryBaseSeconds = retryBaseSeconds;
    }

    public void run() {

    }
}
