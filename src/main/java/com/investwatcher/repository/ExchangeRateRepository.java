package com.investwatcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.investwatcher.model.ExchangeRate;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    ExchangeRate findTopByOrderByUpdatedAtDesc();
}