package com.tradinghub.application.service.auth;

import com.tradinghub.domain.model.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthResult {
    private final User user;
    private final String token;
} 