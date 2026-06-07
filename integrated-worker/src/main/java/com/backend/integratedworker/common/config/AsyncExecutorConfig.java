package com.backend.integratedworker.common.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncExecutorConfig {

    @Bean("collectingExecutor")
    public ThreadPoolTaskExecutor collectingExecutor(
            @Value("${async.collecting.core-pool-size:5}") int corePoolSize,
            @Value("${async.collecting.max-pool-size:10}") int maxPoolSize,
            @Value("${async.collecting.queue-capacity:0}") int queueCapacity,
            @Value("${async.collecting.thread-name-prefix:collecting-}") String threadNamePrefix) {

        return buildExecutor(corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);
    }

    @Bean("indexingExecutor")
    public ThreadPoolTaskExecutor indexingExecutor(
            @Value("${async.indexing.core-pool-size:2}") int corePoolSize,
            @Value("${async.indexing.max-pool-size:5}") int maxPoolSize,
            @Value("${async.indexing.queue-capacity:100}") int queueCapacity,
            @Value("${async.indexing.thread-name-prefix:indexing-}") String threadNamePrefix) {

        return buildExecutor(corePoolSize, maxPoolSize, queueCapacity, threadNamePrefix);
    }

    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize,
                                                 int maxPoolSize,
                                                 int queueCapacity,
                                                 String threadNamePrefix) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        return executor;
    }
}
