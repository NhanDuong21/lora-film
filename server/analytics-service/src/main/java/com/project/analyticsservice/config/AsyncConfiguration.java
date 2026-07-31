package com.project.analyticsservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfiguration {
    @Bean(name = "analyticsJobExecutor")
    public Executor analyticsJobExecutor(
            @Value("${analytics.jobs.pool-size:2}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(1, Math.min(poolSize, 4)));
        executor.setMaxPoolSize(Math.max(1, Math.min(poolSize, 4)));
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("analytics-job-");
        executor.initialize();
        return executor;
    }
}
