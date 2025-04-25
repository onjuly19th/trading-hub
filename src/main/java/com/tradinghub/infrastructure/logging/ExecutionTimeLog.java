package com.tradinghub.infrastructure.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행 시간을 측정하기 위한 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecutionTimeLog {
    /**
     * 로깅 레벨을 지정 (기본값: INFO)
     */
    String level() default "INFO";
    
    /**
     * 로깅 메시지 템플릿
     */
    String message() default "메서드 {} 실행 시간: {}ms";
} 