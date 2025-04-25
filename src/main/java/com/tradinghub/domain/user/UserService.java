package com.tradinghub.domain.user;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.common.exception.auth.AuthenticationFailedException;
import com.tradinghub.common.exception.auth.DuplicateUsernameException;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.dto.AuthRequest;
import com.tradinghub.domain.user.dto.AuthResponse;
import com.tradinghub.infrastructure.security.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 인증 및 계정 관리를 처리하는 서비스
 * 회원가입, 로그인 등 사용자 인증과 관련된 비즈니스 로직을 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PortfolioService portfolioService;
    private final JwtService jwtService;

    /**
     * User 엔티티를 Spring Security의 UserDetails로 변환
     * 
     * @param user 변환할 사용자 엔티티
     * @return Spring Security에서 사용할 UserDetails 객체
     */
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

    /**
     * 새로운 사용자 계정을 생성합니다.
     * 계정 생성 시 기본 포트폴리오도 함께 생성됩니다.
     * 
     * @param request 회원가입 요청 정보 (사용자명, 비밀번호)
     * @return 생성된 계정 정보와 인증 토큰
     * @throws DuplicateUsernameException 이미 존재하는 사용자명인 경우
     */
    @Transactional
    public AuthResponse signup(AuthRequest request) {
        // 중복 검사
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException(request.getUsername());
        }

        // 사용자 생성
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // 사용자 저장
        user = userRepository.save(user);
        
        // 포트폴리오 생성 (초기 자산: 100만 달러)
        portfolioService.createPortfolio(user, "BTC", new java.math.BigDecimal("1000000"));
        
        // JWT 토큰 생성
        UserDetails userDetails = convertToUserDetails(user);
        String token = jwtService.generateToken(userDetails);
        
        // 응답 생성
        return AuthResponse.success(user.getId(), user.getUsername(), token);
    }

    /**
     * 사용자 로그인을 처리합니다.
     * 인증 성공 시 JWT 토큰을 발급합니다.
     * 
     * @param request 로그인 요청 정보 (사용자명, 비밀번호)
     * @return 사용자 정보와 인증 토큰
     * @throws AuthenticationFailedException 인증 실패 시 (잘못된 사용자명 또는 비밀번호)
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        // JWT 토큰 생성
        UserDetails userDetails = convertToUserDetails(user);
        String token = jwtService.generateToken(userDetails);

        // 응답 생성
        return AuthResponse.success(user.getId(), user.getUsername(), token);
    }
} 