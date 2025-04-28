import { BaseAPIClient } from './BaseAPIClient';
import { ENDPOINTS } from '@/config/constants';
import { TokenManager } from '@/lib/auth/TokenManager';

export class AuthAPIClient extends BaseAPIClient {
  static instance = null;

  static getInstance() {
    if (!AuthAPIClient.instance) {
      AuthAPIClient.instance = new AuthAPIClient();
    }
    return AuthAPIClient.instance;
  }

  constructor() {
    super();
    this.tokenManager = TokenManager.getInstance();
  }

  // 로그인
  async login(credentials) {
    const response = await this.post(ENDPOINTS.AUTH.LOGIN, credentials);
    
    if (response && response.token) {
      this.tokenManager.setToken(response.token);
      this.tokenManager.setUsername(credentials.username);
      return response;
    }
    
    throw new Error('로그인은 성공했으나 유효한 토큰을 받지 못했습니다.');
  }

  // 회원가입
  async signup(userData) {
    const signupData = await this.post(ENDPOINTS.AUTH.SIGNUP, userData);
    
    try {
      const loginResponse = await this.login({
        username: userData.username,
        password: userData.password
      });
      
      return { 
        ...signupData, 
        token: loginResponse.token,
        autoLoginSuccess: true 
      };
    } catch (loginError) {
      this.error('Auto login failed after signup:', loginError);
      return { 
        ...signupData, 
        autoLoginSuccess: false
      };
    }
  }

  // 로그아웃
  logout() {
    this.tokenManager.removeToken();
    this.tokenManager.removeUsername();
  }

  // 인증 상태 확인
  isAuthenticated() {
    return this.tokenManager.isAuthenticated();
  }
} 