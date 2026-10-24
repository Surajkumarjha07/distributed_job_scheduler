package com.distributed.jobscheduler.repositories;

import com.distributed.jobscheduler.entities.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
    List<JobEntity> findByUserId(String userId);
    List<JobEntity> findByUserIdAndStatus(String userId, String status);

    @Query(value = """
        UPDATE jobs
            SET status     = 'RUNNING',
                locked_by  = :workerId,
                locked_at  = now(),
                updated_at = now(),
                version    = version + 1
            WHERE job_id IN (
                SELECT job_id
                FROM jobs
                WHERE status = 'PENDING'
                    AND run_at <= now()
                ORDER BY run_at
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            )
            RETURNING *
    """, nativeQuery = true)
    List<JobEntity> claimBatch(@Param("workerId") String workerId, @Param("limit") int limit);

    @Modifying
    @Query(value = """
         UPDATE jobs
            SET status     = 'PENDING',
                locked_by  = NULL,
                locked_at  = NULL,
                updated_at = now(),
                version    = version + 1
            WHERE job_id    = :jobId
                AND status    = 'RUNNING'
                AND locked_by = :workerId
    """, nativeQuery = true)
    void release(@Param("jobId") String jobId, @Param("workerId") String workerId);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE jobs
            SET status = 'COMPLETED',
                locked_by = NULL,
                locked_at = NULL,
                updated_at = now(),
                version = version + 1
            WHERE job_id = :jobId AND status = 'RUNNING' AND locked_by = :workerId
    """, nativeQuery = true)
    int markCompleted(@Param("jobId") String jobId, @Param("workerId") String workerId);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE jobs
            SET status = 'PENDING',
                retry_count = retry_count + 1,
                run_at = now() + make_interval(secs => CAST(:delaySeconds AS double precision)),
                locked_by = NULL,
                locked_at = NULL,
                updated_at = now(),
                version = version + 1
            WHERE job_id = :jobId AND status = 'RUNNING' AND locked_by = :workerId
    """, nativeQuery = true)
    int markRetry(@Param("jobId") String jobId, @Param("workerId") String workerId,
                   @Param("delaySeconds") long delaySeconds);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE jobs
    SET status = 'DEAD', retry_count = retry_count + 1,
        locked_by = NULL, locked_at = NULL,
        updated_at = now(), version = version + 1
    WHERE job_id = :jobId AND status = 'RUNNING' AND locked_by = :workerId
    """, nativeQuery = true)
    int markDead(@Param("jobId") String jobId, @Param("workerId") String workerId);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE jobs
        SET status = CASE WHEN retry_count + 1 > :maxRetries THEN 'DEAD' ELSE 'PENDING' END,
            retry_count = retry_count + 1,
            locked_by = NULL, locked_at = NULL,
            updated_at = now(), version = version + 1
        WHERE status = 'RUNNING'
            AND locked_at < now() - make_interval(secs => CAST(:timeoutSeconds AS double precision))
    """, nativeQuery = true)
    int reapStale(@Param("timeoutSeconds") long timeoutSeconds,
                  @Param("maxRetries") int maxRetries);

    @Transactional
    @Modifying
    @Query(value = """
    UPDATE jobs SET locked_at = now()
    WHERE job_id = :jobId AND status = 'RUNNING' AND locked_by = :token
    """, nativeQuery = true)
    int heartbeat(@Param("jobId") String jobId, @Param("token") String token);
}
