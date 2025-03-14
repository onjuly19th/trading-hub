package com.tradinghub.domain.user;

import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.dto.LoginRequest;
import com.tradinghub.domain.user.dto.LoginResponse;
import com.tradinghub.domain.user.dto.SignupRequest;
import com.tradinghub.domain.user.dto.SignupResponse;
import com.tradinghub.infrastructure.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortfolioService portfolioService;
    private final JwtService jwtService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        logger.info("Starting signup process for username: {}", request.getUsername());
        
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
        
        // 초기 포트폴리오 생성
        Portfolio portfolio = portfolioService.createPortfolio(user, "BTC", request.getInitialBalance());
        user.setPortfolio(portfolio);
        
        // 사용자 다시 저장
        user = userRepository.save(user);
        
        // JWT 토큰 생성
        String token = jwtService.generateToken(user);
        logger.info("JWT token generated for user: {}", user.getUsername());
        
        // 응답 생성
        return SignupResponse.builder()
            .token(token)
            .username(user.getUsername())
            .initialBalance(request.getInitialBalance())
            .message("회원가입이 완료되었습니다. 초기 자산 1백만 달러가 지급되었습니다.")
            .build();
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        try {
            logger.info("Attempting to find user by username: {}", request.getUsername());
            User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    logger.error("User not found: {}", request.getUsername());
                    return new BadCredentialsException("Invalid username or password");
                });

            logger.info("Checking password for user: {}", request.getUsername());
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.error("Invalid password for user: {}", request.getUsername());
                throw new BadCredentialsException("Invalid username or password");
            }

            logger.info("Generating JWT token for user: {} (ID: {})", request.getUsername(), user.getId());
            String token = jwtService.generateToken(user);
            
            return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .message("로그인이 완료되었습니다.")
                .build();
        } catch (Exception e) {
            logger.error("Error during login process for username: {}", request.getUsername(), e);
            throw e;
        }
    }
} 