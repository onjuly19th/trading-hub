package com.tradinghub.infrastructure.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 웹소켓 통신을 위한 설정 클래스
 * STOMP 프로토콜을 사용하여 웹소켓 메시지 브로커를 활성화
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정을 구성하는 메소드
     * 
     * @param config 메시지 브로커 레지스트리 설정 객체
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 브로커 활성화: 클라이언트가 구독할 수 있는 주제(topic)와 개인별 대기열(queue) 설정
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 접두사 설정
        config.setApplicationDestinationPrefixes("/app");
        // /topic은 일반적인 구독용(모든 클라이언트에게 브로드캐스트),
        // /queue는 사용자별 구독용(특정 사용자에게만 전송)
    }

    /**
     * STOMP 엔드포인트 등록 메소드
     * 클라이언트가 웹소켓 서버에 연결하기 위한 엔드포인트를 설정
     * 
     * @param registry STOMP 엔드포인트 등록을 위한 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 웹소켓 연결 엔드포인트 '/ws' 등록
        registry.addEndpoint("/ws")
                // CORS 설정: 모든 오리진에서의 접근 허용
                .setAllowedOriginPatterns("*")
                // SockJS 지원 추가: 웹소켓을 지원하지 않는 브라우저에서도 동작하도록 폴백 옵션 제공
                .withSockJS();
    }
} 