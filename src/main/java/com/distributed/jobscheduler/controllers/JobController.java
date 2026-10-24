package com.distributed.jobscheduler.controllers;

import com.distributed.jobscheduler.dto.CreateJobDTO;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    JobService jobService;

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@Valid CreateJobDTO request) {
        JobEntity job = jobService.createJob(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<?> getJob(@PathVariable String jobId) {
        Optional<JobEntity> job = jobService.getJob(jobId);

        if (job.isEmpty())
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return ResponseEntity.status(HttpStatus.FOUND).body(job);
    }

    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam String userId,
            @RequestParam String status,
            @PageableDefault(
                    size = 10, sort = "createdAt", direction = Sort.Direction.DESC
            ) Pageable pageable) {
        List<JobEntity> jobs = jobService.getJobs(userId, status, pageable);

        return ResponseEntity.status(HttpStatus.FOUND).body(jobs);
    }

}
