package com.distributed.jobscheduler.entities;

import com.distributed.jobscheduler.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity(name = "jobs")
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_status_run_at", columnList = "status, run_at")
})
public class JobEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private String jobId;

    @Version
    private Long version;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "type", nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private Map<String, Object> payload;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "run_at", nullable = false)
    private Instant runAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
