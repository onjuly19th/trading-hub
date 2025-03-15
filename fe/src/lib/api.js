import { API_CONFIG } from '@/config/constants';

// 공통 에러 처리
const handleResponse = async (response, endpoint) => {
  if (!response.ok) {
    // 인증 관련 엔드포인트는 자동 리다이렉트하지 않음
    if ((response.status === 403 || response.status === 401) && 
        !endpoint.startsWith('/auth/')) {
      localStorage.removeItem('token');
      //window.location.href = '/auth/login';
      throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.');
    }
    
    const errorData = await response.json().catch(() => null);
    throw {
      status: response.status,
      message: errorData?.message || '요청 처리 중 오류가 발생했습니다.',
      data: errorData
    };
  }
  return response.json();
};

// API 엔드포인트 정의
export const ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    SIGNUP: '/auth/signup',
  },
  PORTFOLIO: {
    SUMMARY: '/portfolio',
    TRADES: '/portfolio/trades'
  },
  TRADING: {
    ORDER: '/order',
    CANCEL_ORDER: (orderId) => `/order/${orderId}`,
    ORDER_BOOK: (symbol) => `/order/book/${symbol}`,
  },
};

// API 메서드 구현
export const api = {
  // GET 요청
  get: async (endpoint, options = {}) => {
    const token = localStorage.getItem('token');
    const queryString = options.params ? `?${new URLSearchParams(options.params)}` : '';
    
    const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}${queryString}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Authorization': token ? `Bearer ${token}` : undefined,
        ...options.headers,
      },
    });

    return handleResponse(response, endpoint);
  },

  // POST 요청
  post: async (endpoint, data) => {
    const token = localStorage.getItem('token');
    console.log('Making POST request to:', endpoint);
    console.log('Request headers:', {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : 'No token'
    });
    
    const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': token ? `Bearer ${token}` : ''
      },
      body: JSON.stringify(data)
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error('Request failed:', {
        status: response.status,
        statusText: response.statusText,
        headers: Object.fromEntries([...response.headers]),
        error: errorData
      });
      throw {
        status: response.status,
        message: errorData?.message || `Request failed with status ${response.status}`,
        data: errorData
      };
    }

    return response.json();
  },

  // PUT 요청
  put: async (endpoint, data, options = {}) => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': token ? `Bearer ${token}` : undefined,
        ...options.headers,
      },
      body: JSON.stringify(data),
    });

    return handleResponse(response, endpoint);
  },

  // DELETE 요청
  delete: async (endpoint, options = {}) => {
    const token = localStorage.getItem('token');
    
    const response = await fetch(`${API_CONFIG.BASE_URL}${endpoint}`, {
      method: 'DELETE',
      headers: {
        'Accept': 'application/json',
        'Authorization': token ? `Bearer ${token}` : undefined,
        ...options.headers,
      },
    });

    return handleResponse(response, endpoint);
  },
}; 