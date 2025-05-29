package com.tradinghub.application.usecase.auth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.dto.AuthResult;
import com.tradinghub.application.dto.SignupCommand;
import com.tradinghub.application.event.UserSignedUpEvent;
import com.tradinghub.application.exception.auth.DuplicateUsernameException;
import com.tradinghub.application.port.JwtProvider;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.model.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignupUseCaseImpl implements SignupUseCase {
    private final ApplicationEventPublisher eventPublisher;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AuthResult execute(SignupCommand command) {
        String username = command.username();
        String password = command.password();

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUsernameException(username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        
        User savedUser = userRepository.save(user);

        eventPublisher.publishEvent(new UserSignedUpEvent(savedUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = jwtProvider.generateToken(userDetails);

        return new AuthResult(savedUser.getId(), savedUser.getUsername(), token);
    }
}