package com.spark.campusday.study.calendar.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({GoogleCalendarProperties.class,
        CalendarIntegrationProperties.class})
public class CalendarIntegrationConfiguration {
    @Bean("googleCalendarRestClient")
    RestClient googleCalendarRestClient() {
        return RestClient.create();
    }

    @Bean("studyCalendarSyncExecutor")
    TaskExecutor studyCalendarSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("study-calendar-sync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}
