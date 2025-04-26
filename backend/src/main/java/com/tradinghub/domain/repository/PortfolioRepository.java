package com.tradinghub.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tradinghub.domain.model.portfolio.Portfolio;

import jakarta.persistence.LockModeType;

/**
 * 포트폴리오(Portfolio) 엔티티에 대한 데이터 액세스 인터페이스
 * 사용자의 포트폴리오 조회 및 거래 처리를 위한 메소드 제공
 */
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    /**
     * 사용자 ID로 포트폴리오 조회
     * 조회 전용 작업에 사용 (잔액 확인 등)
     * 
     * @param userId 사용자 ID
     * @return 포트폴리오 (존재하지 않으면 빈 Optional)
     */
    Optional<Portfolio> findByUserId(Long userId);
    
    /**
     * 사용자 ID로 포트폴리오와 자산 컬렉션을 함께 조회 (FETCH JOIN 사용)
     * 지연 로딩 문제를 해결하기 위한 메소드
     * 
     * @param userId 사용자 ID
     * @return 포트폴리오와 자산 컬렉션이 함께 로딩된 결과 (존재하지 않으면 빈 Optional)
     */
    @Query("SELECT p FROM Portfolio p LEFT JOIN FETCH p.assets WHERE p.user.id = :userId")
    Optional<Portfolio> findByUserIdWithAssets(@Param("userId") Long userId);
    
    /**
     * 사용자 ID로 포트폴리오 조회 (비관적 쓰기 락 적용)
     * 포트폴리오 업데이트 작업에 사용 (잔액 변경, 자산 추가/제거 등)
     * 동시성 제어를 위해 비관적 락을 사용하여 트랜잭션 충돌 방지
     * 
     * @param userId 사용자 ID
     * @return 락이 걸린 포트폴리오 (존재하지 않으면 빈 Optional)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolio p WHERE p.user.id = :userId")
    Optional<Portfolio> findByUserIdForUpdate(@Param("userId") Long userId);
} 