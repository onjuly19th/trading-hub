import { API_CONFIG } from '@/config/constants';

// 공통 에러 처리
const handleResponse = async (response, endpoint, options = {}) => {
  console.log(`API Response for ${endpoint}:`, {
    status: response.status,
    statusText: response.statusText,
    headers: Object.fromEntries([...response.headers]),
  });
  
  if (!response.ok) {
    // 인증 관련 오류에 대한 처리 (skipAuthError 옵션 확인)
    if ((response.status === 403 || response.status === 401) && 
        !endpoint.startsWith('/auth/')) {
      
      // skipAuthError 옵션이 설정된 경우 인증 오류를 무시
      if (options.skipAuthError) {
        console.warn(`Ignoring auth error for endpoint ${endpoint} due to skipAuthError option`);
        return { status: 'ignored_auth_error' };
      }
      
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
      errorData = JSON.parse(text);
      console.error(`Error response for ${endpoint}:`, errorData);
    } catch (e) {
      console.error('Failed to parse error response as JSON:', e);
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
    const text = await response.text();
    console.log(`Raw response text for ${endpoint}:`, text);
    
    if (!text) {
      console.warn(`Empty response body for ${endpoint}`);
      return null;
    }
    
    const data = JSON.parse(text);
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
    CHECK_ORDERS: '/trading/check/price',
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
    
    console.log(`Making POST request to: ${url} (endpoint: ${endpoint})`);
    console.log('Request headers:', headers);
    console.log('Request data:', data);
    
    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(data)
      });

      console.log(`Raw response status: ${response.status} ${response.statusText}`);
      console.log(`Response headers:`, Object.fromEntries([...response.headers]));
      
      // Clone the response to log the raw text without consuming the original
      const responseClone = response.clone();
      const rawText = await responseClone.text();
      console.log(`Raw response text (${rawText.length} bytes):`, rawText);
      
      // Check if the response is empty or non-JSON
      if (!rawText || rawText.trim() === '') {
        console.error('Empty response received from server');
        return { status: 'error', message: '서버로부터 빈 응답이 수신되었습니다' };
      }
      
      // Try to parse manually to check JSON validity before passing to handleResponse
      try {
        JSON.parse(rawText);
        console.log('Response is valid JSON');
      } catch (jsonError) {
        console.error('Response is not valid JSON:', jsonError);
        return { status: 'error', message: '서버 응답이 유효한 JSON 형식이 아닙니다' };
      }
      
      return handleResponse(response, endpoint, options);
    } catch (networkError) {
      console.error(`Network error for ${endpoint}:`, networkError);
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
    
    console.log(`Making PUT request to: ${url}`);
    console.log('Request headers:', headers);
    
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
    
    console.log(`Making DELETE request to: ${url}`);
    console.log('Request headers:', headers);
    
    const response = await fetch(url, {
      method: 'DELETE',
      headers,
    });

    return handleResponse(response, endpoint, options);
  },
}; 