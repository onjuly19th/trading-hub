package com.tradinghub.domain.model.user;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 사용자 정보를 저장하는 엔티티
 * 사용자의 기본 정보와 인증에 필요한 정보를 포함합니다.
 */
@Entity
@Table(name = "users")
@Getter 
@Setter
public class User {
    /**
     * 사용자 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 사용자명
     * 로그인에 사용되는 고유한 값으로, 중복될 수 없음
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * 암호화된 비밀번호
     * 평문이 아닌 해시된 형태로 저장됨
     */
    @Column(nullable = false)
    private String password;

    /**
     * 계정 생성 일시
     * 엔티티 생성 시 자동으로 설정됨
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 정보 수정 일시
     * 엔티티 수정 시 자동으로 갱신됨
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 엔티티 생성 시 호출되는 메서드
     * createdAt과 updatedAt을 현재 시간으로 설정
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티 수정 시 호출되는 메서드
     * updatedAt을 현재 시간으로 갱신
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 