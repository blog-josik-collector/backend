package com.backend.integratedworker.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SpringThreadPoolConfig {

    @Bean("collectingExecutor")
    public ThreadPoolTaskExecutor collectingExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("collecting-");
        return executor;
    }
}
