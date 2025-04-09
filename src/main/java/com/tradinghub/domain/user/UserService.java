package com.tradinghub.domain.user;

import java.util.Collections;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.dto.AuthRequest;
import com.tradinghub.domain.user.dto.AuthResponse;
import com.tradinghub.infrastructure.security.JwtService;

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

    @Transactional
    public AuthResponse signup(AuthRequest request) {
        log.info("User signup initiated: username={}", request.getUsername());
        
        try {
            // 중복 검사
            if (userRepository.existsByUsername(request.getUsername())) {
                log.warn("Signup failed: Username already exists: {}", request.getUsername());
                throw new RuntimeException("Username already exists");
            }

            // 사용자 생성
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // 사용자 저장
            user = userRepository.save(user);
            log.info("User saved successfully: userId={}, username={}", user.getId(), user.getUsername());
            
            try {
                // 포트폴리오 생성 (초기 자산: 100만 달러)
                Portfolio portfolio = portfolioService.createPortfolio(user, "BTC", new java.math.BigDecimal("1000000"));
                log.info("Portfolio created successfully: userId={}, portfolioId={}", user.getId(), portfolio.getId());
            } catch (Exception e) {
                log.error("Failed to create portfolio: userId={}, username={}, error={}", user.getId(), user.getUsername(), e.getMessage(), e);
                throw new RuntimeException("Failed to create portfolio: " + e.getMessage());
            }
            
            // JWT 토큰 생성
            UserDetails userDetails = convertToUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            log.debug("JWT token generated: userId={}", user.getId());
            
            // 응답 생성
            log.info("Signup completed successfully: userId={}, username={}", user.getId(), user.getUsername());
            return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
                
        } catch (Exception e) {
            log.error("Exception during signup process: username={}, error={}", request.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Error occurred during user registration: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        log.info("User login attempt: username={}", request.getUsername());
        
        try {
            // 사용자 조회
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found: username={}", request.getUsername());
                    return new BadCredentialsException("Invalid username or password");
                });

            // 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.warn("Login failed: Invalid password: userId={}, username={}", user.getId(), user.getUsername());
                throw new BadCredentialsException("Invalid username or password");
            }

            // JWT 토큰 생성
            UserDetails userDetails = convertToUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            log.debug("JWT token generated: userId={}", user.getId());

            // 응답 생성 및 반환
            log.info("Login successful: userId={}, username={}", user.getId(), user.getUsername());
            return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
        } catch (Exception e) {
            if (e instanceof BadCredentialsException) {
                throw e; // 인증 실패는 그대로 전파
            }
            log.error("Exception during login process: username={}, error={}", request.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Error occurred during login process.");
        }
    }
} 