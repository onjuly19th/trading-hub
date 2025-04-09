package com.tradinghub.domain.user.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.user.UserService;
import com.tradinghub.domain.user.dto.AuthRequest;
import com.tradinghub.domain.user.dto.AuthResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {
    private final UserService userService;

    @PostMapping(
        value = "/signup", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@RequestBody AuthRequest request) {
        log.info("User signup request: username={}", request.getUsername());
        AuthResponse response = userService.signup(request);
        log.info("User signup completed: userId={}, username={}", response.getUserId(), response.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(
        value = "/login", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        log.info("User login request: username={}", request.getUsername());
        AuthResponse response = userService.login(request);
        log.info("User login successful: userId={}, username={}", response.getUserId(), response.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
} 