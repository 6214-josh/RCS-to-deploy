package com.rcs.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 針對工業高併發優化參數
        executor.setCorePoolSize(10); // 核心執行緒數
        executor.setMaxPoolSize(50);  // 最大執行緒數
        executor.setQueueCapacity(500); // 隊列容量
        executor.setThreadNamePrefix("RCS-Async-");
        executor.initialize();
        return executor;
    }
}
