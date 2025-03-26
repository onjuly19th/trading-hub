package com.tradinghub.domain.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;

import com.tradinghub.domain.user.dto.SignupRequest;
import com.tradinghub.domain.user.dto.SignupResponse;
import com.tradinghub.domain.user.dto.LoginRequest;
import com.tradinghub.domain.user.dto.LoginResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserAuthController {
    private static final Logger logger = LoggerFactory.getLogger(UserAuthController.class);
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
        logger.info("Signup request received for username: {}", request.getUsername());
        return ResponseEntity.ok(userService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        logger.info("Login request received for username: {}", request.getUsername());
        return ResponseEntity.ok(userService.login(request));
    }
} 