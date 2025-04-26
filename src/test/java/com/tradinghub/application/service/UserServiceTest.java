package com.tradinghub.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradinghub.application.service.auth.AuthResult;
import com.tradinghub.application.service.auth.UserService;
import com.tradinghub.application.service.portfolio.PortfolioService;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.infrastructure.security.JwtService;
import com.tradinghub.interfaces.dto.auth.AuthRequest;
import com.tradinghub.interfaces.dto.auth.AuthResponse;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private PortfolioService portfolioService;
    
    @Mock
    private JwtService jwtService;
    
    @InjectMocks
    private UserService userService;
    
    @Mock
    private Portfolio portfolio;
    
    private AuthRequest signupRequest;
    
    @BeforeEach
    void setUp() {
        signupRequest = new AuthRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setPassword("password123");
    }
    
    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_success() {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setPassword("encodedPassword");
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        when(portfolio.getId()).thenReturn(1L);
        when(portfolioService.createPortfolio(any(User.class), eq("BTC"), any(BigDecimal.class))).thenReturn(portfolio);
        
        when(jwtService.generateToken(any())).thenReturn("jwtToken");
        
        // when
        AuthResult authResult = userService.signup(signupRequest);
        AuthResponse response = AuthResponse.success(
            authResult.getUser().getId(), 
            authResult.getUser().getUsername(), 
            authResult.getToken()
        );
        
        // then
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("jwtToken", response.getToken());
        
        verify(userRepository).existsByUsername("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(portfolioService).createPortfolio(eq(savedUser), eq("BTC"), any(BigDecimal.class));
        verify(jwtService).generateToken(any());
    }
    
    @Test
    @DisplayName("중복된 사용자명으로 회원가입 시 예외 발생")
    void signup_duplicateUsername() {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        
        // when & then
        assertThrows(RuntimeException.class, () -> {
            userService.signup(signupRequest);
        });
        
        verify(userRepository).existsByUsername("testuser");
        verifyNoMoreInteractions(userRepository, passwordEncoder, portfolioService, jwtService);
    }
} 