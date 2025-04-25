package com.tradinghub.domain.user.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 인증 요청(로그인/회원가입)에 사용되는 DTO
 */
@Getter
@Setter
public class AuthRequest {
    /**
     * 사용자명
     * 로그인 및 회원가입 시 사용자 식별자로 사용
     */
    private String username;

    /**
     * 사용자 비밀번호
     * 회원가입 시 암호화되어 저장됨
     */
    private String password;
} 