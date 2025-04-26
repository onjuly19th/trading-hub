package com.tradinghub.interfaces.exception;

/**
 * 애플리케이션 전체에서 사용되는 에러 코드를 중앙화하여 관리하는 클래스
 * 각 도메인별로 에러 코드를 그룹화하여 관리
 */
public final class ErrorCodes {
    
    // 일반적인 에러
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    
    // 인증 관련 에러
    public static class Auth {
        public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
        public static final String UNAUTHORIZED_OPERATION = "UNAUTHORIZED_OPERATION";
        public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
        public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
        public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
        public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
        public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
        public static final String INVALID_REQUEST = "INVALID_REQUEST";
    }
    
    // 주문 관련 에러
    public static class Order {
        public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
        public static final String INVALID_ORDER = "INVALID_ORDER";
        public static final String ORDER_EXECUTION_ERROR = "ORDER_EXECUTION_ERROR";
        public static final String ORDER_ALREADY_CANCELLED = "ORDER_ALREADY_CANCELLED";
        public static final String ORDER_ALREADY_FILLED = "ORDER_ALREADY_FILLED";
    }
    
    // 포트폴리오 관련 에러
    public static class Portfolio {
        public static final String PORTFOLIO_NOT_FOUND = "PORTFOLIO_NOT_FOUND";
        public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
        public static final String INSUFFICIENT_ASSET = "INSUFFICIENT_ASSET";
        public static final String ASSET_NOT_FOUND = "ASSET_NOT_FOUND";
    }
    
    // 생성자를 private으로 선언하여 인스턴스화 방지
    private ErrorCodes() {
        throw new AssertionError("ErrorCodes 클래스는 인스턴스화할 수 없습니다.");
    }
} 