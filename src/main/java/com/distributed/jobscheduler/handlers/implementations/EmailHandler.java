package com.distributed.jobscheduler.handlers.implementations;

import com.distributed.jobscheduler.configs.RabbitQueueConfig;
import com.distributed.jobscheduler.entities.JobEntity;
import com.distributed.jobscheduler.handlers.interfaces.JobStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailHandler implements JobStrategy {
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public String type() { return "EMAIL"; };

    @Override
    public void handle(JobEntity job) throws InterruptedException {
        log.info("SENDING EMAIL AT SCHEDULED TIME =========== {}", job.getJobId());

        String jobId = job.getJobId();
        CorrelationData correlationData = new CorrelationData(jobId);

        rabbitTemplate.convertAndSend(
                RabbitQueueConfig.EXCHANGE_KEY,
                RabbitQueueConfig.REGISTRATION_ROUTING_KEY,
                job.getPayload(),
                message -> {
                    message.getMessageProperties().setMessageId(jobId);
                    message.getMessageProperties().setHeader("__TypeId__", "registration");
                    return message;
                },
                correlationData
        );
    }
}
