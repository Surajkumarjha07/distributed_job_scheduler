package com.distributed.jobscheduler.handlers.implementations;

import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.handlers.interfaces.JobStrategy;
import com.distributed.jobscheduler.repositories.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class HTTPHandler implements JobStrategy {
    private final RestClient restClient;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final JobRepository jobRepository;

    @Override
    public String type() {
        return "HTTP";
    }

    @Override
    public void handle(JobEntity job) {
        String jobId = job.getJobId();
        String workerId = job.getLockedBy();

        Map<String, Object> payload = job.getPayload();
        String url = payload.get("url").toString();
        HttpMethod method = HttpMethod.valueOf(payload.get("method").toString().toUpperCase());

        @SuppressWarnings("unchecked")
        Map<String, String> headers = (Map<String, String>) payload.get("headers");
        Object body = payload.get("body");
        int timeoutMs = payload.get("timeoutMs") instanceof Number n ? n.intValue() : 5000;

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        RestClient client = restClient.mutate()
                .requestFactory(factory)
                .build();

        RestClient.RequestBodySpec request = client.method(method)
                .uri(url)
                .headers(h -> {
                    if (headers != null) {
                        headers.forEach(h::add);
                        h.add("Idempotency-Key", jobId);
                    }
                });

        if (body != null) {
            request.body(body);
        }

        ResponseEntity<String> response = request
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {})
                .toEntity(String.class);

        int status = response.getStatusCode().value();
        String responseBody = response.getBody();

        log.info("Status {}", status);
        log.info("Body {}", responseBody);

        HttpStatusCode code = response.getStatusCode();

        if (code.is2xxSuccessful()) {
            if (jobRepository.markCompleted(jobId, workerId) == 0) {
                log.warn("Job {} finished but lease was lost, not marking complete", jobId);
                return;
            }

            log.warn("Job {} finished, and marking complete", jobId);
        } else if (code.is4xxClientError() && code.value() != 408 && code.value() != 429) {
            jobRepository.markDead(jobId, workerId);
        } else {
            throw new IllegalStateException("HTTP call failed with status " + code.value());
        }
    }
}
