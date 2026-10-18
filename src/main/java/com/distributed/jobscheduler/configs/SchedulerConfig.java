package com.distributed.jobscheduler.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public ThreadPoolExecutor jobExecutor() {
        return new ThreadPoolExecutor(
                8, 20, 0L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(16),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
