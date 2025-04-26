package com.tradinghub.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tradinghub.application.event.UserSignedUpEvent;
import com.tradinghub.application.exception.auth.DuplicateUsernameException;
import com.tradinghub.application.service.auth.AuthSuccessDto;
import com.tradinghub.application.service.auth.UserService;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.infrastructure.security.JwtService;
import com.tradinghub.interfaces.dto.auth.AuthRequest;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private UserDetailsService userDetailsService;
    
    @InjectMocks
    private UserService userService;
    
    private AuthRequest signupRequest;
    private AuthRequest loginRequest;
    
    @BeforeEach
    void setUp() {
        signupRequest = new AuthRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setPassword("password123");

        loginRequest = new AuthRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }
    
    @Test
    @DisplayName("회원가입 성공 테스트")
    void signup_success() {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        
        User userToSave = new User();
        userToSave.setUsername("testuser");
        userToSave.setPassword("encodedPassword");
        
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setPassword("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(savedUser.getUsername())
                .password(savedUser.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
        
        when(jwtService.generateToken(userDetails)).thenReturn("test-jwt-token");
        
        // when
        AuthSuccessDto authSuccessDto = userService.signup(signupRequest);
        
        // then
        assertNotNull(authSuccessDto);
        assertEquals(1L, authSuccessDto.getUserId());
        assertEquals("testuser", authSuccessDto.getUsername());
        assertEquals("test-jwt-token", authSuccessDto.getToken());
        
        verify(userRepository).existsByUsername("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(UserSignedUpEvent.class));
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtService).generateToken(userDetails);
    }
    
    @Test
    @DisplayName("중복된 사용자명으로 회원가입 시 DuplicateUsernameException 발생")
    void signup_duplicateUsername() {
        // given
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        
        // when & then
        assertThrows(DuplicateUsernameException.class, () -> {
            userService.signup(signupRequest);
        });
        
        verify(userRepository).existsByUsername("testuser");
        verifyNoInteractions(passwordEncoder, jwtService, eventPublisher, userDetailsService);
    }
} 