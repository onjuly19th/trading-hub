package com.tradinghub.interfaces.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradinghub.application.service.auth.AuthResult;
import com.tradinghub.application.service.auth.UserService;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.infrastructure.security.JwtAuthenticationFilter;
import com.tradinghub.infrastructure.security.JwtService;
import com.tradinghub.interfaces.dto.auth.AuthRequest;

@WebMvcTest(UserAuthController.class)
@Import(UserAuthControllerTest.TestSecurityConfig.class)
public class UserAuthControllerTest {

    @Configuration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    
    @MockBean
    private JwtService jwtService;
    
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private AuthRequest authRequest;
    private AuthResult authResult;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 요청 데이터 설정
        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");

        // 테스트용 사용자 객체 생성
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        
        // 테스트용 인증 결과 객체 생성
        authResult = new AuthResult(testUser, "test-jwt-token");
    }

    @Test
    @DisplayName("회원가입 API 테스트")
    void signup_success() throws Exception {
        // given
        when(userService.signup(any(AuthRequest.class))).thenReturn(authResult);

        // when & then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.token").value(authResult.getToken()));
    }

    @Test
    @DisplayName("로그인 API 테스트")
    void login_success() throws Exception {
        // given
        when(userService.login(any(AuthRequest.class))).thenReturn(authResult);

        // when & then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value(testUser.getUsername()))
                .andExpect(jsonPath("$.token").value(authResult.getToken()));
    }
} 