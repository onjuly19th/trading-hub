package com.tradinghub.application.usecase.portfolio;

import com.tradinghub.application.dto.UpdatePortfolioCommand;

public interface UpdatePortfolioUseCase {
    void execute(Long userId, UpdatePortfolioCommand command);
}
