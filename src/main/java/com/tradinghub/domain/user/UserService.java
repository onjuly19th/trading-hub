package com.tradinghub.domain.user;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.dto.AuthRequest;
import com.tradinghub.domain.user.dto.AuthResponse;
import com.tradinghub.infrastructure.security.JwtService;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
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
        logger.info("Starting signup process for username: {}", request.getUsername());
        
        try {
            // 중복 검사
            if (userRepository.existsByUsername(request.getUsername())) {
                logger.error("Username already exists: {}", request.getUsername());
                throw new RuntimeException("Username already exists");
            }

            // 사용자 생성
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            // 사용자 저장
            user = userRepository.save(user);
            logger.info("User saved with ID: {}", user.getId());
            
            try {
                // 포트폴리오 생성 (초기 자산: 100만 달러)
                Portfolio portfolio = portfolioService.createPortfolio(user, "BTC", new java.math.BigDecimal("1000000"));
                user.setPortfolio(portfolio);
                
                // 사용자 업데이트
                user = userRepository.save(user);
                logger.info("Portfolio created and user updated successfully");
            } catch (Exception e) {
                logger.error("Error creating portfolio for user: {}", user.getUsername(), e);
                throw new RuntimeException("Failed to create portfolio: " + e.getMessage());
            }
            
            // JWT 토큰 생성
            UserDetails userDetails = convertToUserDetails(user);
            String token = jwtService.generateToken(userDetails);
            logger.info("JWT token generated for user: {}", user.getUsername());
            
            // 응답 생성
            return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
                
        } catch (Exception e) {
            logger.error("Error during signup process", e);
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        try {
            // 사용자 조회
            logger.info("Attempting to find user by username: {}", request.getUsername());
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.error("User not found: {}", request.getUsername());
                    return new BadCredentialsException("Invalid username or password");
                });

            // 비밀번호 검증
            logger.info("Checking password for user: {}", request.getUsername());
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.error("Invalid password for user: {}", request.getUsername());
                throw new BadCredentialsException("Invalid username or password");
            }

            // JWT 토큰 생성
            logger.info("Generating JWT token for user: {} (ID: {})", request.getUsername(), user.getId());
            UserDetails userDetails = convertToUserDetails(user);
            String token = jwtService.generateToken(userDetails);

            // 응답 생성 및 반환
            return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
        } catch (Exception e) {
            logger.error("Error during login process for username: {}", request.getUsername(), e);
            throw e;
        }
    }
} 