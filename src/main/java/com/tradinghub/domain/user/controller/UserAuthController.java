package com.tradinghub.domain.user.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.domain.user.UserService;
import com.tradinghub.domain.user.dto.AuthRequest;
import com.tradinghub.domain.user.dto.AuthResponse;

import lombok.RequiredArgsConstructor;

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
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest request) {
        AuthResponse response = userService.signup(request);
        log.info("User signup completed: userId={}, username={}", response.getUserId(), response.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(
        value = "/login", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
} 