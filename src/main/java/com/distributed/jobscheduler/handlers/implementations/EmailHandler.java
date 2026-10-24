package com.distributed.jobscheduler.handlers.implementations;

import com.distributed.jobscheduler.configs.RabbitQueueConfig;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.handlers.interfaces.JobStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class EmailHandler implements JobStrategy {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public String type() { return "EMAIL"; };

    @Override
    public void handle(JobEntity job) {
        log.info("SENDING EMAIL AT SCHEDULED TIME =========== {}", job.getJobId());

        String payload = new ObjectMapper().writeValueAsString(job.getPayload());
        String jobId = job.getType();
        CorrelationData correlationData = new CorrelationData(jobId);

        rabbitTemplate.convertAndSend(
                RabbitQueueConfig.EXCHANGE_KEY,
                RabbitQueueConfig.REGISTRATION_ROUTING_KEY,
                payload,
                message -> {
                    message.getMessageProperties().setMessageId(jobId);
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                },
                correlationData
        );
    }
}
