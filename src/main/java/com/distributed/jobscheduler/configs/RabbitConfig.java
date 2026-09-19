package com.distributed.jobscheduler.configs;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.time.Duration;

@Configuration
public class RabbitConfig {
    @Bean
    public ConnectionFactory rabbitConnectionFactoryBean() throws Exception {
        RabbitConnectionFactoryBean factory = new RabbitConnectionFactoryBean();

        factory.setHost("puffin.rmq2.cloudamqp.com");
        factory.setPort(5671);
        factory.setUsername("hwfrgjdo");
        factory.setPassword("JRfAaIVATjJumNgua1dg6wl_2HKMmk8m");
        factory.setVirtualHost("hwfrgjdo");

        factory.setUseSSL(true);
        factory.setSslAlgorithm("TLSv1.2");
        factory.afterPropertiesSet();

        return factory.getObject();
    }

    @Bean
    public CachingConnectionFactory connectionFactory(ConnectionFactory connectionFactory) {
        CachingConnectionFactory factory = new CachingConnectionFactory(connectionFactory);

        factory.setChannelCacheSize(5);
        factory.setChannelCheckoutTimeout(5000);

        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);

        RetryPolicy connectionRetryPolicy = RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ofSeconds(2))
                .multiplier(2)
                .build();

        new RetryTemplate(connectionRetryPolicy).invoke(() -> {
            factory.createConnection().close();
            System.out.println("Successfully connected to RabbitMQ!");
        });

        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory factory, MessageConverter converter, RetryTemplate retryTemplate) {
        RabbitTemplate template = new RabbitTemplate();

        template.setMandatory(true);
        template.setRetryTemplate(retryTemplate);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Publish not confirmed:::::::: " + cause);
            }
        });

        template.setReturnsCallback((returned) -> {
            System.err.println("Message returned (unroutable): " + returned.getMessage());
        });

        return template;
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(2)
                .delay(Duration.ofSeconds(1))
                .multiplier(2)
                .maxDelay(Duration.ofSeconds(10))
                .build();

        return new RetryTemplate(retryPolicy);
    }
}
