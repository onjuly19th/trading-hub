package com.tradinghub.infrastructure.logging;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.tradinghub.common.exception.auth.UnauthorizedOperationException;
import com.tradinghub.domain.user.User;

@ExtendWith(MockitoExtension.class)
class ControllerLoggerTest {

    @InjectMocks
    private ControllerLogger controllerLogger;

    @Mock
    private JoinPoint joinPoint;
    
    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private org.aspectj.lang.Signature signature;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private User user;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(proceedingJoinPoint.getTarget()).thenReturn(new Object());
        when(signature.getDeclaringTypeName()).thenReturn("TestClass");
        when(signature.getName()).thenReturn("testMethod");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void logControllerExecution_withAuthenticatedUser() throws Throwable {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        when(proceedingJoinPoint.proceed()).thenReturn("testResult");
        
        // When
        controllerLogger.logControllerExecution(proceedingJoinPoint);
        
        // Then
        verify(proceedingJoinPoint).proceed();
    }

    @Test
    void logControllerExecution_withException() throws Throwable {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException("Test exception"));
        
        // When & Then
        try {
            controllerLogger.logControllerExecution(proceedingJoinPoint);
        } catch (Exception e) {
            // Exception을 catch하고 verify
            verify(proceedingJoinPoint).proceed();
        }
    }

    @Test
    void logAfterThrowing_withException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        Exception ex = new RuntimeException("Test exception");

        // When
        controllerLogger.logAfterThrowing(joinPoint, ex);

        // Then
        verify(joinPoint).getSignature();
    }

    @Test
    void logAfterThrowing_withBusinessException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        UnauthorizedOperationException ex = new UnauthorizedOperationException("Test exception");

        // When
        controllerLogger.logAfterThrowing(joinPoint, ex);

        // Then
        verify(joinPoint).getSignature();
    }
} 