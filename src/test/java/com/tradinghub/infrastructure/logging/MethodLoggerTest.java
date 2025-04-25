package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MethodLoggerTest {

    private MethodLogger methodLogger;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature signature;

    @BeforeEach
    void setUp() {
        methodLogger = new MethodLogger();
    }

    @Test
    void logExecutionTime_success() throws Throwable {
        // given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(TestService.class);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testArg"});
        when(joinPoint.proceed()).thenReturn("testResult");

        // when
        Object result = methodLogger.logExecutionTime(joinPoint);

        // then
        assertEquals("testResult", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logExecutionTime_exception() throws Throwable {
        // given
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn(TestService.class);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testArg"});
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));

        // when & then
        assertThrows(RuntimeException.class, () -> methodLogger.logExecutionTime(joinPoint));
        verify(joinPoint).proceed();
    }

    // 테스트를 위한 내부 클래스
    private static class TestService {
        @SuppressWarnings("unused")
        public String testMethod(String arg) {
            return arg;
        }
    }
} 