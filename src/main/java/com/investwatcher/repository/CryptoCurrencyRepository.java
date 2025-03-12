package com.investwatcher.repository;

import com.investwatcher.model.CryptoCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CryptoCurrencyRepository extends JpaRepository<CryptoCurrency, String> {
    // 쿼리 메서드 추가 예정
} 