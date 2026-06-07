package com.backend.interactionservice.common.config.scheduling;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SpringSchedulingConfig implements SchedulingConfigurer {

    @Value("${scheduling.pool-size:2}")
    private int poolSize;

    @Value("${scheduling.thread-name-prefix:interaction-scheduler-}")
    private String threadNamePrefix;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}
