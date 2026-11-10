package com.distributed.jobscheduler.controllers;

import com.distributed.jobscheduler.dto.CreateJobDTO;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.services.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    JobService jobService;

    @PostMapping("/create")
    public ResponseEntity<?> createJob(@Valid @RequestBody CreateJobDTO request) {
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

    @PutMapping("/cancel/{jobId}")
    public ResponseEntity<?> cancelJob(@PathVariable String jobId) {
        try {
            if (jobService.cancelJob(jobId) == 1) {
                return ResponseEntity.ok(Map.of("jobId", jobId, "status", "CANCELLED"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        return jobService.getJob(jobId)
                .<ResponseEntity<?>>map(job -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "cannot cancel, job is "
                                + job.getStatus().name())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "job not found: " + jobId)));
    }
}
