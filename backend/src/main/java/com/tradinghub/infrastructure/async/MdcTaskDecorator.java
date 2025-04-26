package com.tradinghub.infrastructure.async;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 비동기 작업 실행 시 MDC(Mapped Diagnostic Context) 컨텍스트를 부모 스레드에서
 * 자식 스레드(작업 실행 스레드)로 전파하는 TaskDecorator.
 *
 * @Async 와 함께 사용되는 ThreadPoolTaskExecutor에 설정하여 사용합니다.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        // 작업을 제출하는 스레드(부모 스레드)의 MDC 컨텍스트를 가져옴
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                // 작업을 실행하는 스레드(자식 스레드)에 부모 스레드의 MDC 컨텍스트를 설정
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                // 실제 작업 실행
                runnable.run();
            } finally {
                // 작업 실행 후 자식 스레드의 MDC 컨텍스트 정리 (매우 중요)
                MDC.clear();
            }
        };
    }
}
