package com.tradinghub.application.usecase.portfolio;

import com.tradinghub.domain.model.portfolio.Portfolio;

public interface GetPortfolioUseCase {
    Portfolio execute(Long userId);
}
