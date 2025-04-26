package com.tradinghub.infrastructure.security;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security의 UserDetailsService 인터페이스 구현체
 * 사용자명으로 사용자 정보를 로드하고 UserDetails 객체로 변환합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 사용자명으로 사용자 정보를 로드하여 UserDetails 객체를 반환합니다.
     * @param username 사용자명
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 해당 사용자명을 찾을 수 없을 경우
     */
    @Override
    @Transactional(readOnly = true) // 사용자 조회는 읽기 전용 트랜잭션
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // User 엔티티를 UserDetails 객체로 변환 (기존 UserService의 로직과 동일)
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword()) // DB에 저장된 암호화된 비밀번호
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))) // 역할/권한 설정
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
} 