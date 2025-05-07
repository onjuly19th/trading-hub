package com.tradinghub.interfaces.dto.auth;

/**
 * 인증 요청(로그인/회원가입)에 사용되는 DTO
 * 사용자명과 비밀번호를 포함합니다.
 */
public record AuthRequest(
    /**
     * 사용자명
     * 로그인 및 회원가입 시 사용자 식별자로 사용
     */
    String username,

    /**
     * 사용자 비밀번호
     * 회원가입 시 암호화되어 저장됨
     */
    String password
) {} 