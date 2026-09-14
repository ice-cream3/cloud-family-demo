package com.kfpd.cloud.manager.base.config;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
@EnableAsync
public class ManagerAsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(ManagerAsyncConfig.class);

    @Bean
    Executor managerOperationLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("manager-operation-log-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(1000);
        executor.setTaskDecorator(runnable -> {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            return () -> {
                try {
                    if (requestAttributes != null) {
                        RequestContextHolder.setRequestAttributes(requestAttributes);
                    }
                    runnable.run();
                } finally {
                    RequestContextHolder.resetRequestAttributes();
                }
            };
        });
        executor.setRejectedExecutionHandler((runnable, threadPoolExecutor) -> log.warn(
                "Operation log async queue is full, dropping log task: activeCount={}, poolSize={}, queueSize={}",
                threadPoolExecutor.getActiveCount(),
                threadPoolExecutor.getPoolSize(),
                threadPoolExecutor.getQueue().size()
        ));
        executor.initialize();
        return executor;
    }
}
