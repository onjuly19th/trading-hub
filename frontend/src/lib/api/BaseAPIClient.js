import { API_CONFIG } from '@/config/constants';

export class BaseAPIClient {
  constructor({ getToken }) {
    this.baseURL = API_CONFIG.BASE_URL;
    this.debug = process.env.NODE_ENV === 'development';
    this.getToken = getToken;
  }

  // 로깅 메서드
  log(...args) {
    if (this.debug) {
      console.log('[API]', ...args);
    }
  }

  error(...args) {
    if (this.debug) {
      console.error('[API Error]', ...args);
    }
  }

  warn(...args) {
    if (this.debug) {
      console.warn('[API Warning]', ...args);
    }
  }

  // 헤더 생성
  getHeaders(customHeaders = {}) {
    const headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      ...customHeaders
    };

    const token = this.getToken?.();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
  }

  // 에러 처리
  handleError(error) {
    if (error.response) {
      // 서버가 응답했지만 에러 상태 코드를 반환한 경우
      const { status, data } = error.response;
      const errorMessage = data?.message || '알 수 없는 오류가 발생했습니다.';
      
      // 로그 레벨 조정
      if (status >= 500) {
        this.error(`${status} - ${error.request?.url}: ${errorMessage}`, data);
      } else {
        this.warn(`${status} - ${error.request?.url}: ${errorMessage}`);
      }
      
      throw {
          status,
          code: data?.code || 'AUTH_ERROR',
        message: errorMessage,
        data
      };
    } else if (error.request) {
      // 요청은 성공했지만 응답을 받지 못한 경우
      this.error('No response received:', error.request);
      throw {
        status: 0,
        code: 'NETWORK_ERROR',
        message: '서버로부터 응답이 없습니다. 네트워크 연결을 확인해주세요.',
        data: error.request
      };
    } else {
      // 요청 설정 중 오류가 발생한 경우
      this.error('Error during request setup:', error.message);
      throw {
        status: 0,
        code: 'REQUEST_ERROR',
        message: '요청 처리 중 오류가 발생했습니다.',
        data: error
      };
    }
  }

  // 응답 처리
  async handleResponse(response, endpoint, options = {}) {
    if (!response.ok) {
      // 새로운 에러 객체 생성 및 handleError로 전달
      const error = new Error('API request failed');
      error.response = response;
      error.request = { url: endpoint };
      return this.handleError(error);
    }
    
    const text = await response.text();
    if (!text) {
      return null;
    }
    
    try {
      return JSON.parse(text);
    } catch (e) {
      const error = new Error('응답 데이터 처리 중 오류가 발생했습니다.');
      error.response = response;
      error.request = { url: endpoint };
      error.data = text;
      return this.handleError(error);
    }
  }

  // HTTP 메서드 구현
  async request(method, endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const config = {
      method,
      headers: this.getHeaders(options.headers),
      ...options
    };

    this.log(`${method} ${endpoint}`);
    
    try {
      const response = await fetch(url, config);
      return this.handleResponse(response, endpoint, options);
    } catch (error) {
      this.handleError(error);
    }
  }

  async get(endpoint, params, options = {}) {
    const queryString = params ? `?${new URLSearchParams(params)}` : '';
    return this.request('GET', `${endpoint}${queryString}`, options);
  }

  async post(endpoint, data, options = {}) {
    return this.request('POST', endpoint, {
      ...options,
      body: JSON.stringify(data)
    });
  }

  async put(endpoint, data, options = {}) {
    return this.request('PUT', endpoint, {
      ...options,
      body: JSON.stringify(data)
    });
  }

  async delete(endpoint, options = {}) {
    return this.request('DELETE', endpoint, options);
  }
}
