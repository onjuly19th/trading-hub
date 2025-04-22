package com.tradinghub.infrastructure.aop;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.aspectj.lang.JoinPoint;
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
class SecurityAspectTest {

    @InjectMocks
    private SecurityAspect securityAspect;

    @Mock
    private JoinPoint joinPoint;

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
        when(signature.getDeclaringTypeName()).thenReturn("TestClass");
        when(signature.getName()).thenReturn("testMethod");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void logControllerAccess_withAuthenticatedUser() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");

        // When
        securityAspect.logControllerAccess(joinPoint);

        // Then
        verify(joinPoint).getSignature();
    }

    @Test
    void logControllerAccess_withAnonymousUser() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(null);

        // When
        securityAspect.logControllerAccess(joinPoint);

        // Then
        verify(joinPoint).getSignature();
    }

    @Test
    void logOrderOperation_withAuthenticatedUser() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(user.getUsername()).thenReturn("testUser");
        when(user.getId()).thenReturn(1L);

        // When
        securityAspect.logOrderOperation(joinPoint, new Object());

        // Then
        verify(joinPoint).getSignature();
    }

    @Test
    void logSecurityException_withUnauthorizedOperationException() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        UnauthorizedOperationException ex = new UnauthorizedOperationException("Test exception");

        // When
        securityAspect.logSecurityException(joinPoint, ex);

        // Then
        verify(joinPoint).getSignature();
    }

    @Test
    void logSecurityException_withOtherException() {
        // Given
        RuntimeException ex = new RuntimeException("Test exception");

        // When
        securityAspect.logSecurityException(joinPoint, ex);

        // Then
        verify(joinPoint).getSignature();
    }
} 