package com.example.slabiak.appointmentscheduler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.util.ErrorHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class AsyncExecutionConfig implements AsyncConfigurer, SchedulingConfigurer {

    @Value("${app.async.mail.core-pool-size:2}")
    private int mailCorePoolSize;

    @Value("${app.async.mail.max-pool-size:4}")
    private int mailMaxPoolSize;

    @Value("${app.async.mail.queue-capacity:50}")
    private int mailQueueCapacity;

    @Value("${app.async.mail.await-termination-seconds:30}")
    private int mailAwaitTerminationSeconds;

    @Value("${app.scheduling.pool-size:2}")
    private int schedulerPoolSize;

    @Value("${app.scheduling.await-termination-seconds:30}")
    private int schedulerAwaitTerminationSeconds;

    @Bean(name = "mailExecutor")
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(mailCorePoolSize);
        executor.setMaxPoolSize(mailMaxPoolSize);
        executor.setQueueCapacity(mailQueueCapacity);
        executor.setThreadNamePrefix("mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(mailAwaitTerminationSeconds);
        return executor;
    }

    @Bean(name = "taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerPoolSize);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(schedulerAwaitTerminationSeconds);
        scheduler.setErrorHandler(taskSchedulerErrorHandler());
        return scheduler;
    }

    @Bean
    public ErrorHandler taskSchedulerErrorHandler() {
        return throwable -> log.error("Scheduled task execution failed", throwable);
    }

    @Override
    public TaskExecutor getAsyncExecutor() {
        return mailExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncUncaughtExceptionHandler();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }

    private static final class LoggingAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Async execution failed for method {}", method.getName(), ex);
        }
    }
}
