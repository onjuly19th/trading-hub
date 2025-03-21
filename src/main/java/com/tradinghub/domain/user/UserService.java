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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

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
    public SignupResponse signup(SignupRequest request) {
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
                // 포트폴리오 생성
                Portfolio portfolio = portfolioService.createPortfolio(user, "BTC", request.getInitialBalance());
                user.setPortfolio(portfolio);
                
                // 사용자 다시 저장
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
            return SignupResponse.builder()
                .token(token)
                .username(user.getUsername())
                .initialBalance(request.getInitialBalance())
                .message("회원가입이 완료되었습니다. 초기 자산 1백만 달러가 지급되었습니다.")
                .build();
                
        } catch (Exception e) {
            logger.error("Error during signup process", e);
            throw new RuntimeException("회원가입 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
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

    /**
     * 인증된 사용자 조회
     * 
     * @return 인증된 사용자
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("No authenticated user found");
        }
        
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
    
    /**
     * 사용자명으로 사용자 정보를 조회
     * 
     * @param username 사용자명
     * @return 사용자 정보
     * @throws UsernameNotFoundException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
} 