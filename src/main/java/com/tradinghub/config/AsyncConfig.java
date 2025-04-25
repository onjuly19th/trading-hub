package com.tradinghub.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 이벤트 처리를 위한 설정 클래스
 * 이벤트 핸들러의 비동기 실행을 위한 스레드 풀 및 관련 설정을 정의합니다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 비동기 작업을 처리할 스레드 풀 설정
     * 
     * @return 설정된 스레드 풀 실행기
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 풀 크기
        executor.setCorePoolSize(5);
        
        // 최대 스레드 풀 크기
        executor.setMaxPoolSize(10);
        
        // 작업 대기열 크기
        executor.setQueueCapacity(25);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("Async-");
        
        // 태스크가 완료되면 스레드를 제거
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 초기화
        executor.initialize();
        
        return executor;
    }
} 