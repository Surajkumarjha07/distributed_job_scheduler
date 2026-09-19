package com.distributed.jobscheduler.configs;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitQueueConfig {
    public static final String EXCHANGE_KEY = "notification.exchange";

    public static final String REGISTRATION_ROUTING_KEY = "notification.registration";
    public static final String INVITATION_ROUTING_KEY = "notification.invitation";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_KEY);
    }
}
