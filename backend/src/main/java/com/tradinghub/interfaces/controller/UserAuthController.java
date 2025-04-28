package com.tradinghub.interfaces.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.application.service.auth.UserService;
import com.tradinghub.interfaces.dto.auth.AuthRequest;
import com.tradinghub.interfaces.dto.auth.AuthResponse;
import com.tradinghub.interfaces.dto.auth.AuthSuccessDto;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 인증 관련 엔드포인트를 제공하는 컨트롤러
 * 회원가입과 로그인 기능을 처리합니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {
    private final UserService userService;

    /**
     * 새로운 사용자 계정을 생성합니다.
     * 
     * @param request 회원가입 요청 정보 (사용자명, 비밀번호)
     * @return 생성된 사용자 정보와 인증 토큰
     * @throws com.tradinghub.application.exception.auth.DuplicateUsernameException 이미 존재하는 사용자명인 경우
     * @throws com.tradinghub.application.exception.auth.InvalidRequestException 요청 데이터가 유효하지 않은 경우
     * 
     * @apiNote
     * 성공 시 HTTP 200 응답과 함께 사용자 ID, 사용자명, 인증 토큰을 반환합니다.
     * 실패 시 적절한 HTTP 상태 코드와 에러 메시지를 반환합니다.
     */
    @PostMapping(
        value = "/signup", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest request) {
        AuthSuccessDto authSuccessDto = userService.signup(request);
        AuthResponse response = AuthResponse.success(
            authSuccessDto.getUserId(),
            authSuccessDto.getUsername(),
            authSuccessDto.getToken()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 기존 사용자 계정으로 로그인합니다.
     * 
     * @param request 로그인 요청 정보 (사용자명, 비밀번호)
     * @return 사용자 정보와 새로 발급된 인증 토큰
     * @throws com.tradinghub.application.exception.auth.AuthenticationFailedException 인증 실패 시
     * @throws com.tradinghub.application.exception.auth.AccountLockedException 계정이 잠겨있는 경우
     * 
     * @apiNote
     * 성공 시 HTTP 200 응답과 함께 사용자 ID, 사용자명, 인증 토큰을 반환합니다.
     * 실패 시 HTTP 401 (Unauthorized) 또는 적절한 상태 코드와 에러 메시지를 반환합니다.
     */
    @PostMapping(
        value = "/login", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthSuccessDto authSuccessDto = userService.login(request);
        AuthResponse response = AuthResponse.success(
            authSuccessDto.getUserId(),
            authSuccessDto.getUsername(),
            authSuccessDto.getToken()
        );
        return ResponseEntity.ok(response);
    }
} 