package com.tradinghub.application.usecase.portfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.portfolio.PortfolioNotFoundException;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPortfolioUseCaseImpl implements GetPortfolioUseCase {
    private final PortfolioRepository portfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public Portfolio execute(Long userId) {
        return portfolioRepository.findByUserIdWithAssets(userId)
        .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));
    }
}
