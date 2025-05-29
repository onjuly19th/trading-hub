package com.tradinghub.application.usecase.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.dto.AuthResult;
import com.tradinghub.application.dto.LoginCommand;
import com.tradinghub.application.exception.auth.AuthenticationFailedException;
import com.tradinghub.application.port.JwtProvider;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.model.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AuthResult execute(LoginCommand command) {
        String username = command.username();
        String password = command.password();

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
        String token = jwtProvider.generateToken(userDetails);

        return new AuthResult(user.getId(), user.getUsername(), token);
    }
}
