package com.distributed.jobscheduler.handlers.interfaces;

import com.distributed.jobscheduler.entities.JobEntity;

public interface JobStrategy {
    String type();
    void handle(JobEntity job) throws Exception;
}