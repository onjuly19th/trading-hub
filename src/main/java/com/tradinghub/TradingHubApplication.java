package com.tradinghub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TradingHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradingHubApplication.class, args);
	}

}