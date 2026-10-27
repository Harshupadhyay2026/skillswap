package com.skillswap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables the best-effort async email path used by notifications.
 *
 * A small bounded pool keeps SMTP latency off request threads without letting
 * a mail outage grow an unbounded queue. When the queue is full the task is
 * dropped by the caller, which then logs it; in-app notifications are already
 * saved, so nothing user-visible is lost.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationEmailExecutor")
    public Executor notificationEmailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("notification-mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
