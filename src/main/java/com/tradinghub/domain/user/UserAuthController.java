package com.tradinghub.domain.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.common.response.ApiResponse;
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

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@RequestBody AuthRequest request) {
        log.info("New user signup - Username: {}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(userService.signup(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody AuthRequest request) {
        log.info("User login attempt - Username: {}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(userService.login(request)));
    }
} 