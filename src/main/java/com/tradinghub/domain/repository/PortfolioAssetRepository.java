package com.tradinghub.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tradinghub.domain.model.portfolio.PortfolioAsset;

/**
 * 포트폴리오 자산(PortfolioAsset) 엔티티에 대한 데이터 액세스 인터페이스
 * 사용자가 보유한 암호화폐 자산의 조회 및 관리 기능 제공
 */
public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    /**
     * 포트폴리오 ID와 암호화폐 심볼로 해당 자산 조회
     * 주로 자산 보유량 확인 및 거래 검증에 사용
     * 
     * @param portfolioId 포트폴리오 ID
     * @param symbol 암호화폐 심볼 (예: BTC, ETH)
     * @return 자산 정보 (존재하지 않으면 빈 Optional)
     */
    Optional<PortfolioAsset> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
} 