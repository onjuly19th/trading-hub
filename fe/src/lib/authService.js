import { api, ENDPOINTS } from './api';

export const authService = {
  // 토큰 관리
  getToken: () => {
    const token = localStorage.getItem('token');
    //console.log('Getting token:', token);
    return token;
  },
  setToken: (token) => {
    console.log('Setting token:', token);
    localStorage.setItem('token', token);
  },
  removeToken: () => localStorage.removeItem('token'),

  // 사용자 정보 관리
  getUsername: () => {
    const username = localStorage.getItem('username');
    //console.log('Getting username:', username);
    return username;
  },
  setUsername: (username) => {
    console.log('Setting username:', username);
    localStorage.setItem('username', username);
  },
  removeUsername: () => localStorage.removeItem('username'),

  // 로그인 상태 확인
  checkAuth: () => {
    const token = authService.getToken();
    const username = authService.getUsername();
    return !!(token && username); // 토큰과 사용자명이 모두 있으면 true, 아니면 false
  },

  // 인증 상태 확인 (checkAuth와 동일 - 더 직관적인 이름)
  isAuthenticated: () => {
    return authService.checkAuth();
  },

  // 회원가입
  signup: async (credentials) => {
    const data = await api.post(ENDPOINTS.AUTH.SIGNUP, credentials);
    console.log('Signup response:', data);
    if (data.token) {
      authService.setToken(data.token);
      authService.setUsername(credentials.username);
    }
    return data;
  },

  // 로그인
  login: async (credentials) => {
    const data = await api.post(ENDPOINTS.AUTH.LOGIN, credentials);
    console.log('Login response:', data);
    if (data.token) {
      authService.setToken(data.token);
      authService.setUsername(credentials.username);
    } else {
      console.error('Login response missing token:', data);
    }
    return data;
  },

  // 로그아웃
  logout: () => {
    authService.removeToken();
    authService.removeUsername();
  }
}; 