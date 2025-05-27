package com.tradinghub.application.service.portfolio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.portfolio.PortfolioNotFoundException;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 조회 전용 서비스
 * 읽기 전용 작업만을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class PortfolioQueryService {
    
    private final PortfolioRepository portfolioRepository;
    
    /**
     * 사용자의 포트폴리오를 조회합니다.
     * 연관된 자산 정보도 함께 조회됩니다.
     * 
     * @param userId 사용자 ID
     * @return 포트폴리오 정보
     * @throws PortfolioNotFoundException 포트폴리오를 찾을 수 없는 경우
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        return portfolioRepository.findByUserIdWithAssets(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));
    }
}
