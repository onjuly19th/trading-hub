package com.tradinghub.domain.user;

import java.util.Collections;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.dto.AuthRequest;
import com.tradinghub.domain.user.dto.AuthResponse;
import com.tradinghub.infrastructure.aop.LogExecutionTime;
import com.tradinghub.infrastructure.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortfolioService portfolioService;
    private final JwtService jwtService;

    private UserDetails convertToUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    @LogExecutionTime
    @Transactional
    public AuthResponse signup(AuthRequest request) {
        try {
            // 중복 검사
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }

            // 사용자 생성
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // 사용자 저장
            user = userRepository.save(user);
            
            try {
                // 포트폴리오 생성 (초기 자산: 100만 달러)
                portfolioService.createPortfolio(user, "BTC", new java.math.BigDecimal("1000000"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create portfolio: " + e.getMessage());
            }
            
            // JWT 토큰 생성
            UserDetails userDetails = convertToUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            
            // 응답 생성
            return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
                
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during user registration: " + e.getMessage());
        }
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        try {
            // 사용자 조회
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

            // 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadCredentialsException("Invalid username or password");
            }

            // JWT 토큰 생성
            UserDetails userDetails = convertToUserDetails(user);
            String token = jwtService.generateToken(userDetails);

            // 응답 생성
            return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Error occurred during login: " + e.getMessage());
        }
    }
} 