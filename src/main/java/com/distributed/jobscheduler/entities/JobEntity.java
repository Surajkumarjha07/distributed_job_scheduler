package com.distributed.jobscheduler.entities;

import com.distributed.jobscheduler.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity(name = "jobs")
@Table(name = "jobs")
public class JobEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private String jobId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "run_at", nullable = false)
    private Instant runAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "locked_by")
    private String LockedBy;

    @Column(name = "locked_at")
    private Instant LockedAt;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
