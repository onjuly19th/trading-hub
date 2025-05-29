package com.tradinghub.application.usecase.portfolio;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;
import com.tradinghub.domain.model.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreatePortfolioUseCaseImpl implements CreatePortfolioUseCase {
    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio execute(User user, String symbol, BigDecimal initialBalance) {
        return portfolioRepository.save(Portfolio.createWithBalance(user, symbol, initialBalance));
    }
}
