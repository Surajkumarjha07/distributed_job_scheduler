package com.distributed.jobscheduler.services;

import com.distributed.jobscheduler.dto.CreateJobDTO;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.repositories.JobRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    @Autowired
    JobRepository jobRepository;

    public JobEntity createJob(@Valid CreateJobDTO request) {
        JobEntity job = new JobEntity();

        job.setUserId(request.userId());
        job.setType(request.type());
        job.setMessage(request.message());
        job.setPayload(request.payload());
        job.setRunAt(request.runAt());

        jobRepository.save(job);

        return job;
    }

    public Optional<JobEntity> getJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    public List<JobEntity> getJobs(String userId, String status, Pageable pageable) {
        return status == null
                ? jobRepository.findByUserId(userId)
                : jobRepository.findByUserIdAndStatus(userId, status);
    }

    @Transactional
    public List<JobEntity> claimJob(String workerId, int limit) {
        if (limit <= 0) return List.of();
        return jobRepository.claimBatch(workerId, limit);
    }

    @Transactional
    public void releaseJob(String jobId, String workerId) {
        if (jobId == null || workerId == null) return;
        jobRepository.release(jobId, workerId);
    }
}
