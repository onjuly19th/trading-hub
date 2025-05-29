package com.tradinghub.application.usecase.auth;

import com.tradinghub.application.dto.AuthResult;
import com.tradinghub.application.dto.LoginCommand;

public interface LoginUseCase {
    AuthResult execute(LoginCommand command);
}
