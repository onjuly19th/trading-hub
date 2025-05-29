package com.tradinghub.application.port;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtProvider {
    String generateToken(UserDetails userDetails);
}
