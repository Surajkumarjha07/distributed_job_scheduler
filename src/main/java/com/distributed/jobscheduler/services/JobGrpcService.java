package com.distributed.jobscheduler.services;

import com.distributed.jobscheduler.dto.CreateJobDTO;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.enums.JobStatus;
import com.distributed.jobscheduler.grpc.*;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobGrpcService extends JobServiceGrpc.JobServiceImplBase {
    private final JobService jobService;
    private final ObjectMapper mapper;

    private static Timestamp toTimeStamp(Instant time) {
        return Timestamp.newBuilder()
                .setSeconds(time.getEpochSecond())
                .setNanos(time.getNano())
                .build();
    }

    @Override
    public void submitJob(SubmitJobRequest req, StreamObserver<SubmitJobReply> out) {
        try {
            if (req.getUserId().isBlank() || req.getType().isBlank()) {
                throw new IllegalArgumentException("userId and type are required");
            }
            if (req.getPayloadJson().isBlank()) {
                throw new IllegalArgumentException("payloadJson is required");
            }
            if (!req.hasRunAt()) {
                throw new IllegalArgumentException("runAt is required");
            }

            Instant runAt = Instant.ofEpochSecond(
                    req.getRunAt().getSeconds(),
                    req.getRunAt().getNanos()
            );
            if (!runAt.isAfter(Instant.now())) {
                throw new IllegalArgumentException("runAt must be a future timestamp");
            }

            Map<String, Object> payload = mapper.readValue(req.getPayloadJson(), new TypeReference<>() {
            });

            CreateJobDTO dto = new CreateJobDTO(
                    req.getUserId(),
                    req.getType(),
                    payload,
                    req.getMessage(),
                    runAt
            );

            JobEntity job = jobService.createJob(dto);
            out.onNext(SubmitJobReply.newBuilder()
                    .setJobId(job.getJobId()).setStatus(job.getStatus().name()).build());
            out.onCompleted();
        } catch (JacksonException | IllegalArgumentException e) {
            log.warn("submitJob rejected", e);
            out.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getJob(GetJobRequest req, StreamObserver<JobReply> out) {
        try {
            String jobId = req.getJobId();

            if (jobId.isBlank()) {
                throw new IllegalArgumentException("JobId is not defined!");
            }

            Optional<JobEntity> found = jobService.getJob(jobId);
            if (found.isEmpty()) {
                out.onError(Status.NOT_FOUND.withDescription("job not found: " + jobId).asRuntimeException());
                return;
            }

            JobEntity job = found.get();
            out.onNext(JobReply.newBuilder()
                    .setJobId(job.getJobId())
                    .setType(job.getType())
                    .setStatus(job.getStatus().name())
                    .setRetryCount(job.getRetryCount())
                    .setRunAt(toTimeStamp(job.getRunAt()))
                    .build());
            out.onCompleted();
        } catch (JacksonException | IllegalArgumentException e) {
            out.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void cancelJob(CancelJobRequest req, StreamObserver<CancelJobReply> out) {
        try {
            String jobId = req.getJobId();

            if (jobId.isBlank()) {
                throw new IllegalArgumentException("jobId is not defined");
            }

            if (jobService.cancelJob(jobId) == 1) {
                out.onNext(CancelJobReply.newBuilder()
                        .setJobId(jobId)
                        .setStatus(JobStatus.CANCELLED.toString())
                        .build());

                out.onCompleted();
                return;
            }

            Optional<JobEntity> job = jobService.getJob(jobId);

            if (job.isEmpty()) {
                out.onError(Status.NOT_FOUND
                        .withDescription("job not found: " + jobId)
                        .asRuntimeException());
            } else {
                out.onError(Status.FAILED_PRECONDITION
                        .withDescription("cannot cancel, job is " + job.get().getStatus().name())
                        .asRuntimeException()
                );
            }
        } catch (JacksonException | IllegalArgumentException e) {
            out.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void updateJobStatus(UpdateJobStatusRequest req, StreamObserver<JobStatusReply> out) {
        try {
            String jobId = req.getJobId();
            String status = req.getStatus();

            if (jobId.isBlank() || status.isBlank()) {
                throw new IllegalArgumentException("jobId or status is not defined");
            }

            JobStatus newStatus;
            try {
                newStatus = JobStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown status: " + status);
            }

            int updated = jobService.updateJobStatus(jobId, JobStatus.valueOf(status));
            Optional<JobEntity> found = jobService.getJob(jobId);

            if (found.isEmpty()) {
                out.onError(Status.NOT_FOUND
                        .withDescription("job not found: " + jobId).asRuntimeException());
                return;
            }

            JobEntity job = found.get();

            if (updated == 0) {
                out.onError(Status.FAILED_PRECONDITION
                        .withDescription("cannot update, job is " + job.getStatus().name())
                        .asRuntimeException());
                return;
            }

            out.onNext(JobStatusReply.newBuilder()
                    .setJobId(jobId)
                    .setType(job.getType())
                    .setStatus(newStatus.name())
                    .build()
            );
            out.onCompleted();
        } catch (JacksonException | IllegalArgumentException e) {
            out.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}