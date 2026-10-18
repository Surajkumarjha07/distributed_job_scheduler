package com.distributed.jobscheduler.worker;

import com.distributed.jobscheduler.entities.JobEntity;

public interface JobHandler {
    String type();
    void handle(JobEntity job) throws Exception;
}