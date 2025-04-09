package com.tradinghub.domain.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사용자(User) 엔티티에 대한 데이터 액세스 인터페이스
 * 사용자 등록, 인증 및 조회 작업을 위한 메소드 제공
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * 주어진 사용자명이 이미 존재하는지 확인
     * 
     * @param username 확인할 사용자명
     * @return 존재하면 true, 존재하지 않으면 false
     */
    boolean existsByUsername(String username);
    
    /**
     * 사용자명으로 사용자 조회
     * 주로 로그인 및 인증 처리에 사용됨
     * 
     * @param username 조회할 사용자명
     * @return 조회된 사용자 (존재하지 않으면 빈 Optional)
     */
    Optional<User> findByUsername(String username);
} 