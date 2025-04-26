package com.tradinghub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class TradingHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingHubApplication.class, args);
	}

}