package com.tradinghub.application.usecase.auth;

import com.tradinghub.application.dto.AuthResult;
import com.tradinghub.application.dto.SignupCommand;

public interface SignupUseCase {
    AuthResult execute(SignupCommand command);
}
