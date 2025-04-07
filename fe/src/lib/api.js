import { API_CONFIG } from '@/config/constants';

// 디버그 모드 설정 (development 환경에서만 작동)
const API_DEBUG = true;

// 로깅 유틸리티
const logger = {
  log: (...args) => {
    if (process.env.NODE_ENV === 'development' && API_DEBUG) {
      console.log('[API]', ...args);
    }
  },
  error: (...args) => {
    if (process.env.NODE_ENV === 'development') {
      console.error('[API Error]', ...args);
    }
  },
  warn: (...args) => {
    if (process.env.NODE_ENV === 'development') {
      console.warn('[API Warning]', ...args);
    }
  }
};

// 공통 에러 처리
const handleResponse = async (response, endpoint, options = {}) => {
  if (!response.ok) {
    // 인증 관련 오류에 대한 처리 (skipAuthError 옵션 확인)
    if ((response.status === 403 || response.status === 401) && 
        !endpoint.startsWith('/auth/')) {
      
      // skipAuthError 옵션이 설정된 경우 인증 오류를 무시
      if (options.skipAuthError) {
        return { status: 'ignored_auth_error' };
      }
      
      localStorage.removeItem('token');
      throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.');
    }
    
    // 응답 본문을 텍스트로 파싱
    const clonedResponse = response.clone();
    const text = await clonedResponse.text();
    
    let errorData;
    try {
      errorData = JSON.parse(text);
      logger.error(`${response.status} - ${endpoint}:`, 
        errorData?.error?.message || 
        errorData?.message || 
        '알 수 없는 오류');
    } catch (e) {
      errorData = null;
    }
    
    const error = {
      status: response.status,
      message: errorData?.error?.message || 
               errorData?.message || 
               '요청 처리 중 오류가 발생했습니다.',
      data: errorData
    };
    
    throw error;
  }
  
  try {
    const text = await response.text();
    
    if (!text) {
      return null;
    }
    
    return JSON.parse(text);
  } catch (e) {
    throw new Error('응답 데이터 처리 중 오류가 발생했습니다.');
  }
};

// API 엔드포인트 정의
export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    SIGNUP: '/auth/signup',
    REFRESH: '/auth/refresh'
  },
  PORTFOLIO: {
    SUMMARY: '/portfolio',
    TRADES: '/portfolio/history'
  },
  ORDERS: {
    CREATE: '/orders',
    CANCEL: (orderId) => `/orders/${orderId}`,
    LIST: '/orders',
    HISTORY: '/orders/history',
    SYMBOL: (symbol) => `/orders/symbol/${symbol}`,
    BOOK: '/orders/book'
  },
  MARKET: {
    PRICE: '/market/price'
  }
};

// 인증 헤더 생성 함수
const getAuthHeader = () => {
  const token = localStorage.getItem('token');
  return token ? `Bearer ${token}` : '';
};

// API 메서드 구현
export const api = {
  // GET 요청
  get: async (endpoint, options = {}) => {
    const queryString = options.params ? `?${new URLSearchParams(options.params)}` : '';
    const url = `${API_CONFIG.BASE_URL}${endpoint}${queryString}`;
    
    const headers = {
      'Accept': 'application/json',
      'Authorization': getAuthHeader(),
      ...options.headers,
    };
    
    logger.log(`GET ${endpoint}`);
    
    const response = await fetch(url, {
      method: 'GET',
      headers,
    });

    return handleResponse(response, endpoint, options);
  },

  // POST 요청
  post: async (endpoint, data, options = {}) => {
    const url = `${API_CONFIG.BASE_URL}${endpoint}`;
    
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': getAuthHeader(),
      ...options.headers,
    };
    
    logger.log(`POST ${endpoint}`);
    
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(data)
      });
      
      return handleResponse(response, endpoint, options);
    } catch (networkError) {
      throw new Error('네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.');
    }
  },

  // PUT 요청
  put: async (endpoint, data, options = {}) => {
    const url = `${API_CONFIG.BASE_URL}${endpoint}`;
    
    const headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Authorization': getAuthHeader(),
      ...options.headers,
    };
    
    logger.log(`PUT ${endpoint}`);
    
    const response = await fetch(url, {
      method: 'PUT',
      headers,
      body: JSON.stringify(data),
    });

    return handleResponse(response, endpoint, options);
  },

  // DELETE 요청
  delete: async (endpoint, options = {}) => {
    const url = `${API_CONFIG.BASE_URL}${endpoint}`;
    
    const headers = {
      'Accept': 'application/json',
      'Authorization': getAuthHeader(),
      ...options.headers,
    };
    
    logger.log(`DELETE ${endpoint}`);
    
    const response = await fetch(url, {
      method: 'DELETE',
      headers,
    });

    return handleResponse(response, endpoint, options);
  },
}; 