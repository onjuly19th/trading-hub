package com.tradinghub.application.service.auth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.event.UserSignedUpEvent;
import com.tradinghub.application.exception.auth.AuthenticationFailedException;
import com.tradinghub.application.exception.auth.DuplicateUsernameException;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.infrastructure.security.JwtService;
import com.tradinghub.interfaces.dto.auth.AuthRequest;
import com.tradinghub.interfaces.dto.auth.AuthSuccessDto;

/**
 * 사용자 인증 및 계정 관리를 처리하는 서비스
 * 회원가입, 로그인 등 사용자 인증과 관련된 비즈니스 로직을 처리합니다.
 * UserDetails 로딩은 CustomUserDetailsService에 위임합니다.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserDetailsService userDetailsService; // UserDetailsService 주입

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       ApplicationEventPublisher eventPublisher,
                       UserDetailsService userDetailsService) { // 생성자에 userDetailsService 추가
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.eventPublisher = eventPublisher;
        this.userDetailsService = userDetailsService; // 주입받은 서비스 할당
    }

    // convertToUserDetails 메서드 제거
    /*
    private UserDetails convertToUserDetails(User user) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // 주의: UserDetails에는 인코딩된 비밀번호가 포함되어야 함
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
    */

    /**
     * 새로운 사용자 계정을 생성합니다.
     * 회원가입 성공 시 UserSignedUpEvent를 발행합니다.
     *
     * @param request 회원가입 요청 정보 (사용자명, 비밀번호)
     * @return 생성된 계정 정보와 인증 토큰이 포함된 AuthSuccessDto
     * @throws DuplicateUsernameException 이미 존재하는 사용자명인 경우
     */
    @Transactional
    public AuthSuccessDto signup(AuthRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser));

        // JWT 토큰 생성 시 UserDetailsService 사용
        // 회원가입 직후이므로 사용자가 존재함이 보장됨
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = jwtService.generateToken(userDetails);

        return new AuthSuccessDto(savedUser.getId(), savedUser.getUsername(), token);
    }

    /**
     * 사용자 로그인을 처리합니다.
     * 인증 성공 시 JWT 토큰을 발급합니다.
     *
     * @param request 로그인 요청 정보 (사용자명, 비밀번호)
     * @return 사용자 정보와 인증 토큰이 포함된 AuthSuccessDto
     * @throws AuthenticationFailedException 인증 실패 시 (잘못된 사용자명 또는 비밀번호)
     */
    @Transactional(readOnly = true)
    public AuthSuccessDto login(AuthRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        // 1. 사용자 조회 (기존 로직 유지)
        // UserDetailsService는 Spring Security 인증 필터 체인에서 주로 사용됩니다.
        // 로그인 API 핸들러에서는 명시적으로 사용자를 조회하고 비밀번호를 검증하는 것이 일반적입니다.
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new AuthenticationFailedException("Invalid username or password"));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationFailedException("Invalid username or password");
        }

        // 3. JWT 토큰 생성 시 UserDetailsService 사용
        // 비밀번호 검증이 성공했으므로 해당 사용자가 존재함
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = jwtService.generateToken(userDetails);

        return new AuthSuccessDto(user.getId(), user.getUsername(), token);
    }
} 