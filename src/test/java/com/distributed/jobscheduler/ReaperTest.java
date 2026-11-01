package com.distributed.jobscheduler;

import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.enums.JobStatus;
import com.distributed.jobscheduler.repositories.JobRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class ReaperTest {
    @Value("${spring.datasource.url}")
    String datasourceUrl;

    @Autowired
    JobRepository jobRepository;
    @Autowired
    EntityManager em;

    @BeforeEach
    void guardAndClean() {
        if (datasourceUrl == null || !datasourceUrl.contains("ep-weathered-pond-az78435m")) {
            throw new IllegalArgumentException("Tests must run against the Neon test branch");
        }

        jobRepository.deleteAllInBatch();
    }

    private JobEntity job(JobStatus status, String lockedBy, Instant lockedAt, int retryCount) {
        JobEntity j = new JobEntity();
        j.setUserId("u1");
        j.setType("EMAIL");
        j.setMessage("test message");
        j.setPayload(Map.of(
                "from", "sender@example.com",
                "to", "user@example.com",
                "username", "Test User",
                "type", "REGISTRATION",
                "channel", "EMAIL"));
        j.setStatus(status);
        j.setRunAt(Instant.now());
        j.setLockedBy(lockedBy);
        j.setLockedAt(lockedAt);
        j.setRetryCount(retryCount);
        return jobRepository.saveAndFlush(j);
    }

    private JobEntity reload(JobEntity j) {
        em.clear();
        return jobRepository.findById(j.getJobId()).orElseThrow();
    }

    @Test
    void resetsAbandonedJob() {
        JobEntity j = job(JobStatus.RUNNING, "worker-dead", Instant.now().minusSeconds(600), 0);

        assertThat(jobRepository.reapStale(30, 3)).isEqualTo(1);

        JobEntity after = reload(j);
        assertThat(after.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(after.getLockedBy()).isNull();
        assertThat(after.getLockedAt()).isNull();
        assertThat(after.getRetryCount()).isEqualTo(1);
    }

    @Test
    void ignoresFreshJob() {
        JobEntity j = job(JobStatus.RUNNING, "worker-live", Instant.now(), 0);

        assertThat(jobRepository.reapStale(30, 3)).isEqualTo(0);

        JobEntity after = reload(j);
        assertThat(after.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(after.getLockedBy()).isEqualTo("worker-live");
        assertThat(after.getRetryCount()).isEqualTo(0);
    }

    @Test
    void marksDeadWhenRetriesExhausted() {
        JobEntity j = job(JobStatus.RUNNING, "worker-dead", Instant.now().minusSeconds(600), 3);

        assertThat(jobRepository.reapStale(30, 3)).isEqualTo(1);

        assertThat(reload(j).getStatus()).isEqualTo(JobStatus.DEAD);
    }

    @Test
    void ignoresNonRunningJobs() {
        JobEntity pending = job(JobStatus.PENDING, null, Instant.now().minusSeconds(600), 0);
        JobEntity done = job(JobStatus.COMPLETED, null, Instant.now().minusSeconds(600), 0);

        assertThat(jobRepository.reapStale(30, 3)).isEqualTo(0);

        assertThat(reload(pending).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(reload(done).getStatus()).isEqualTo(JobStatus.COMPLETED);
    }
}