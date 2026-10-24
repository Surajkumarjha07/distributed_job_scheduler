package com.distributed.jobscheduler.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.*;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    @Bean
    public ThreadPoolExecutor jobExecutor() {
        return new ThreadPoolExecutor(
                8, 20, 0L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(20),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService heartbeatScheduler() {
        return Executors.newScheduledThreadPool(3);
    }
}
