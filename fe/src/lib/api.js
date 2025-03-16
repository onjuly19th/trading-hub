import { API_CONFIG } from '@/config/constants';

// 공통 에러 처리
const handleResponse = async (response, endpoint) => {
  console.log(`API Response for ${endpoint}:`, {
    status: response.status,
    statusText: response.statusText,
    headers: Object.fromEntries([...response.headers]),
  });
  
  if (!response.ok) {
    // 인증 관련 엔드포인트는 자동 리다이렉트하지 않음
    if ((response.status === 403 || response.status === 401) && 
        !endpoint.startsWith('/auth/')) {
      console.error('Authentication error:', response.status);
      localStorage.removeItem('token');
      throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.');
    }
    
    // 응답 본문을 텍스트로 로깅
    const clonedResponse = response.clone();
    const text = await clonedResponse.text();
    console.error(`Error response raw text for ${endpoint}:`, text);
    
    let errorData;
    try {
      errorData = await response.json().catch(() => {
        console.error('Failed to parse error response as JSON');
        return null;
      });
      
      console.error(`Error response for ${endpoint}:`, errorData);
    } catch (e) {
      console.error('Error processing response:', e);
      errorData = null;
    }
    
    const error = {
      status: response.status,
      message: errorData?.message || '요청 처리 중 오류가 발생했습니다.',
      data: errorData
    };
    
    console.error('Throwing API error:', error);
    throw error;
  }
  
  try {
    const data = await response.json();
    console.log(`API Success data for ${endpoint}:`, data);
    return data;
  } catch (e) {
    console.error(`Error parsing JSON for ${endpoint}:`, e);
    throw new Error('응답 데이터 처리 중 오류가 발생했습니다.');
  }
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
    
    console.log(`Making GET request to: ${url}`);
    console.log('Request headers:', headers);
    
    const response = await fetch(url, {
      method: 'GET',
      headers,
    });

    return handleResponse(response, endpoint);
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
    
    console.log(`Making POST request to: ${url}`);
    console.log('Request headers:', headers);
    
    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(data)
    });

    return handleResponse(response, endpoint);
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
    
    console.log(`Making PUT request to: ${url}`);
    console.log('Request headers:', headers);
    
    const response = await fetch(url, {
      method: 'PUT',
      headers,
      body: JSON.stringify(data),
    });

    return handleResponse(response, endpoint);
  },

  // DELETE 요청
  delete: async (endpoint, options = {}) => {
    const url = `${API_CONFIG.BASE_URL}${endpoint}`;
    
    const headers = {
      'Accept': 'application/json',
      'Authorization': getAuthHeader(),
      ...options.headers,
    };
    
    console.log(`Making DELETE request to: ${url}`);
    console.log('Request headers:', headers);
    
    const response = await fetch(url, {
      method: 'DELETE',
      headers,
    });

    return handleResponse(response, endpoint);
  },
}; 