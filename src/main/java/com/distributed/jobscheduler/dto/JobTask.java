package com.distributed.jobscheduler.dto;

import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.worker.JobRunner;

public record JobTask(JobEntity job, JobRunner runner) implements Runnable {
    @Override public void run() { runner.run(job); }
}