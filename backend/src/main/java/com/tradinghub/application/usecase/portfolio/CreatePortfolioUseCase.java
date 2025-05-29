package com.tradinghub.application.usecase.portfolio;

import java.math.BigDecimal;

import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;

public interface CreatePortfolioUseCase {
    Portfolio execute(User user, String symbol, BigDecimal initialBalance);
}
