package com.tradinghub.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정 클래스
 * 외부 API 호출을 위한 RestTemplate 빈을 생성합니다.
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * RestTemplate 빈 생성
     * 
     * @return RestTemplate 인스턴스
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
} 