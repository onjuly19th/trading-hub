package com.tradinghub.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI tradingHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trading Hub API")
                        .description("암호화폐 모의투자 플랫폼 API 문서")
                        .version("v1.0.0"));
    }
} 